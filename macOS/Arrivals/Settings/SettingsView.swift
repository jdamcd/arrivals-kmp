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

    // Configured system is derived from mode (TfL/Darwin/BVG/GTFS)
    // GTFS feed URL disambiguates MTA/BART/custom
    static func configured(from settings: ArrivalsLib.Settings) -> TransitSystem {
        let config = SettingsConfig()
        let mode = settings.mode
        if mode == config.MODE_DARWIN { return .darwin }
        if mode == config.MODE_BVG { return .bvg }
        if mode == config.MODE_GTFS {
            if Mta().realtime.values.contains(settings.gtfsRealtime) { return .mta }
            if settings.gtfsRealtime == Bart().REALTIME { return .bart }
            return .customGtfs
        }
        return .tfl
    }
}

struct SettingsView: View {
    @StateObject private var coordinator = SettingsCoordinator()

    @State private var selector: TransitSystem = .tfl
    @State private var displayStyle: DisplayStyle = .london
    @State private var initialDisplayStyle: DisplayStyle = .london

    private var displayStyleChanged: Bool {
        displayStyle != initialDisplayStyle
    }

    var body: some View {
        VStack(spacing: 0) {
            Form {
                Section {
                    Picker("Display style", selection: $displayStyle) {
                        ForEach(DisplayStyle.allCases, id: \.self) {
                            Text($0.name)
                        }
                    }
                    .pickerStyle(.menu)
                    .accessibilityIdentifier("displayStylePicker")
                }

                Section {
                    Picker("Transit system", selection: $selector) {
                        ForEach(TransitSystem.allCases, id: \.self) {
                            Text($0.displayName)
                        }
                    }
                    .pickerStyle(.menu)
                    .accessibilityIdentifier("transitSystemPicker")
                }

                switch selector {
                case .tfl:
                    TflSettingsView()
                case .mta:
                    MtaSettingsView()
                case .bart:
                    let bart = Bart()
                    GtfsFeedSettingsView(
                        fetcher: MacDI.shared.bartSearch,
                        feedUrl: bart.REALTIME,
                        preselected: preselectedGtfsStop(feedUrl: bart.REALTIME),
                        save: { stopId, stopName in
                            MacDI.shared.settings.saveGtfsConfig(
                                stopId: stopId,
                                stopName: stopName,
                                realtimeUrl: bart.REALTIME,
                                scheduleUrl: bart.SCHEDULE,
                                apiKey: bart.API_KEY,
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

            Divider()

            HStack {
                Spacer()
                Button("Cancel") {
                    NSApp.keyWindow?.close()
                }
                .keyboardShortcut(.cancelAction)
                .buttonStyle(.bordered)
                .accessibilityIdentifier("cancelButton")

                Button("Save") {
                    UserDefaults.standard.set(displayStyle.rawValue, forKey: DisplayStyle.storageKey)
                    coordinator.onSave?()
                    NotificationCenter.default.post(name: .settingsSaved, object: nil)
                    NSApp.keyWindow?.close()
                }
                .keyboardShortcut(.defaultAction)
                .disabled(!(coordinator.canSave || displayStyleChanged))
                .buttonStyle(.borderedProminent)
                .accessibilityIdentifier("saveButton")
            }
            .padding()
        }
        .frame(width: 440, height: 360)
        .environmentObject(coordinator)
        .onAppear {
            selector = TransitSystem.configured(from: MacDI.shared.settings)
            if let raw = UserDefaults.standard.string(forKey: DisplayStyle.storageKey),
               let style = DisplayStyle(rawValue: raw)
            {
                displayStyle = style
                initialDisplayStyle = style
            }
        }
        .onChange(of: selector) { _, _ in
            coordinator.reset()
        }
    }

    private func preselectedGtfsStop(feedUrl: String) -> StopResult? {
        let settings = MacDI.shared.settings
        guard settings.mode == SettingsConfig().MODE_GTFS,
              settings.gtfsRealtime == feedUrl
        else {
            return nil
        }
        return settings.configuredStop
    }
}

#Preview {
    SettingsView()
}
