@preconcurrency import ArrivalsLib
import Combine
import SwiftUI

extension Arrival {
    func accessibilityLabel(position: Int) -> String {
        var parts = ["Position \(position)"]
        if lineBadge?.express == true {
            parts.append("express")
        }
        if let line {
            parts.append("\(line) to \(destination)")
        } else {
            parts.append(destination)
        }
        if isDue {
            parts.append("due")
        } else {
            parts.append("\(minutesToStop) minute\(minutesToStop == 1 ? "" : "s")")
        }
        if !realtime {
            parts.append("scheduled")
        }
        return parts.joined(separator: ", ")
    }
}

struct LoadingSpinner: View {
    var body: some View {
        ProgressView()
            .scaleEffect(0.5)
            .accessibilityLabel("Loading")
    }
}

struct ResultsArea<Content: View>: View {
    @ViewBuilder var content: Content

    var body: some View {
        VStack(spacing: 0) {
            content
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .frame(minHeight: 85, maxHeight: .infinity, alignment: .center)
    }
}

struct DebouncingTextField: View {
    @StateObject private var viewModel: DebouncingTextFieldModel
    @Binding private var value: String
    var label: String

    init(
        label: String,
        value: Binding<String>,
        debounceInterval: TimeInterval = 0.75,
        valueChanged: @escaping (String) -> Void
    ) {
        self.label = label
        _value = value
        _viewModel = StateObject(
            wrappedValue: DebouncingTextFieldModel(
                debounceInterval: debounceInterval,
                valueChanged: valueChanged
            )
        )
    }

    var body: some View {
        TextField(label, text: $viewModel.text)
            .onAppear {
                viewModel.text = value
            }
            .onChange(of: viewModel.text) { _, newValue in
                value = newValue
            }
            .accessibilityIdentifier("searchField")
    }
}

class DebouncingTextFieldModel: ObservableObject {
    @Published var text: String = "" {
        didSet {
            guard text != oldValue else { return }
            publisher.send(text)
        }
    }

    let publisher = PassthroughSubject<String, Never>()
    private var cancellable: AnyCancellable?

    init(debounceInterval: TimeInterval, valueChanged: @escaping (String) -> Void) {
        cancellable = publisher
            .debounce(for: .seconds(debounceInterval), scheduler: DispatchQueue.main)
            .sink(receiveValue: valueChanged)
    }
}

struct BlinkViewModifier: ViewModifier {
    let duration: Double
    @State private var isVisible: Bool = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        content
            .opacity(isVisible ? 0 : 1)
            .animation(.easeOut(duration: duration).repeatForever(), value: isVisible)
            .onAppear {
                guard !reduceMotion else { return }
                withAnimation {
                    isVisible = true
                }
            }
    }
}

extension View {
    func helpHint(help: String, spoken: String) -> some View {
        self.help(help).accessibilityHint(spoken)
    }

    func blinking(enabled: Bool = true, duration: Double = 0.75) -> some View {
        Group {
            if enabled {
                self.modifier(BlinkViewModifier(duration: duration))
            } else {
                self
            }
        }
    }
}

struct SelectedStopRow: View {
    var label: String = "Station"
    let name: String
    let onClear: () -> Void

    var body: some View {
        LabeledContent(label) {
            HStack(spacing: 6) {
                Text(name)
                    .lineLimit(1)
                    .accessibilityIdentifier("selectedStopName")
                Button("Change") {
                    onClear()
                }
                .controlSize(.small)
                .accessibilityIdentifier("changeStopButton")
            }
        }
    }
}

extension ArrivalsLib.Settings {
    var configuredStop: StopResult? {
        stopName.isNotEmpty ? StopResult(id: stopId, name: stopName, isHub: false) : nil
    }
}

extension String {
    func trim() -> String {
        trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

extension String {
    var isNotEmpty: Bool {
        !trim().isEmpty
    }
}

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let r = Double((int >> 16) & 0xFF) / 255.0
        let g = Double((int >> 8) & 0xFF) / 255.0
        let b = Double(int & 0xFF) / 255.0
        self.init(red: r, green: g, blue: b)
    }
}
