import Foundation

public struct KeymapInfo: Identifiable, Codable, Hashable {
    public var id: String
    public var filename: String
    public var language: String
    public var encoding: String
    public var keyboard: String
    public var author: String
    public var description: String
    public var isBuiltIn: BooleanLiteralType
    public var ruleCount: Int

    public var displayName: String {
        return "\(language) - \(encoding) (\(keyboard))"
    }

    public init(
        id: String,
        filename: String,
        language: String,
        encoding: String,
        keyboard: String,
        author: String,
        description: String,
        isBuiltIn: Bool,
        ruleCount: Int
    ) {
        self.id = id
        self.filename = filename
        self.language = language
        self.encoding = encoding
        self.keyboard = keyboard
        self.author = author
        self.description = description
        self.isBuiltIn = isBuiltIn
        self.ruleCount = ruleCount
    }
}

public struct EngineResult {
    public let deleteCount: Int
    public let insertText: String
    public let currentBuffer: String

    public init(deleteCount: Int, insertText: String, currentBuffer: String) {
        self.deleteCount = deleteCount
        self.insertText = insertText
        self.currentBuffer = currentBuffer
    }
}
