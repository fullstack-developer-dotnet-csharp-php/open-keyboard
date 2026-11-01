package org.openkeyboard.storage

import android.content.Context
import android.net.Uri
import org.openkeyboard.engine.OpenKeyboardEngine
import org.openkeyboard.engine.OpenXmlParser
import org.openkeyboard.engine.ParsedKeymap
import org.openkeyboard.model.KeymapInfo
import java.io.File
import java.io.FileOutputStream

class KeymapRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("open_keyboard_prefs", Context.MODE_PRIVATE)
    private val customDir = File(context.filesDir, "custom_keymaps")

    init {
        if (!customDir.exists()) {
            customDir.mkdirs()
        }
        // Ensure English is set as default active keymap
        if (!prefs.getBoolean("migrated_default_english_v3", false)) {
            val current = getEnabledKeymapIds().toMutableSet()
            current.add("English")
            prefs.edit()
                .putString(PREF_ACTIVE_KEYMAP_ID, "English")
                .putStringSet(PREF_ENABLED_KEYMAP_IDS, current)
                .putBoolean("migrated_default_english_v3", true)
                .apply()
        }
    }

    companion object {
        private const val PREF_ACTIVE_KEYMAP_ID = "active_keymap_id"
        private const val PREF_ENABLED_KEYMAP_IDS = "enabled_keymap_ids"
        private const val PREF_ENABLED_KEYMAP_ORDER_LIST = "enabled_keymap_order_list"
        private const val PREF_SHOW_THIRD_TEXT = "pref_show_third_text"
        private const val PREF_GLOBAL_SUGGESTIONS = "pref_global_suggestions"
        private const val PREF_PREFIX_SUGGESTIONS_KEYMAP = "pref_suggestions_keymap_"
        private const val DEFAULT_KEYMAP_ID = "English"

        @Volatile
        private var instance: KeymapRepository? = null

        fun getInstance(context: Context): KeymapRepository {
            return instance ?: synchronized(this) {
                instance ?: KeymapRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    fun getActiveKeymapId(): String {
        val active = prefs.getString(PREF_ACTIVE_KEYMAP_ID, DEFAULT_KEYMAP_ID) ?: DEFAULT_KEYMAP_ID
        val enabled = getEnabledOrderedList()
        return if (enabled.contains(active)) active else enabled.firstOrNull() ?: DEFAULT_KEYMAP_ID
    }

    fun setActiveKeymapId(id: String) {
        prefs.edit().putString(PREF_ACTIVE_KEYMAP_ID, id).apply()
        val current = getEnabledOrderedList().toMutableList()
        if (!current.contains(id)) {
            current.add(id)
            saveEnabledOrderedList(current)
        }
    }

    fun getEnabledOrderedList(): List<String> {
        val raw = prefs.getString(PREF_ENABLED_KEYMAP_ORDER_LIST, null)
        if (!raw.isNullOrBlank()) {
            val list = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
            if (!list.contains("English")) {
                list.add(0, "English")
                saveEnabledOrderedList(list)
            }
            return list
        }
        val defaults = listOf("English", "Tamil26ADN", "Tamil 26")
        saveEnabledOrderedList(defaults)
        return defaults
    }

    private fun saveEnabledOrderedList(list: List<String>) {
        val raw = list.joinToString(",")
        prefs.edit()
            .putString(PREF_ENABLED_KEYMAP_ORDER_LIST, raw)
            .putStringSet(PREF_ENABLED_KEYMAP_IDS, list.toSet())
            .apply()
    }

    fun getEnabledKeymapIds(): Set<String> {
        return getEnabledOrderedList().toSet()
    }

    fun isKeymapEnabled(id: String): Boolean {
        return getEnabledOrderedList().contains(id)
    }

    fun setKeymapEnabled(id: String, enabled: Boolean): Boolean {
        val current = getEnabledOrderedList().toMutableList()
        if (enabled) {
            if (!current.contains(id)) {
                // The order of Keyboard is strictly the order of how it is enabled!
                current.add(id)
                saveEnabledOrderedList(current)
            }
            return true
        } else {
            if (current.size <= 1 && current.contains(id)) {
                return false
            }
            current.remove(id)
            saveEnabledOrderedList(current)

            if (prefs.getString(PREF_ACTIVE_KEYMAP_ID, DEFAULT_KEYMAP_ID) == id && current.isNotEmpty()) {
                prefs.edit().putString(PREF_ACTIVE_KEYMAP_ID, current.first()).apply()
            }
            return true
        }
    }

    fun getEnabledKeymaps(): List<KeymapInfo> {
        val orderedIds = getEnabledOrderedList()
        val allMap = listAllKeymaps().associateBy { it.id }
        val result = orderedIds.mapNotNull { allMap[it] }
        return if (result.isNotEmpty()) result else listAllKeymaps().take(1)
    }

    fun enableAllKeymaps() {
        val allIds = listAllKeymaps().map { it.id }
        val current = getEnabledOrderedList().toMutableList()
        for (id in allIds) {
            if (!current.contains(id)) {
                current.add(id)
            }
        }
        saveEnabledOrderedList(current)
    }

    fun resetToDefaultKeymaps() {
        val defaults = listOf("English", "Tamil26ADN", "Tamil 26")
        saveEnabledOrderedList(defaults)
        prefs.edit().putString(PREF_ACTIVE_KEYMAP_ID, "English").apply()
    }

    fun listAllKeymaps(): List<KeymapInfo> {
        val list = ArrayList<KeymapInfo>()

        // 1. Built-in keymaps from assets
        try {
            val assetFiles = context.assets.list("keymaps") ?: emptyArray()
            for (filename in assetFiles) {
                if (filename.endsWith(".xml") && !filename.startsWith("._")) {
                    try {
                        context.assets.open("keymaps/$filename").use { stream ->
                            val parsed = OpenXmlParser.parse(stream, filename, isBuiltIn = true)
                            list.add(parsed.info)
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}

        // 2. User uploaded custom keymaps from customDir
        val customFiles = customDir.listFiles { _, name -> name.endsWith(".xml") } ?: emptyArray()
        for (file in customFiles) {
            try {
                file.inputStream().use { stream ->
                    val parsed = OpenXmlParser.parse(stream, file.name, isBuiltIn = false)
                    list.add(parsed.info)
                }
            } catch (_: Exception) {}
        }

        // Sort priority: Tamil26ADN first (SIVA), then Tamil 26, then rest
        return list.sortedWith(compareBy(
            { it.id != "Tamil26ADN" },
            { it.id != "Tamil 26" },
            { !it.id.contains("Hindi") },
            { !it.id.contains("Telugu") },
            { !it.id.contains("French") },
            { !it.id.contains("Unicode_Phonetic") },
            { it.displayName }
        ))
    }

    fun importXmlFromUri(uri: Uri, originalFilename: String): Result<KeymapInfo> {
        return try {
            val safeName = if (originalFilename.endsWith(".xml", ignoreCase = true)) {
                originalFilename
            } else {
                "$originalFilename.xml"
            }
            val targetFile = File(customDir, safeName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(Exception("Cannot open file stream"))

            // Validate XML
            val parsed = targetFile.inputStream().use {
                OpenXmlParser.parse(it, safeName, isBuiltIn = false)
            }

            // Automatically enable imported custom keymap and set as active
            setKeymapEnabled(parsed.info.id, true)
            setActiveKeymapId(parsed.info.id)

            Result.success(parsed.info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun loadActiveKeymap(): ParsedKeymap {
        val activeId = getActiveKeymapId()

        // Check custom dir first
        val customFile = File(customDir, "$activeId.xml")
        if (customFile.exists()) {
            try {
                return customFile.inputStream().use {
                    OpenXmlParser.parse(it, customFile.name, isBuiltIn = false)
                }
            } catch (_: Exception) {}
        }

        // Check assets
        try {
            val assetPath = "keymaps/$activeId.xml"
            context.assets.open(assetPath).use { stream ->
                return OpenXmlParser.parse(stream, "$activeId.xml", isBuiltIn = true)
            }
        } catch (_: Exception) {}

        // Fallback to Tamil26ADN
        return try {
            context.assets.open("keymaps/Tamil26ADN.xml").use { stream ->
                OpenXmlParser.parse(stream, "Tamil26ADN.xml", isBuiltIn = true)
            }
        } catch (_: Exception) {
            OpenXmlParser.parse("<Table><Header><Language>Universal</Language></Header><Data></Data></Table>", "fallback.xml")
        }
    }

    fun createActiveEngine(): OpenKeyboardEngine {
        return OpenKeyboardEngine(loadActiveKeymap())
    }

    // --- 3rd Text & Suggestions Preferences ---

    fun isThirdTextEnabled(): Boolean {
        return prefs.getBoolean(PREF_SHOW_THIRD_TEXT, true)
    }

    fun setThirdTextEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_SHOW_THIRD_TEXT, enabled).apply()
    }

    fun isGlobalSuggestionsEnabled(): Boolean {
        return prefs.getBoolean(PREF_GLOBAL_SUGGESTIONS, true)
    }

    fun setGlobalSuggestionsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_GLOBAL_SUGGESTIONS, enabled).apply()
    }

    /**
     * Checks if suggestions are enabled for a specific keymap.
     * Sanskrit / Sanscrit keymaps avoid suggestions by default unless explicitly enabled.
     */
    fun isSuggestionsEnabled(keymapId: String, language: String = ""): Boolean {
        if (!isGlobalSuggestionsEnabled()) return false

        // Check explicit user preference for this keymap
        val prefKey = PREF_PREFIX_SUGGESTIONS_KEYMAP + keymapId
        if (prefs.contains(prefKey)) {
            return prefs.getBoolean(prefKey, true)
        }

        // Default behavior: Sanskrit avoids suggestions; English and Tamil have suggestions enabled
        val idLower = keymapId.lowercase()
        val langLower = language.lowercase()
        if (idLower.contains("sans") || idLower.contains("sanskrit") ||
            langLower.contains("sans") || langLower.contains("sanskrit")) {
            return false // Avoid text suggestions for Sanskrit by default
        }

        return true
    }

    fun setSuggestionsEnabled(keymapId: String, enabled: Boolean) {
        val prefKey = PREF_PREFIX_SUGGESTIONS_KEYMAP + keymapId
        prefs.edit().putBoolean(prefKey, enabled).apply()
    }
}
