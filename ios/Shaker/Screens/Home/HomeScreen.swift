import SwiftUI

struct HomeScreen: View {

    @StateObject private var viewModel = HomeViewModel()
    @EnvironmentObject private var premiumManager: PremiumManager
    @Environment(\.shakerTokens) private var tokens
    @State private var showFilterSheet = false
    @State private var hostViewController: UIViewController?

    private let categories: [(id: String, label: String)] = [
        ("All", "All"),
        ("classic", "Classic"),
        ("non-alcoholic", "Non-alcoholic"),
        ("tropical", "Tropical"),
        ("easy", "Easy"),
    ]

    private var activeCat: String {
        if viewModel.selectedDifficulty == "easy" { return "easy" }
        if viewModel.selectedSpirits.contains("non-alcoholic") { return "non-alcoholic" }
        if let first = viewModel.selectedCategories.first { return first }
        return "All"
    }

    private var columns: [GridItem] {
        [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)]
    }

    var body: some View {
        ZStack {
            tokens.bg.ignoresSafeArea()
            VStack(spacing: 0) {
                header
                searchBar
                    .padding(.top, 4)

                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            if !premiumManager.isPremium, let controller = viewModel.inlineController {
                                let height = viewModel.inlineHeight > 0 ? CGFloat(viewModel.inlineHeight) : 200
                                EmbeddedScreenBanner(controller: controller)
                                    .frame(height: height)
                                    .padding(.horizontal, 20)
                                    .id("inline_banner")
                            }

                            categoryChips
                                .padding(.horizontal, 20)
                                .padding(.top, 8)

                            if activeCat == "All", let hero = heroCocktail {
                                NavigationLink(value: hero.id) {
                                    TonightsPickCard(cocktail: hero)
                                }
                                .buttonStyle(.plain)
                                .padding(.horizontal, 20)
                            }

                            LazyVGrid(columns: columns, spacing: 12) {
                                ForEach(viewModel.cocktails) { cocktail in
                                    NavigationLink(value: cocktail.id) {
                                        HomeCocktailCard(cocktail: cocktail)
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                            .padding(.horizontal, 20)
                            .padding(.bottom, 40)
                        }
                        .padding(.top, 12)
                    }
                    .onChange(of: viewModel.inlinePresentation != nil) { _ in
                        withAnimation { proxy.scrollTo("inline_banner", anchor: .top) }
                    }
                }
            }
        }
        .background {
            ViewControllerResolver { vc in hostViewController = vc }
        }
        .navigationBarHidden(true)
        .searchable(text: $viewModel.searchQuery, prompt: "Search cocktails…")
        .sheet(isPresented: $showFilterSheet) {
            FilterSheet(viewModel: viewModel)
                .presentationDetents([.medium, .large])
        }
        .navigationDestination(for: String.self) { id in
            DetailScreen(cocktailId: id)
        }
        .onAppear {
            viewModel.prefetchPresentations(isPremium: premiumManager.isPremium)
        }
    }

    // MARK: - Header
    private var header: some View {
        HStack(spacing: 10) {
            ShakerLogoView(size: 28, color: tokens.indigoText)
            Text("Shaker")
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(tokens.indigoText)
            Spacer()
            Text(premiumManager.isPremium ? "PRO" : "FREE")
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(tokens.indigoText)
                .padding(.horizontal, 10)
                .padding(.vertical, 4)
                .background(Capsule().fill(premiumManager.isPremium ? tokens.goldSoft : tokens.indigoSoft))
        }
        .padding(.horizontal, 20)
        .padding(.top, 14)
        .padding(.bottom, 8)
    }

    // MARK: - Search
    private var searchBar: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 16))
                .foregroundStyle(tokens.textSec)
            Text(viewModel.searchQuery.isEmpty ? "Search cocktails…" : viewModel.searchQuery)
                .font(.system(size: 15))
                .foregroundStyle(viewModel.searchQuery.isEmpty ? tokens.textSec : tokens.text)
                .lineLimit(1)
            Spacer()
            Button {
                if premiumManager.isPremium {
                    showFilterSheet = true
                } else {
                    viewModel.displayFiltersPaywall(from: hostViewController)
                }
            } label: {
                if !premiumManager.isPremium && viewModel.isFiltersLoading {
                    ProgressView().scaleEffect(0.8)
                } else {
                    Image(systemName: viewModel.hasActiveFilters ? "line.3.horizontal.decrease.circle.fill" : "slider.horizontal.3")
                        .font(.system(size: 16))
                        .foregroundStyle(tokens.textSec)
                }
            }
        }
        .padding(.horizontal, 16)
        .frame(height: 44)
        .background(Capsule().fill(tokens.inputBg))
        .padding(.horizontal, 20)
    }

    // MARK: - Category chips
    private var categoryChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(categories, id: \.id) { cat in
                    let selected = activeCat == cat.id
                    Text(cat.label)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(selected ? tokens.onIndigo : tokens.text)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 7)
                        .background(
                            Capsule().fill(selected ? tokens.indigo : tokens.bgCard)
                        )
                        .overlay(
                            Capsule().stroke(selected ? .clear : tokens.hair, lineWidth: 1)
                        )
                        .onTapGesture {
                            if cat.id == "All" {
                                viewModel.clearFilters()
                            } else if cat.id == "easy" {
                                viewModel.clearFilters()
                                viewModel.selectDifficulty("easy")
                            } else if cat.id == "non-alcoholic" {
                                viewModel.clearFilters()
                                viewModel.toggleSpirit("non-alcoholic")
                            } else {
                                viewModel.clearFilters()
                                viewModel.toggleCategory(cat.id)
                            }
                        }
                }
            }
        }
    }

    private var heroCocktail: Cocktail? {
        viewModel.cocktails.first(where: { $0.id == "manhattan" }) ?? viewModel.cocktails.first
    }
}

// MARK: - Cards

struct HomeCocktailCard: View {
    let cocktail: Cocktail
    @Environment(\.shakerTokens) private var tokens

    private var isPro: Bool { cocktail.tags.contains("premium") }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack(alignment: .topLeading) {
                CocktailArt(cocktail: cocktail)
                    .aspectRatio(1, contentMode: .fit)
                if isPro {
                    HStack(spacing: 4) {
                        Image(systemName: "lock.fill")
                            .font(.system(size: 9, weight: .bold))
                        Text("PRO").font(.system(size: 10, weight: .heavy))
                    }
                    .foregroundStyle(Color(hex: 0xFFD572))
                    .padding(.horizontal, 8).padding(.vertical, 4)
                    .background(Capsule().fill(Color(hex: 0x0F1020, alpha: 0.72)))
                    .padding(8)
                }
            }

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
        .background(
            RoundedRectangle(cornerRadius: 20).fill(tokens.bgCard)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 20).stroke(tokens.hair, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

struct TonightsPickCard: View {
    let cocktail: Cocktail
    @Environment(\.shakerTokens) private var tokens

    var body: some View {
        ZStack {
            CocktailArt(cocktail: cocktail)
            LinearGradient(
                colors: [.clear, Color(hex: 0x0F1020, alpha: 0.75)],
                startPoint: .top,
                endPoint: .bottom
            )
            VStack {
                HStack {
                    Text("✦ TONIGHT'S PICK")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(tokens.indigoText)
                        .padding(.horizontal, 10).padding(.vertical, 4)
                        .background(Capsule().fill(.white.opacity(0.9)))
                    Spacer()
                }
                .padding(12)
                Spacer()
                VStack(alignment: .leading, spacing: 2) {
                    Text(cocktail.name)
                        .font(.system(size: 22, weight: .bold))
                        .foregroundStyle(.white)
                    Text(cocktail.description)
                        .font(.system(size: 13))
                        .foregroundStyle(.white.opacity(0.9))
                        .lineLimit(1)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 16).padding(.bottom, 14)
            }
        }
        .aspectRatio(16.0 / 9.0, contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

struct CocktailCard: View {
    let cocktail: Cocktail
    var body: some View { HomeCocktailCard(cocktail: cocktail) }
}
