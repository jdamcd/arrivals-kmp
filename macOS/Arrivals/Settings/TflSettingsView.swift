@preconcurrency import ArrivalsLib
import SwiftUI

struct TflSettingsView: View {
    @EnvironmentObject var coordinator: SettingsCoordinator

    @StateObject private var viewModel = TflSettingsViewModel()

    @State private var searchQuery: String = ""
    @State private var selectedResult: StopResult?

    @State private var platformFilter: String = ""

    private let directions = ["all", "inbound", "outbound"]
    @State private var directionFilter: String = "all"

    private var isValid: Bool {
        selectedResult != nil
    }

    var body: some View {
        Section {
            HStack {
                DebouncingTextField(label: "Station", value: $searchQuery) { value in
                    if value.isEmpty {
                        viewModel.reset()
                    } else {
                        viewModel.performSearch(value)
                    }
                }
                .autocorrectionDisabled()
                Image(systemName: "questionmark.app")
                    .foregroundColor(Color.gray)
                    .help("London Overground, Tube, DLR, and Tram stations. No arrival times at the end of the line.")
            }
            ResultsArea {
                switch viewModel.state {
                case let .data(results):
                    List(results, id: \.self, selection: $selectedResult) { result in
                        Text(result.name)
                    }
                    .onChange(of: selectedResult) { _, newValue in
                        if let result = newValue, result.isHub {
                            viewModel.disambiguate(stop: result)
                            selectedResult = nil
                        }
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

            Picker("Direction", selection: $directionFilter) {
                ForEach(directions, id: \.self) { direction in
                    Text(direction.capitalized).tag(direction)
                }
            }
            .pickerStyle(.automatic)

            TextField("Platform", text: $platformFilter)
                .help("Platform number (e.g. 1, 2A, 10)")
                .autocorrectionDisabled()
        }
        .onAppear {
            coordinator.onSave = {
                if let selectedResult {
                    viewModel.save(
                        stopPoint: selectedResult,
                        platformFilter: platformFilter.trim(),
                        directionFilter: directionFilter
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
private class TflSettingsViewModel: StopSearchViewModel {
    private let tflSearch: TflSearch

    init() {
        let search = MacDI().tflSearch
        tflSearch = search
        super.init { query in try await search.searchStops(query: query) }
    }

    func disambiguate(stop: StopResult) {
        state = .loading
        Task {
            do {
                let result = try await tflSearch.stopDetails(id: stop.id)
                state = result.children.isEmpty ? .empty : .data(result.children)
            } catch {
                state = .error
            }
        }
    }

    func save(stopPoint: StopResult, platformFilter: String, directionFilter: String) {
        settings.clearStopConfig()
        settings.stopId = stopPoint.id
        settings.platform = platformFilter
        settings.direction = directionFilter
        settings.mode = SettingsConfig().MODE_TFL
    }
}

#Preview {
    Form {
        TflSettingsView()
    }
    .formStyle(.grouped)
}
