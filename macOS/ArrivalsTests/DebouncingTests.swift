@testable import Arrivals
import Combine
import XCTest

final class DebouncingTests: XCTestCase {
    var cancellables: Set<AnyCancellable>!

    override func setUp() {
        super.setUp()
        cancellables = []
    }

    func testDebounceDelaysValuePropagation() {
        let expectation = expectation(description: "Debounced value should be received after delay")
        let publisher = PassthroughSubject<String, Never>()
        var receivedValue: String?

        publisher
            .debounce(for: .seconds(0.3), scheduler: DispatchQueue.main)
            .sink { value in
                receivedValue = value
                expectation.fulfill()
            }
            .store(in: &cancellables)

        publisher.send("test")

        XCTAssertNil(receivedValue, "Value should not be received immediately")

        wait(for: [expectation], timeout: 1.0)
        XCTAssertEqual(receivedValue, "test", "Value should be received after debounce delay")
    }

    func testDebounceIgnoresRapidChanges() {
        let expectation = expectation(description: "Only final value should be received")
        let publisher = PassthroughSubject<String, Never>()
        var receivedValues: [String] = []

        publisher
            .debounce(for: .seconds(0.3), scheduler: DispatchQueue.main)
            .sink { value in
                receivedValues.append(value)
                if value == "four" {
                    expectation.fulfill()
                }
            }
            .store(in: &cancellables)

        publisher.send("one")
        publisher.send("two")
        publisher.send("three")
        publisher.send("four")

        wait(for: [expectation], timeout: 1.0)

        XCTAssertEqual(receivedValues.count, 1, "Should only receive one value")
        XCTAssertEqual(receivedValues.first, "four", "Should receive the final value")
    }

    @MainActor
    func testDebounceAllowsSlowChanges() {
        let expectation = expectation(description: "All slow values should be received")
        let publisher = PassthroughSubject<String, Never>()
        var receivedValues: [String] = []

        publisher
            .debounce(for: .seconds(0.2), scheduler: DispatchQueue.main)
            .sink { value in
                receivedValues.append(value)
                if receivedValues.count == 2 {
                    expectation.fulfill()
                }
            }
            .store(in: &cancellables)

        publisher.send("one")

        // Wait longer than debounce interval before sending second value
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            publisher.send("two")
        }

        wait(for: [expectation], timeout: 2.0)

        XCTAssertEqual(receivedValues, ["one", "two"], "Both values should be received when spaced out")
    }

    func testDebounceHandlesEmptyStrings() {
        let expectation = expectation(description: "Empty string should be debounced")
        let publisher = PassthroughSubject<String, Never>()
        var receivedValue: String?

        publisher
            .debounce(for: .seconds(0.2), scheduler: DispatchQueue.main)
            .sink { value in
                receivedValue = value
                expectation.fulfill()
            }
            .store(in: &cancellables)

        publisher.send("")

        wait(for: [expectation], timeout: 1.0)
        XCTAssertEqual(receivedValue, "", "Empty string should be handled")
    }

    override func tearDown() {
        cancellables = nil
        super.tearDown()
    }
}
