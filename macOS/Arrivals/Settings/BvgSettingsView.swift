@preconcurrency import ArrivalsLib
import SwiftUI

struct BvgSettingsView: View {
    @EnvironmentObject var coordinator: SettingsCoordinator

    @StateObject private var viewModel = BvgSettingsViewModel()

    private let settings = MacDI.shared.settings

    @State private var searchQuery: String = ""
    @State private var selectedResult: StopResult?

    @State private var lineFilter: String = ""
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
                let stationHint = "S-Bahn, U-Bahn, and Tram stations."
                HStack {
                    DebouncingTextField(label: "Station", value: $searchQuery) { value in
                        if value.isEmpty {
                            viewModel.reset()
                        } else {
                            viewModel.performSearch(value)
                        }
                    }
                    .autocorrectionDisabled()
                    .accessibilityHint(stationHint)
                    Image(systemName: "questionmark.app")
                        .foregroundColor(Color.gray)
                        .help(stationHint)
                        .accessibilityHidden(true)
                }

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
                        LoadingSpinner()
                    }
                }
            }
        }
        .onAppear {
            if selectedResult == nil, settings.mode == SettingsConfig().MODE_BVG, let stop = settings.configuredStop {
                selectedResult = stop
                lineFilter = settings.line
                platformFilter = settings.platform
            }
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

        if isValid {
            Section {
                TextField("Line", text: $lineFilter, prompt: Text("Optional"))
                    .helpHint(help: "e.g. U8, S41", spoken: "For example, U8 or S41")
                    .autocorrectionDisabled()

                TextField("Platform", text: $platformFilter, prompt: Text("Optional"))
                    .helpHint(help: "e.g. 1, 2", spoken: "For example, 1 or 2")
                    .autocorrectionDisabled()
            }
        }
    }
}

@MainActor
private class BvgSettingsViewModel: StopSearchViewModel {
    init() {
        let search = MacDI.shared.bvgSearch
        super.init { query in try await search.searchStops(query: query) }
    }

    func save(station: StopResult, lineFilter: String, platformFilter: String) {
        settings.clearStopConfig()
        settings.stopId = station.id
        settings.stopName = station.name
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
    .environmentObject(SettingsCoordinator())
}
