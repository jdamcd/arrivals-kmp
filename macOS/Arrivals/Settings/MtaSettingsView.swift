@preconcurrency import ArrivalsLib
import SwiftUI

struct MtaSettingsView: View {
    @StateObject private var viewModel = MtaSettingsViewModel()

    @State private var selectedLine: String?
    @State private var selectedStop: StopResult?

    private var lines = Mta().realtime

    init() {
        selectedLine = lines.keys.sorted().first
    }

    var body: some View {
        Section {
            Picker("Line", selection: $selectedLine) {
                Text("--").tag(String?.none)
                ForEach(lines.keys.sorted(), id: \.self) { line in
                    Text(line).tag(String?.some(line))
                }
                .pickerStyle(.menu)
                .onChange(of: selectedLine ?? "") { newValue in
                    selectedStop = nil
                    if newValue.isNotEmpty, let feedUrl = lines[newValue] {
                        viewModel.getStops(feedUrl: feedUrl)
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
            HStack {
                Spacer()
                Button("Save") {
                    if let selectedLine = selectedLine,
                       let lineUrl = lines[selectedLine],
                       let selectedStop = selectedStop {
                        viewModel.save(lineUrl: lineUrl, stopId: selectedStop.id)
                        NSApp.keyWindow?.close()
                    }
                }
                .disabled(selectedLine == nil || selectedStop == nil)
                .buttonStyle(.borderedProminent)
            }
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
