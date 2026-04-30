@preconcurrency import ArrivalsLib
import SwiftUI

@MainActor
class StopSearchViewModel: ObservableObject {
    @Published var state: SettingsState = .idle

    let settings = MacDI.shared.settings

    private let searchStops: (String) async throws -> [StopResult]
    private var searchTask: Task<Void, Never>?

    init(search: @escaping (String) async throws -> [StopResult]) {
        searchStops = search
    }

    deinit {
        searchTask?.cancel()
    }

    func reset() {
        searchTask?.cancel()
        state = .idle
    }

    func performSearch(_ query: String) {
        searchTask?.cancel()
        state = .loading
        searchTask = Task {
            do {
                let result = try await searchStops(query)
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
