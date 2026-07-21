@preconcurrency import ArrivalsLib
import SwiftUI

struct GtfsFeedSettingsView: View {
    @EnvironmentObject var coordinator: SettingsCoordinator

    @StateObject private var viewModel: GtfsFeedSettingsViewModel

    @State private var selectedStop: StopResult?
    @State private var filterText: String = ""

    private var isValid: Bool {
        selectedStop != nil
    }

    init(fetcher: GtfsSearch, feedUrl: String, preselected: StopResult?, save: @escaping (String, String) -> Void) {
        _viewModel = StateObject(wrappedValue: GtfsFeedSettingsViewModel(
            fetcher: fetcher,
            feedUrl: feedUrl,
            save: save
        ))
        _selectedStop = State(initialValue: preselected)
    }

    var body: some View {
        Section {
            if let selected = selectedStop {
                SelectedStopRow(label: "Stop", name: selected.name) {
                    selectedStop = nil
                    if viewModel.state == .idle {
                        viewModel.getStops()
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
                        Text("Loading stops...")
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
            if selectedStop == nil {
                viewModel.getStops()
            }
            coordinator.onSave = {
                if let selectedStop {
                    viewModel.save(stopId: selectedStop.id, stopName: selectedStop.name)
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
    private let onSave: (String, String) -> Void
    private var loadTask: Task<Void, Never>?

    init(fetcher: GtfsSearch, feedUrl: String, save: @escaping (String, String) -> Void) {
        self.fetcher = fetcher
        self.feedUrl = feedUrl
        onSave = save
    }

    deinit {
        loadTask?.cancel()
    }

    func getStops() {
        if state != .loading {
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
    }

    func save(stopId: String, stopName: String) {
        onSave(stopId, stopName)
    }
}
