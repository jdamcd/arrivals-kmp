import AppKit
@testable import Arrivals
import SnapshotTesting
import SwiftUI
import XCTest

@MainActor
final class ArrivalsSnapshotTests: XCTestCase {
    override func invokeTest() {
        withSnapshotTesting(record: .missing) {
            super.invokeTest()
        }
    }

    func testTflLed() {
        assertSnapshot(of: host(previewLed(arrivals: tflArrivals, station: "Shoreditch High Street: Platform 2")), as: .image)
    }

    func testTflLcd() {
        assertSnapshot(of: host(previewLcd(arrivals: tflArrivals, station: "Shoreditch High Street: Platform 2")), as: .image)
    }

    func testMtaLed() {
        assertSnapshot(of: host(previewLed(arrivals: mtaArrivals, station: "42 St-Bryant Park")), as: .image)
    }

    func testMtaLcd() {
        assertSnapshot(of: host(previewLcd(arrivals: mtaArrivals, station: "42 St-Bryant Park")), as: .image)
    }

    func testEdgeCasesLed() {
        assertSnapshot(of: host(previewLed(arrivals: edgeCaseArrivals)), as: .image)
    }

    func testEdgeCasesLcd() {
        assertSnapshot(of: host(previewLcd(arrivals: edgeCaseArrivals)), as: .image)
    }

    func testLegacyLed() {
        assertSnapshot(of: host(previewLed(arrivals: mtaArrivals, station: "42 St-Bryant Park", metrics: .legacy), metrics: .legacy), as: .image)
    }

    func testLegacyLcd() {
        assertSnapshot(of: host(previewLcd(arrivals: mtaArrivals, station: "42 St-Bryant Park", metrics: .legacy), metrics: .legacy), as: .image)
    }

    func testErrorLed() {
        assertSnapshot(of: host(previewLed(error: previewError)), as: .image)
    }

    func testErrorLcd() {
        assertSnapshot(of: host(previewLcd(error: previewError)), as: .image)
    }

    private func host(_ view: some View, metrics: DisplayMetrics = .glass) -> NSViewController {
        let controller = NSHostingController(rootView: view.background(Color(white: 0.2)))
        controller.view.frame = CGRect(origin: .zero, size: CGSize(width: 350, height: metrics.frameHeight))
        return controller
    }
}
