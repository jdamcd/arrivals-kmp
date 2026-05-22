@preconcurrency import ArrivalsLib
import SwiftUI

struct LedContent: View {
    var arrivals: [Arrival]

    var body: some View {
        VStack(spacing: 6) {
            ForEach(Array(arrivals.enumerated()), id: \.element.id) { index, arrival in
                DotMatrixRow(leading: arrival.displayName, trailing: arrival.displayTime,
                             animateTrailing: arrival.isDue)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(arrival.accessibilityLabel(position: index + 1))
            }
            ForEach(0 ..< max(0, 3 - arrivals.count), id: \.self) { _ in
                DotMatrixRow(leading: " ")
                    .hidden()
                    .accessibilityHidden(true)
            }
        }
    }
}

struct LedErrorContent: View {
    var message: String

    var body: some View {
        DotMatrixText(text: message)
            .accessibilityLabel(message)
            .accessibilityIdentifier("errorMessage")
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
        Text(UtilsKt.filterLedChars(input: text))
            .font(.custom("LondonUnderground", size: 14))
            .foregroundColor(.yellow)
    }
}
