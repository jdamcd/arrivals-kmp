@preconcurrency import ArrivalsLib
import SwiftUI

enum TransitSystem: String, CaseIterable {
    static let storageKey = "settingsTransitSystem"

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

    @State private var selector: TransitSystem = .tfl
    @State private var displayStyle: DisplayStyle = .london

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

                    Picker("Transit system", selection: $selector) {
                        ForEach(TransitSystem.allCases, id: \.self) {
                            Text($0.displayName)
                        }
                    }
                    .pickerStyle(.menu)
                    .accessibilityIdentifier("transitSystemPicker")
                }
            }
            .formStyle(.grouped)
            .fixedSize(horizontal: false, vertical: true)

            Divider()

            Form {
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
                        save: { stopId in
                            MacDI.shared.settings.saveGtfsConfig(
                                stopId: stopId,
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
                    UserDefaults.standard.set(selector.rawValue, forKey: TransitSystem.storageKey)
                    coordinator.onSave?()
                    NotificationCenter.default.post(name: .settingsSaved, object: nil)
                    NSApp.keyWindow?.close()
                }
                .keyboardShortcut(.defaultAction)
                .disabled(!coordinator.canSave)
                .buttonStyle(.borderedProminent)
                .accessibilityIdentifier("saveButton")
            }
            .padding()
        }
        .frame(width: 440, height: 360)
        .environmentObject(coordinator)
        .onAppear {
            if let raw = UserDefaults.standard.string(forKey: TransitSystem.storageKey),
               let system = TransitSystem(rawValue: raw)
            {
                selector = system
            }
            if let raw = UserDefaults.standard.string(forKey: DisplayStyle.storageKey),
               let style = DisplayStyle(rawValue: raw)
            {
                displayStyle = style
            }
        }
        .onChange(of: selector) { _, _ in
            coordinator.reset()
        }
    }
}

#Preview {
    SettingsView()
}
