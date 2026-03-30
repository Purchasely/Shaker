import Foundation
import Purchasely

class PremiumManager: ObservableObject {

    static let shared = PremiumManager()

    @Published var isPremium = false

    private init() {}

    func refreshPremiumStatus() {
        // PURCHASELY: Fetch the current user's active subscriptions to determine premium status
        // Call after purchase, restore, login, or app foreground to keep entitlements up to date
        // Docs: https://docs.purchasely.com/advanced-features/subscription-status
        Purchasely.userSubscriptions(
            success: { [weak self] subscriptions in
                let premium = subscriptions?.contains { subscription in
                    switch subscription.status {
                    case .autoRenewing, .inGracePeriod, .autoRenewingCanceled, .onHold:
                        return true
                    default:
                        return false
                    }
                } ?? false

                DispatchQueue.main.async {
                    self?.isPremium = premium
                    print("[Shaker] Premium status: \(premium)")
                }
            },
            failure: { error in
                print("[Shaker] Error checking premium: \(error.localizedDescription)")
            }
        )
    }
}
