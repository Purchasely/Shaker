import XCTest
@testable import Shaker

/// Mirrors the Android `CocktailMoodTest` — same matching semantics and
/// the same pinned attribute keys so console audiences behave identically.
final class CocktailMoodTests: XCTestCase {

    private func cocktail(spirit: String = "rum", tags: [String]) -> Cocktail {
        Cocktail(
            id: "test",
            name: "Test",
            image: "test.jpg",
            description: "A test cocktail",
            category: "classic",
            spirit: spirit,
            difficulty: "easy",
            tags: tags,
            ingredients: [],
            instructions: []
        )
    }

    func testRefreshingMatchesRefreshingOrMintyTags() {
        XCTAssertTrue(CocktailMood.refreshing.matches(cocktail(tags: ["refreshing", "summer"])))
        XCTAssertTrue(CocktailMood.refreshing.matches(cocktail(tags: ["minty"])))
        XCTAssertFalse(CocktailMood.refreshing.matches(cocktail(tags: ["sweet"])))
    }

    func testStrongMatchesBoldAndSmokyTags() {
        XCTAssertTrue(CocktailMood.strong.matches(cocktail(tags: ["bold"])))
        XCTAssertTrue(CocktailMood.strong.matches(cocktail(tags: ["smoky", "complex"])))
        XCTAssertFalse(CocktailMood.strong.matches(cocktail(tags: ["light"])))
    }

    func testSweetMatchesSweetFruityCreamyTropicalTags() {
        XCTAssertTrue(CocktailMood.sweet.matches(cocktail(tags: ["fruity"])))
        XCTAssertTrue(CocktailMood.sweet.matches(cocktail(tags: ["creamy"])))
        XCTAssertFalse(CocktailMood.sweet.matches(cocktail(tags: ["bitter"])))
    }

    func testZeroProofMatchesOnSpiritNotTags() {
        XCTAssertTrue(CocktailMood.zeroProof.matches(cocktail(spirit: "non-alcoholic", tags: ["bold"])))
        XCTAssertFalse(CocktailMood.zeroProof.matches(cocktail(spirit: "rum", tags: ["refreshing"])))
    }

    func testPartyMatchesCelebratoryAndBubblyTags() {
        XCTAssertTrue(CocktailMood.party.matches(cocktail(tags: ["bubbly"])))
        XCTAssertTrue(CocktailMood.party.matches(cocktail(tags: ["celebratory", "elegant"])))
        XCTAssertFalse(CocktailMood.party.matches(cocktail(tags: ["rustic"])))
    }

    func testEveryMoodExposesAStableKeyForThePurchaselyUserAttribute() {
        // Keys feed the `preferred_mood` user attribute used by console audiences —
        // renaming one silently breaks targeting AND platform parity, so pin them.
        XCTAssertEqual(
            CocktailMood.allCases.map(\.key),
            ["refreshing", "strong", "sweet", "zero_proof", "party"]
        )
    }
}
