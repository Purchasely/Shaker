import SwiftUI

struct FavoritesScreen: View {

    @ObservedObject private var favoritesRepository = FavoritesRepository.shared
    @EnvironmentObject private var premiumManager: PremiumManager

    @State private var hostViewController: UIViewController?
    @State private var favoritesFetchResult: FetchResult?

    private let wrapper = PurchaselyWrapper.shared
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
        .background {
            ViewControllerResolver { vc in
                hostViewController = vc
            }
        }
        .navigationTitle("Favorites")
        .navigationDestination(for: String.self) { cocktailId in
            DetailScreen(cocktailId: cocktailId)
        }
        .onAppear {
            prefetchFavoritesPaywall()
        }
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

    private func prefetchFavoritesPaywall() {
        guard !premiumManager.isPremium else { return }
        Task {
            favoritesFetchResult = await wrapper.loadPresentation(placementId: "favorites") { result in
                switch result {
                case .purchased, .restored:
                    PremiumManager.shared.refreshPremiumStatus()
                case .cancelled:
                    break
                }
            }
        }
    }

    private func showFavoritesPaywall() {
        guard case .success(let presentation) = favoritesFetchResult else { return }
        wrapper.display(presentation: presentation, from: hostViewController)
    }
}

struct FavoriteRow: View {

    let cocktail: Cocktail

    var body: some View {
        HStack(spacing: 12) {
            CocktailImage(cocktail: cocktail)
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
