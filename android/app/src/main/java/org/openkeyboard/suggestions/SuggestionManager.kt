package org.openkeyboard.suggestions

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

class SuggestionManager(private val context: Context) {

    private val englishWords = mutableListOf<String>()
    private val tamilWords = mutableListOf<String>()
    private var isLoaded = false

    init {
        loadDictionaries()
    }

    private fun loadDictionaries() {
        try {
            // Load English words from assets
            context.assets.open("dictionaries/english_words.txt").use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).useLines { lines ->
                    englishWords.addAll(lines.filter { it.isNotBlank() })
                }
            }
        } catch (_: Exception) {
            // Fallback essential English list
            englishWords.addAll(listOf(
                "the", "be", "to", "of", "and", "a", "in", "that", "have", "i", "it", "for", "not", "on", "with",
                "he", "as", "you", "do", "at", "this", "but", "his", "by", "from", "they", "we", "say", "her",
                "she", "or", "an", "will", "my", "one", "all", "would", "there", "their", "what", "so", "up",
                "out", "if", "about", "who", "get", "which", "go", "me", "when", "make", "can", "like", "time",
                "keyboard", "typing", "android", "device", "mobile", "message", "please", "thank", "you", "welcome"
            ))
        }

        try {
            // Load Tamil words from assets
            context.assets.open("dictionaries/tamil_words.txt").use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).useLines { lines ->
                    tamilWords.addAll(lines.filter { it.isNotBlank() })
                }
            }
        } catch (_: Exception) {
            // Fallback essential Tamil list
            tamilWords.addAll(listOf(
                "வணக்கம்", "தமிழ்", "நன்றி", "இந்த", "ஒரு", "நான்", "நீ", "அவர்", "அவர்கள்", "நாம்", "நீங்கள்",
                "வேண்டும்", "இல்லை", "இருக்கிறது", "உள்ளது", "செய்ய", "சொல்லுங்கள்", "வாருங்கள்", "போங்கள்",
                "நல்ல", "பெரிய", "முக்கியமான", "நேரம்", "இடம்", "நாடு", "ஊர்", "மனிதன்", "மக்கள்", "அம்மா", "அப்பா"
            ))
        }

        englishWords.sort()
        tamilWords.sort()
        isLoaded = true
    }

    /**
     * Look up word predictions for the given prefix and active language.
     * If the language is Sanskrit, returns emptyList() to avoid suggestions.
     */
    fun getSuggestions(
        prefix: String,
        language: String,
        isEnglishLayout: Boolean,
        avoidSuggestions: Boolean = false
    ): List<String> {
        if (avoidSuggestions) return emptyList()

        val cleanPrefix = prefix.trim()
        if (cleanPrefix.isEmpty()) return emptyList()

        // Explicit rule: avoid suggestions for Sanskrit or Devanagari languages
        val langLower = language.lowercase()
        if (langLower.contains("sans") || langLower.contains("sanskrit") || langLower.contains("devanagari")) {
            return emptyList()
        }

        val isEnglish = isEnglishLayout || langLower.contains("english")
        val isTamil = langLower.contains("tamil")

        val matches = when {
            isEnglish -> {
                val lower = cleanPrefix.lowercase()
                val isCapitalized = cleanPrefix.first().isUpperCase()
                val found = englishWords.filter { it.startsWith(lower) }.take(3)
                if (isCapitalized) {
                    found.map { it.replaceFirstChar { c -> c.uppercase() } }
                } else {
                    found
                }
            }
            isTamil -> {
                tamilWords.filter { it.startsWith(cleanPrefix) }.take(3)
            }
            else -> emptyList()
        }

        return when (matches.size) {
            0 -> {
                if (isEnglish) {
                    val cap = cleanPrefix.replaceFirstChar { it.uppercase() }
                    listOf(cleanPrefix, cap, "")
                } else {
                    listOf("", cleanPrefix, "")
                }
            }
            1 -> listOf("", matches[0], "")
            2 -> listOf(matches[0], matches[1], "")
            else -> listOf(matches[0], matches[1], matches[2])
        }
    }
}
