import Foundation

/// App-level view of an active Purchasely subscription. SDK-free on purpose:
/// `PurchaselyWrapper.fetchSubscriptions` maps the SDK's `PLYSubscription` to
/// this type so the data layer never imports `Purchasely`.
/// Mirrors the Android `SubscriptionInfo`.
struct SubscriptionInfo {
    let planName: String?
    let productName: String?
    let isActive: Bool
}
