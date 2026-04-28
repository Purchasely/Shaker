import SwiftUI

struct DetailScreen: View {

    let cocktailId: String
    @StateObject private var viewModel: DetailViewModel
    @EnvironmentObject private var premiumManager: PremiumManager
    @ObservedObject private var favoritesRepository = FavoritesRepository.shared
    @Environment(\.shakerTokens) private var tokens
    @Environment(\.dismiss) private var dismiss
    @State private var hostViewController: UIViewController?
    @State private var showMixing = false

    init(cocktailId: String) {
        self.cocktailId = cocktailId
        _viewModel = StateObject(wrappedValue: DetailViewModel(cocktailId: cocktailId))
    }

    var body: some View {
        Group {
            if let cocktail = viewModel.cocktail {
                content(cocktail: cocktail)
            }
        }
        .background { ViewControllerResolver { vc in hostViewController = vc } }
        .background(tokens.bg.ignoresSafeArea())
        .navigationBarHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .onAppear {
            guard !premiumManager.isPremium else { return }
            viewModel.prefetchRecipePresentation(contentId: cocktailId)
            viewModel.prefetchFavoritesPresentation()
        }
        .fullScreenCover(isPresented: $showMixing) {
            if let cocktail = viewModel.cocktail {
                MixingView(cocktail: cocktail)
            }
        }
    }

    private var locked: Bool { !premiumManager.isPremium }

    private func content(cocktail: Cocktail) -> some View {
        ScrollView {
            ZStack(alignment: .topLeading) {
                // Hero
                CocktailArt(cocktail: cocktail)
                    .frame(height: 300)
                    .clipped()

                LinearGradient(
                    colors: [.black.opacity(0.35), .clear],
                    startPoint: .top,
                    endPoint: UnitPoint(x: 0.5, y: 0.5)
                )
                .frame(height: 160)
                .allowsHitTesting(false)

                HStack {
                    roundButton(icon: "chevron.left") { dismiss() }
                    Spacer()
                    roundButton(icon: favoritesRepository.isFavorite(cocktail.id) ? "heart.fill" : "heart",
                                tint: favoritesRepository.isFavorite(cocktail.id) ? tokens.danger : .black) {
                        if premiumManager.isPremium {
                            favoritesRepository.toggleFavorite(cocktail.id)
                        } else {
                            viewModel.displayFavoritesPaywall(from: hostViewController)
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 54)

                if locked {
                    HStack(spacing: 6) {
                        Image(systemName: "lock.fill").font(.system(size: 10, weight: .bold))
                        Text("PRO RECIPE").font(.system(size: 11, weight: .heavy))
                    }
                    .foregroundStyle(Color(hex: 0xFFD572))
                    .padding(.horizontal, 12).padding(.vertical, 6)
                    .background(Capsule().fill(Color(hex: 0x1C1E2C, alpha: 0.82)))
                    .padding(.leading, 16).padding(.top, 240)
                }
            }
            .frame(height: 300)
            .clipped()

            // Content sheet
            VStack(alignment: .leading, spacing: 0) {
                Capsule()
                    .fill(tokens.hairStrong)
                    .frame(width: 40, height: 4)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 14).padding(.bottom, 14)

                Text(cocktail.name)
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(tokens.text)
                Text(cocktail.description)
                    .font(.system(size: 15))
                    .foregroundStyle(tokens.textSec)
                    .padding(.top, 8)

                HStack(spacing: 8) {
                    ChipView(text: cocktail.category.capitalized)
                    ChipView(text: cocktail.spirit.capitalized)
                    ChipView(text: cocktail.difficulty.capitalized)
                }
                .padding(.top, 16)

                ingredientsSection(cocktail.ingredients)
                    .padding(.top, 24)

                instructionsSection(steps: cocktail.instructions)
                    .padding(.top, 24)

                if !locked {
                    actionsRow(cocktail: cocktail)
                        .padding(.top, 20)
                }

                Color.clear.frame(height: 60)
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 24)
            .background(
                RoundedRectangle(cornerRadius: 28, style: .continuous)
                    .fill(tokens.bg)
            )
            .offset(y: -28)
        }
        .ignoresSafeArea(edges: .top)
        .scrollIndicators(.hidden)
    }

    private func roundButton(icon: String, tint: Color = .black, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(tint)
                .frame(width: 40, height: 40)
                .background(Circle().fill(Color.white.opacity(0.85)))
        }
    }

    private func ingredientsSection(_ ingredients: [Ingredient]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .bottom) {
                Text("Ingredients")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(tokens.text)
                Spacer()
                Text("\(ingredients.count) items")
                    .font(.system(size: 13))
                    .foregroundStyle(tokens.textSec)
            }
            VStack(spacing: 0) {
                ForEach(Array(ingredients.enumerated()), id: \.offset) { idx, ing in
                    HStack(spacing: 12) {
                        Text("\(idx + 1)")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(tokens.indigoText)
                            .frame(width: 26, height: 26)
                            .background(RoundedRectangle(cornerRadius: 6).fill(tokens.indigoSoft))
                        Text(ing.name)
                            .font(.system(size: 15))
                            .foregroundStyle(tokens.text)
                        Spacer()
                        Text(ing.amount)
                            .font(.system(size: 13))
                            .foregroundStyle(tokens.textTer)
                    }
                    .padding(.horizontal, 16).padding(.vertical, 12)
                    if idx < ingredients.count - 1 {
                        Rectangle().fill(tokens.hair).frame(height: 1)
                    }
                }
            }
            .background(
                RoundedRectangle(cornerRadius: 16).fill(tokens.bgCard)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 16).stroke(tokens.hair, lineWidth: 1)
            )
        }
    }

    private func instructionsSection(steps: [String]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Instructions")
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(tokens.text)

            if locked {
                LockedInstructionsCard(stepsCount: steps.count) {
                    viewModel.displayRecipePaywall(from: hostViewController)
                }
            } else {
                VStack(spacing: 0) {
                    ForEach(Array(steps.enumerated()), id: \.offset) { idx, step in
                        HStack(alignment: .top, spacing: 12) {
                            Text("\(idx + 1)")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundStyle(tokens.onIndigo)
                                .frame(width: 24, height: 24)
                                .background(Circle().fill(tokens.indigo))
                            Text(step)
                                .font(.system(size: 15))
                                .foregroundStyle(tokens.text)
                        }
                        .padding(.horizontal, 16).padding(.vertical, 12)
                        if idx < steps.count - 1 {
                            Rectangle().fill(tokens.hair).frame(height: 1)
                        }
                    }
                }
                .background(
                    RoundedRectangle(cornerRadius: 16).fill(tokens.bgCard)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 16).stroke(tokens.hair, lineWidth: 1)
                )
            }
        }
    }

    private func actionsRow(cocktail: Cocktail) -> some View {
        HStack(spacing: 10) {
            Button { showMixing = true } label: {
                HStack {
                    Image(systemName: "fork.knife")
                    Text("Start mixing").fontWeight(.semibold)
                }
                .foregroundStyle(tokens.onIndigo)
                .frame(maxWidth: .infinity, minHeight: 52)
                .background(Capsule().fill(tokens.indigo))
            }

            Button {
                favoritesRepository.toggleFavorite(cocktail.id)
            } label: {
                HStack {
                    Image(systemName: favoritesRepository.isFavorite(cocktail.id) ? "heart.fill" : "heart")
                    Text(favoritesRepository.isFavorite(cocktail.id) ? "Saved" : "Save").fontWeight(.semibold)
                }
                .foregroundStyle(tokens.indigoText)
                .frame(maxWidth: .infinity, minHeight: 52)
                .overlay(Capsule().stroke(tokens.indigoText, lineWidth: 1.5))
            }
        }
    }
}

struct ChipView: View {
    let text: String
    @Environment(\.shakerTokens) private var tokens
    var body: some View {
        Text(text)
            .font(.system(size: 13))
            .foregroundStyle(tokens.text)
            .padding(.horizontal, 16).padding(.vertical, 8)
            .background(Capsule().fill(tokens.bgCard))
            .overlay(Capsule().stroke(tokens.hair, lineWidth: 1))
    }
}

struct LockedInstructionsCard: View {
    let stepsCount: Int
    let onUnlock: () -> Void
    @Environment(\.shakerTokens) private var tokens

    var body: some View {
        VStack(spacing: 14) {
            ZStack {
                Circle().fill(Color(hex: 0xF5B93A, alpha: 0.18))
                    .frame(width: 56, height: 56)
                    .overlay(Circle().stroke(Color(hex: 0xF5B93A, alpha: 0.4), lineWidth: 1.5))
                Image(systemName: "lock.fill")
                    .font(.system(size: 22))
                    .foregroundStyle(Color(hex: 0xF5B93A))
            }
            Text("Pro recipe")
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(.white)
            Text("Unlock Shaker Pro to see the \(stepsCount)-step guided recipe, plus 35+ more recipes.")
                .font(.system(size: 14))
                .foregroundStyle(.white.opacity(0.75))
                .multilineTextAlignment(.center)
                .frame(maxWidth: 260)

            HStack(spacing: 16) {
                Text("✦ 35+ recipes").font(.system(size: 12)).foregroundStyle(.white.opacity(0.6))
                Text("✦ Save unlimited").font(.system(size: 12)).foregroundStyle(.white.opacity(0.6))
            }

            Button(action: onUnlock) {
                Text("Unlock Shaker Pro")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(Color(hex: 0x2A1D05))
                    .padding(.horizontal, 28).padding(.vertical, 14)
                    .background(Capsule().fill(Color(hex: 0xF5B93A)))
                    .shadow(color: Color(hex: 0xF5B93A, alpha: 0.35), radius: 12, y: 4)
            }
            .padding(.top, 4)
        }
        .padding(24)
        .frame(maxWidth: .infinity)
        .background(
            LinearGradient(
                colors: tokens.dark
                    ? [Color(hex: 0x1D2040), Color(hex: 0x2A1D3F)]
                    : [Color(hex: 0x2D345F), Color(hex: 0x3F2A55)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

// MARK: - Mixing mode
struct MixingView: View {
    let cocktail: Cocktail
    @Environment(\.dismiss) private var dismiss
    @Environment(\.shakerTokens) private var tokens
    @State private var step = 0

    private var total: Int { cocktail.instructions.count }

    var body: some View {
        let bg = tokens.dark ? Color(hex: 0x0A0B16) : Color(hex: 0x15182B)
        VStack(spacing: 0) {
            HStack {
                Button { dismiss() } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(width: 36, height: 36)
                        .background(Circle().fill(.white.opacity(0.1)))
                }
                Spacer()
                Text("Step \(step + 1) of \(max(total, 1))")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(.white.opacity(0.7))
                Spacer()
                Color.clear.frame(width: 36, height: 36)
            }
            .padding(.horizontal, 20).padding(.vertical, 14)

            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(.white.opacity(0.1))
                    Capsule().fill(tokens.gold)
                        .frame(width: geo.size.width * CGFloat(step + 1) / CGFloat(max(total, 1)))
                }
            }
            .frame(height: 4)
            .padding(.horizontal, 20)

            Spacer()

            VStack(spacing: 24) {
                CocktailArt(cocktail: cocktail)
                    .frame(width: 140, height: 140)
                    .clipShape(Circle())
                    .overlay(Circle().stroke(.white.opacity(0.1), lineWidth: 3))
                Text(cocktail.name.uppercased())
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(.white.opacity(0.6))
                    .tracking(2)
                Text(cocktail.instructions[safe: step] ?? "")
                    .font(.system(size: 26, weight: .bold))
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }

            Spacer()

            HStack(spacing: 10) {
                Button { if step > 0 { step -= 1 } } label: {
                    Text("Back").fontWeight(.semibold)
                        .foregroundStyle(.white.opacity(step == 0 ? 0.4 : 1))
                        .frame(maxWidth: .infinity, minHeight: 50)
                        .overlay(Capsule().stroke(.white, lineWidth: 1.5))
                }
                let isLast = step == max(total - 1, 0)
                Button {
                    if isLast { dismiss() } else { step += 1 }
                } label: {
                    Text(isLast ? "Done — Cheers!" : "Next step")
                        .fontWeight(.semibold)
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity, minHeight: 50)
                        .background(Capsule().fill(isLast ? tokens.green : tokens.accent))
                }
            }
            .padding(.horizontal, 20).padding(.bottom, 40)
        }
        .background(bg.ignoresSafeArea())
    }
}

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
