import Foundation

public class OpenKeyboardEngine {

    private class TrieNode {
        var children = [Character: TrieNode]()
        var mappedValue: String? = nil
    }

    public let parsedKeymap: ParsedKeymap
    private let root = TrieNode()
    private var buffer = ""
    private var lastOutput = ""

    public init(parsedKeymap: ParsedKeymap) {
        self.parsedKeymap = parsedKeymap
        buildTrie()
    }

    private func buildTrie() {
        for (keySeq, target) in parsedKeymap.mappings {
            var curr = root
            for ch in keySeq {
                if let child = curr.children[ch] {
                    curr = child
                } else {
                    let newNode = TrieNode()
                    curr.children[ch] = newNode
                    curr = newNode
                }
            }
            curr.mappedValue = target
        }
    }

    public func handleKeyPress(_ char: Character) -> EngineResult {
        let candidate = buffer + String(char)
        if hasPrefix(candidate) {
            buffer = candidate
            if let match = getExactMatch(buffer) {
                let delCount = lastOutput.count
                lastOutput = match
                return EngineResult(deleteCount: delCount, insertText: match, currentBuffer: buffer)
            } else {
                return EngineResult(deleteCount: 0, insertText: "", currentBuffer: buffer)
            }
        } else {
            buffer = ""
            lastOutput = ""

            let single = String(char)
            if hasPrefix(single) {
                buffer = single
                if let match = getExactMatch(single) {
                    lastOutput = match
                    return EngineResult(deleteCount: 0, insertText: match, currentBuffer: buffer)
                } else {
                    return EngineResult(deleteCount: 0, insertText: "", currentBuffer: buffer)
                }
            } else {
                return EngineResult(deleteCount: 0, insertText: single, currentBuffer: "")
            }
        }
    }

    public func handleBackspace() -> EngineResult {
        if !buffer.isEmpty {
            let delCount = lastOutput.count
            buffer.removeLast()
            if !buffer.isEmpty {
                let match = getExactMatch(buffer) ?? ""
                lastOutput = match
                return EngineResult(deleteCount: delCount, insertText: lastOutput, currentBuffer: buffer)
            } else {
                lastOutput = ""
                return EngineResult(deleteCount: delCount, insertText: "", currentBuffer: "")
            }
        }
        return EngineResult(deleteCount: 1, insertText: "", currentBuffer: "")
    }

    public func reset() {
        buffer = ""
        lastOutput = ""
    }

    private func hasPrefix(_ prefix: String) -> Bool {
        var curr = root
        for ch in prefix {
            guard let next = curr.children[ch] else { return false }
            curr = next
        }
        return true
    }

    private func getExactMatch(_ seq: String) -> String? {
        var curr = root
        for ch in seq {
            guard let next = curr.children[ch] else { return nil }
            curr = next
        }
        return curr.mappedValue
    }

    public func getBadgeForChar(_ char: Character) -> String? {
        return parsedKeymap.singleCharBadges[String(char)]
    }
}
