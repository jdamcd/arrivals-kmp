import SwiftUI
import TflArrivals

struct GtfsSettingsView: View {
    private let settings = MacDI().settings

    @State private var realtimeUrl: String
    @State private var scheduleUrl: String
    @State private var stopId: String

    init() {
        realtimeUrl = settings.gtfsRealtime
        scheduleUrl = settings.gtfsSchedule
        stopId = settings.gtfsStop
    }

    var body: some View {
        Section {
            TextField("Realtime URL", text: $realtimeUrl)
            TextField("Schedule URL", text: $scheduleUrl)
            TextField("Stop ID", text: $stopId)

            HStack {
                Spacer()
                Button("Save") {
                    if allFieldsCompleted() {
                        settings.gtfsRealtime = realtimeUrl.trim()
                        settings.gtfsSchedule = scheduleUrl.trim()
                        settings.gtfsStop = stopId.trim()
                        settings.gtfsStopsUpdated = 0
                        settings.mode = SettingsConfig().MODE_GTFS
                        NSApp.keyWindow?.close()
                    }
                }
                .disabled(!allFieldsCompleted())
                .buttonStyle(.borderedProminent)
            }
        }
    }

    private func allFieldsCompleted() -> Bool {
        realtimeUrl.isNotEmpty && scheduleUrl.isNotEmpty && stopId.isNotEmpty
    }
}

#Preview {
    Form {
        GtfsSettingsView()
    }
    .formStyle(.grouped)
}
