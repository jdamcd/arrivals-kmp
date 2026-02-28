@preconcurrency import ArrivalsLib
import SwiftUI

struct CustomGtfsSettingsView: View {
    @EnvironmentObject var coordinator: SettingsCoordinator

    private let settings = MacDI().settings

    @State private var realtimeUrl: String
    @State private var scheduleUrl: String
    @State private var stopId: String
    @State private var apiKey: String

    private var isValid: Bool {
        realtimeUrl.isNotEmpty && scheduleUrl.isNotEmpty && stopId.isNotEmpty
    }

    init() {
        realtimeUrl = settings.gtfsRealtime
        scheduleUrl = settings.gtfsSchedule
        stopId = settings.gtfsStop
        apiKey = ""
    }

    var body: some View {
        Section {
            TextField("Realtime URL", text: $realtimeUrl)
            TextField("Schedule URL", text: $scheduleUrl)
            TextField("Stop ID", text: $stopId)
            TextField("API Key (optional)", text: $apiKey)
        }
        .onAppear {
            coordinator.onSave = {
                settings.gtfsRealtime = realtimeUrl.trim()
                settings.gtfsSchedule = scheduleUrl.trim()
                settings.gtfsStop = stopId.trim()
                settings.gtfsApiKey = apiKey.trim()
                settings.gtfsStopsUpdated = 0
                settings.mode = SettingsConfig().MODE_GTFS
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
    }
}

#Preview {
    Form {
        CustomGtfsSettingsView()
    }
    .formStyle(.grouped)
}
