import Foundation

class CocktailRepository {

    static let shared = CocktailRepository()

    private var cocktails: [Cocktail] = []

    private init() {
        loadCocktails()
    }

    @discardableResult
    func loadCocktails() -> [Cocktail] {
        guard cocktails.isEmpty else { return cocktails }

        guard let url = Bundle.main.url(forResource: "cocktails", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let decoded = try? JSONDecoder().decode(CocktailsData.self, from: data) else {
            print("[Shaker] Failed to load cocktails.json")
            return []
        }

        cocktails = decoded.cocktails
        return cocktails
    }

    func allCocktails() -> [Cocktail] {
        return cocktails
    }

    func cocktail(byId id: String) -> Cocktail? {
        return cocktails.first { $0.id == id }
    }

    func spirits() -> [String] {
        return Array(Set(cocktails.map(\.spirit))).sorted()
    }

    func categories() -> [String] {
        return Array(Set(cocktails.map(\.category))).sorted()
    }

    func difficulties() -> [String] {
        return Array(Set(cocktails.map(\.difficulty)))
    }
}
