import Foundation

public class ParsedKeymap {
    public let info: KeymapInfo
    public let mappings: [String: String]
    public let singleCharBadges: [String: String]
    public let longPressMappings: [String: String]
    public let longPressBadges: [String: String]

    public init(
        info: KeymapInfo,
        mappings: [String: String],
        singleCharBadges: [String: String],
        longPressMappings: [String: String] = [:],
        longPressBadges: [String: String] = [:]
    ) {
        self.info = info
        self.mappings = mappings
        self.singleCharBadges = singleCharBadges
        self.longPressMappings = longPressMappings
        self.longPressBadges = longPressBadges
    }
}

public class OpenXmlParser: NSObject, XMLParserDelegate {

    private var language = "Universal"
    private var encoding = "Unicode"
    private var keyboard = "Custom"
    private var author = "Open Keyboard Community"
    private var desc = ""

    private var mappings = [String: String]()
    private var singleCharBadges = [String: String]()
    private var longPressMappings = [String: String]()
    private var longPressBadges = [String: String]()

    private var currentElement = ""
    private var currentCharacterHex = ""
    private var currentDisplayHex = ""
    private var currentKeysHex = ""
    private var currentLayer = ""
    private var currentThirdKeyHex = ""
    private var currentThirdDisplayHex = ""
    private var currentThirdKeyAttr = ""
    private var currentThirdDisplayAttr = ""
    private var currentText = ""

    public static func parse(data: Data, filename: String, isBuiltIn: Bool = false) -> ParsedKeymap? {
        let parser = OpenXmlParser()
        let xmlParser = XMLParser(data: data)
        xmlParser.delegate = parser
        if xmlParser.parse() {
            // Auto-detect Sanskrit 2-key mappings (e.g. yg -> ஜ்)
            for (k, v) in parser.mappings {
                if k.count == 2 && k.hasSuffix("g") {
                    let first = String(k.prefix(1)).lowercased()
                    if parser.longPressMappings[first] == nil {
                        parser.longPressMappings[first] = v
                        parser.longPressBadges[first] = parser.singleCharBadges[k] ?? v
                    }
                }
            }

            let info = KeymapInfo(
                id: (filename as NSString).deletingPathExtension,
                filename: filename,
                language: parser.language,
                encoding: parser.encoding,
                keyboard: parser.keyboard,
                author: parser.author,
                description: parser.desc,
                isBuiltIn: isBuiltIn,
                ruleCount: parser.mappings.count
            )
            return ParsedKeymap(
                info: info,
                mappings: parser.mappings,
                singleCharBadges: parser.singleCharBadges,
                longPressMappings: parser.longPressMappings,
                longPressBadges: parser.longPressBadges
            )
        }
        return nil
    }

    public static func parse(string: String, filename: String, isBuiltIn: Bool = false) -> ParsedKeymap? {
        guard let data = string.data(using: .utf8) else { return nil }
        return parse(data: data, filename: filename, isBuiltIn: isBuiltIn)
    }

    public func parser(_ parser: XMLParser, didStartElement elementName: String, namespaceURI: String?, qualifiedName qName: String?, attributes attributeDict: [String : String] = [:]) {
        currentElement = elementName
        currentText = ""
        if elementName.caseInsensitiveCompare("Map") == .orderedSame ||
           elementName.caseInsensitiveCompare("ThirdKey") == .orderedSame ||
           elementName.caseInsensitiveCompare("LongPress") == .orderedSame {
            currentLayer = attributeDict["Layer"] ?? attributeDict["layer"] ?? (elementName.caseInsensitiveCompare("ThirdKey") == .orderedSame ? "ThirdKey" : "")
            currentThirdKeyAttr = attributeDict["ThirdKey"] ?? attributeDict["thirdKey"] ?? attributeDict["LongPress"] ?? ""
            currentThirdDisplayAttr = attributeDict["ThirdDisplay"] ?? attributeDict["thirdDisplay"] ?? attributeDict["LongPressDisplay"] ?? ""
        }
    }

    public func parser(_ parser: XMLParser, foundCharacters string: String) {
        currentText += string
    }

    public func parser(_ parser: XMLParser, didEndElement elementName: String, namespaceURI: String?, qualifiedName qName: String?) {
        let trimmed = currentText.trimmingCharacters(in: .whitespacesAndNewlines)
        switch elementName {
        case "Language":
            if !trimmed.isEmpty { language = trimmed }
        case "Encoding":
            if !trimmed.isEmpty { encoding = trimmed }
        case "Keyboard":
            if !trimmed.isEmpty { keyboard = trimmed }
        case "Author":
            if !trimmed.isEmpty { author = trimmed }
        case "Description":
            if !trimmed.isEmpty { desc = trimmed }
        case "Character":
            currentCharacterHex = trimmed
        case "Display":
            currentDisplayHex = trimmed
        case "Keys":
            currentKeysHex = trimmed
        case "Layer":
            currentLayer = trimmed
        case "ThirdKey", "LongPress", "Key3":
            currentThirdKeyHex = trimmed
        case "ThirdDisplay", "LongPressDisplay":
            currentThirdDisplayHex = trimmed
        case "Map", "ThirdKey", "LongPress":
            let targetHex = !currentCharacterHex.isEmpty ? currentCharacterHex : currentDisplayHex
            if !currentKeysHex.isEmpty && !targetHex.isEmpty {
                let keyStr = OpenXmlParser.hexToKeys(currentKeysHex)
                let charStr = OpenXmlParser.hexToChar(targetHex)
                let displayStr = !currentDisplayHex.isEmpty ? OpenXmlParser.hexToChar(currentDisplayHex) : charStr

                let isThirdKeyLayer = currentLayer.caseInsensitiveCompare("LongPress") == .orderedSame ||
                                      currentLayer.caseInsensitiveCompare("ThirdKey") == .orderedSame ||
                                      currentLayer.caseInsensitiveCompare("3rdKey") == .orderedSame

                if isThirdKeyLayer {
                    longPressMappings[keyStr.lowercased()] = charStr
                    longPressBadges[keyStr.lowercased()] = displayStr
                } else {
                    mappings[keyStr] = charStr
                    if keyStr.count == 1 && singleCharBadges[keyStr] == nil {
                        singleCharBadges[keyStr] = displayStr
                    }
                }

                let thirdHex = !currentThirdKeyHex.isEmpty ? currentThirdKeyHex : currentThirdKeyAttr
                if !thirdHex.isEmpty {
                    let thirdChar = OpenXmlParser.hexToChar(thirdHex)
                    let thirdDispHex = !currentThirdDisplayHex.isEmpty ? currentThirdDisplayHex : currentThirdDisplayAttr
                    let thirdDisp = !thirdDispHex.isEmpty ? OpenXmlParser.hexToChar(thirdDispHex) : thirdChar
                    longPressMappings[keyStr.lowercased()] = thirdChar
                    longPressBadges[keyStr.lowercased()] = thirdDisp
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
        default:
            break
        }
        currentElement = ""
    }

    public static func hexToKeys(_ hex: String) -> String {
        var str = ""
        var idx = hex.startIndex
        while idx < hex.endIndex {
            let nextIdx = hex.index(idx, offsetBy: 2, limitedBy: hex.endIndex) ?? hex.endIndex
            let sub = String(hex[idx..<nextIdx])
            if let byteVal = UInt8(sub, radix: 16) {
                str.append(Character(UnicodeScalar(byteVal)))
            }
            idx = nextIdx
        }
        return str
    }

    public static func hexToChar(_ hex: String) -> String {
        var str = ""
        var idx = hex.startIndex
        let step = (hex.count % 4 == 0) ? 4 : 2
        while idx < hex.endIndex {
            let nextIdx = hex.index(idx, offsetBy: step, limitedBy: hex.endIndex) ?? hex.endIndex
            let sub = String(hex[idx..<nextIdx])
            if let code = UInt32(sub, radix: 16), let scalar = UnicodeScalar(code) {
                str.append(Character(scalar))
            }
            idx = nextIdx
        }
        return str
    }
}
