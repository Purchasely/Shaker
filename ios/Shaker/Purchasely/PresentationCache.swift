import Foundation

/// In-memory cache for prefetched Purchasely presentations.
///
/// Fetching a presentation is expensive (network + parse). A given presentation
/// remains valid until the user's targeting context changes — user attributes,
/// subscription state, or SDK mode. Caching avoids duplicate network calls when
/// a screen re-appears (e.g., SwiftUI `.onAppear` firing on navigation back,
/// sheet dismiss, scroll, etc.) and prevents accumulating flow state entries
/// in the SDK's `FlowsManager`.
///
/// Invalidation is coarse-grained (`invalidateAll`) because the SDK does not
/// expose which audiences depend on which attribute. This is the simplest
/// correct approach; native caching is expected in Purchasely SDK 6.x.
///
/// `PurchaselyWrapper` invalidates the cache on:
///   - any user attribute set/removed (via `PLYUserAttributeDelegate`)
///   - successful `synchronize()`
///   - SDK mode restart (Full ↔ Observer)
final class PresentationCache {

    static let shared = PresentationCache()

    private var cache: [String: FetchResult] = [:]
    private let lock = NSLock()

    private init() {}

    func get(placementId: String, contentId: String? = nil) -> FetchResult? {
        lock.lock(); defer { lock.unlock() }
        return cache[Self.key(placementId, contentId)]
    }

    func set(_ result: FetchResult, placementId: String, contentId: String? = nil) {
        lock.lock(); defer { lock.unlock() }
        cache[Self.key(placementId, contentId)] = result
    }

    func invalidateAll() {
        lock.lock(); defer { lock.unlock() }
        cache.removeAll()
    }

    private static func key(_ placementId: String, _ contentId: String?) -> String {
        contentId.map { "\(placementId)/\($0)" } ?? placementId
    }
}
