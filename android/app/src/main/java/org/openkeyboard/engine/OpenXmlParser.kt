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
    val layerBadges: Map<String, Map<String, String>> = emptyMap(),
    val longPressMappings: Map<String, String> = emptyMap(),
    val longPressBadges: Map<String, String> = emptyMap()
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
        var shiftCycleList: MutableList<String> = mutableListOf("Base", "CapsEnglish", "SmallEnglish")
        var smallShiftSymbol = "★"

        val mappings = HashMap<String, String>()
        val singleCharBadges = HashMap<String, String>()
        val layerMappings = HashMap<String, HashMap<String, String>>()
        val layerBadges = HashMap<String, HashMap<String, String>>()
        val longPressMappings = HashMap<String, String>()
        val longPressBadges = HashMap<String, String>()

        var currentTag = ""
        var currentCharacterHex = ""
        var currentDisplayHex = ""
        var currentKeysHex = ""
        var currentLayer = ""
        var currentThirdKeyHex = ""
        var currentThirdDisplayHex = ""
        var currentThirdKeyAttr = ""
        var currentThirdDisplayAttr = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag.equals("Map", ignoreCase = true) ||
                        currentTag.equals("ThirdKey", ignoreCase = true) ||
                        currentTag.equals("LongPress", ignoreCase = true)) {
                        currentLayer = parser.getAttributeValue(null, "Layer")
                            ?: parser.getAttributeValue(null, "layer")
                            ?: if (currentTag.equals("ThirdKey", ignoreCase = true) || currentTag.equals("LongPress", ignoreCase = true)) "ThirdKey" else ""
                        currentThirdKeyAttr = parser.getAttributeValue(null, "ThirdKey")
                            ?: parser.getAttributeValue(null, "thirdKey")
                            ?: parser.getAttributeValue(null, "LongPress")
                            ?: parser.getAttributeValue(null, "longPress")
                            ?: ""
                        currentThirdDisplayAttr = parser.getAttributeValue(null, "ThirdDisplay")
                            ?: parser.getAttributeValue(null, "thirdDisplay")
                            ?: parser.getAttributeValue(null, "LongPressDisplay")
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
                            "ThirdKey", "LongPress", "Key3" -> currentThirdKeyHex = text
                            "ThirdDisplay", "LongPressDisplay" -> currentThirdDisplayHex = text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("Map", ignoreCase = true) ||
                        parser.name.equals("ThirdKey", ignoreCase = true) ||
                        parser.name.equals("LongPress", ignoreCase = true)) {
                        val targetHex = if (currentCharacterHex.isNotEmpty()) currentCharacterHex else currentDisplayHex
                        if (currentKeysHex.isNotEmpty() && targetHex.isNotEmpty()) {
                            try {
                                val keyStr = hexToKeyString(currentKeysHex)
                                val charStr = parseDisplayOrChar(targetHex)
                                val displayStr = if (currentDisplayHex.isNotEmpty()) parseDisplayOrChar(currentDisplayHex) else charStr

                                val effectiveLayer = if (currentLayer.isNotEmpty()) currentLayer else language
                                val isThirdKeyLayer = effectiveLayer.equals("LongPress", ignoreCase = true) ||
                                                      effectiveLayer.equals("ThirdKey", ignoreCase = true) ||
                                                      effectiveLayer.equals("3rdKey", ignoreCase = true) ||
                                                      effectiveLayer.equals("Key3", ignoreCase = true)

                                if (isThirdKeyLayer) {
                                    val lower = keyStr.lowercase()
                                    val upper = keyStr.uppercase()
                                    longPressMappings[lower] = charStr
                                    longPressMappings[upper] = charStr
                                    longPressBadges[lower] = displayStr
                                    longPressBadges[upper] = displayStr
                                } else {
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
                                        effectiveLayer.equals("base", ignoreCase = true) ||
                                        effectiveLayer.isEmpty()) {
                                        mappings[keyStr] = charStr
                                        if (keyStr.length == 1) {
                                            if (!singleCharBadges.containsKey(keyStr)) {
                                                singleCharBadges[keyStr] = displayStr
                                            }
                                            // Auto-detect desktop Shift keys from XML:
                                            // When keyStr is uppercase ASCII ('A'..'Z') and charStr is non-ASCII script character
                                            if (keyStr[0] in 'A'..'Z' && charStr.isNotEmpty() && charStr[0].code > 127) {
                                                val lowerKey = keyStr.lowercase()
                                                if (!longPressMappings.containsKey(lowerKey)) {
                                                    longPressMappings[lowerKey] = charStr
                                                    longPressBadges[lowerKey] = displayStr
                                                }
                                            }
                                        }
                                    }
                                }

                                // Check if <ThirdKey> or ThirdKey attribute was specified inside this Map
                                val thirdHex = if (currentThirdKeyHex.isNotEmpty()) currentThirdKeyHex else currentThirdKeyAttr
                                if (thirdHex.isNotEmpty()) {
                                    val thirdChar = parseDisplayOrChar(thirdHex)
                                    val thirdDisp = if (currentThirdDisplayHex.isNotEmpty()) parseDisplayOrChar(currentThirdDisplayHex)
                                                    else if (currentThirdDisplayAttr.isNotEmpty()) parseDisplayOrChar(currentThirdDisplayAttr)
                                                    else thirdChar
                                    val lower = keyStr.lowercase()
                                    val upper = keyStr.uppercase()
                                    longPressMappings[lower] = thirdChar
                                    longPressMappings[upper] = thirdChar
                                    longPressBadges[lower] = thirdDisp
                                    longPressBadges[upper] = thirdDisp
                                }
                            } catch (_: Exception) {
                                // Skip malformed map entry
                            }
                        }
                        currentCharacterHex = ""
                        currentDisplayHex = ""
                        currentKeysHex = ""
                        currentLayer = ""
                        currentThirdKeyHex = ""
                        currentThirdDisplayHex = ""
                        currentThirdKeyAttr = ""
                        currentThirdDisplayAttr = ""
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

        // Auto-detect modifier-based Sanskrit 3rd keys (e.g. yg -> ஜ், tg -> ஸ், rg -> ஷ், eg -> ஹ், wg -> க்ஷ், qg -> ஸ்ரீ, ug -> ஶ், mg -> ஃ)
        for ((k, v) in mappings) {
            if (k.length == 2 && k[1] == 'g' && k[0] in 'a'..'z') {
                val lowerKey = k[0].toString()
                if (!longPressMappings.containsKey(lowerKey)) {
                    longPressMappings[lowerKey] = v
                    val badge = singleCharBadges[k] ?: v
                    longPressBadges[lowerKey] = badge
                }
            }
        }

        val totalRules = if (layerMappings.isNotEmpty()) layerMappings.values.sumOf { it.size } else mappings.size
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

        return ParsedKeymap(info, mappings, singleCharBadges, layerMappings, layerBadges, longPressMappings, longPressBadges)
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

    fun parseDisplayOrChar(hexOrLiteral: String): String {
        val trimmed = hexOrLiteral.trim()
        if (trimmed.length >= 2 && trimmed.length % 2 == 0 && trimmed.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
            return try {
                hexToCharString(trimmed)
            } catch (_: Exception) {
                trimmed
            }
        }
        return trimmed
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
