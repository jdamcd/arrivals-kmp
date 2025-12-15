import SwiftUI

@MainActor
class SettingsCoordinator: ObservableObject {
    @Published var canSave: Bool = false
    var onSave: (() -> Void)?

    func reset() {
        canSave = false
        onSave = nil
    }
}
