import Foundation
import Purchasely

class PremiumManager: ObservableObject {

    static let shared = PremiumManager()
    static let entitlementId = "SHAKER_PREMIUM"

    @Published var isPremium = false

    private init() {}

    func refreshPremiumStatus() {
        Purchasely.userSubscriptions { [weak self] subscriptions, error in
            guard let subscriptions = subscriptions else {
                if let error = error {
                    print("[Shaker] Error checking premium: \(error.localizedDescription)")
                }
                return
            }

            let premium = subscriptions.contains { subscription in
                subscription.plan?.hasEntitlement(PremiumManager.entitlementId) == true
            }

            DispatchQueue.main.async {
                self?.isPremium = premium
                print("[Shaker] Premium status: \(premium)")
            }
        }
    }
}
