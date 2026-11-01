package org.openkeyboard.engine

import org.openkeyboard.model.EngineResult

class OpenKeyboardEngine(val parsedKeymap: ParsedKeymap) {

    private class TrieNode {
        val children = HashMap<Char, TrieNode>()
        var mappedValue: String? = null
    }

    private val rootDefault = TrieNode()
    private val layerRoots = HashMap<String, TrieNode>()
    private val buffer = StringBuilder()
    private var lastOutput: String = ""

    init {
        buildTries()
    }

    private fun buildTries() {
        // Build default/Tamil Trie
        for ((keySeq, target) in parsedKeymap.mappings) {
            var curr = rootDefault
            for (ch in keySeq) {
                curr = curr.children.getOrPut(ch) { TrieNode() }
            }
            curr.mappedValue = target
        }

        // Build Tries for specific layers (e.g. CapsEnglish, SmallEnglish, Tamil)
        for ((layerName, map) in parsedKeymap.layerMappings) {
            val root = layerRoots.getOrPut(layerName) { TrieNode() }
            for ((keySeq, target) in map) {
                var curr = root
                for (ch in keySeq) {
                    curr = curr.children.getOrPut(ch) { TrieNode() }
                }
                curr.mappedValue = target
            }
        }
    }

    private fun getRootForLayer(layer: String): TrieNode {
        return layerRoots[layer] ?: rootDefault
    }

    @Synchronized
    fun handleKeyPress(char: Char, layer: String = "Tamil"): EngineResult {
        // Handle English bypass if no custom Trie rules exist
        if (layer.equals("CapsEnglish", ignoreCase = true) && !layerRoots.containsKey("CapsEnglish")) {
            reset()
            return EngineResult(
                deleteCount = 0,
                insertText = char.uppercaseChar().toString(),
                currentBuffer = ""
            )
        }
        if (layer.equals("SmallEnglish", ignoreCase = true) && !layerRoots.containsKey("SmallEnglish")) {
            reset()
            return EngineResult(
                deleteCount = 0,
                insertText = char.lowercaseChar().toString(),
                currentBuffer = ""
            )
        }

        val root = getRootForLayer(layer)
        val candidate = buffer.toString() + char
        if (hasPrefix(root, candidate)) {
            buffer.append(char)
            val match = getExactMatch(root, buffer.toString())
            if (match != null) {
                val deleteCount = lastOutput.length
                lastOutput = match
                return EngineResult(
                    deleteCount = deleteCount,
                    insertText = match,
                    currentBuffer = buffer.toString()
                )
            } else {
                return EngineResult(
                    deleteCount = 0,
                    insertText = "",
                    currentBuffer = buffer.toString()
                )
            }
        } else {
            buffer.clear()
            lastOutput = ""

            val charStr = char.toString()
            if (hasPrefix(root, charStr)) {
                buffer.append(char)
                val match = getExactMatch(root, charStr)
                if (match != null) {
                    lastOutput = match
                    return EngineResult(
                        deleteCount = 0,
                        insertText = match,
                        currentBuffer = buffer.toString()
                    )
                } else {
                    return EngineResult(
                        deleteCount = 0,
                        insertText = "",
                        currentBuffer = buffer.toString()
                    )
                }
            } else {
                // If not in root Trie, check if Caps or Small
                val fallback = when {
                    layer.equals("CapsEnglish", ignoreCase = true) -> char.uppercaseChar().toString()
                    layer.equals("SmallEnglish", ignoreCase = true) -> char.lowercaseChar().toString()
                    else -> charStr
                }
                return EngineResult(
                    deleteCount = 0,
                    insertText = fallback,
                    currentBuffer = ""
                )
            }
        }
    }

    @Synchronized
    fun handleBackspace(layer: String = "Tamil"): EngineResult {
        if (layer.equals("CapsEnglish", ignoreCase = true) || layer.equals("SmallEnglish", ignoreCase = true)) {
            reset()
            return EngineResult(
                deleteCount = 1,
                insertText = "",
                currentBuffer = ""
            )
        }

        val root = getRootForLayer(layer)
        if (buffer.isNotEmpty()) {
            val deleteCount = lastOutput.length
            buffer.deleteCharAt(buffer.length - 1)
            if (buffer.isNotEmpty()) {
                val match = getExactMatch(root, buffer.toString())
                lastOutput = match ?: ""
                return EngineResult(
                    deleteCount = deleteCount,
                    insertText = lastOutput,
                    currentBuffer = buffer.toString()
                )
            } else {
                lastOutput = ""
                return EngineResult(
                    deleteCount = deleteCount,
                    insertText = "",
                    currentBuffer = ""
                )
            }
        }
        return EngineResult(
            deleteCount = 1,
            insertText = "",
            currentBuffer = ""
        )
    }

    @Synchronized
    fun reset() {
        buffer.clear()
        lastOutput = ""
    }

    private fun hasPrefix(root: TrieNode, prefix: String): Boolean {
        var curr = root
        for (ch in prefix) {
            val next = curr.children[ch] ?: return false
            curr = next
        }
        return true
    }

    private fun getExactMatch(root: TrieNode, seq: String): String? {
        var curr = root
        for (ch in seq) {
            val next = curr.children[ch] ?: return null
            curr = next
        }
        return curr.mappedValue
    }

    fun getBadgeForChar(key: Char, layer: String = "Tamil"): String? {
        val layerBadgeMap = parsedKeymap.layerBadges[layer]
        if (layerBadgeMap != null && layerBadgeMap.containsKey(key.toString())) {
            return layerBadgeMap[key.toString()]
        }
        return parsedKeymap.singleCharBadges[key.toString()]
    }
}
