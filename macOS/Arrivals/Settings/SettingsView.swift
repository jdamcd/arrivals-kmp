@preconcurrency import ArrivalsLib
import SwiftUI

enum TransitSystem: String, CaseIterable {
    case tfl, mta, bart, bvg, darwin, customGtfs

    var displayName: String {
        switch self {
        case .tfl: "London (TfL)"
        case .mta: "NYC (MTA)"
        case .bart: "SF Bay Area (BART)"
        case .bvg: "Berlin (BVG)"
        case .darwin: "UK (National Rail)"
        case .customGtfs: "Custom GTFS"
        }
    }
}

struct SettingsView: View {
    @StateObject private var coordinator = SettingsCoordinator()

    @AppStorage("settingsTransitSystem") private var selector: TransitSystem = .tfl

    var body: some View {
        VStack(spacing: 0) {
            Form {
                Section {
                    Picker("Transit system", selection: $selector) {
                        ForEach(TransitSystem.allCases, id: \.self) {
                            Text($0.displayName)
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
                    case .tfl:
                        TflSettingsView()
                    case .mta:
                        MtaSettingsView()
                    case .bart:
                        GtfsFeedSettingsView(
                            fetcher: MacDI().bartSearch,
                            feedUrl: Bart().REALTIME,
                            save: { stopId in
                                MacDI().settings.saveGtfsConfig(
                                    stopId: stopId,
                                    realtimeUrl: Bart().REALTIME,
                                    scheduleUrl: Bart().SCHEDULE,
                                    apiKey: Bart().API_KEY,
                                    apiKeyParam: ""
                                )
                            }
                        )
                    case .bvg:
                        BvgSettingsView()
                    case .darwin:
                        DarwinSettingsView()
                    case .customGtfs:
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
