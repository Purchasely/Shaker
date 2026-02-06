import Foundation

struct CocktailsData: Codable {
    let cocktails: [Cocktail]
}

struct Cocktail: Codable, Identifiable {
    let id: String
    let name: String
    let image: String
    let description: String
    let category: String
    let spirit: String
    let difficulty: String
    let tags: [String]
    let ingredients: [Ingredient]
    let instructions: [String]
}

struct Ingredient: Codable, Identifiable {
    let name: String
    let amount: String

    var id: String { name }
}
