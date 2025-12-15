@preconcurrency import ArrivalsLib
import SwiftUI

struct MtaSettingsView: View {
    @EnvironmentObject var coordinator: SettingsCoordinator

    @StateObject private var viewModel = MtaSettingsViewModel()

    @State private var selectedLine: String?
    @State private var selectedStop: StopResult?

    private var lines = Mta().realtime

    private var isValid: Bool {
        selectedLine != nil && selectedStop != nil
    }

    init() {
        selectedLine = lines.keys.sorted().first!
    }

    var body: some View {
        Section {
            Picker("Line", selection: $selectedLine) {
                Text("--").tag(String?.none)
                ForEach(lines.keys.sorted(), id: \.self) { line in
                    Text(line).tag(String?.some(line))
                }
                .pickerStyle(.menu)
                .onChange(of: selectedLine ?? "") { _, newValue in
                    selectedStop = nil
                    if newValue.isNotEmpty {
                        viewModel.getStops(feedUrl: lines[newValue]!)
                    } else {
                        viewModel.reset()
                    }
                }
            }
            ResultsArea {
                switch viewModel.state {
                case let .data(results):
                    List(results, id: \.self, selection: $selectedStop) { result in
                        Text(result.name)
                    }
                    .listStyle(PlainListStyle())
                case .idle:
                    Text("Select a line")
                case .error:
                    Text("Error fetching stops")
                case .loading:
                    ProgressView()
                        .scaleEffect(0.5)
                }
            }
        }
        .onAppear {
            coordinator.onSave = {
                if let selectedLine, let selectedStop {
                    viewModel.save(lineUrl: lines[selectedLine]!, stopId: selectedStop.id)
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

    private let fetcher = MacDI().gtfsSearch
    private let settings = MacDI().settings

    func reset() {
        state = .idle
    }

    func getStops(feedUrl: String) {
        if state != .loading {
            state = .loading
            Task {
                do {
                    let result = try await fetcher.getStops(feedUrl: feedUrl)
                    state = .data(result)
                } catch {
                    state = .error
                }
            }
        }
    }

    func save(lineUrl: String, stopId: String) {
        settings.gtfsRealtime = lineUrl
        settings.gtfsSchedule = Mta().SCHEDULE
        settings.gtfsStop = stopId
        settings.gtfsStopsUpdated = 0
        settings.mode = SettingsConfig().MODE_GTFS
    }
}

private enum SettingsState: Equatable {
    case idle
    case loading
    case data([StopResult])
    case error
}

#Preview {
    Form {
        MtaSettingsView()
    }
    .formStyle(.grouped)
}
