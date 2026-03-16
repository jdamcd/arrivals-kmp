@preconcurrency import ArrivalsLib
import SwiftUI

struct BvgSettingsView: View {
    @EnvironmentObject var coordinator: SettingsCoordinator

    @StateObject private var viewModel = BvgSettingsViewModel()

    @State private var searchQuery: String = ""
    @State private var selectedResult: StopResult?

    @State private var lineFilter: String = ""
    @State private var platformFilter: String = ""

    private var isValid: Bool {
        selectedResult != nil
    }

    var body: some View {
        Section {
            DebouncingTextField(label: "Station", value: $searchQuery) { value in
                if value.isEmpty {
                    viewModel.reset()
                } else {
                    viewModel.performSearch(value)
                }
            }
            .autocorrectionDisabled()

            ResultsArea {
                switch viewModel.state {
                case let .data(results):
                    List(results, id: \.self, selection: $selectedResult) { result in
                        Text(result.name)
                    }
                    .listStyle(PlainListStyle())
                case .idle:
                    Text("Search for a station")
                case .empty:
                    Text("No results found")
                case .error:
                    Text("Search error")
                case .loading:
                    ProgressView()
                        .scaleEffect(0.5)
                }
            }

            TextField("Line", text: $lineFilter)
                .help("Line name (e.g. U2, S5, M10)")
                .autocorrectionDisabled()
                .onAppear {
                    lineFilter = viewModel.initialLine()
                }

            TextField("Platform", text: $platformFilter)
                .help("Platform number (e.g. 1, 2)")
                .autocorrectionDisabled()
                .onAppear {
                    platformFilter = viewModel.initialPlatform()
                }
        }
        .onAppear {
            coordinator.onSave = {
                if let selectedResult {
                    viewModel.save(
                        station: selectedResult,
                        lineFilter: lineFilter.trim(),
                        platformFilter: platformFilter.trim()
                    )
                }
            }
            coordinator.canSave = isValid
        }
        .onChange(of: selectedResult) { _, _ in
            coordinator.canSave = isValid
        }
    }
}

@MainActor
private class BvgSettingsViewModel: ObservableObject {
    @Published var state: SettingsState = .idle

    private let fetcher = MacDI().bvgSearch
    private let settings = MacDI().settings

    func reset() {
        state = .idle
    }

    func performSearch(_ query: String) {
        state = .loading
        Task {
            do {
                let result = try await fetcher.searchStops(query: query)
                if result.isEmpty {
                    state = .empty
                } else {
                    state = .data(result)
                }
            } catch {
                state = .error
            }
        }
    }

    func initialLine() -> String {
        settings.line
    }

    func initialPlatform() -> String {
        settings.platform
    }

    func save(station: StopResult, lineFilter: String, platformFilter: String) {
        settings.clearStopConfig()
        settings.stopId = station.id
        settings.line = lineFilter
        settings.platform = platformFilter
        settings.mode = SettingsConfig().MODE_BVG
    }
}

#Preview {
    Form {
        BvgSettingsView()
    }
    .formStyle(.grouped)
}
