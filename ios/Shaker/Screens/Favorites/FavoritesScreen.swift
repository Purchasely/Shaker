import SwiftUI

struct FavoritesScreen: View {

    var body: some View {
        // TODO: Implement favorites list with gating in Phase 3
        ContentUnavailableView(
            "Favorites coming soon",
            systemImage: "heart",
            description: Text("Save your favorite cocktails here.")
        )
        .navigationTitle("Favorites")
    }
}
