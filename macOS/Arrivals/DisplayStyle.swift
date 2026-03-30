enum DisplayStyle: String, CaseIterable {
    case london, nyc

    var name: String {
        switch self {
        case .london: "Dot Matrix"
        case .nyc: "LCD"
        }
    }
}
