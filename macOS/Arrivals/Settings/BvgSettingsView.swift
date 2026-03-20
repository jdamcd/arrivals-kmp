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

            TextField("Platform", text: $platformFilter)
                .help("Platform number (e.g. 1, 2)")
                .autocorrectionDisabled()
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
private class BvgSettingsViewModel: StopSearchViewModel {
    init() {
        let search = MacDI().bvgSearch
        super.init { query in try await search.searchStops(query: query) }
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
