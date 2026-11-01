package org.openkeyboard.model

data class KeymapInfo(
    val id: String,
    val filename: String,
    val language: String,
    val encoding: String,
    val keyboard: String,
    val author: String,
    val description: String,
    val isBuiltIn: Boolean,
    val ruleCount: Int,
    val shiftCycle: List<String> = listOf("Base", "CapsEnglish", "SmallEnglish"),
    val smallShiftSymbol: String = "★"
) {
    val displayName: String
        get() = "[$language] $keyboard ($encoding) - $author"
}

data class KeyMapping(
    val keySequence: String,
    val targetCharacters: String,
    val displayCharacters: String? = null,
    val layer: String = "Base"
)

data class EngineResult(
    val deleteCount: Int,
    val insertText: String,
    val currentBuffer: String
)
