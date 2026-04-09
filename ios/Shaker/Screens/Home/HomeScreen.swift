import SwiftUI

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
                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVGrid(columns: columns, spacing: 12) {
                            // Embedded inline paywall banner
                            if !premiumManager.isPremium, let controller = viewModel.inlineController {
                                let height = viewModel.inlineHeight > 0 ? CGFloat(viewModel.inlineHeight) : 200
                                EmbeddedScreenBanner(controller: controller, height: height)
                                    .frame(height: height)
                                    .id("inline_banner")
                                    .gridCellColumns(2)
                            }

                            ForEach(viewModel.cocktails) { cocktail in
                                NavigationLink(value: cocktail.id) {
                                    CocktailCard(cocktail: cocktail)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(16)
                    }
                    .onChange(of: viewModel.inlinePresentation != nil) { _ in
                        // Scroll to top when inline presentation arrives
                        withAnimation {
                            proxy.scrollTo("inline_banner", anchor: .top)
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
        .navigationTitle("Shaker")
        .searchable(text: $viewModel.searchQuery, prompt: "Search cocktails...")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                if !premiumManager.isPremium && viewModel.isFiltersLoading {
                    ProgressView()
                } else {
                    Button {
                        if premiumManager.isPremium {
                            showFilterSheet = true
                        } else {
                            viewModel.displayFiltersPaywall(from: hostViewController)
                        }
                    } label: {
                        Image(systemName: viewModel.hasActiveFilters ? "line.3.horizontal.decrease.circle.fill" : "slider.horizontal.3")
                    }
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
        .onAppear {
            viewModel.prefetchPresentations(isPremium: premiumManager.isPremium)
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
