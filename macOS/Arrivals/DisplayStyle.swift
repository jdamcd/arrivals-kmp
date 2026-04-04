enum DisplayStyle: String, CaseIterable {
    static let storageKey = "displayStyle"

    case london, nyc

    var name: String {
        switch self {
        case .london: "Dot Matrix"
        case .nyc: "LCD"
        }
    }
}
