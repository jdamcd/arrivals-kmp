@preconcurrency import ArrivalsLib

enum SettingsState: Equatable {
    case idle
    case loading
    case data([StopResult])
    case empty
    case error
}
