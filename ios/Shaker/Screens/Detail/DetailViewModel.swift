import Foundation
import UIKit

class DetailViewModel: ObservableObject {

    @Published var cocktail: Cocktail?
    @Published var recipeFetchResult: FetchResult?
    @Published var favoritesFetchResult: FetchResult?

    private let repository = CocktailRepository.shared
    private let wrapper = PurchaselyWrapper.shared

    init(cocktailId: String) {
        cocktail = repository.cocktail(byId: cocktailId)
        trackCocktailViewed()
    }

    private func trackCocktailViewed() {
        // PURCHASELY: Increment a counter attribute each time the user views a recipe detail
        // Enables targeting users based on engagement depth (e.g., "viewed 5+ recipes")
        // Docs: https://docs.purchasely.com/advanced-features/user-attributes
        wrapper.incrementUserAttribute(forKey: "cocktails_viewed")
        if let spirit = cocktail?.spirit {
            // PURCHASELY: Record the spirit of the last-viewed cocktail as a user attribute
            // Allows personalized paywall content based on the user's preferred spirit category
            // Docs: https://docs.purchasely.com/advanced-features/user-attributes
            wrapper.setUserAttribute(spirit, forKey: "favorite_spirit")
        }
    }

    // MARK: - Prefetch Presentations

    func prefetchRecipePresentation(contentId: String) {
        Task {
            // PURCHASELY: Prefetch the "recipe_detail" paywall with a contentId for context
            // The contentId lets the paywall content reference the specific cocktail being viewed
            // Docs: https://docs.purchasely.com/quick-start/sdk-implementation/display-placements
            recipeFetchResult = await wrapper.loadPresentation(
                placementId: "recipe_detail",
                contentId: contentId
            ) { result in
                switch result {
                case .purchased, .restored:
                    PremiumManager.shared.refreshPremiumStatus()
                case .cancelled:
                    print("[Shaker] Recipe detail paywall cancelled")
                }
            }
        }
    }

    func prefetchFavoritesPresentation() {
        Task {
            // PURCHASELY: Prefetch the "favorites" paywall when the user views a detail screen
            // Docs: https://docs.purchasely.com/quick-start/sdk-implementation/display-placements
            favoritesFetchResult = await wrapper.loadPresentation(
                placementId: "favorites"
            ) { result in
                switch result {
                case .purchased, .restored:
                    PremiumManager.shared.refreshPremiumStatus()
                case .cancelled:
                    print("[Shaker] Favourites paywall cancelled")
                }
            }
        }
    }

    // MARK: - Display Paywalls

    func displayRecipePaywall(from viewController: UIViewController?) {
        guard case .success(let presentation) = recipeFetchResult else { return }
        wrapper.display(presentation: presentation, from: viewController)
    }

    func displayFavoritesPaywall(from viewController: UIViewController?) {
        guard case .success(let presentation) = favoritesFetchResult else { return }
        wrapper.display(presentation: presentation, from: viewController)
    }
}
