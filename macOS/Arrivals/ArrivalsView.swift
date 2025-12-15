@preconcurrency import ArrivalsLib
import Combine
import Foundation
import SettingsAccess
import SwiftUI

struct ArrivalsView: View {
    @StateObject var viewModel = ArrivalsViewModel()
    @ObservedObject var popoverState: PopoverState
    @State private var timer: AnyCancellable?

    let onOpenSettings: () -> Void
    let onQuit: () -> Void

    private let metrics = DisplayMetrics.current

    var body: some View {
        let refresh = RefreshBehaviour(isLoading: viewModel.loading) {
            viewModel.load()
        }
        ZStack {
            switch viewModel.state {
            case .idle:
                ProgressView()
                    .scaleEffect(0.5)
            case let .error(message):
                MainDisplay(content: {
                    DotMatrixRow(leading: message, trailing: nil)
                }, footer: {
                    ControlFooter(text: nil,
                                  refresh: refresh,
                                  onOpenSettings: onOpenSettings,
                                  onQuit: onQuit)
                }, metrics: metrics)
            case let .data(arrivalsInfo):
                MainDisplay(content: {
                    VStack(spacing: 6) {
                        ForEach(arrivalsInfo.arrivals, id: \.id) { arrival in
                            DotMatrixRow(leading: arrival.destination, trailing: arrival.time,
                                         animateTrailing: arrival.secondsToStop < 60)
                        }
                    }
                }, footer: {
                    ControlFooter(text: arrivalsInfo.station,
                                  refresh: refresh,
                                  onOpenSettings: onOpenSettings,
                                  onQuit: onQuit)
                }, metrics: metrics)
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

private struct MainDisplay<Content: View, Footer: View>: View {
    @ViewBuilder var content: Content
    @ViewBuilder var footer: Footer
    var metrics: DisplayMetrics

    var body: some View {
        VStack(spacing: 0) {
            ZStack { content }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding(8)
                .background(Color.black)
                .cornerRadius(metrics.cornerRadius)
            footer
        }
    }
}

private struct ControlFooter: View {
    var text: String?
    var refresh: RefreshBehaviour
    let onOpenSettings: () -> Void
    let onQuit: () -> Void

    var body: some View {
        HStack(spacing: 2) {
            if let text {
                Text(text)
                    .font(.footnote)
                    .foregroundColor(Color.yellow)
                    .padding(.leading, 2)
            }
            Spacer()
            SettingsLink {
                Image(systemName: "gearshape.circle.fill")
                    .foregroundColor(Color.yellow)
            } preAction: {
                onOpenSettings()
            } postAction: {}
            Button {
                refresh.onRefresh()
            } label: {
                Image(systemName: "arrow.clockwise.circle.fill")
                    .foregroundColor(refresh.isLoading ? Color.gray : Color.yellow)
            }.disabled(refresh.isLoading)
            Button {
                onQuit()
            } label: {
                Image(systemName: "x.circle.fill")
                    .foregroundColor(Color.yellow)
            }
        }
        .buttonStyle(PlainButtonStyle())
        .padding(.bottom, 2)
        .frame(height: 28)
    }
}

private struct DotMatrixRow: View {
    var leading: String
    var trailing: String?
    var animateTrailing: Bool = false

    var body: some View {
        HStack {
            DotMatrixText(text: leading)
            Spacer()
            if let trailing {
                DotMatrixText(text: trailing)
                    .blinking(enabled: animateTrailing)
            }
        }
    }
}

private struct DotMatrixText: View {
    var text: String

    var body: some View {
        Text(text)
            .font(.custom("LondonUnderground", size: 14))
            .foregroundColor(.yellow)
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

#Preview {
    ArrivalsView(popoverState: PopoverState(),
                 onOpenSettings: {},
                 onQuit: {})
}
