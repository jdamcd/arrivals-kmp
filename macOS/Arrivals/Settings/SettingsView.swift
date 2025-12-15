@preconcurrency import ArrivalsLib
import SwiftUI

struct SettingsView: View {
    @StateObject private var coordinator = SettingsCoordinator()

    let transitSystem = ["TfL", "MTA", "UK National Rail", "Custom GTFS"]
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
                    case "UK National Rail":
                        DarwinSettingsView()
                    default:
                        GtfsSettingsView()
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
