package org.openkeyboard.engine

import org.openkeyboard.model.KeymapInfo
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.StringReader

class ParsedKeymap(
    val info: KeymapInfo,
    val mappings: Map<String, String>,
    val singleCharBadges: Map<String, String>,
    val layerMappings: Map<String, Map<String, String>> = emptyMap(),
    val layerBadges: Map<String, Map<String, String>> = emptyMap()
)

object OpenXmlParser {

    fun parse(xmlContent: String, filename: String = "custom.xml", isBuiltIn: Boolean = false): ParsedKeymap {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xmlContent))
        return parseInternal(parser, filename, isBuiltIn)
    }

    fun parse(inputStream: InputStream, filename: String = "custom.xml", isBuiltIn: Boolean = false): ParsedKeymap {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")
        return parseInternal(parser, filename, isBuiltIn)
    }

    private fun parseInternal(parser: XmlPullParser, filename: String, isBuiltIn: Boolean): ParsedKeymap {
        var language = "Universal"
        var encoding = "Unicode"
        var keyboard = "Custom"
        var author = "Open Keyboard Community"
        var description = ""
        var shiftCycleList: MutableList<String> = mutableListOf("Tamil", "CapsEnglish", "SmallEnglish")
        var smallShiftSymbol = "★"

        val mappings = HashMap<String, String>()
        val singleCharBadges = HashMap<String, String>()
        val layerMappings = HashMap<String, HashMap<String, String>>()
        val layerBadges = HashMap<String, HashMap<String, String>>()

        var currentTag = ""
        var currentCharacterHex = ""
        var currentDisplayHex = ""
        var currentKeysHex = ""
        var currentLayer = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag.equals("Map", ignoreCase = true)) {
                        currentLayer = parser.getAttributeValue(null, "Layer")
                            ?: parser.getAttributeValue(null, "layer")
                            ?: ""
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim() ?: ""
                    if (text.isNotEmpty()) {
                        when (currentTag) {
                            "Language" -> language = text
                            "Encoding" -> encoding = text
                            "Keyboard" -> keyboard = text
                            "Author" -> author = text
                            "Description" -> description = text
                            "ShiftCycle" -> {
                                val parts = text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                if (parts.isNotEmpty()) {
                                    shiftCycleList = parts.toMutableList()
                                }
                            }
                            "Character" -> currentCharacterHex = text
                            "Display" -> currentDisplayHex = text
                            "Keys" -> currentKeysHex = text
                            "Layer" -> currentLayer = text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("Map", ignoreCase = true)) {
                        val targetHex = if (currentCharacterHex.isNotEmpty()) currentCharacterHex else currentDisplayHex
                        if (currentKeysHex.isNotEmpty() && targetHex.isNotEmpty()) {
                            try {
                                val keyStr = hexToKeyString(currentKeysHex)
                                val charStr = hexToCharString(targetHex)
                                val displayStr = if (currentDisplayHex.isNotEmpty()) hexToCharString(currentDisplayHex) else charStr

                                val effectiveLayer = if (currentLayer.isNotEmpty()) currentLayer else language

                                val mapForLayer = layerMappings.getOrPut(effectiveLayer) { HashMap() }
                                mapForLayer[keyStr] = charStr

                                if (keyStr.length == 1) {
                                    val badgesForLayer = layerBadges.getOrPut(effectiveLayer) { HashMap() }
                                    if (!badgesForLayer.containsKey(keyStr)) {
                                        badgesForLayer[keyStr] = displayStr
                                    }
                                }

                                if (effectiveLayer.equals(language, ignoreCase = true) ||
                                    effectiveLayer.equals("default", ignoreCase = true) ||
                                    effectiveLayer.equals("Tamil", ignoreCase = true)) {
                                    mappings[keyStr] = charStr
                                    if (keyStr.length == 1 && !singleCharBadges.containsKey(keyStr)) {
                                        singleCharBadges[keyStr] = displayStr
                                    }
                                }
                            } catch (_: Exception) {
                                // Skip malformed map entry
                            }
                        }
                        currentCharacterHex = ""
                        currentDisplayHex = ""
                        currentKeysHex = ""
                        currentLayer = ""
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        if (singleCharBadges.isEmpty() && mappings.isNotEmpty()) {
            for ((k, v) in mappings) {
                if (k.length == 1 && !singleCharBadges.containsKey(k)) {
                    singleCharBadges[k] = v
                }
            }
        }

        val totalRules = mappings.size + layerMappings.values.sumOf { it.size }
        val info = KeymapInfo(
            id = filename.removeSuffix(".xml"),
            filename = filename,
            language = language,
            encoding = encoding,
            keyboard = keyboard,
            author = author,
            description = description,
            isBuiltIn = isBuiltIn,
            ruleCount = totalRules,
            shiftCycle = shiftCycleList,
            smallShiftSymbol = smallShiftSymbol
        )

        return ParsedKeymap(info, mappings, singleCharBadges, layerMappings, layerBadges)
    }

    fun hexToKeyString(hex: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < hex.length) {
            val part = hex.substring(i, (i + 2).coerceAtMost(hex.length))
            sb.append(part.toInt(16).toChar())
            i += 2
        }
        return sb.toString()
    }

    fun hexToCharString(hex: String): String {
        val sb = StringBuilder()
        var i = 0
        val step = if (hex.length % 4 == 0) 4 else 2
        while (i < hex.length) {
            val part = hex.substring(i, (i + step).coerceAtMost(hex.length))
            val code = part.toInt(16)
            sb.append(code.toChar())
            i += step
        }
        return sb.toString()
    }
}
