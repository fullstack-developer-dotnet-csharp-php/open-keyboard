import SwiftUI
import UniformTypeIdentifiers

struct ContentView: View {
    @StateObject private var store = KeymapStore.shared
    @State private var showImporter = false
    @State private var importErrorMessage: String? = nil
    @State private var showImportSuccess = false
    @State private var importedName = ""
    @State private var testText = ""

    var body: some View {
        NavigationView {
            List {
                Section(header: Text("iOS Keyboard Setup").font(.subheadline).bold()) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("To use Open Keyboard across all apps:")
                            .font(.subheadline)
                            .foregroundColor(.secondary)

                        HStack(alignment: .top, spacing: 10) {
                            Text("1.").bold()
                            Text("Open **Settings > General > Keyboard > Keyboards**")
                        }
                        .font(.footnote)

                        HStack(alignment: .top, spacing: 10) {
                            Text("2.").bold()
                            Text("Tap **Add New Keyboard...** and select **Open Keyboard**")
                        }
                        .font(.footnote)

                        HStack(alignment: .top, spacing: 10) {
                            Text("3.").bold()
                            Text("Tap Open Keyboard and toggle **Allow Full Access** (required to load uploaded XMLs)")
                        }
                        .font(.footnote)
                    }
                    .padding(.vertical, 4)
                }

                Section(header: Text("Typing Playground").font(.subheadline).bold()) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Active: \(activeKeymapName)")
                            .font(.caption)
                            .foregroundColor(.blue)

                        TextEditor(text: $testText)
                            .frame(height: 80)
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                            )

                        if testText.isEmpty {
                            Text("Tap here to bring up your keyboard and test typing...")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        } else {
                            Button(action: { testText = "" }) {
                                Text("Clear Text")
                                    .font(.caption)
                                    .foregroundColor(.red)
                            }
                        }
                    }
                    .padding(.vertical, 4)
                }

                Section(header: HStack {
                    Text("Installed Keymap Layouts")
                        .font(.subheadline)
                        .bold()
                    Spacer()
                    Button(action: { showImporter = true }) {
                        Label("Import XML", systemImage: "plus.circle.fill")
                            .font(.caption)
                    }
                }) {
                    ForEach(store.availableKeymaps) { keymap in
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(keymap.displayName)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(.primary)

                                Text("\(keymap.ruleCount) mappings • Author: \(keymap.author)")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }

                            Spacer()

                            if keymap.id == store.activeKeymapId {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundColor(.blue)
                                    .font(.title3)
                            }

                            Text(keymap.isBuiltIn ? "BUILT-IN" : "CUSTOM")
                                .font(.system(size: 9, weight: .bold))
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(keymap.isBuiltIn ? Color.blue.opacity(0.1) : Color.green.opacity(0.1))
                                .foregroundColor(keymap.isBuiltIn ? .blue : .green)
                                .cornerRadius(4)
                        }
                        .contentShape(Rectangle())
                        .onTapGesture {
                            store.selectKeymap(id: keymap.id)
                        }
                    }
                }
            }
            .listStyle(InsetGroupedListStyle())
            .navigationTitle("Open Keyboard")
            .fileImporter(
                isPresented: $showImporter,
                allowedContentTypes: [UTType.xml, UTType(filenameExtension: "xml") ?? .xml],
                allowsMultipleSelection: false
            ) { result in
                switch result {
                case .success(let urls):
                    guard let url = urls.first else { return }
                    do {
                        let info = try store.importXml(from: url)
                        importedName = info.displayName
                        showImportSuccess = true
                    } catch {
                        importErrorMessage = error.localizedDescription
                    }
                case .failure(let err):
                    importErrorMessage = err.localizedDescription
                }
            }
            .alert(isPresented: $showImportSuccess) {
                Alert(
                    title: Text("Keymap Imported"),
                    message: Text("Successfully loaded '\(importedName)' and set as active keyboard."),
                    dismissButton: .default(Text("OK"))
                )
            }
        }
    }

    private var activeKeymapName: String {
        store.availableKeymaps.first(where: { $0.id == store.activeKeymapId })?.displayName ?? "Tamil - Unicode (Tamil26ADN)"
    }
}
