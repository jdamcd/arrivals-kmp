@preconcurrency import ArrivalsLib
import SwiftUI

@MainActor
class StopSearchViewModel: ObservableObject {
    @Published var state: SettingsState = .idle

    let settings = MacDI().settings

    private let searchStops: (String) async throws -> [StopResult]

    init(search: @escaping (String) async throws -> [StopResult]) {
        searchStops = search
    }

    func reset() {
        state = .idle
    }

    func performSearch(_ query: String) {
        state = .loading
        Task {
            do {
                let result = try await searchStops(query)
                state = result.isEmpty ? .empty : .data(result)
            } catch {
                state = .error
            }
        }
    }
}
