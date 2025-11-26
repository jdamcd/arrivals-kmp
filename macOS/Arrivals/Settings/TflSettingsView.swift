@preconcurrency import ArrivalsLib
import SwiftUI

struct TflSettingsView: View {
    @StateObject private var viewModel = TflSettingsViewModel()

    @State private var searchQuery: String = ""
    @State private var selectedResult: StopResult?

    @State private var platformFilter: String = ""

    private let directions = ["all", "inbound", "outbound"]
    @State private var directionFilter: String = "all"

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
            .onAppear {
                platformFilter = viewModel.initialPlatform()
            }

            TextField("Platform", text: $platformFilter)
                .autocorrectionDisabled()
                .onAppear {
                    directionFilter = viewModel.initialDirection()
                }

            HStack {
                Spacer()
                Button("Save") {
                    if let selectedResult {
                        viewModel.save(
                            stopPoint: selectedResult,
                            platformFilter: platformFilter.trim(),
                            directionFilter: directionFilter
                        )
                        NSApp.keyWindow?.close()
                    }
                }.disabled(selectedResult == nil)
                    .buttonStyle(.borderedProminent)
            }
        }
    }
}

@MainActor
private class TflSettingsViewModel: ObservableObject {
    @Published var state: SettingsState = .idle

    private let fetcher = MacDI().tflSearch
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

    func disambiguate(stop: StopResult) {
        state = .loading
        Task {
            do {
                let result = try await fetcher.stopDetails(id: stop.id)
                if result.children.isEmpty {
                    state = .empty
                } else {
                    state = .data(result.children)
                }
            } catch {
                state = .error
            }
        }
    }

    func initialPlatform() -> String {
        settings.tflPlatform
    }

    func initialDirection() -> String {
        settings.tflDirection
    }

    func save(stopPoint: StopResult, platformFilter: String, directionFilter: String) {
        settings.tflStopId = stopPoint.id
        settings.tflPlatform = platformFilter
        settings.tflDirection = directionFilter
        settings.mode = SettingsConfig().MODE_TFL
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
        TflSettingsView()
    }
    .formStyle(.grouped)
}
