import Foundation
import Purchasely

class DetailViewModel: ObservableObject {

    @Published var cocktail: Cocktail?

    private let repository = CocktailRepository.shared

    init(cocktailId: String) {
        cocktail = repository.cocktail(byId: cocktailId)
        trackCocktailViewed()
    }

    private func trackCocktailViewed() {
        Purchasely.incrementUserAttribute(withKey: "cocktails_viewed")
        if let spirit = cocktail?.spirit {
            Purchasely.setUserAttribute(withStringValue: spirit, forKey: "favorite_spirit")
        }
    }

    func showPaywall(for cocktailId: String, from viewController: UIViewController?) {
        guard let vc = viewController else { return }

        let paywallCtrl = Purchasely.presentationController(
            for: "recipe_detail",
            contentId: cocktailId,
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

        if let paywallCtrl = paywallCtrl {
            vc.present(paywallCtrl, animated: true)
        }
    }

    func showFavoritesPaywall(from viewController: UIViewController?) {
        guard let vc = viewController else { return }

        let paywallCtrl = Purchasely.presentationController(
            for: "favorites",
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

        if let paywallCtrl = paywallCtrl {
            vc.present(paywallCtrl, animated: true)
        }
    }
}
