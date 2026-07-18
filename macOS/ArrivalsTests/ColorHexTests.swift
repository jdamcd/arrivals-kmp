import AppKit
@testable import Arrivals
import SwiftUI
import XCTest

final class ColorHexTests: XCTestCase {
    func testParsesSixDigitHex() {
        assertRgb(Color(hex: "FF8000"), red: 1.0, green: 0.5, blue: 0.0)
    }

    func testStripsLeadingHashAndSurroundingWhitespace() {
        assertRgb(Color(hex: " #00FF00 "), red: 0.0, green: 1.0, blue: 0.0)
    }

    func testMalformedInputDoesNotCrashAndFallsBackToBlack() {
        for input in ["", "nothex", "#zzzzzz"] {
            assertRgb(Color(hex: input), red: 0.0, green: 0.0, blue: 0.0)
        }
    }

    private func assertRgb(
        _ color: Color,
        red: CGFloat,
        green: CGFloat,
        blue: CGFloat,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        guard let ns = NSColor(color).usingColorSpace(.sRGB) else {
            return XCTFail("Could not resolve color components", file: file, line: line)
        }
        XCTAssertEqual(ns.redComponent, red, accuracy: 0.02, file: file, line: line)
        XCTAssertEqual(ns.greenComponent, green, accuracy: 0.02, file: file, line: line)
        XCTAssertEqual(ns.blueComponent, blue, accuracy: 0.02, file: file, line: line)
    }
}
