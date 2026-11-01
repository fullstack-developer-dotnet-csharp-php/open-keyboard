import Foundation
import Combine

public class KeymapStore: ObservableObject {
    public static let shared = KeymapStore()

    public static let appGroupId = "group.org.openkeyboard"
    private static let activeKeymapPref = "active_keymap_id"
    private static let defaultKeymapId = "Tamil26ADN"

    @Published public var availableKeymaps: [KeymapInfo] = []
    @Published public var activeKeymapId: String = defaultKeymapId

    private var sharedDefaults: UserDefaults? {
        return UserDefaults(suiteName: KeymapStore.appGroupId)
    }

    private var sharedContainerUrl: URL? {
        return FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: KeymapStore.appGroupId)
    }

    private init() {
        self.activeKeymapId = sharedDefaults?.string(forKey: KeymapStore.activeKeymapPref) ?? KeymapStore.defaultKeymapId
        setupDirectories()
        refreshList()
    }

    private func setupDirectories() {
        guard let container = sharedContainerUrl else { return }
        let keymapsDir = container.appendingPathComponent("keymaps", isDirectory: true)
        if !FileManager.default.fileExists(atPath: keymapsDir.path) {
            try? FileManager.default.createDirectory(at: keymapsDir, withIntermediateDirectories: true)
        }

        let contents = (try? FileManager.default.contentsOfDirectory(atPath: keymapsDir.path)) ?? []
        if contents.isEmpty {
            copyBundledKeymaps(to: keymapsDir)
        }
    }

    private func copyBundledKeymaps(to targetDir: URL) {
        if let bundleUrls = Bundle.main.urls(forResourcesWithExtension: "xml", subdirectory: nil) {
            for url in bundleUrls {
                let dest = targetDir.appendingPathComponent(url.lastPathComponent)
                try? FileManager.default.copyItem(at: url, to: dest)
            }
        }
    }

    public func refreshList() {
        var result = [KeymapInfo]()

        // 1. From shared container
        if let container = sharedContainerUrl {
            let keymapsDir = container.appendingPathComponent("keymaps", isDirectory: true)
            if let files = try? FileManager.default.contentsOfDirectory(at: keymapsDir, includingPropertiesForKeys: nil) {
                for fileUrl in files where fileUrl.pathExtension.lowercased() == "xml" {
                    if let data = try? Data(contentsOf: fileUrl),
                       let parsed = OpenXmlParser.parse(data: data, filename: fileUrl.lastPathComponent, isBuiltIn: false) {
                        result.append(parsed.info)
                    }
                }
            }
        }

        // 2. From App Bundle
        if let bundleUrls = Bundle.main.urls(forResourcesWithExtension: "xml", subdirectory: nil) {
            for url in bundleUrls {
                if !result.contains(where: { $0.filename == url.lastPathComponent }),
                   let data = try? Data(contentsOf: url),
                   let parsed = OpenXmlParser.parse(data: data, filename: url.lastPathComponent, isBuiltIn: true) {
                    result.append(parsed.info)
                }
            }
        }

        // Sort: Tamil26ADN (SIVA) first, then Hindi, Telugu, French, German, Spanish, Russian, then rest
        result.sort { (a, b) -> Bool in
            if a.id == "Tamil26ADN" { return true }
            if b.id == "Tamil26ADN" { return false }
            if a.id == "Tamil 26" { return true }
            if b.id == "Tamil 26" { return false }
            if a.id.contains("Hindi") { return true }
            if b.id.contains("Hindi") { return false }
            if a.id.contains("French") { return true }
            if b.id.contains("French") { return false }
            return a.displayName < b.displayName
        }

        DispatchQueue.main.async {
            self.availableKeymaps = result
        }
    }

    public func selectKeymap(id: String) {
        activeKeymapId = id
        sharedDefaults?.set(id, forKey: KeymapStore.activeKeymapPref)
        sharedDefaults?.synchronize()
    }

    public func importXml(from sourceUrl: URL) throws -> KeymapInfo {
        let isSecured = sourceUrl.startAccessingSecurityScopedResource()
        defer {
            if isSecured { sourceUrl.stopAccessingSecurityScopedResource() }
        }

        let data = try Data(contentsOf: sourceUrl)
        let filename = sourceUrl.lastPathComponent

        guard let parsed = OpenXmlParser.parse(data: data, filename: filename, isBuiltIn: false) else {
            throw NSError(domain: "OpenKeyboard", code: 400, userInfo: [NSLocalizedDescriptionKey: "Invalid XML keymap file"])
        }

        if let container = sharedContainerUrl {
            let keymapsDir = container.appendingPathComponent("keymaps", isDirectory: true)
            let destUrl = keymapsDir.appendingPathComponent(filename)
            try? FileManager.default.removeItem(at: destUrl)
            try data.write(to: destUrl)
        }

        selectKeymap(id: parsed.info.id)
        refreshList()
        return parsed.info
    }

    public func loadActiveKeymap() -> ParsedKeymap {
        let activeId = sharedDefaults?.string(forKey: KeymapStore.activeKeymapPref) ?? activeKeymapId

        if let container = sharedContainerUrl {
            let fileUrl = container.appendingPathComponent("keymaps/\(activeId).xml")
            if let data = try? Data(contentsOf: fileUrl),
               let parsed = OpenXmlParser.parse(data: data, filename: "\(activeId).xml", isBuiltIn: false) {
                return parsed
            }
        }

        if let bundleUrl = Bundle.main.url(forResource: activeId, withExtension: "xml"),
           let data = try? Data(contentsOf: bundleUrl),
           let parsed = OpenXmlParser.parse(data: data, filename: "\(activeId).xml", isBuiltIn: true) {
            return parsed
        }

        if let bundleUrl = Bundle.main.url(forResource: "Tamil26ADN", withExtension: "xml"),
           let data = try? Data(contentsOf: bundleUrl),
           let parsed = OpenXmlParser.parse(data: data, filename: "Tamil26ADN.xml", isBuiltIn: true) {
            return parsed
        }

        let fallbackXml = "<Table><Header><Language>Universal</Language></Header><Data></Data></Table>"
        return OpenXmlParser.parse(string: fallbackXml, filename: "fallback.xml")!
    }
}
