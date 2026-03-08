@preconcurrency import ArrivalsLib
import SwiftUI

struct GtfsFeedSettingsView: View {
    @EnvironmentObject var coordinator: SettingsCoordinator

    @StateObject private var viewModel: GtfsFeedSettingsViewModel

    @State private var selectedStop: StopResult?

    private var isValid: Bool {
        selectedStop != nil
    }

    init(fetcher: GtfsSearch, feedUrl: String, save: @escaping (String) -> Void) {
        _viewModel = StateObject(wrappedValue: GtfsFeedSettingsViewModel(
            fetcher: fetcher,
            feedUrl: feedUrl,
            save: save
        ))
    }

    var body: some View {
        Section {
            ResultsArea {
                switch viewModel.state {
                case let .data(results):
                    List(results, id: \.self, selection: $selectedStop) { result in
                        Text(result.name)
                    }
                    .listStyle(PlainListStyle())
                case .idle:
                    Text("Loading stops...")
                case .error:
                    Text("Failed to load stops")
                case .loading:
                    ProgressView()
                        .scaleEffect(0.5)
                }
            }
        }
        .onAppear {
            viewModel.getStops()
            coordinator.onSave = {
                if let selectedStop {
                    viewModel.save(stopId: selectedStop.id)
                }
            }
            coordinator.canSave = isValid
        }
        .onChange(of: selectedStop) { _, _ in
            coordinator.canSave = isValid
        }
    }
}

@MainActor
private class GtfsFeedSettingsViewModel: ObservableObject {
    @Published var state: SettingsState = .idle

    private let fetcher: GtfsSearch
    private let feedUrl: String
    private let onSave: (String) -> Void

    init(fetcher: GtfsSearch, feedUrl: String, save: @escaping (String) -> Void) {
        self.fetcher = fetcher
        self.feedUrl = feedUrl
        onSave = save
    }

    func getStops() {
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

    func save(stopId: String) {
        onSave(stopId)
    }
}

private enum SettingsState: Equatable {
    case idle
    case loading
    case data([StopResult])
    case error
}
