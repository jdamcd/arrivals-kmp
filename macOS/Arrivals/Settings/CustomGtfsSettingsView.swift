@preconcurrency import ArrivalsLib
import SwiftUI

struct CustomGtfsSettingsView: View {
    @EnvironmentObject var coordinator: SettingsCoordinator

    private let settings = MacDI.shared.settings

    @State private var realtimeUrl: String
    @State private var scheduleUrl: String
    @State private var stopId: String
    @State private var apiKey: String
    @State private var apiKeyParam: String

    private var isValid: Bool {
        realtimeUrl.isNotEmpty && scheduleUrl.isNotEmpty && stopId.isNotEmpty
    }

    init() {
        realtimeUrl = settings.gtfsRealtime
        scheduleUrl = settings.gtfsSchedule
        // Don't surface a shared stopID from a non-GTFS system
        stopId = settings.mode == SettingsConfig().MODE_GTFS ? settings.stopId : ""
        // Don't surface a stored key from another transit system
        apiKey = ""
        apiKeyParam = settings.gtfsApiKeyParam
    }

    var body: some View {
        Section {
            TextField("Realtime URL", text: $realtimeUrl)
            TextField("Schedule URL", text: $scheduleUrl)
            TextField("Stop ID", text: $stopId)
        }
        .onAppear {
            coordinator.onSave = {
                settings.saveGtfsConfig(
                    stopId: stopId.trim(),
                    realtimeUrl: realtimeUrl.trim(),
                    scheduleUrl: scheduleUrl.trim(),
                    apiKey: apiKey.trim(),
                    apiKeyParam: apiKeyParam.trim()
                )
            }
            coordinator.canSave = isValid
        }
        .onChange(of: realtimeUrl) { _, _ in
            coordinator.canSave = isValid
        }
        .onChange(of: scheduleUrl) { _, _ in
            coordinator.canSave = isValid
        }
        .onChange(of: stopId) { _, _ in
            coordinator.canSave = isValid
        }

        Section("Authentication options") {
            TextField("API key param", text: $apiKeyParam)
                .helpHint(help: "e.g. 'app_id', 'header:Authorization'", spoken: "For example, app_id or header:Authorization")
            TextField("API key", text: $apiKey)
        }
    }
}

#Preview {
    Form {
        CustomGtfsSettingsView()
    }
    .formStyle(.grouped)
    .environmentObject(SettingsCoordinator())
}
