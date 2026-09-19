@preconcurrency import ArrivalsLib
import SwiftUI

struct TflBusSettingsView: View {
    @EnvironmentObject var coordinator: SettingsCoordinator

    @StateObject private var viewModel = TflBusSettingsViewModel()

    private let settings = MacDI.shared.settings

    @State private var searchQuery: String = ""
    @State private var selectedResult: StopResult?

    @State private var routeFilter: String = ""

    private var isValid: Bool {
        guard let result = selectedResult else { return false }
        return !result.isHub
    }

    var body: some View {
        Section {
            if let selected = selectedResult, !selected.isHub {
                SelectedStopRow(label: "Bus stop", name: selected.name) {
                    selectedResult = nil
                }
            } else {
                let stopHint = "London bus stops. Select a stop group to see its individual stops with directions."
                HStack {
                    DebouncingTextField(label: "Bus stop", value: $searchQuery) { value in
                        if value.isEmpty {
                            viewModel.reset()
                        } else {
                            viewModel.performSearch(value)
                        }
                    }
                    .autocorrectionDisabled()
                    .accessibilityHint(stopHint)
                    Image(systemName: "questionmark.app")
                        .foregroundColor(Color.gray)
                        .help(stopHint)
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
                        Text("Search for a bus stop")
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
            if selectedResult == nil, settings.mode == SettingsConfig().MODE_TFL_BUS, let stop = settings.configuredStop {
                selectedResult = stop
                routeFilter = settings.line
            }
            coordinator.onSave = {
                if let selectedResult {
                    viewModel.save(
                        stopPoint: selectedResult,
                        routeFilter: routeFilter.trim()
                    )
                }
            }
            coordinator.hasValidConfig = isValid
        }
        .onChange(of: selectedResult) { _, newValue in
            if let result = newValue, result.isHub {
                viewModel.disambiguate(stop: result)
                selectedResult = nil
            }
            coordinator.hasValidConfig = isValid
        }

        if isValid {
            Section {
                TextField("Route", text: $routeFilter, prompt: Text("Optional"))
                    .helpHint(help: "e.g. 176, N343", spoken: "For example, 176 or N343")
                    .autocorrectionDisabled()
                    .accessibilityIdentifier("routeField")
            }
        }
    }
}

@MainActor
private class TflBusSettingsViewModel: StopSearchViewModel {
    private let busSearch: TflSearch

    init() {
        let search = MacDI.shared.tflBusSearch
        busSearch = search
        super.init { query in try await search.searchStops(query: query) }
    }

    func disambiguate(stop: StopResult) {
        load { [busSearch] in try await busSearch.stopDetails(id: stop.id).children }
    }

    func save(stopPoint: StopResult, routeFilter: String) {
        settings.clearStopConfig()
        settings.stopId = stopPoint.id
        settings.stopName = stopPoint.name
        settings.line = routeFilter
        settings.mode = SettingsConfig().MODE_TFL_BUS
    }
}

#Preview {
    Form {
        TflBusSettingsView()
    }
    .formStyle(.grouped)
    .environmentObject(SettingsCoordinator())
}
