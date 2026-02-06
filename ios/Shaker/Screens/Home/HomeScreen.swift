import SwiftUI
import Purchasely

struct HomeScreen: View {

    @StateObject private var viewModel = HomeViewModel()
    @EnvironmentObject private var premiumManager: PremiumManager
    @State private var showFilterSheet = false
    @State private var hostViewController: UIViewController?

    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12)
    ]

    var body: some View {
        Group {
            if viewModel.cocktails.isEmpty {
                VStack(spacing: 12) {
                    Spacer()
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 48))
                        .foregroundStyle(.secondary.opacity(0.5))

                    Text("No cocktails found")
                        .font(.title3)
                        .fontWeight(.medium)
                        .foregroundStyle(.secondary)

                    Text("Try a different search or filter.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary.opacity(0.7))
                    Spacer()
                }
                .padding(32)
            } else {
                ScrollView {
                    LazyVGrid(columns: columns, spacing: 12) {
                        ForEach(viewModel.cocktails) { cocktail in
                            NavigationLink(value: cocktail.id) {
                                CocktailCard(cocktail: cocktail)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(16)
                }
            }
        }
        .navigationTitle("Shaker")
        .searchable(text: $viewModel.searchQuery, prompt: "Search cocktails...")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    if premiumManager.isPremium {
                        showFilterSheet = true
                    } else {
                        showFiltersPaywall()
                    }
                } label: {
                    Image(systemName: viewModel.hasActiveFilters ? "line.3.horizontal.decrease.circle.fill" : "slider.horizontal.3")
                }
            }
        }
        .sheet(isPresented: $showFilterSheet) {
            FilterSheet(viewModel: viewModel)
                .presentationDetents([.medium, .large])
        }
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

    private func showFiltersPaywall() {
        guard let vc = hostViewController else { return }

        let paywallCtrl = Purchasely.presentationController(
            for: "filters",
            completion: { result, plan in
                switch result {
                case .purchased, .restored:
                    print("[Shaker] Purchased/Restored from filters: \(plan?.name ?? "unknown")")
                    PremiumManager.shared.refreshPremiumStatus()
                case .cancelled:
                    print("[Shaker] Filters paywall cancelled")
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

struct CocktailCard: View {

    let cocktail: Cocktail

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            CocktailImage(cocktail: cocktail)
                .frame(height: 150)
                .clipped()

            VStack(alignment: .leading, spacing: 4) {
                Text(cocktail.name)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .lineLimit(1)

                Text(cocktail.category.capitalized)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .padding(12)
        }
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.1), radius: 2, y: 1)
    }
}
