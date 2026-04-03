@preconcurrency import ArrivalsLib
import SwiftUI

struct LedContent: View {
    var arrivals: [Arrival]

    var body: some View {
        VStack(spacing: 6) {
            ForEach(arrivals, id: \.id) { arrival in
                DotMatrixRow(leading: arrival.displayName, trailing: arrival.displayTime,
                             animateTrailing: arrival.isDue)
            }
            ForEach(0 ..< max(0, 3 - arrivals.count), id: \.self) { _ in
                DotMatrixRow(leading: " ").hidden()
            }
        }
    }
}

struct LedErrorContent: View {
    var message: String

    var body: some View {
        DotMatrixText(text: message)
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
