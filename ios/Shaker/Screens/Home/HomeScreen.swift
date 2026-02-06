import SwiftUI

struct HomeScreen: View {

    @StateObject private var viewModel = HomeViewModel()

    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12)
    ]

    var body: some View {
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
        .navigationTitle("Shaker")
        .searchable(text: $viewModel.searchQuery, prompt: "Search cocktails...")
        .navigationDestination(for: String.self) { cocktailId in
            DetailScreen(cocktailId: cocktailId)
        }
    }
}

struct CocktailCard: View {

    let cocktail: Cocktail

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Image(cocktail.image.replacingOccurrences(of: ".webp", with: ""))
                .resizable()
                .scaledToFill()
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
