import Combine
import SwiftUI

struct ResultsArea<Content: View>: View {
    @ViewBuilder var content: Content

    var body: some View {
        VStack(spacing: 0) {
            content
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .frame(minHeight: 70, alignment: .center)
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
    }
}

private class DebouncingTextFieldModel: ObservableObject {
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

    func body(content: Content) -> some View {
        content
            .opacity(isVisible ? 0 : 1)
            .animation(.easeOut(duration: duration).repeatForever(), value: isVisible)
            .onAppear {
                withAnimation {
                    isVisible = true
                }
            }
    }
}

extension View {
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
