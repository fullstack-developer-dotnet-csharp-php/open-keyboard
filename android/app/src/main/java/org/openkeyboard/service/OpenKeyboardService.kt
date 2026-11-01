package org.openkeyboard.service

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import org.openkeyboard.engine.OpenKeyboardEngine
import org.openkeyboard.storage.KeymapRepository
import org.openkeyboard.suggestions.SuggestionManager
import org.openkeyboard.ui.KeyboardView
import org.openkeyboard.ui.MainActivity

class OpenKeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private lateinit var repository: KeymapRepository
    private lateinit var suggestionManager: SuggestionManager
    private var engine: OpenKeyboardEngine? = null
    private var keyboardView: KeyboardView? = null

    override fun onCreate() {
        super.onCreate()
        repository = KeymapRepository.getInstance(this)
        suggestionManager = SuggestionManager(this)
    }

    override fun onCreateInputView(): View {
        val view = KeyboardView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            listener = this@OpenKeyboardService
        }
        keyboardView = view
        reloadEngine()
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        reloadEngine()
        engine?.reset()
        keyboardView?.currentBufferText = ""

        updateActionKey(info)
    }

    private fun updateActionKey(info: EditorInfo?) {
        val actionType = getActionType(info)
        keyboardView?.actionKeyType = actionType
    }

    private fun isSearchField(info: EditorInfo?): Boolean {
        if (info == null) return false
        val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
        if (action == EditorInfo.IME_ACTION_SEARCH) return true
        if (info.actionId == EditorInfo.IME_ACTION_SEARCH) return true
        val variation = info.inputType and InputType.TYPE_MASK_VARIATION
        if (variation == InputType.TYPE_TEXT_VARIATION_FILTER) return true
        if (variation == InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT && action == EditorInfo.IME_ACTION_SEARCH) return true
        if (info.actionLabel?.toString()?.contains("search", ignoreCase = true) == true) return true
        return false
    }

    private fun getActionType(info: EditorInfo?): KeyboardView.ActionKeyType {
        if (info == null) return KeyboardView.ActionKeyType.ENTER
        if (isSearchField(info)) return KeyboardView.ActionKeyType.SEARCH

        val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
        return when (action) {
            EditorInfo.IME_ACTION_GO -> KeyboardView.ActionKeyType.GO
            EditorInfo.IME_ACTION_SEND -> KeyboardView.ActionKeyType.SEND
            EditorInfo.IME_ACTION_NEXT -> KeyboardView.ActionKeyType.NEXT
            EditorInfo.IME_ACTION_DONE -> KeyboardView.ActionKeyType.DONE
            else -> KeyboardView.ActionKeyType.ENTER
        }
    }

    fun reloadEngine() {
        val activeParsed = repository.loadActiveKeymap()
        engine = OpenKeyboardEngine(activeParsed)
        keyboardView?.let { kv ->
            kv.engine = engine
            kv.layoutName = activeParsed.info.keyboard
            kv.currentBufferText = ""
            kv.resetShift()
            kv.showThirdText = repository.isThirdTextEnabled()
            kv.isSuggestionsEnabled = repository.isSuggestionsEnabled(activeParsed.info.id, activeParsed.info.language)
            updateKeyboardLayoutList()
        }
        currentWordPrefix.clear()
        keyboardView?.currentSuggestions = emptyList()
    }

    private fun updateKeyboardLayoutList() {
        val enabled = repository.getEnabledKeymaps()
        val currentId = repository.getActiveKeymapId()
        keyboardView?.enabledKeymaps = enabled.map {
            KeyboardView.DropdownItem(
                id = it.id,
                displayName = it.keyboard,
                isActive = (it.id == currentId)
            )
        }
    }

    private var currentWordPrefix = StringBuilder()

    private fun updateSuggestions() {
        val kv = keyboardView ?: return
        if (!kv.isSuggestionsEnabled) {
            kv.currentSuggestions = emptyList()
            return
        }

        val prefix = currentWordPrefix.toString().trim()
        if (prefix.isEmpty()) {
            kv.currentSuggestions = emptyList()
            return
        }

        val activeKeymap = repository.loadActiveKeymap()
        val avoid = !repository.isSuggestionsEnabled(activeKeymap.info.id, activeKeymap.info.language)
        if (avoid) {
            kv.currentSuggestions = emptyList()
            return
        }

        val results = suggestionManager.getSuggestions(
            prefix = prefix,
            language = activeKeymap.info.language,
            isEnglishLayout = kv.isEnglishLayout,
            avoidSuggestions = avoid
        )
        kv.currentSuggestions = results
    }

    override fun onSuggestion(suggestion: String) {
        val ic = currentInputConnection ?: return
        val prefixLen = currentWordPrefix.length
        if (prefixLen > 0) {
            ic.deleteSurroundingText(prefixLen, 0)
        }
        ic.commitText(suggestion + " ", 1)
        currentWordPrefix.clear()
        keyboardView?.currentSuggestions = emptyList()
    }

    override fun onToggleThirdText(enabled: Boolean) {
        repository.setThirdTextEnabled(enabled)
        keyboardView?.showThirdText = enabled
    }

    override fun onToggleSuggestions(enabled: Boolean) {
        val activeId = repository.getActiveKeymapId()
        repository.setSuggestionsEnabled(activeId, enabled)
        keyboardView?.isSuggestionsEnabled = enabled
        if (!enabled) {
            keyboardView?.currentSuggestions = emptyList()
        } else {
            updateSuggestions()
        }
    }

    override fun onKeyText(text: String) {
        val ic = currentInputConnection ?: return
        val eng = engine ?: return

        // Direct commit for emojis and emoji mode
        if (keyboardView?.isEmojiMode == true || text.any { Character.isSurrogate(it) }) {
            eng.reset()
            currentWordPrefix.clear()
            keyboardView?.currentSuggestions = emptyList()
            keyboardView?.currentBufferText = ""
            ic.commitText(text, 1)
            return
        }

        // Direct commit for English layout (instant, no syllable buffer)
        if (keyboardView?.isEnglishLayout == true) {
            eng.reset()
            keyboardView?.currentBufferText = ""
            ic.commitText(text, 1)
            if (text.all { it.isLetter() }) {
                currentWordPrefix.append(text)
                updateSuggestions()
            } else {
                currentWordPrefix.clear()
                keyboardView?.currentSuggestions = emptyList()
            }
            return
        }

        val currentLayer = when (keyboardView?.shiftMode) {
            KeyboardView.ShiftMode.CAPS_ENGLISH -> "CapsEnglish"
            KeyboardView.ShiftMode.SMALL_ENGLISH -> "SmallEnglish"
            else -> "Tamil"
        }

        for (ch in text) {
            val res = eng.handleKeyPress(ch, currentLayer)
            if (res.deleteCount > 0) {
                ic.deleteSurroundingText(res.deleteCount, 0)
                if (currentWordPrefix.length >= res.deleteCount) {
                    currentWordPrefix.delete(currentWordPrefix.length - res.deleteCount, currentWordPrefix.length)
                }
            }
            if (res.insertText.isNotEmpty()) {
                ic.commitText(res.insertText, 1)
                currentWordPrefix.append(res.insertText)
            }
            keyboardView?.currentBufferText = res.currentBuffer
        }
        updateSuggestions()
    }

    override fun onBackspace() {
        val ic = currentInputConnection ?: return
        val eng = engine ?: return

        if (currentWordPrefix.isNotEmpty()) {
            currentWordPrefix.deleteCharAt(currentWordPrefix.length - 1)
            updateSuggestions()
        } else {
            keyboardView?.currentSuggestions = emptyList()
        }

        val currentLayer = when (keyboardView?.shiftMode) {
            KeyboardView.ShiftMode.CAPS_ENGLISH -> "CapsEnglish"
            KeyboardView.ShiftMode.SMALL_ENGLISH -> "SmallEnglish"
            else -> "Tamil"
        }

        val res = eng.handleBackspace(currentLayer)
        if (res.deleteCount > 0) {
            ic.deleteSurroundingText(res.deleteCount, 0)
        }
        if (res.insertText.isNotEmpty()) {
            ic.commitText(res.insertText, 1)
        }
        keyboardView?.currentBufferText = res.currentBuffer
    }

    override fun onSpace() {
        val ic = currentInputConnection ?: return
        engine?.reset()
        currentWordPrefix.clear()
        keyboardView?.currentSuggestions = emptyList()
        keyboardView?.currentBufferText = ""
        ic.commitText(" ", 1)
    }

    override fun onEnter() {
        val ic = currentInputConnection ?: return
        engine?.reset()
        currentWordPrefix.clear()
        keyboardView?.currentSuggestions = emptyList()
        keyboardView?.currentBufferText = ""

        val info = currentInputEditorInfo
        if (isSearchField(info)) {
            ic.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
            return
        }

        val action = if (info != null) info.imeOptions and EditorInfo.IME_MASK_ACTION else EditorInfo.IME_ACTION_NONE
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action)
        } else {
            sendKeyChar('\n')
        }
    }

    override fun onSearch() {
        val ic = currentInputConnection ?: return
        engine?.reset()
        keyboardView?.currentBufferText = ""

        val info = currentInputEditorInfo
        val action = if (info != null) info.imeOptions and EditorInfo.IME_MASK_ACTION else EditorInfo.IME_ACTION_NONE

        if (action == EditorInfo.IME_ACTION_SEARCH || isSearchField(info)) {
            ic.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
        } else if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action)
        } else {
            ic.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
        }
    }

    override fun onSelectKeymap(keymapId: String) {
        repository.setActiveKeymapId(keymapId)
        reloadEngine()
    }

    override fun onOpenSettings() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    override fun onSwitchLayout() {
        val enabled = repository.getEnabledKeymaps()
        if (enabled.size > 1) {
            val currentId = repository.getActiveKeymapId()
            val currentIndex = enabled.indexOfFirst { it.id == currentId }
            val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % enabled.size else 0
            val next = enabled[nextIndex]
            repository.setActiveKeymapId(next.id)
            reloadEngine()
        } else {
            onOpenSettings()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.isPrintingKey && !event.isCtrlPressed && !event.isAltPressed && !event.isMetaPressed) {
            val unicodeChar = event.getUnicodeChar(event.metaState)
            if (unicodeChar > 0) {
                onKeyText(unicodeChar.toChar().toString())
                return true
            }
        } else if (keyCode == KeyEvent.KEYCODE_DEL) {
            onBackspace()
            return true
        } else if (keyCode == KeyEvent.KEYCODE_SPACE) {
            onSpace()
            return true
        } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
            onEnter()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
