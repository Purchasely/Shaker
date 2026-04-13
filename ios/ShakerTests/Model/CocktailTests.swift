import XCTest
@testable import Shaker

final class CocktailTests: XCTestCase {

    func testDecodeSingleCocktail() throws {
        let json = """
        {
            "id": "mojito",
            "name": "Mojito",
            "image": "mojito.jpg",
            "description": "A refreshing Cuban cocktail",
            "category": "Classic",
            "spirit": "Rum",
            "difficulty": "Easy",
            "tags": ["refreshing", "summer"],
            "ingredients": [
                {"name": "White Rum", "amount": "60ml"},
                {"name": "Lime Juice", "amount": "30ml"}
            ],
            "instructions": ["Muddle mint", "Add rum and lime", "Top with soda"]
        }
        """.data(using: .utf8)!

        let cocktail = try JSONDecoder().decode(Cocktail.self, from: json)

        XCTAssertEqual(cocktail.id, "mojito")
        XCTAssertEqual(cocktail.name, "Mojito")
        XCTAssertEqual(cocktail.image, "mojito.jpg")
        XCTAssertEqual(cocktail.description, "A refreshing Cuban cocktail")
        XCTAssertEqual(cocktail.category, "Classic")
        XCTAssertEqual(cocktail.spirit, "Rum")
        XCTAssertEqual(cocktail.difficulty, "Easy")
        XCTAssertEqual(cocktail.tags, ["refreshing", "summer"])
        XCTAssertEqual(cocktail.ingredients.count, 2)
        XCTAssertEqual(cocktail.ingredients[0].name, "White Rum")
        XCTAssertEqual(cocktail.ingredients[0].amount, "60ml")
        XCTAssertEqual(cocktail.instructions.count, 3)
    }

    func testDecodeCocktailsData() throws {
        let json = """
        {
            "cocktails": [
                {
                    "id": "1", "name": "Mojito", "image": "mojito.jpg",
                    "description": "Desc", "category": "Classic", "spirit": "Rum",
                    "difficulty": "Easy", "tags": [], "ingredients": [], "instructions": []
                },
                {
                    "id": "2", "name": "Margarita", "image": "margarita.jpg",
                    "description": "Desc", "category": "Classic", "spirit": "Tequila",
                    "difficulty": "Medium", "tags": [], "ingredients": [], "instructions": []
                }
            ]
        }
        """.data(using: .utf8)!

        let data = try JSONDecoder().decode(CocktailsData.self, from: json)
        XCTAssertEqual(data.cocktails.count, 2)
        XCTAssertEqual(data.cocktails[0].name, "Mojito")
        XCTAssertEqual(data.cocktails[1].name, "Margarita")
    }

    func testCocktailWithEmptyCollections() {
        let cocktail = testCocktail()
        XCTAssertFalse(cocktail.tags.isEmpty) // testCocktail has ["test"]
        XCTAssertEqual(cocktail.ingredients.count, 1)
        XCTAssertEqual(cocktail.instructions.count, 1)
    }

    func testIngredientIdentifiable() {
        let ingredient = Ingredient(name: "Rum", amount: "60ml")
        XCTAssertEqual(ingredient.id, "Rum")
    }

    func testCocktailIdentifiable() {
        let cocktail = testCocktail(id: "mojito")
        XCTAssertEqual(cocktail.id, "mojito")
    }

    func testEncodeDecode() throws {
        let original = testCocktail(id: "test", name: "Test Cocktail")
        let data = try JSONEncoder().encode(original)
        let decoded = try JSONDecoder().decode(Cocktail.self, from: data)
        XCTAssertEqual(original.id, decoded.id)
        XCTAssertEqual(original.name, decoded.name)
        XCTAssertEqual(original.spirit, decoded.spirit)
    }
}
