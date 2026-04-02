@preconcurrency import ArrivalsLib
import Combine
import Foundation
import SettingsAccess
import SwiftUI

struct ArrivalsView: View {
    @StateObject var viewModel = ArrivalsViewModel()
    @ObservedObject var popoverState: PopoverState
    @State private var timer: AnyCancellable?
    @AppStorage("displayStyle") private var displayStyle: DisplayStyle = .london

    let onOpenSettings: () -> Void
    let onQuit: () -> Void

    private let metrics = DisplayMetrics.current

    var body: some View {
        let theme = DisplayTheme.from(displayStyle)
        let refresh = RefreshBehaviour(isLoading: viewModel.loading) {
            viewModel.load()
        }
        ZStack {
            switch viewModel.state {
            case .idle:
                ProgressView()
                    .scaleEffect(0.5)
            case let .error(message):
                ContentDisplay(theme: theme, metrics: metrics, content: {
                    if displayStyle == .nyc {
                        LcdErrorContent(message: message)
                    } else {
                        LedErrorContent(message: message)
                    }
                }, footer: {
                    ControlFooter(tint: theme.tint,
                                  text: nil,
                                  refresh: refresh,
                                  onOpenSettings: onOpenSettings,
                                  onQuit: onQuit)
                })
            case let .data(arrivalsInfo):
                ContentDisplay(theme: theme, metrics: metrics, content: {
                    if displayStyle == .nyc {
                        LcdContent(arrivals: arrivalsInfo.arrivals)
                    } else {
                        LedContent(arrivals: arrivalsInfo.arrivals)
                    }
                }, footer: {
                    ControlFooter(tint: theme.tint,
                                  text: arrivalsInfo.station,
                                  refresh: refresh,
                                  onOpenSettings: onOpenSettings,
                                  onQuit: onQuit)
                })
            }
        }
        .padding(.horizontal, metrics.framePadding)
        .padding(.top, metrics.framePadding)
        .frame(width: 350, height: metrics.frameHeight)
        .onReceive(popoverState.$isShown) { isShown in
            if isShown {
                viewModel.load()
                startTimer()
            } else {
                stopTimer()
            }
        }
    }

    private func startTimer() {
        timer = Timer.publish(every: 60, on: .main, in: .common)
            .autoconnect()
            .sink { _ in
                viewModel.load()
            }
    }

    private func stopTimer() {
        timer?.cancel()
        timer = nil
    }
}

private struct DisplayTheme {
    let contentPadding: CGFloat
    let background: Color
    let borderColor: Color?
    let tint: Color

    static let led = DisplayTheme(contentPadding: 8, background: .black, borderColor: nil, tint: .yellow.opacity(0.8))
    static let lcd = DisplayTheme(contentPadding: 0, background: .lcdBackground, borderColor: .lcdBackground, tint: .white.opacity(0.6))

    static func from(_ style: DisplayStyle) -> DisplayTheme {
        switch style {
        case .london: .led
        case .nyc: .lcd
        }
    }
}

private struct ContentDisplay<Content: View, Footer: View>: View {
    var theme: DisplayTheme
    var metrics: DisplayMetrics
    @ViewBuilder var content: Content
    @ViewBuilder var footer: Footer

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: metrics.cornerRadius)
        VStack(spacing: 0) {
            ZStack { content }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding(theme.contentPadding)
                .background(theme.background)
                .clipShape(shape)
                .overlay(
                    theme.borderColor.map { color in
                        shape.strokeBorder(color, lineWidth: 2)
                    }
                )
            footer
        }
    }
}

private struct ControlFooter: View {
    var tint: Color
    var text: String?
    var refresh: RefreshBehaviour
    let onOpenSettings: () -> Void
    let onQuit: () -> Void

    private let disabledTint: Color = .gray

    var body: some View {
        HStack(spacing: 2) {
            if let text {
                Text(text)
                    .font(.footnote)
                    .foregroundColor(tint)
                    .padding(.leading, 2)
                    .accessibilityIdentifier("stationName")
            }
            Spacer()
            SettingsLink {
                Image(systemName: "gearshape.circle.fill")
                    .foregroundColor(tint)
            } preAction: {
                onOpenSettings()
            } postAction: {}
                .accessibilityIdentifier("settingsButton")
            Button {
                refresh.onRefresh()
            } label: {
                Image(systemName: "arrow.clockwise.circle.fill")
                    .foregroundColor(refresh.isLoading ? disabledTint : tint)
            }.disabled(refresh.isLoading)
                .accessibilityIdentifier("refreshButton")
            Button {
                onQuit()
            } label: {
                Image(systemName: "x.circle.fill")
                    .foregroundColor(tint)
            }
            .accessibilityIdentifier("quitButton")
        }
        .buttonStyle(PlainButtonStyle())
        .padding(.bottom, 2)
        .frame(height: 28)
    }
}

private struct RefreshBehaviour {
    var isLoading: Bool
    var onRefresh: () -> Void
}

struct DisplayMetrics {
    let framePadding: CGFloat
    let frameHeight: CGFloat
    let cornerRadius: CGFloat

    static let glass = DisplayMetrics(framePadding: 12, frameHeight: 118, cornerRadius: 10)

    static let legacy = DisplayMetrics(framePadding: 8, frameHeight: 110, cornerRadius: 4)

    static var current: DisplayMetrics {
        if #available(macOS 26.0, *) {
            glass
        } else {
            legacy
        }
    }
}

// MARK: - Previews

let tflArrivals = [
    Arrival(id: 1, destination: "New Cross", secondsToStop: 30, realtime: true, line: nil, lineBadge: tflBadge),
    Arrival(id: 2, destination: "Crystal Palace", secondsToStop: 450, realtime: true, line: nil, lineBadge: tflBadge),
    Arrival(id: 3, destination: "Clapham Junction", secondsToStop: 900, realtime: true, line: nil, lineBadge: tflBadge),
]
let tflBadge = LineBadge(label: "WIN", color: "D22730", textColor: nil, express: false)

let mtaArrivals = [
    Arrival(id: 1, destination: "Brighton Beach", secondsToStop: 70, realtime: true, line: "B", lineBadge: LineBadge(label: "B", color: "FF6319", textColor: nil, express: false)),
    Arrival(id: 2, destination: "Coney Island-Stillwell Av", secondsToStop: 506, realtime: true, line: "F", lineBadge: LineBadge(label: "F", color: "FF6319", textColor: nil, express: true)),
    Arrival(id: 3, destination: "Coney Island-Stillwell Av", secondsToStop: 956, realtime: true, line: "F", lineBadge: LineBadge(label: "F", color: "FF6319", textColor: nil, express: false)),
]

let edgeCaseArrivals = [
    Arrival(id: 1, destination: "Dest", secondsToStop: 0, realtime: true, line: "A", lineBadge: nil),
    Arrival(id: 2, destination: "Very very very long destination name", secondsToStop: 60, realtime: true, line: "F", lineBadge: LineBadge(label: "M", color: "FF3399", textColor: nil, express: true)),
    Arrival(id: 3, destination: "Scheduled", secondsToStop: 956, realtime: false, line: nil, lineBadge: LineBadge(label: "F", color: "ffffff", textColor: "000000", express: false)),
]

let previewError = "Error: long multi-line error message to test wrapping"

@MainActor func previewLed(arrivals: [Arrival] = [], error: String? = nil, station: String? = nil, metrics: DisplayMetrics = .glass) -> some View {
    ContentDisplay(theme: .led, metrics: metrics, content: {
        if let error { LedErrorContent(message: error) } else { LedContent(arrivals: arrivals) }
    }, footer: {
        ControlFooter(tint: DisplayTheme.led.tint, text: station,
                      refresh: RefreshBehaviour(isLoading: false) {},
                      onOpenSettings: {}, onQuit: {})
    })
    .padding(.horizontal, metrics.framePadding)
    .padding(.top, metrics.framePadding)
    .frame(width: 350, height: metrics.frameHeight)
}

@MainActor func previewLcd(arrivals: [Arrival] = [], error: String? = nil, station: String? = nil, metrics: DisplayMetrics = .glass) -> some View {
    ContentDisplay(theme: .lcd, metrics: metrics, content: {
        if let error { LcdErrorContent(message: error) } else { LcdContent(arrivals: arrivals) }
    }, footer: {
        ControlFooter(tint: DisplayTheme.lcd.tint, text: station,
                      refresh: RefreshBehaviour(isLoading: false) {},
                      onOpenSettings: {}, onQuit: {})
    })
    .padding(.horizontal, metrics.framePadding)
    .padding(.top, metrics.framePadding)
    .frame(width: 350, height: metrics.frameHeight)
}

#Preview("TfL (LED)") {
    previewLed(arrivals: tflArrivals, station: "Shoreditch High Street: Platform 2")
}

#Preview("TfL (LCD)") {
    previewLcd(arrivals: tflArrivals, station: "Shoreditch High Street: Platform 2")
}

#Preview("MTA (LED)") {
    previewLed(arrivals: mtaArrivals, station: "42 St-Bryant Park")
}

#Preview("MTA (LCD)") {
    previewLcd(arrivals: mtaArrivals, station: "42 St-Bryant Park")
}

#Preview("Edge cases (LED)") {
    previewLed(arrivals: edgeCaseArrivals, station: nil)
}

#Preview("Edge cases (LCD)") {
    previewLcd(arrivals: edgeCaseArrivals, station: nil)
}

#Preview("Legacy (LED)") {
    previewLed(arrivals: mtaArrivals, station: "42 St-Bryant Park", metrics: .legacy)
}

#Preview("Legacy (LCD)") {
    previewLcd(arrivals: mtaArrivals, station: "42 St-Bryant Park", metrics: .legacy)
}

#Preview("Error (LED)") {
    previewLed(error: previewError)
}

#Preview("Error (LCD)") {
    previewLcd(error: previewError)
}
