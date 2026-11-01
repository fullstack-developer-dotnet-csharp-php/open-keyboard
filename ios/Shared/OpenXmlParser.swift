import Foundation

public class ParsedKeymap {
    public let info: KeymapInfo
    public let mappings: [String: String]
    public let singleCharBadges: [String: String]

    public init(info: KeymapInfo, mappings: [String: String], singleCharBadges: [String: String]) {
        self.info = info
        self.mappings = mappings
        self.singleCharBadges = singleCharBadges
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

    private var currentElement = ""
    private var currentCharacterHex = ""
    private var currentDisplayHex = ""
    private var currentKeysHex = ""
    private var currentText = ""

    public static func parse(data: Data, filename: String, isBuiltIn: Bool = false) -> ParsedKeymap? {
        let parser = OpenXmlParser()
        let xmlParser = XMLParser(data: data)
        xmlParser.delegate = parser
        if xmlParser.parse() {
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
            return ParsedKeymap(info: info, mappings: parser.mappings, singleCharBadges: parser.singleCharBadges)
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
        case "Map":
            let targetHex = !currentCharacterHex.isEmpty ? currentCharacterHex : currentDisplayHex
            if !currentKeysHex.isEmpty && !targetHex.isEmpty {
                let keyStr = OpenXmlParser.hexToKeys(currentKeysHex)
                let charStr = OpenXmlParser.hexToChar(targetHex)
                let displayStr = !currentDisplayHex.isEmpty ? OpenXmlParser.hexToChar(currentDisplayHex) : charStr

                mappings[keyStr] = charStr
                if keyStr.count == 1 && singleCharBadges[keyStr] == nil {
                    singleCharBadges[keyStr] = displayStr
                }
            }
            currentCharacterHex = ""
            currentDisplayHex = ""
            currentKeysHex = ""
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
