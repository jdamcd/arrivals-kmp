@testable import Arrivals
import XCTest

final class DebouncingTests: XCTestCase {
    private var model: DebouncingTextFieldModel!

    override func tearDown() {
        model = nil
        super.tearDown()
    }

    func testDelaysValuePropagation() {
        let expectation = expectation(description: "Debounced value should be delivered after delay")
        var receivedValue: String?
        model = DebouncingTextFieldModel(debounceInterval: 0.3) { value in
            receivedValue = value
            expectation.fulfill()
        }

        model.text = "test"

        XCTAssertNil(receivedValue, "Value should not be delivered immediately")

        wait(for: [expectation], timeout: 1.0)
        XCTAssertEqual(receivedValue, "test", "Value should be delivered after the debounce interval")
    }

    func testIgnoresRapidChanges() {
        let expectation = expectation(description: "Only the final value should be delivered")
        var receivedValues: [String] = []
        model = DebouncingTextFieldModel(debounceInterval: 0.3) { value in
            receivedValues.append(value)
            if value == "four" { expectation.fulfill() }
        }

        model.text = "one"
        model.text = "two"
        model.text = "three"
        model.text = "four"

        wait(for: [expectation], timeout: 1.0)

        XCTAssertEqual(receivedValues, ["four"], "Rapid changes should collapse to the final value")
    }

    @MainActor
    func testAllowsSlowChanges() {
        let expectation = expectation(description: "Both spaced-out values should be delivered")
        var receivedValues: [String] = []
        model = DebouncingTextFieldModel(debounceInterval: 0.2) { value in
            receivedValues.append(value)
            if receivedValues.count == 2 { expectation.fulfill() }
        }

        model.text = "one"

        // Wait longer than the debounce interval before sending the second value
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            self.model.text = "two"
        }

        wait(for: [expectation], timeout: 2.0)

        XCTAssertEqual(receivedValues, ["one", "two"], "Both values should be delivered when spaced out")
    }

    func testIgnoresDuplicateConsecutiveValues() {
        let expectation = expectation(description: "Duplicate value should be delivered once")
        expectation.assertForOverFulfill = false
        var receivedValues: [String] = []
        model = DebouncingTextFieldModel(debounceInterval: 0.2) { value in
            receivedValues.append(value)
            expectation.fulfill()
        }

        model.text = "repeat"
        model.text = "repeat"

        wait(for: [expectation], timeout: 1.0)

        XCTAssertEqual(receivedValues, ["repeat"], "Setting the same value twice should publish once")
    }
}
