@preconcurrency import ArrivalsLib
import SwiftUI

extension Color {
    static let lcdBackground = Color(red: 0.04, green: 0.07, blue: 0.10)
    static let lcdRow = Color(red: 0.11, green: 0.19, blue: 0.27)
    static let lcdBadgeFallback = Color(red: 0.49, green: 0.52, blue: 0.55)
}

extension Font {
    static let lcdFamilyName = "HelveticaNeue-Medium"

    static func lcd(size: CGFloat) -> Font {
        .custom(lcdFamilyName, size: size)
    }
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
                .font(.lcd(size: 15))
                .foregroundColor(.white.opacity(0.5))
                .frame(width: 18, alignment: .center)

            HStack(spacing: 6) {
                if let badge = arrival.lineBadge {
                    LineBadge(line: badge.label, colorHex: badge.color, textColorHex: badge.textColor, express: badge.express)
                }

                Text(arrival.destination)
                    .font(.lcd(size: 15))
                    .foregroundColor(arrival.isDue ? .lcdRow : .white)
                    .lineLimit(1)

                Spacer()

                MinutesDisplay(arrival: arrival)
                    .padding(.trailing, 4)
            }
            .padding(.horizontal, 6)
            .frame(maxHeight: .infinity)
            .background(Rectangle().fill(arrival.isDue ? .white.opacity(0.85) : .lcdRow))
        }
    }
}

struct LcdErrorContent: View {
    var message: String

    var body: some View {
        Text(message)
            .font(.lcd(size: 15))
            .foregroundColor(.white)
            .accessibilityIdentifier("errorMessage")
    }
}

private struct LineBadge: View {
    var line: String
    var colorHex: String?
    var textColorHex: String?
    var express: Bool = false

    var body: some View {
        ZStack {
            if express {
                Rectangle()
                    .fill(fillColor)
                    .frame(width: 22, height: 22)
                    .rotationEffect(.degrees(45))
            } else {
                Circle()
                    .fill(fillColor)
            }
            Text(line)
                .font(.lcd(size: line.count > 1 ? 10 : 18))
                .foregroundColor(textColor)
        }
        .frame(width: 26, height: 26)
    }

    private var fillColor: Color {
        colorHex.map { Color(hex: $0) } ?? .lcdBadgeFallback
    }

    private var textColor: Color {
        textColorHex.map { Color(hex: $0) } ?? .white
    }
}

private struct MinutesDisplay: View {
    var arrival: Arrival

    var body: some View {
        VStack(spacing: -2) {
            Text("\(arrival.minutesToStop)")
                .font(.lcd(size: 17))
                .foregroundColor(arrival.isDue ? .lcdRow : .white)
            Text(arrival.realtime ? "MIN" : "MIN*")
                .font(.lcd(size: 6))
                .foregroundColor(arrival.isDue ? .lcdRow.opacity(0.6) : .white.opacity(0.6))
        }
    }
}
