import SwiftUI

struct DetailScreen: View {

    let cocktailId: String
    @StateObject private var viewModel: DetailViewModel
    @EnvironmentObject private var premiumManager: PremiumManager
    @ObservedObject private var favoritesRepository = FavoritesRepository.shared
    @State private var hostViewController: UIViewController?

    init(cocktailId: String) {
        self.cocktailId = cocktailId
        _viewModel = StateObject(wrappedValue: DetailViewModel(cocktailId: cocktailId))
    }

    var body: some View {
        Group {
            if let cocktail = viewModel.cocktail {
                ScrollView {
                    // Hero image — full bleed behind nav bar
                    // GeometryReader constrains the .fill image layout to screen width
                    GeometryReader { geo in
                        CocktailImage(cocktail: cocktail)
                            .frame(width: geo.size.width, height: geo.size.height)
                            .clipped()
                    }
                    .frame(height: 420)

                    // Content with horizontal padding
                    VStack(alignment: .leading, spacing: 20) {
                        // Name and description
                        Text(cocktail.name)
                            .font(.largeTitle)
                            .fontWeight(.bold)

                        Text(cocktail.description)
                            .font(.body)
                            .foregroundStyle(.secondary)

                        // Badges
                        HStack(spacing: 8) {
                            BadgeView(text: cocktail.category.capitalized)
                            BadgeView(text: cocktail.spirit.capitalized)
                            BadgeView(text: cocktail.difficulty.capitalized)
                        }

                        // Ingredients
                        VStack(alignment: .leading, spacing: 0) {
                            Text("Ingredients")
                                .font(.title2)
                                .fontWeight(.semibold)
                                .padding(.bottom, 8)

                            ForEach(cocktail.ingredients) { ingredient in
                                HStack {
                                    Text(ingredient.name)
                                        .font(.body)
                                    Spacer()
                                    Text(premiumManager.isPremium ? ingredient.amount : "---")
                                        .font(.body)
                                        .foregroundStyle(premiumManager.isPremium ? .secondary : .quaternary)
                                }
                                .padding(.vertical, 10)
                                Divider()
                            }
                        }

                        // Instructions
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Instructions")
                                .font(.title2)
                                .fontWeight(.semibold)

                            if premiumManager.isPremium {
                                ForEach(Array(cocktail.instructions.enumerated()), id: \.offset) { index, instruction in
                                    Text("\(index + 1). \(instruction)")
                                        .font(.body)
                                        .padding(.vertical, 2)
                                }
                            } else {
                                // Blurred instructions with unlock CTA
                                ZStack {
                                    VStack(alignment: .leading, spacing: 4) {
                                        ForEach(Array(cocktail.instructions.enumerated()), id: \.offset) { index, instruction in
                                            Text("\(index + 1). \(instruction)")
                                                .font(.body)
                                                .padding(.vertical, 2)
                                        }
                                    }
                                    .blur(radius: 8)

                                    // Gradient overlay + CTA
                                    LinearGradient(
                                        colors: [.clear, Color(.systemBackground).opacity(0.9), Color(.systemBackground)],
                                        startPoint: .top,
                                        endPoint: .bottom
                                    )

                                    Button {
                                        viewModel.displayRecipePaywall(from: hostViewController)
                                    } label: {
                                        Label("Unlock Full Recipe", systemImage: "lock.fill")
                                            .fontWeight(.semibold)
                                            .padding(.horizontal, 24)
                                            .padding(.vertical, 12)
                                    }
                                    .buttonStyle(.borderedProminent)
                                    .tint(.orange)
                                }
                            }
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 16)
                }
                .ignoresSafeArea(edges: .top)
                .overlay(alignment: .top) {
                    // Fixed dark gradient scrim for status bar / nav bar readability
                    LinearGradient(
                        colors: [.black.opacity(0.4), .clear],
                        startPoint: .top,
                        endPoint: UnitPoint(x: 0.5, y: 0.4)
                    )
                    .frame(height: 420)
                    .ignoresSafeArea(edges: .top)
                    .allowsHitTesting(false)
                }
                .navigationTitle(cocktail.name)
                .navigationBarTitleDisplayMode(.inline)
                .toolbarBackground(.hidden, for: .navigationBar)
                .toolbarColorScheme(.dark, for: .navigationBar)
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button {
                            if premiumManager.isPremium {
                                favoritesRepository.toggleFavorite(cocktail.id)
                            } else {
                                viewModel.displayFavoritesPaywall(from: hostViewController)
                            }
                        } label: {
                            Image(systemName: favoritesRepository.isFavorite(cocktail.id) ? "heart.fill" : "heart")
                                .foregroundStyle(favoritesRepository.isFavorite(cocktail.id) ? .red : .white)
                                .shadow(color: .black.opacity(0.5), radius: 2)
                        }
                    }
                }
            }
        }
        .background {
            ViewControllerResolver { vc in
                hostViewController = vc
            }
        }
        .onAppear {
            guard !premiumManager.isPremium else { return }
            viewModel.prefetchRecipePresentation(contentId: cocktailId)
            viewModel.prefetchFavoritesPresentation()
        }
    }
}

struct BadgeView: View {

    let text: String

    var body: some View {
        Text(text)
            .font(.caption)
            .fontWeight(.medium)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(Color.orange.opacity(0.15))
            .foregroundStyle(.orange)
            .clipShape(Capsule())
    }
}
