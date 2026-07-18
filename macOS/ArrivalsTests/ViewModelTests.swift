@testable import Arrivals
@preconcurrency import ArrivalsLib
import Combine
import XCTest

private final class FakeArrivals: ArrivalsLib.Arrivals, @unchecked Sendable {
    private let result: Result<ArrivalsInfo, Error>
    private(set) var callCount = 0

    init(_ result: Result<ArrivalsInfo, Error>) {
        self.result = result
    }

    func latest(count _: Int32) async throws -> ArrivalsInfo {
        callCount += 1
        return try result.get()
    }
}

@MainActor
final class ArrivalsViewModelTests: XCTestCase {
    func testLoadSuccessPublishesData() async {
        let info = ArrivalsInfo(station: "Shoreditch High Street", arrivals: [])
        let viewModel = ArrivalsViewModel(fetcher: FakeArrivals(.success(info)))

        viewModel.load()
        let state = await awaitState(viewModel) { if case .data = $0 { true } else { false } }

        XCTAssertEqual(state, .data(info))
        XCTAssertFalse(viewModel.loading)
    }

    func testLoadFailurePublishesError() async {
        let error = NSError(domain: "test", code: 1, userInfo: [NSLocalizedDescriptionKey: "No data"])
        let viewModel = ArrivalsViewModel(fetcher: FakeArrivals(.failure(error)))

        viewModel.load()
        let state = await awaitState(viewModel) { if case .error = $0 { true } else { false } }

        XCTAssertEqual(state, .error("No data"))
        XCTAssertFalse(viewModel.loading)
    }

    func testConcurrentLoadFetchesOnce() async {
        let fake = FakeArrivals(.success(ArrivalsInfo(station: "Shoreditch High Street", arrivals: [])))
        let viewModel = ArrivalsViewModel(fetcher: fake)

        viewModel.load()
        viewModel.load()
        _ = await awaitState(viewModel) { if case .data = $0 { true } else { false } }

        XCTAssertEqual(fake.callCount, 1)
    }

    private func awaitState(
        _ viewModel: ArrivalsViewModel,
        where predicate: @escaping (ArrivalsState) -> Bool
    ) async -> ArrivalsState {
        await withCheckedContinuation { continuation in
            var cancellable: AnyCancellable?
            cancellable = viewModel.$state.sink { state in
                if predicate(state) {
                    cancellable?.cancel()
                    continuation.resume(returning: state)
                }
            }
        }
    }
}

@MainActor
final class StopSearchViewModelTests: XCTestCase {
    func testSearchWithResultsPublishesData() async {
        let results = [StopResult(id: "1", name: "Shoreditch", isHub: false)]
        let viewModel = StopSearchViewModel { _ in results }

        viewModel.performSearch("shore")
        let state = await awaitState(viewModel) { $0 != .idle && $0 != .loading }

        XCTAssertEqual(state, .data(results))
    }

    func testSearchWithNoResultsPublishesEmpty() async {
        let viewModel = StopSearchViewModel { _ in [] }

        viewModel.performSearch("nowhere")
        let state = await awaitState(viewModel) { $0 != .idle && $0 != .loading }

        XCTAssertEqual(state, .empty)
    }

    func testSearchFailurePublishesError() async {
        let viewModel = StopSearchViewModel { _ in throw NSError(domain: "test", code: 1) }

        viewModel.performSearch("boom")
        let state = await awaitState(viewModel) { $0 != .idle && $0 != .loading }

        XCTAssertEqual(state, .error)
    }

    func testResetReturnsToIdle() {
        let viewModel = StopSearchViewModel { _ in [] }

        viewModel.performSearch("shore")
        XCTAssertEqual(viewModel.state, .loading)

        viewModel.reset()
        XCTAssertEqual(viewModel.state, .idle)
    }

    private func awaitState(
        _ viewModel: StopSearchViewModel,
        where predicate: @escaping (SettingsState) -> Bool
    ) async -> SettingsState {
        await withCheckedContinuation { continuation in
            var cancellable: AnyCancellable?
            cancellable = viewModel.$state.sink { state in
                if predicate(state) {
                    cancellable?.cancel()
                    continuation.resume(returning: state)
                }
            }
        }
    }
}
