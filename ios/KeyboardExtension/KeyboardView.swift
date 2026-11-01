import SwiftUI

public protocol KeyboardDelegate: AnyObject {
    func didTapKey(_ text: String)
    func didTapBackspace()
    func didTapSpace()
    func didTapReturn()
    func didTapGlobe()
    func didCycleLayout()
}

public struct KeyboardExtensionView: View {
    public weak var delegate: KeyboardDelegate?
    public var engine: OpenKeyboardEngine?

    @State private var isShifted = false
    @State private var isSymbols = false

    private let qwertyRows: [[String]] = [
        ["q", "w", "e", "r", "t", "y", "u", "i", "o", "p"],
        ["a", "s", "d", "f", "g", "h", "j", "k", "l"],
        ["SHIFT", "z", "x", "c", "v", "b", "n", "m", "DEL"],
        ["123", "SMILEY", "SPACE", "RETURN"]
    ]

    private let symbolRows: [[String]] = [
        ["1", "2", "3", "4", "5", "6", "7", "8", "9", "0"],
        ["@", "#", "$", "%", "&", "-", "+", "(", ")", "/"],
        ["ABC", "*", "\"", "'", ":", ";", "!", "?", "DEL"],
        ["123", "SMILEY", "SPACE", "RETURN"]
    ]

    public init(delegate: KeyboardDelegate? = nil, engine: OpenKeyboardEngine? = nil) {
        self.delegate = delegate
        self.engine = engine
    }

    public var body: some View {
        VStack(spacing: 6) {
            HStack {
                Text(engine?.parsedKeymap.info.displayName.uppercased() ?? "OPEN KEYBOARD")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(.gray)

                Spacer()

                Button(action: { delegate?.didCycleLayout() }) {
                    Text("Switch Layout")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(.blue)
                }
            }
            .padding(.horizontal, 10)
            .padding(.top, 4)

            let rows = isSymbols ? symbolRows : qwertyRows
            ForEach(0..<rows.count, id: \.self) { rowIndex in
                HStack(spacing: 5) {
                    ForEach(rows[rowIndex], id: \.self) { key in
                        keyButton(for: key)
                    }
                }
            }
        }
        .padding(.horizontal, 4)
        .padding(.bottom, 6)
        .background(Color(UIColor.secondarySystemBackground))
    }

    @ViewBuilder
    private func keyButton(for key: String) -> some View {
        switch key {
        case "SHIFT", "ABC":
            Button(action: {
                if key == "ABC" { isSymbols = false }
                else { isShifted.toggle() }
            }) {
                Image(systemName: isShifted ? "capslock.fill" : "shift")
                    .font(.system(size: 16))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(isShifted ? Color.blue : Color(UIColor.systemGray4))
                    .foregroundColor(isShifted ? .white : .primary)
                    .cornerRadius(6)
            }
            .frame(height: 42)
            .frame(maxWidth: 50)

        case "DEL":
            Button(action: { delegate?.didTapBackspace() }) {
                Image(systemName: "delete.left")
                    .font(.system(size: 16))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color(UIColor.systemGray4))
                    .foregroundColor(.primary)
                    .cornerRadius(6)
            }
            .frame(height: 42)
            .frame(maxWidth: 50)

        case "123":
            Button(action: { isSymbols.toggle() }) {
                Text(isSymbols ? "ABC" : "123")
                    .font(.system(size: 14, weight: .medium))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color(UIColor.systemGray4))
                    .foregroundColor(.primary)
                    .cornerRadius(6)
            }
            .frame(height: 42)
            .frame(maxWidth: 50)

        case "SMILEY":
            Button(action: { delegate?.didTapGlobe() }) {
                Text("😊")
                    .font(.system(size: 18))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color(UIColor.systemGray4))
                    .cornerRadius(6)
            }
            .frame(height: 42)
            .frame(maxWidth: 44)

        case "SPACE":
            Button(action: { delegate?.didTapSpace() }) {
                Text("Space / இடைவெளி")
                    .font(.system(size: 13))
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color(UIColor.systemBackground))
                    .cornerRadius(6)
            }
            .frame(height: 42)

        case "RETURN":
            Button(action: { delegate?.didTapReturn() }) {
                Text("⏎")
                    .font(.system(size: 16, weight: .bold))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(6)
            }
            .frame(height: 42)
            .frame(maxWidth: 60)

        default:
            let displayChar = isShifted ? key.uppercased() : key.lowercased()
            let badge = (key.count == 1) ? engine?.getBadgeForChar(displayChar.first!) : nil

            Button(action: { delegate?.didTapKey(displayChar) }) {
                ZStack(alignment: .topTrailing) {
                    Text(displayChar)
                        .font(.system(size: 20))
                        .foregroundColor(.primary)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)

                    if let badge = badge {
                        Text(badge)
                            .font(.system(size: 9, weight: .semibold))
                            .foregroundColor(.orange)
                            .padding(.top, 2)
                            .padding(.trailing, 3)
                    }
                }
                .background(Color(UIColor.systemBackground))
                .cornerRadius(6)
                .shadow(color: Color.black.opacity(0.1), radius: 0.5, y: 1)
            }
            .frame(height: 42)
        }
    }
}
