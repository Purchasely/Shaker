import Foundation
import UIKit
import Purchasely

class DetailViewModel: ObservableObject {

    @Published var cocktail: Cocktail?

    private let repository = CocktailRepository.shared

    init(cocktailId: String) {
        cocktail = repository.cocktail(byId: cocktailId)
        trackCocktailViewed()
    }

    private func trackCocktailViewed() {
        // PURCHASELY: Increment a counter attribute each time the user views a recipe detail
        // Enables targeting users based on engagement depth (e.g., "viewed 5+ recipes")
        // Docs: https://docs.purchasely.com/advanced-features/user-attributes
        Purchasely.incrementUserAttribute(withKey: "cocktails_viewed")
        if let spirit = cocktail?.spirit {
            // PURCHASELY: Record the spirit of the last-viewed cocktail as a user attribute
            // Allows personalized paywall content based on the user's preferred spirit category
            // Docs: https://docs.purchasely.com/advanced-features/user-attributes
            Purchasely.setUserAttribute(withStringValue: spirit, forKey: "favorite_spirit")
        }
    }

    private var topViewController: UIViewController? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }?
            .rootViewController
    }

    func showPaywall(for cocktailId: String) {
        let vc = topViewController

        // PURCHASELY: Fetch and display the "recipe_detail" paywall with a contentId for context
        // The contentId lets the paywall content reference the specific cocktail being viewed
        // Docs: https://docs.purchasely.com/quick-start/sdk-implementation/display-placements
        Purchasely.fetchPresentation(
            for: "recipe_detail",
            contentId: cocktailId,
            fetchCompletion: { presentation, error in
                guard let presentation = presentation, presentation.type != .deactivated else {
                    print("[Shaker] Recipe detail presentation not available: \(error?.localizedDescription ?? "deactivated")")
                    return
                }

                if presentation.type == .client {
                    // PURCHASELY: CLIENT type — app builds its own paywall UI
                    // The presentation contains plan data but no server-built screen
                    // Docs: https://docs.purchasely.com/advanced-features/customize-screens/custom-paywall
                    print("[Shaker] CLIENT presentation received — build custom UI here")
                    return
                }

                DispatchQueue.main.async {
                    presentation.display(from: vc)
                }
            },
            completion: { result, plan in
                switch result {
                case .purchased:
                    print("[Shaker] Purchased: \(plan?.name ?? "unknown")")
                    PremiumManager.shared.refreshPremiumStatus()
                case .restored:
                    print("[Shaker] Restored: \(plan?.name ?? "unknown")")
                    PremiumManager.shared.refreshPremiumStatus()
                case .cancelled:
                    print("[Shaker] Cancelled")
                @unknown default:
                    break
                }
            }
        )
    }

    func showFavoritesPaywall() {
        let vc = topViewController

        // PURCHASELY: Fetch and display the "favorites" paywall when the user tries to favorite while non-premium
        // Docs: https://docs.purchasely.com/quick-start/sdk-implementation/display-placements
        Purchasely.fetchPresentation(
            for: "favorites",
            fetchCompletion: { presentation, error in
                guard let presentation = presentation, presentation.type != .deactivated else {
                    print("[Shaker] Favorites presentation not available: \(error?.localizedDescription ?? "deactivated")")
                    return
                }

                if presentation.type == .client {
                    // PURCHASELY: CLIENT type — app builds its own paywall UI
                    // The presentation contains plan data but no server-built screen
                    // Docs: https://docs.purchasely.com/advanced-features/customize-screens/custom-paywall
                    print("[Shaker] CLIENT presentation received — build custom UI here")
                    return
                }

                DispatchQueue.main.async {
                    presentation.display(from: vc)
                }
            },
            completion: { result, plan in
                switch result {
                case .purchased, .restored:
                    print("[Shaker] Purchased/Restored from favorites: \(plan?.name ?? "unknown")")
                    PremiumManager.shared.refreshPremiumStatus()
                case .cancelled:
                    print("[Shaker] Favourites paywall cancelled")
                @unknown default:
                    break
                }
            }
        )
    }
}
