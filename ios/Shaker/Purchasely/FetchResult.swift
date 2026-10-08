import Foundation

/// Result of a presentation fetch, mirroring the Android `FetchResult` sealed
/// class. Carries the opaque `PresentationHandle` so callers stay SDK-free.
@MainActor
enum FetchResult {
    case success(handle: PresentationHandle)
    case client(handle: PresentationHandle)
    case deactivated
    case error(Error?)

    var handle: PresentationHandle? {
        switch self {
        case .success(let handle), .client(let handle): return handle
        default: return nil
        }
    }

    var height: Int {
        handle?.presentation.height ?? 0
    }
}
