import Foundation

class FavoritesRepository: ObservableObject {

    static let shared = FavoritesRepository()

    @Published var favoriteIds: Set<String> = []

    private let defaults: UserDefaults
    private let key: String

    private init() {
        self.defaults = .standard
        self.key = "favorite_cocktail_ids"
        let saved = defaults.stringArray(forKey: key) ?? []
        favoriteIds = Set(saved)
    }

    /// For testing: create a repository with a custom UserDefaults suite
    init(defaults: UserDefaults, key: String = "favorite_cocktail_ids") {
        self.defaults = defaults
        self.key = key
        let saved = defaults.stringArray(forKey: key) ?? []
        favoriteIds = Set(saved)
    }

    func isFavorite(_ cocktailId: String) -> Bool {
        favoriteIds.contains(cocktailId)
    }

    func toggleFavorite(_ cocktailId: String) {
        if favoriteIds.contains(cocktailId) {
            favoriteIds.remove(cocktailId)
        } else {
            favoriteIds.insert(cocktailId)
        }
        save()
    }

    func addFavorite(_ cocktailId: String) {
        favoriteIds.insert(cocktailId)
        save()
    }

    func removeFavorite(_ cocktailId: String) {
        favoriteIds.remove(cocktailId)
        save()
    }

    private func save() {
        defaults.set(Array(favoriteIds), forKey: key)
    }
}
