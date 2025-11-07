@preconcurrency import ArrivalsLib
import SwiftUI

struct SettingsView: View {
    let transitSystem = ["TfL", "MTA", "Custom GTFS"]
    @State private var selector: String

    init() {
        selector = transitSystem.first ?? "TfL"
    }

    var body: some View {
        Form {
            Section {
                Picker("Transit system",
                       selection: $selector) {
                    ForEach(transitSystem, id: \.self) {
                        Text($0)
                    }
                    .pickerStyle(.menu)
                }
            }
            switch selector {
            case "TfL":
                TflSettingsView()
            case "MTA":
                MtaSettingsView()
            default:
                GtfsSettingsView()
            }
        }
        .formStyle(.grouped)
        .frame(width: 450, height: 335)
    }
}

#Preview {
    SettingsView()
}
