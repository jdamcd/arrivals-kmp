import SwiftUI

@MainActor
class SettingsCoordinator: ObservableObject {
    @Published var hasValidConfig: Bool = false
    var onSave: (() -> Void)?

    func reset() {
        hasValidConfig = false
        onSave = nil
    }
}
