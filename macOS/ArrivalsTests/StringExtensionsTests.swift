@testable import Arrivals
import XCTest

final class StringExtensionsTests: XCTestCase {

    func testTrimRemovesWhitespace() {
        XCTAssertEqual("  test  ".trim(), "test")
        XCTAssertEqual("\ntest\n".trim(), "test")
        XCTAssertEqual(" \t\ntest\n\t ".trim(), "test")
    }

    func testTrimHandlesEdgeCases() {
        XCTAssertEqual("".trim(), "")
        XCTAssertEqual("   ".trim(), "")
        XCTAssertEqual("test".trim(), "test")
    }

    func testIsNotEmptyWithContent() {
        XCTAssertTrue("test".isNotEmpty)
        XCTAssertTrue("  test  ".isNotEmpty, "Content after trim should not be empty")
    }

    func testIsNotEmptyWithNoContent() {
        XCTAssertFalse("".isNotEmpty)
        XCTAssertFalse("   ".isNotEmpty, "Whitespace-only should be empty")
        XCTAssertFalse(" \t\n ".isNotEmpty, "Whitespace-only should be empty")
    }
}
