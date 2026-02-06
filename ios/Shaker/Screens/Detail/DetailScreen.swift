import SwiftUI

struct DetailScreen: View {

    let cocktailId: String
    @StateObject private var viewModel: DetailViewModel

    init(cocktailId: String) {
        self.cocktailId = cocktailId
        _viewModel = StateObject(wrappedValue: DetailViewModel(cocktailId: cocktailId))
    }

    var body: some View {
        Group {
            if let cocktail = viewModel.cocktail {
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        // Hero image
                        Image(cocktail.image.replacingOccurrences(of: ".webp", with: ""))
                            .resizable()
                            .scaledToFill()
                            .frame(height: 300)
                            .clipped()

                        VStack(alignment: .leading, spacing: 16) {
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
                            VStack(alignment: .leading, spacing: 8) {
                                Text("Ingredients")
                                    .font(.title2)
                                    .fontWeight(.semibold)

                                ForEach(cocktail.ingredients) { ingredient in
                                    HStack {
                                        Text(ingredient.name)
                                            .font(.body)
                                        Spacer()
                                        // TODO: Gate amounts behind premium in Phase 2
                                        Text(ingredient.amount)
                                            .font(.body)
                                            .foregroundStyle(.secondary)
                                    }
                                    .padding(.vertical, 2)
                                }
                            }

                            // Instructions
                            VStack(alignment: .leading, spacing: 8) {
                                Text("Instructions")
                                    .font(.title2)
                                    .fontWeight(.semibold)

                                // TODO: Add blur overlay + CTA for free users in Phase 2
                                ForEach(Array(cocktail.instructions.enumerated()), id: \.offset) { index, instruction in
                                    Text("\(index + 1). \(instruction)")
                                        .font(.body)
                                        .padding(.vertical, 2)
                                }
                            }
                        }
                        .padding(16)
                    }
                }
                .navigationTitle(cocktail.name)
                .navigationBarTitleDisplayMode(.inline)
            }
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
