@testable import Shaker

func testCocktail(
    id: String = "1",
    name: String = "Mojito",
    spirit: String = "Rum",
    category: String = "Classic",
    difficulty: String = "Easy"
) -> Cocktail {
    Cocktail(
        id: id,
        name: name,
        image: "\(id).jpg",
        description: "A test cocktail",
        category: category,
        spirit: spirit,
        difficulty: difficulty,
        tags: ["test"],
        ingredients: [Ingredient(name: "Ingredient", amount: "60ml")],
        instructions: ["Mix and serve"]
    )
}
