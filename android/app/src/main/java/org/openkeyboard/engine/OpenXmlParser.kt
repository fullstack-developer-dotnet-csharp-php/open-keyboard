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
    val longPressMappings: Map<String, String> = emptyMap()
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
        val longPressMappings = HashMap<String, String>()

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

                                if (effectiveLayer.equals("LongPress", ignoreCase = true)) {
                                    longPressMappings[keyStr.lowercase()] = charStr
                                    longPressMappings[keyStr.uppercase()] = charStr
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
                                        effectiveLayer.equals("Tamil", ignoreCase = true)) {
                                        mappings[keyStr] = charStr
                                        if (keyStr.length == 1) {
                                            if (!singleCharBadges.containsKey(keyStr)) {
                                                singleCharBadges[keyStr] = displayStr
                                            }
                                            // Auto-detect Kaladi desktop Shift keys:
                                            // When keyStr is uppercase ASCII ('A'..'Z') and charStr is non-ASCII (Tamil/Sanskrit/numerals)
                                            if (keyStr[0] in 'A'..'Z' && charStr.isNotEmpty() && charStr[0].code > 127) {
                                                val lowerKey = keyStr.lowercase()
                                                if (!longPressMappings.containsKey(lowerKey)) {
                                                    longPressMappings[lowerKey] = charStr
                                                }
                                            }
                                        }
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

        if (longPressMappings.isEmpty() && language.equals("Tamil", ignoreCase = true)) {
            // Home Row Sanskrit (Kaladi original standard)
            longPressMappings["a"] = "\u0BB7" // ஷ (SSA)
            longPressMappings["s"] = "\u0BB8" // ஸ (SA)
            longPressMappings["d"] = "\u0BB9" // ஹ (HA)
            longPressMappings["f"] = "\u0B9C" // ஜ (JA)
            // Bottom Row Sanskrit (Kaladi original standard)
            longPressMappings["z"] = "\u0BB8\u0BCD\u0BB0\u0BC0" // ஸ்ரீ (SRII)
            longPressMappings["x"] = "\u0BD0" // ௐ (OM)
            longPressMappings["c"] = "\u0B95\u0BCD\u0BB7" // க்ஷ (KSSA)
            longPressMappings["v"] = "\u0BB6" // ஶ (SHA)
            // Top Row Tamil Numerals (Kaladi original standard)
            longPressMappings["q"] = "\u0BE7" // ௧ (1)
            longPressMappings["w"] = "\u0BE8" // ௨ (2)
            longPressMappings["e"] = "\u0BE9" // ௩ (3)
            longPressMappings["r"] = "\u0BEA" // ௪ (4)
            longPressMappings["t"] = "\u0BEB" // ௫ (5)
            longPressMappings["y"] = "\u0BEC" // ௬ (6)
            longPressMappings["u"] = "\u0BED" // ௭ (7)
            longPressMappings["i"] = "\u0BEE" // ௮ (8)
            longPressMappings["o"] = "\u0BEF" // ௯ (9)
            longPressMappings["p"] = "\u0BE6" // ௦ (0)
            // Tamil Special Signs (Kaladi original standard)
            longPressMappings["h"] = "\u0BF0" // ௰ (10)
            longPressMappings["j"] = "\u0BF1" // ௱ (100)
            longPressMappings["k"] = "\u0BF2" // ௲ (1000)
            longPressMappings["l"] = "\u0BF9" // ௹ (Rupee)
            longPressMappings["b"] = "\u0BF3" // ௳ (Day)
            longPressMappings["n"] = "\u0BF4" // ௴ (Month)
            longPressMappings["m"] = "\u0BF5" // ௵ (Year)
        }

        return ParsedKeymap(info, mappings, singleCharBadges, layerMappings, layerBadges, longPressMappings)
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
