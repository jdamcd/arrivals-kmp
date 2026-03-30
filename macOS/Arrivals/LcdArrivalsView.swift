@preconcurrency import ArrivalsLib
import SwiftUI

extension Color {
    static let lcdBackground = Color(red: 0.04, green: 0.07, blue: 0.10)
    static let lcdRow = Color(red: 0.11, green: 0.19, blue: 0.27)
    static let lcdBadgeFallback = Color(red: 0.50, green: 0.51, blue: 0.51)
}

struct LcdContent: View {
    var arrivals: [Arrival]
    @State private var showingThird = false
    @State private var secondRowVisible = true
    @State private var cycleTask: Task<Void, Never>?

    private let cycleInterval: TimeInterval = 15
    private let fadeDuration: TimeInterval = 0.8

    private var hasThird: Bool { arrivals.count >= 3 }

    private let rowSpacing: CGFloat = 4

    private let verticalPadding: CGFloat = 2

    var body: some View {
        GeometryReader { geo in
            let rowHeight = (geo.size.height - rowSpacing - verticalPadding * 2) / 2
            VStack(spacing: rowSpacing) {
                if let first = arrivals.first {
                    LcdArrivalRow(position: 1, arrival: first)
                        .frame(height: rowHeight)
                }
                if hasThird {
                    AlternatingRow(
                        second: arrivals[1],
                        third: arrivals[2],
                        showingThird: showingThird,
                        visible: secondRowVisible
                    )
                    .frame(height: rowHeight)
                } else if arrivals.count >= 2 {
                    LcdArrivalRow(position: 2, arrival: arrivals[1])
                        .frame(height: rowHeight)
                } else {
                    Spacer()
                        .frame(height: rowHeight)
                }
            }
            .padding(.vertical, verticalPadding)
        }
        .onAppear { resetAnimation() }
        .onDisappear { cancelCycle() }
        .onChange(of: arrivals.map(\.id)) { _, _ in resetAnimation() }
        .onReceive(
            Timer.publish(every: cycleInterval, on: .main, in: .common).autoconnect()
        ) { _ in
            guard hasThird else { return }
            startCycle()
        }
    }

    private func startCycle() {
        cycleTask?.cancel()
        cycleTask = Task { @MainActor in
            withAnimation(.easeOut(duration: fadeDuration)) {
                secondRowVisible = false
            }
            try? await Task.sleep(for: .milliseconds(Int(fadeDuration * 1000)))
            guard !Task.isCancelled else { return }
            showingThird.toggle()
            withAnimation(.easeIn(duration: fadeDuration)) {
                secondRowVisible = true
            }
        }
    }

    private func resetAnimation() {
        cancelCycle()
        showingThird = false
        secondRowVisible = true
    }

    private func cancelCycle() {
        cycleTask?.cancel()
        cycleTask = nil
    }
}

private struct AlternatingRow: View {
    var second: Arrival
    var third: Arrival
    var showingThird: Bool
    var visible: Bool

    var body: some View {
        ZStack {
            if showingThird {
                LcdArrivalRow(position: 3, arrival: third)
            } else {
                LcdArrivalRow(position: 2, arrival: second)
            }
        }
        .opacity(visible ? 1 : 0)
    }
}

private struct LcdArrivalRow: View {
    var position: Int
    var arrival: Arrival

    var body: some View {
        HStack(spacing: 0) {
            Text("\(position)")
                .font(.custom("HelveticaNeue-Medium", size: 15))
                .foregroundColor(.white.opacity(0.5))
                .frame(width: 18, alignment: .center)

            HStack(spacing: 4) {
                if let line = arrival.line {
                    LineBadge(line: line, colorHex: arrival.lineColor)
                }

                Text(arrival.destination)
                    .font(.custom("HelveticaNeue-Medium", size: 15))
                    .foregroundColor(arrival.isDue ? .lcdRow : .white)
                    .lineLimit(1)

                Spacer()

                MinutesDisplay(arrival: arrival)
                    .padding(.trailing, 4)
            }
            .padding(.horizontal, 6)
            .frame(maxHeight: .infinity)
            .background(Rectangle().fill(arrival.isDue ? Color.white : .lcdRow))
        }
    }
}

struct LcdErrorContent: View {
    var message: String

    var body: some View {
        Text(message)
            .font(.custom("HelveticaNeue-Medium", size: 15))
            .foregroundColor(.white)
            .accessibilityIdentifier("errorMessage")
    }
}

private struct LineBadge: View {
    var line: String
    var colorHex: String?

    var body: some View {
        ZStack {
            Circle()
                .fill(colorHex.map { Color(hex: $0) } ?? .lcdBadgeFallback)
            Text(line)
                .font(.custom("HelveticaNeue-Medium", size: line.count > 1 ? 10 : 18))
                .foregroundColor(textColor)
        }
        .frame(width: 26, height: 26)
    }

    private var textColor: Color {
        colorHex.map { Color.contrastingTextColor(forHex: $0) } ?? .white
    }
}

private struct MinutesDisplay: View {
    var arrival: Arrival

    var body: some View {
        VStack(spacing: -2) {
            Text(minutesText)
                .font(.custom("HelveticaNeue-Medium", size: 17))
                .foregroundColor(arrival.isDue ? .lcdRow : .white)
            Text("MIN")
                .font(.custom("HelveticaNeue-Medium", size: 6))
                .foregroundColor(arrival.isDue ? .lcdRow.opacity(0.6) : .white.opacity(0.6))
        }
    }

    // TODO: move to minutesToStop in Arrivals.kt
    private var minutesText: String {
        "\(max(0, arrival.secondsToStop / 60))"
    }
}
