import SwiftUI

struct HomeScreen: View {

    @StateObject private var viewModel = HomeViewModel()
    @EnvironmentObject private var premiumManager: PremiumManager
    @Environment(\.shakerTokens) private var tokens
    @State private var showFilterSheet = false
    @State private var hostViewController: UIViewController?
    @State private var surpriseCocktailId: String?

    private var columns: [GridItem] {
        [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)]
    }

    /// Time-of-day greeting: sets the lounge tone — same buckets as Android.
    private var greeting: String {
        switch Calendar.current.component(.hour, from: Date()) {
        case 5...11: return "Good morning"
        case 12...17: return "Good afternoon"
        default: return "Good evening"
        }
    }

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            tokens.bg.ignoresSafeArea()
            VStack(spacing: 0) {
                header
                searchBar
                    .padding(.top, 4)

                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            if !premiumManager.isPremium,
                               let inlineResult = viewModel.inlinePresentation,
                               inlineResult.handle != nil {
                                let height = inlineResult.height > 0 ? CGFloat(inlineResult.height) : 200
                                EmbeddedScreenBanner(fetchResult: inlineResult)
                                    .frame(height: height)
                                    .padding(.horizontal, 20)
                                    .id("inline_banner")
                            }

                            moodChips
                                .padding(.top, 8)

                            if viewModel.selectedMood == nil, let hero = heroCocktail {
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
                            .padding(.bottom, 84)
                        }
                        .padding(.top, 12)
                    }
                    .onChange(of: viewModel.inlinePresentation != nil) { _ in
                        withAnimation { proxy.scrollTo("inline_banner", anchor: .top) }
                    }
                }
            }

            surpriseMeButton
                .padding(.trailing, 20)
                .padding(.bottom, 16)
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
        .navigationDestination(isPresented: Binding(
            get: { surpriseCocktailId != nil },
            set: { if !$0 { surpriseCocktailId = nil } }
        )) {
            if let id = surpriseCocktailId {
                DetailScreen(cocktailId: id)
            }
        }
        .onAppear {
            viewModel.prefetchPresentations(isPremium: premiumManager.isPremium)
        }
    }

    // MARK: - Header
    private var header: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 8) {
                ShakerLogoView(size: 22, color: tokens.gold)
                Text("SHAKER")
                    .font(.system(size: 13, weight: .bold))
                    .kerning(3)
                    .foregroundStyle(tokens.textSec)
                Spacer()
                Text(premiumManager.isPremium ? "PRO" : "FREE")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(premiumManager.isPremium ? tokens.gold : tokens.indigoText)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(Capsule().fill(premiumManager.isPremium ? tokens.goldSoft : tokens.indigoSoft))
            }
            Spacer().frame(height: 10)
            Text("\(greeting),")
                .font(.system(size: 16))
                .foregroundStyle(tokens.textSec)
            Text("What are we mixing?")
                .font(.system(size: 28, weight: .bold, design: .serif))
                .foregroundStyle(tokens.text)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 20)
        .padding(.top, 14)
        .padding(.bottom, 12)
    }

    // MARK: - Surprise me
    /// Floating "Surprise me" action — random pick + Purchasely engagement counter.
    private var surpriseMeButton: some View {
        Button {
            surpriseCocktailId = viewModel.surpriseMe()
        } label: {
            HStack(spacing: 8) {
                Text("🎲").font(.system(size: 16))
                Text("Surprise me")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(Color(hex: 0x231503))
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 13)
            .background(
                Capsule().fill(
                    LinearGradient(
                        colors: [tokens.gold, tokens.orange],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
            )
        }
        .buttonStyle(.plain)
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

    // MARK: - Mood chips
    private var moodChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                moodChip(label: "All", emoji: nil, selected: viewModel.selectedMood == nil) {
                    viewModel.selectMood(nil)
                }
                ForEach(CocktailMood.allCases) { mood in
                    moodChip(label: mood.label, emoji: mood.emoji, selected: viewModel.selectedMood == mood) {
                        viewModel.selectMood(mood)
                    }
                }
            }
            .padding(.horizontal, 20)
        }
    }

    private func moodChip(label: String, emoji: String?, selected: Bool, action: @escaping () -> Void) -> some View {
        HStack(spacing: 5) {
            if let emoji {
                Text(emoji).font(.system(size: 13))
            }
            Text(label)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(selected ? tokens.onIndigo : tokens.text)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 7)
        .background(
            Capsule().fill(selected ? tokens.indigo : tokens.bgCard)
        )
        .overlay(
            Capsule().stroke(selected ? .clear : tokens.hair, lineWidth: 1)
        )
        .onTapGesture(perform: action)
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
                        .font(.system(size: 24, weight: .bold, design: .serif))
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
