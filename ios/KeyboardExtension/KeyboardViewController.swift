import UIKit
import SwiftUI

class KeyboardViewController: UIInputViewController, KeyboardDelegate {

    private var engine: OpenKeyboardEngine?
    private var hostingController: UIHostingController<KeyboardExtensionView>?

    override func viewDidLoad() {
        super.viewDidLoad()
        loadActiveEngine()
        setupKeyboardView()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        loadActiveEngine()
        updateKeyboardRootView()
    }

    private func loadActiveEngine() {
        let parsed = KeymapStore.shared.loadActiveKeymap()
        engine = OpenKeyboardEngine(parsedKeymap: parsed)
    }

    private func setupKeyboardView() {
        let keyboardView = KeyboardExtensionView(delegate: self, engine: engine)
        let host = UIHostingController(rootView: keyboardView)
        host.view.translatesAutoresizingMaskIntoConstraints = false
        host.view.backgroundColor = .clear

        addChild(host)
        view.addSubview(host.view)
        host.didMove(toParent: self)

        NSLayoutConstraint.activate([
            host.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            host.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            host.view.topAnchor.constraint(equalTo: view.topAnchor),
            host.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            host.view.heightAnchor.constraint(equalToConstant: 250)
        ])

        hostingController = host
    }

    private func updateKeyboardRootView() {
        hostingController?.rootView = KeyboardExtensionView(delegate: self, engine: engine)
    }

    // MARK: - KeyboardDelegate Implementation

    func didTapKey(_ text: String) {
        guard let eng = engine, let ch = text.first else { return }
        let result = eng.handleKeyPress(ch)

        if result.deleteCount > 0 {
            for _ in 0..<result.deleteCount {
                textDocumentProxy.deleteBackward()
            }
        }

        if !result.insertText.isEmpty {
            textDocumentProxy.insertText(result.insertText)
        }
    }

    func didTapBackspace() {
        guard let eng = engine else { return }
        let result = eng.handleBackspace()

        if result.deleteCount > 0 {
            for _ in 0..<result.deleteCount {
                textDocumentProxy.deleteBackward()
            }
        }

        if !result.insertText.isEmpty {
            textDocumentProxy.insertText(result.insertText)
        }
    }

    func didTapSpace() {
        engine?.reset()
        textDocumentProxy.insertText(" ")
    }

    func didTapReturn() {
        engine?.reset()
        textDocumentProxy.insertText("\n")
    }

    func didTapGlobe() {
        advanceToNextInputMode()
    }

    func didCycleLayout() {
        let all = KeymapStore.shared.availableKeymaps
        guard all.count > 1 else { return }

        let currentId = KeymapStore.shared.activeKeymapId
        let currentIndex = all.firstIndex(where: { $0.id == currentId }) ?? 0
        let nextIndex = (currentIndex + 1) % all.count
        let next = all[nextIndex]

        KeymapStore.shared.selectKeymap(id: next.id)
        loadActiveEngine()
        updateKeyboardRootView()
    }
}
