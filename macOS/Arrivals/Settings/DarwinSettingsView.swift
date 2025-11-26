@preconcurrency import ArrivalsLib
import SwiftUI

struct DarwinSettingsView: View {
    @StateObject private var viewModel = DarwinSettingsViewModel()

    @State private var searchQuery: String = ""
    @State private var selectedResult: StopResult?

    @State private var platformFilter: String = ""

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

            TextField("Platform filter (optional)", text: $platformFilter)
                .autocorrectionDisabled()
                .onAppear {
                    platformFilter = viewModel.initialPlatform()
                }

            HStack {
                Spacer()
                Button("Save") {
                    if let selectedResult {
                        viewModel.save(
                            station: selectedResult,
                            platformFilter: platformFilter.trim()
                        )
                        NSApp.keyWindow?.close()
                    }
                }
                .disabled(selectedResult == nil)
                .buttonStyle(.borderedProminent)
            }
        }
    }
}

@MainActor
private class DarwinSettingsViewModel: ObservableObject {
    @Published var state: SettingsState = .idle

    private let fetcher = MacDI().darwinSearch
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

    func initialPlatform() -> String {
        settings.darwinPlatform
    }

    func save(station: StopResult, platformFilter: String) {
        settings.darwinCrsCode = station.id
        settings.darwinPlatform = platformFilter
        settings.mode = SettingsConfig().MODE_DARWIN
    }
}

private enum SettingsState: Equatable {
    case idle
    case loading
    case data([StopResult])
    case empty
    case error
}

#Preview {
    Form {
        DarwinSettingsView()
    }
    .formStyle(.grouped)
}
