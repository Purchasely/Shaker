import SwiftUI
import Purchasely

struct FavoritesScreen: View {

    @ObservedObject private var favoritesRepository = FavoritesRepository.shared
    @EnvironmentObject private var premiumManager: PremiumManager
    @State private var hostViewController: UIViewController?

    private let repository = CocktailRepository.shared

    private var favoriteCocktails: [Cocktail] {
        let ids = favoritesRepository.favoriteIds
        return repository.allCocktails().filter { ids.contains($0.id) }
    }

    var body: some View {
        Group {
            if favoriteCocktails.isEmpty {
                emptyState
            } else {
                List(favoriteCocktails) { cocktail in
                    NavigationLink(value: cocktail.id) {
                        FavoriteRow(cocktail: cocktail)
                    }
                }
                .listStyle(.plain)
            }
        }
        .navigationTitle("Favorites")
        .navigationDestination(for: String.self) { cocktailId in
            DetailScreen(cocktailId: cocktailId)
        }
        .background(
            ViewControllerResolver { vc in
                hostViewController = vc
            }
            .frame(width: 0, height: 0)
        )
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "heart")
                .font(.system(size: 48))
                .foregroundStyle(.secondary.opacity(0.5))

            Text("No favorites yet")
                .font(.title3)
                .fontWeight(.medium)
                .foregroundStyle(.secondary)

            Text("Tap the heart icon on a cocktail to save it here.")
                .font(.subheadline)
                .foregroundStyle(.secondary.opacity(0.7))
                .multilineTextAlignment(.center)

            if !premiumManager.isPremium {
                Button {
                    showFavoritesPaywall()
                } label: {
                    Label("Unlock Favorites", systemImage: "lock.fill")
                        .fontWeight(.semibold)
                        .padding(.horizontal, 24)
                        .padding(.vertical, 12)
                }
                .buttonStyle(.borderedProminent)
                .tint(.orange)
                .padding(.top, 8)
            }
        }
        .padding(32)
    }

    private func showFavoritesPaywall() {
        guard let vc = hostViewController else { return }

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

struct FavoriteRow: View {

    let cocktail: Cocktail

    var body: some View {
        HStack(spacing: 12) {
            Image(cocktail.image.replacingOccurrences(of: ".webp", with: ""))
                .resizable()
                .scaledToFill()
                .frame(width: 60, height: 60)
                .clipShape(RoundedRectangle(cornerRadius: 8))

            VStack(alignment: .leading, spacing: 4) {
                Text(cocktail.name)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                Text(cocktail.category.capitalized)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}
