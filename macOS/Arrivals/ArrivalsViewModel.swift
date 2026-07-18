@preconcurrency import ArrivalsLib
import Foundation

@MainActor
class ArrivalsViewModel: ObservableObject {
    @Published var state: ArrivalsState = .idle
    @Published var loading = false

    private let fetcher: ArrivalsLib.Arrivals

    init(fetcher: ArrivalsLib.Arrivals = MacDI.shared.arrivals) {
        self.fetcher = fetcher
    }

    func load() {
        if !loading {
            loading = true
            Task {
                do {
                    let result = try await fetcher.latest(count: 3)
                    state = .data(result)
                } catch {
                    let message = (error as NSError).localizedDescription
                    state = .error(message)
                }
                loading = false
            }
        }
    }
}

enum ArrivalsState: Equatable {
    case idle
    case data(ArrivalsInfo)
    case error(String)
}
