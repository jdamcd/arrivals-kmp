@preconcurrency import ArrivalsLib
import SwiftUI

struct SettingsView: View {
    @StateObject private var coordinator = SettingsCoordinator()

    let transitSystem = ["TfL", "MTA", "BART", "UK National Rail", "Custom GTFS"]
    @State private var selector: String

    init() {
        selector = transitSystem.first!
    }

    var body: some View {
        VStack(spacing: 0) {
            Form {
                Section {
                    Picker("Transit system", selection: $selector) {
                        ForEach(transitSystem, id: \.self) {
                            Text($0)
                        }
                        .pickerStyle(.menu)
                    }
                }
            }
            .formStyle(.grouped)
            .fixedSize(horizontal: false, vertical: true)

            Divider()

            ScrollView {
                Form {
                    switch selector {
                    case "TfL":
                        TflSettingsView()
                    case "MTA":
                        MtaSettingsView()
                    case "BART":
                        GtfsFeedSettingsView(
                            fetcher: MacDI().bartSearch,
                            feedUrl: Bart().REALTIME,
                            save: { stopId in
                                let settings = MacDI().settings
                                settings.gtfsRealtime = Bart().REALTIME
                                settings.gtfsSchedule = Bart().SCHEDULE
                                settings.gtfsStop = stopId
                                settings.gtfsApiKey = Bart().API_KEY
                                settings.gtfsStopsUpdated = 0
                                settings.mode = SettingsConfig().MODE_GTFS
                            }
                        )
                    case "UK National Rail":
                        DarwinSettingsView()
                    default:
                        CustomGtfsSettingsView()
                    }
                }
                .formStyle(.grouped)
            }
            .frame(maxHeight: .infinity)

            Divider()

            HStack {
                Spacer()
                Button("Cancel") {
                    NSApp.keyWindow?.close()
                }
                .keyboardShortcut(.cancelAction)
                .buttonStyle(.bordered)

                Button("Save") {
                    coordinator.onSave?()
                    NSApp.keyWindow?.close()
                }
                .keyboardShortcut(.defaultAction)
                .disabled(!coordinator.canSave)
                .buttonStyle(.borderedProminent)
            }
            .padding()
        }
        .frame(width: 440, height: 380)
        .environmentObject(coordinator)
        .onChange(of: selector) { _, _ in
            coordinator.reset()
        }
    }
}

#Preview {
    SettingsView()
}
