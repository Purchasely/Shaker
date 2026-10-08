import Foundation

@MainActor
class PremiumManager: ObservableObject {

    static let shared = PremiumManager()

    @Published var isPremium = false

    private init() {}

    func refreshPremiumStatus() {
        // PURCHASELY: Fetch the current user's active subscriptions to determine premium status.
        // Call after purchase, restore, login, or app foreground to keep entitlements up to date.
        // The wrapper maps the SDK's PLYSubscription to the SDK-free SubscriptionInfo model,
        // so the data layer never imports Purchasely.
        // Docs: https://docs.purchasely.com/advanced-features/subscription-status
        PurchaselyWrapper.shared.fetchSubscriptions(
            onSuccess: { [weak self] subscriptions in
                Task { @MainActor in
                    self?.updatePremium(from: subscriptions)
                }
            },
            onError: { error in
                print("[Shaker] Error checking premium: \(error.localizedDescription)")
            }
        )
    }

    /// Compute and apply the premium status from a subscriptions list.
    /// Used both by `refreshPremiumStatus()` and by the wrapper's success_payment chain.
    func updatePremium(from subscriptions: [SubscriptionInfo]) {
        let premium = subscriptions.contains { $0.isActive }
        isPremium = premium
        print("[Shaker] Premium status: \(premium)")
    }
}
