@testable import Arrivals
@preconcurrency import ArrivalsLib
import XCTest

final class AccessibilityLabelTests: XCTestCase {
    func testBasicArrivalNoLine() {
        let arrival = Arrival(id: 1, destination: "Brighton Beach", secondsToStop: 480, realtime: true, line: nil, lineBadge: nil)
        XCTAssertEqual(arrival.accessibilityLabel(position: 1), "Position 1, Brighton Beach, 8 minutes")
    }

    func testArrivalWithLine() {
        let arrival = Arrival(id: 1, destination: "Brighton Beach", secondsToStop: 480, realtime: true, line: "B", lineBadge: nil)
        XCTAssertEqual(arrival.accessibilityLabel(position: 1), "Position 1, B to Brighton Beach, 8 minutes")
    }

    func testDueArrival() {
        let arrival = Arrival(id: 1, destination: "Brighton Beach", secondsToStop: 30, realtime: true, line: nil, lineBadge: nil)
        XCTAssertEqual(arrival.accessibilityLabel(position: 1), "Position 1, Brighton Beach, due")
    }

    func testSingularMinute() {
        let arrival = Arrival(id: 1, destination: "Brighton Beach", secondsToStop: 60, realtime: true, line: nil, lineBadge: nil)
        XCTAssertEqual(arrival.accessibilityLabel(position: 1), "Position 1, Brighton Beach, 1 minute")
    }

    func testScheduledArrival() {
        let arrival = Arrival(id: 1, destination: "Brighton Beach", secondsToStop: 480, realtime: false, line: nil, lineBadge: nil)
        XCTAssertEqual(arrival.accessibilityLabel(position: 1), "Position 1, Brighton Beach, 8 minutes, scheduled")
    }

    func testExpressBadge() {
        let badge = LineBadge(label: "F", color: "FF6319", textColor: nil, express: true)
        let arrival = Arrival(id: 1, destination: "Coney Island", secondsToStop: 480, realtime: true, line: "F", lineBadge: badge)
        XCTAssertEqual(arrival.accessibilityLabel(position: 2), "Position 2, express, F to Coney Island, 8 minutes")
    }

    func testExpressScheduledCombined() {
        let badge = LineBadge(label: "F", color: "FF6319", textColor: nil, express: true)
        let arrival = Arrival(id: 1, destination: "Coney Island", secondsToStop: 420, realtime: false, line: "F", lineBadge: badge)
        XCTAssertEqual(arrival.accessibilityLabel(position: 3), "Position 3, express, F to Coney Island, 7 minutes, scheduled")
    }

    func testNonExpressBadgeOmitsExpress() {
        let badge = LineBadge(label: "B", color: "FF6319", textColor: nil, express: false)
        let arrival = Arrival(id: 1, destination: "Brighton Beach", secondsToStop: 480, realtime: true, line: "B", lineBadge: badge)
        XCTAssertEqual(arrival.accessibilityLabel(position: 1), "Position 1, B to Brighton Beach, 8 minutes")
    }
}
