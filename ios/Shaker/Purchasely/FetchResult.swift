import Purchasely

enum FetchResult {
    case success(presentation: PLYPresentation)
    case client(presentation: PLYPresentation)
    case deactivated
    case error(Error?)

    var presentation: PLYPresentation? {
        switch self {
        case .success(let p), .client(let p): return p
        default: return nil
        }
    }

    var height: Int {
        presentation?.height ?? 0
    }
}
