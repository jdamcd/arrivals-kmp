import AppKit
@testable import Arrivals
@preconcurrency import ArrivalsLib
import SnapshotTesting
import SwiftUI
import XCTest

@MainActor
final class ArrivalsSnapshotTests: XCTestCase {
    private let strategy: Snapshotting<NSViewController, NSImage> = .image(precision: 0.8, perceptualPrecision: 0.9)

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

@MainActor
final class SettingsSnapshotTests: XCTestCase {
    private let strategy: Snapshotting<NSView, NSImage> = .image(precision: 0.8, perceptualPrecision: 0.9)
    private var scaleWindow: NSWindow?

    override func setUp() {
        super.setUp()
        // Koin is initialized by the host ArrivalsApp.init(), so tests don't need to start it
        UserDefaults.standard.removeObject(forKey: TransitSystem.storageKey)
        UserDefaults.standard.removeObject(forKey: DisplayStyle.storageKey)
        UserDefaults(suiteName: SettingsConfig.shared.STORE_NAME)?.removePersistentDomain(forName: SettingsConfig.shared.STORE_NAME)
    }

    override func invokeTest() {
        withSnapshotTesting(record: .missing) {
            super.invokeTest()
        }
    }

    func testSettingsDefault() {
        assertSnapshot(of: settingsView(), as: strategy)
    }

    func testSettingsCustomGtfs() {
        UserDefaults.standard.set(TransitSystem.customGtfs.rawValue, forKey: TransitSystem.storageKey)
        assertSnapshot(of: settingsView(), as: strategy)
    }

    private func settingsView() -> NSView {
        // Window-content background gives .bordered (Cancel) button enough contrast to be visible
        let controller = NSHostingController(
            rootView: SettingsView().background(Color(NSColor.windowBackgroundColor))
        )
        let window = ScaledWindow(scaleFactor: 1.0, contentRect: NSRect(origin: .zero, size: NSSize(width: 2000, height: 2000)))
        window.contentViewController = controller
        controller.view.appearance = NSAppearance(named: .darkAqua)
        controller.view.layoutSubtreeIfNeeded()
        let natural = controller.view.fittingSize
        controller.view.frame = NSRect(origin: .zero, size: natural)
        scaleWindow = window
        return controller.view
    }
}
