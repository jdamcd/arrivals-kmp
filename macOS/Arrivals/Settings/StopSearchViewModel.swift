@preconcurrency import ArrivalsLib
import SwiftUI

@MainActor
class StopSearchViewModel: ObservableObject {
    @Published var state: SettingsState = .idle

    let settings = MacDI.shared.settings

    private let searchStops: (String) async throws -> [StopResult]
    private var loadTask: Task<Void, Never>?

    init(search: @escaping (String) async throws -> [StopResult]) {
        searchStops = search
    }

    deinit {
        loadTask?.cancel()
    }

    func reset() {
        loadTask?.cancel()
        state = .idle
    }

    func performSearch(_ query: String) {
        load { [searchStops] in try await searchStops(query) }
    }

    // One task for everything that writes `state`, so a reset or new search
    // can't be overwritten by a slower load that started before it
    func load(_ fetch: @escaping () async throws -> [StopResult]) {
        loadTask?.cancel()
        state = .loading
        loadTask = Task {
            do {
                let result = try await fetch()
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
