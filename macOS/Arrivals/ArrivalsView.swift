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
                        shape.stroke(color, lineWidth: 2)
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

private struct DisplayMetrics {
    let framePadding: CGFloat
    let frameHeight: CGFloat
    let cornerRadius: CGFloat

    static var current: DisplayMetrics {
        if #available(macOS 26.0, *) {
            DisplayMetrics(framePadding: 12, frameHeight: 118, cornerRadius: 10)
        } else {
            DisplayMetrics(framePadding: 8, frameHeight: 110, cornerRadius: 4)
        }
    }
}

// MARK: - Previews

private let previewBadge = LineBadge(label: "G", color: "6CBE45", textColor: "FFFFFF", express: false)

private let previewArrivals = [
    Arrival(id: 1, destination: "Church Av", secondsToStop: 30,
            realtime: true, line: "G", lineBadge: previewBadge),
    Arrival(id: 2, destination: "Court Sq", secondsToStop: 300,
            realtime: true, line: "G", lineBadge: previewBadge),
    Arrival(id: 3, destination: "Church Av", secondsToStop: 720,
            realtime: true, line: "G", lineBadge: previewBadge),
]

private let previewMetrics = DisplayMetrics.current
private let legacyMetrics = DisplayMetrics(framePadding: 8, frameHeight: 110, cornerRadius: 4)

#Preview("LED") {
    ContentDisplay(theme: .led, metrics: previewMetrics, content: {
        LedContent(arrivals: previewArrivals)
    }, footer: {
        ControlFooter(tint: DisplayTheme.led.tint, text: "Nassau Av",
                      refresh: RefreshBehaviour(isLoading: false) {},
                      onOpenSettings: {}, onQuit: {})
    })
    .padding(.horizontal, previewMetrics.framePadding)
    .padding(.top, previewMetrics.framePadding)
    .frame(width: 350, height: previewMetrics.frameHeight)
}

#Preview("LCD") {
    ContentDisplay(theme: .lcd, metrics: previewMetrics, content: {
        LcdContent(arrivals: previewArrivals)
    }, footer: {
        ControlFooter(tint: DisplayTheme.lcd.tint, text: "Nassau Av",
                      refresh: RefreshBehaviour(isLoading: false) {},
                      onOpenSettings: {}, onQuit: {})
    })
    .padding(.horizontal, previewMetrics.framePadding)
    .padding(.top, previewMetrics.framePadding)
    .frame(width: 350, height: previewMetrics.frameHeight)
}

#Preview("LED Legacy") {
    ContentDisplay(theme: .led, metrics: legacyMetrics, content: {
        LedContent(arrivals: previewArrivals)
    }, footer: {
        ControlFooter(tint: DisplayTheme.led.tint, text: "Nassau Av",
                      refresh: RefreshBehaviour(isLoading: false) {},
                      onOpenSettings: {}, onQuit: {})
    })
    .padding(.horizontal, legacyMetrics.framePadding)
    .padding(.top, legacyMetrics.framePadding)
    .frame(width: 350, height: legacyMetrics.frameHeight)
}

#Preview("LCD Legacy") {
    ContentDisplay(theme: .lcd, metrics: legacyMetrics, content: {
        LcdContent(arrivals: previewArrivals)
    }, footer: {
        ControlFooter(tint: DisplayTheme.lcd.tint, text: "Nassau Av",
                      refresh: RefreshBehaviour(isLoading: false) {},
                      onOpenSettings: {}, onQuit: {})
    })
    .padding(.horizontal, legacyMetrics.framePadding)
    .padding(.top, legacyMetrics.framePadding)
    .frame(width: 350, height: legacyMetrics.frameHeight)
}
