@preconcurrency import ArrivalsLib
import SwiftUI

struct DarwinSettingsView: View {
    @EnvironmentObject var coordinator: SettingsCoordinator

    @StateObject private var viewModel = DarwinSettingsViewModel()

    @State private var searchQuery: String = ""
    @State private var selectedResult: StopResult?

    @State private var platformFilter: String = ""

    private var isValid: Bool {
        selectedResult != nil
    }

    var body: some View {
        Section {
            if let selected = selectedResult {
                SelectedStopRow(name: selected.name) {
                    selectedResult = nil
                }
            } else {
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
                        .accessibilityIdentifier("searchResultsList")
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
            }
        }
        .onAppear {
            coordinator.onSave = {
                if let selectedResult {
                    viewModel.save(
                        station: selectedResult,
                        platformFilter: platformFilter.trim()
                    )
                }
            }
            coordinator.canSave = isValid
        }
        .onChange(of: selectedResult) { _, _ in
            coordinator.canSave = isValid
        }

        if isValid {
            Section {
                TextField("Platform", text: $platformFilter)
                    .help("Optional platform (e.g. 2, 5A)")
                    .autocorrectionDisabled()
            }
        }
    }
}

@MainActor
private class DarwinSettingsViewModel: StopSearchViewModel {
    init() {
        let search = MacDI().darwinSearch
        super.init { query in try await search.searchStops(query: query) }
    }

    func save(station: StopResult, platformFilter: String) {
        settings.clearStopConfig()
        settings.stopId = station.id
        settings.platform = platformFilter
        settings.mode = SettingsConfig().MODE_DARWIN
    }
}

#Preview {
    Form {
        DarwinSettingsView()
    }
    .formStyle(.grouped)
    .environmentObject(SettingsCoordinator())
}
