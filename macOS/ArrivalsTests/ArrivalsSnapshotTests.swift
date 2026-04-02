import AppKit
@testable import Arrivals
import SnapshotTesting
import SwiftUI
import XCTest

@MainActor
final class ArrivalsSnapshotTests: XCTestCase {
    private let strategy: Snapshotting<NSViewController, NSImage> = .image(perceptualPrecision: 0.98)

    override func invokeTest() {
        withSnapshotTesting(record: .missing) {
            super.invokeTest()
        }
    }

    func testTflLed() {
        assertSnapshot(of: host(previewLed(arrivals: tflArrivals, station: "Shoreditch High Street: Platform 2")), as: strategy)
    }

    func testTflLcd() {
        assertSnapshot(of: host(previewLcd(arrivals: tflArrivals, station: "Shoreditch High Street: Platform 2")), as: strategy)
    }

    func testMtaLed() {
        assertSnapshot(of: host(previewLed(arrivals: mtaArrivals, station: "42 St-Bryant Park")), as: strategy)
    }

    func testMtaLcd() {
        assertSnapshot(of: host(previewLcd(arrivals: mtaArrivals, station: "42 St-Bryant Park")), as: strategy)
    }

    func testEdgeCasesLed() {
        assertSnapshot(of: host(previewLed(arrivals: edgeCaseArrivals)), as: strategy)
    }

    func testEdgeCasesLcd() {
        assertSnapshot(of: host(previewLcd(arrivals: edgeCaseArrivals)), as: strategy)
    }

    func testLegacyLed() {
        assertSnapshot(of: host(previewLed(arrivals: mtaArrivals, station: "42 St-Bryant Park", metrics: .legacy), metrics: .legacy), as: strategy)
    }

    func testLegacyLcd() {
        assertSnapshot(of: host(previewLcd(arrivals: mtaArrivals, station: "42 St-Bryant Park", metrics: .legacy), metrics: .legacy), as: strategy)
    }

    func testErrorLed() {
        assertSnapshot(of: host(previewLed(error: previewError)), as: strategy)
    }

    func testErrorLcd() {
        assertSnapshot(of: host(previewLcd(error: previewError)), as: strategy)
    }

    private var scaleWindow: NSWindow?

    private func host(_ view: some View, metrics: DisplayMetrics = .glass) -> NSViewController {
        let size = CGSize(width: 350, height: metrics.frameHeight)
        let controller = NSHostingController(rootView: view.background(Color(white: 0.2)))
        controller.view.frame = CGRect(origin: .zero, size: size)
        let window = ScaledWindow(scaleFactor: 1.0, contentRect: CGRect(origin: .zero, size: size))
        window.contentView = controller.view
        scaleWindow = window
        return controller
    }
}

private class ScaledWindow: NSWindow {
    private let scaleFactor: CGFloat

    init(scaleFactor: CGFloat, contentRect: NSRect) {
        self.scaleFactor = scaleFactor
        super.init(contentRect: contentRect, styleMask: [], backing: .nonretained, defer: false)
    }

    override var backingScaleFactor: CGFloat { scaleFactor }
}
