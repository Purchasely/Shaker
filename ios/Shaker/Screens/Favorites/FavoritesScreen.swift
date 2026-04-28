import SwiftUI

struct FavoritesScreen: View {

    @ObservedObject private var favoritesRepository = FavoritesRepository.shared
    @EnvironmentObject private var premiumManager: PremiumManager
    @Environment(\.shakerTokens) private var tokens
    @State private var hostViewController: UIViewController?
    @State private var favoritesFetchResult: FetchResult?

    private let wrapper = PurchaselyWrapper.shared
    private let repository = CocktailRepository.shared

    private var favoriteCocktails: [Cocktail] {
        let ids = favoritesRepository.favoriteIds
        return repository.allCocktails().filter { ids.contains($0.id) }
    }

    private let columns = [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)]

    var body: some View {
        ZStack {
            tokens.bg.ignoresSafeArea()
            VStack(alignment: .leading, spacing: 0) {
                Text("Favorites")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(tokens.text)
                    .padding(.horizontal, 20)
                    .padding(.top, 16)

                if favoriteCocktails.isEmpty {
                    emptyState
                } else {
                    Text("\(favoriteCocktails.count) saved")
                        .font(.system(size: 14))
                        .foregroundStyle(tokens.textSec)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 8)

                    ScrollView {
                        LazyVGrid(columns: columns, spacing: 12) {
                            ForEach(favoriteCocktails) { cocktail in
                                NavigationLink(value: cocktail.id) {
                                    FavoriteGridCard(cocktail: cocktail)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.horizontal, 20)
                        .padding(.bottom, 40)
                    }
                }
            }
        }
        .background { ViewControllerResolver { vc in hostViewController = vc } }
        .navigationBarHidden(true)
        .navigationDestination(for: String.self) { id in
            DetailScreen(cocktailId: id)
        }
        .onAppear { prefetchFavoritesPaywall() }
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Spacer()
            ZStack {
                Circle().fill(tokens.indigoSoft).frame(width: 110, height: 110)
                Image(systemName: "heart")
                    .font(.system(size: 44, weight: .light))
                    .foregroundStyle(tokens.indigoText)
            }
            Text("No favorites yet")
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(tokens.text)
            Text("Tap the heart on a card to save it. Unlock Pro to save as many as you like.")
                .font(.system(size: 15))
                .foregroundStyle(tokens.textSec)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            if !premiumManager.isPremium {
                Button {
                    showFavoritesPaywall()
                } label: {
                    HStack(spacing: 8) {
                        Image(systemName: "lock.fill").font(.system(size: 14, weight: .bold))
                        Text("Unlock Favorites").fontWeight(.semibold)
                    }
                    .foregroundStyle(tokens.onIndigo)
                    .frame(maxWidth: 260, minHeight: 52)
                    .background(Capsule().fill(tokens.indigo))
                }
                .padding(.top, 8)
            }
            Spacer()
        }
        .frame(maxWidth: .infinity)
        .padding(32)
    }

    private func prefetchFavoritesPaywall() {
        guard !premiumManager.isPremium else { return }
        Task {
            favoritesFetchResult = await wrapper.loadPresentation(placementId: "favorites") { result in
                switch result {
                case .purchased, .restored: PremiumManager.shared.refreshPremiumStatus()
                case .cancelled: break
                }
            }
        }
    }

    private func showFavoritesPaywall() {
        guard case .success(let presentation) = favoritesFetchResult else { return }
        wrapper.display(presentation: presentation, from: hostViewController)
    }
}

struct FavoriteGridCard: View {
    let cocktail: Cocktail
    @Environment(\.shakerTokens) private var tokens

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            CocktailArt(cocktail: cocktail)
                .aspectRatio(1, contentMode: .fit)
            VStack(alignment: .leading, spacing: 2) {
                Text(cocktail.name)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(tokens.text)
                    .lineLimit(1)
                Text("\(cocktail.category.capitalized) · \(cocktail.difficulty.capitalized)")
                    .font(.system(size: 12))
                    .foregroundStyle(tokens.textSec)
                    .lineLimit(1)
            }
            .padding(.horizontal, 12).padding(.vertical, 10)
        }
        .background(RoundedRectangle(cornerRadius: 20).fill(tokens.bgCard))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(tokens.hair, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}
