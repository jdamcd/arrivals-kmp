@preconcurrency import ArrivalsLib
import SwiftUI

struct MtaSettingsView: View {
    @EnvironmentObject var coordinator: SettingsCoordinator

    @StateObject private var viewModel = MtaSettingsViewModel()

    @State private var selectedLine: String?
    @State private var selectedStop: StopResult?
    @State private var filterText: String = ""

    private let lines: [String: String]

    private var isValid: Bool {
        selectedLine != nil && selectedStop != nil
    }

    init() {
        let mta = Mta()
        lines = mta.realtime
        let settings = MacDI.shared.settings
        if settings.mode == SettingsConfig().MODE_GTFS,
           let stop = settings.configuredStop,
           let line = mta.realtime.first(where: { $0.value == settings.gtfsRealtime })?.key
        {
            _selectedLine = State(initialValue: line)
            _selectedStop = State(initialValue: stop)
        }
    }

    var body: some View {
        Section {
            Picker("Line", selection: $selectedLine) {
                Text("--").tag(String?.none)
                ForEach(lines.keys.sorted(), id: \.self) { line in
                    Text(line).tag(String?.some(line))
                }
            }
            .pickerStyle(.menu)
            .accessibilityIdentifier("linePicker")
            .onChange(of: selectedLine) { _, newValue in
                selectedStop = nil
                filterText = ""
                if let line = newValue, let feedUrl = lines[line] {
                    viewModel.getStops(feedUrl: feedUrl)
                } else {
                    viewModel.reset()
                }
            }

            if let selected = selectedStop {
                SelectedStopRow(label: "Stop", name: selected.name) {
                    selectedStop = nil
                    if viewModel.state == .idle, let selectedLine, let feedUrl = lines[selectedLine] {
                        viewModel.getStops(feedUrl: feedUrl)
                    }
                }
            } else {
                ResultsArea {
                    switch viewModel.state {
                    case let .data(results):
                        let filtered = filterText.isNotEmpty
                            ? results.filter { $0.name.localizedCaseInsensitiveContains(filterText) }
                            : results
                        VStack(spacing: 4) {
                            TextField("Filter stops", text: $filterText)
                                .padding(.bottom, 6)
                                .accessibilityIdentifier("stopFilterField")
                            Divider()
                            if filtered.isEmpty {
                                Text("No matching stops")
                                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                            } else {
                                List(filtered, id: \.self, selection: $selectedStop) { result in
                                    Text(result.name)
                                }
                                .listStyle(PlainListStyle())
                                .accessibilityIdentifier("searchResultsList")
                            }
                        }
                    case .idle:
                        Text("Select a line")
                    case .empty:
                        Text("No stops found")
                    case .error:
                        Text("Failed to load stops")
                    case .loading:
                        LoadingSpinner()
                    }
                }
            }
        }
        .onAppear {
            coordinator.onSave = {
                if let selectedLine, let feedUrl = lines[selectedLine], let selectedStop {
                    viewModel.save(lineUrl: feedUrl, stopId: selectedStop.id, stopName: selectedStop.name)
                }
            }
            coordinator.canSave = isValid
        }
        .onChange(of: selectedLine) { _, _ in
            coordinator.canSave = isValid
        }
        .onChange(of: selectedStop) { _, _ in
            coordinator.canSave = isValid
        }
    }
}

@MainActor
private class MtaSettingsViewModel: ObservableObject {
    @Published var state: SettingsState = .idle

    private let fetcher = MacDI.shared.mtaSearch
    private let settings = MacDI.shared.settings
    private let scheduleUrl = Mta().SCHEDULE
    private var loadTask: Task<Void, Never>?

    deinit {
        loadTask?.cancel()
    }

    func reset() {
        loadTask?.cancel()
        state = .idle
    }

    func getStops(feedUrl: String) {
        loadTask?.cancel()
        state = .loading
        loadTask = Task {
            do {
                let result = try await fetcher.getStops(feedUrl: feedUrl)
                if !Task.isCancelled {
                    state = result.isEmpty ? .empty : .data(result)
                }
            } catch {
                if !Task.isCancelled {
                    state = .error
                }
            }
        }
    }

    func save(lineUrl: String, stopId: String, stopName: String) {
        settings.saveGtfsConfig(
            stopId: stopId,
            stopName: stopName,
            realtimeUrl: lineUrl,
            scheduleUrl: scheduleUrl,
            apiKey: "",
            apiKeyParam: ""
        )
    }
}

#Preview {
    Form {
        MtaSettingsView()
    }
    .formStyle(.grouped)
    .environmentObject(SettingsCoordinator())
}
