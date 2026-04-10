import XCTest
@testable import Shaker

final class FavoritesRepositoryTests: XCTestCase {

    private var defaults: UserDefaults!
    private var repo: FavoritesRepository!

    override func setUp() {
        super.setUp()
        defaults = UserDefaults(suiteName: "FavoritesRepositoryTests")!
        defaults.removePersistentDomain(forName: "FavoritesRepositoryTests")
        repo = FavoritesRepository(defaults: defaults)
    }

    override func tearDown() {
        defaults.removePersistentDomain(forName: "FavoritesRepositoryTests")
        super.tearDown()
    }

    func testInitialStateIsEmpty() {
        XCTAssertTrue(repo.favoriteIds.isEmpty)
    }

    func testAddFavorite() {
        repo.addFavorite("cocktail1")
        XCTAssertTrue(repo.favoriteIds.contains("cocktail1"))
    }

    func testRemoveFavorite() {
        repo.addFavorite("cocktail1")
        repo.removeFavorite("cocktail1")
        XCTAssertFalse(repo.favoriteIds.contains("cocktail1"))
    }

    func testToggleFavoriteAddsWhenNotPresent() {
        repo.toggleFavorite("cocktail1")
        XCTAssertTrue(repo.favoriteIds.contains("cocktail1"))
    }

    func testToggleFavoriteRemovesWhenPresent() {
        repo.addFavorite("cocktail1")
        repo.toggleFavorite("cocktail1")
        XCTAssertFalse(repo.favoriteIds.contains("cocktail1"))
    }

    func testIsFavorite() {
        repo.addFavorite("cocktail1")
        XCTAssertTrue(repo.isFavorite("cocktail1"))
        XCTAssertFalse(repo.isFavorite("cocktail2"))
    }

    func testMultipleOperations() {
        repo.addFavorite("a")
        repo.addFavorite("b")
        repo.addFavorite("c")
        XCTAssertEqual(repo.favoriteIds, Set(["a", "b", "c"]))

        repo.removeFavorite("b")
        XCTAssertEqual(repo.favoriteIds, Set(["a", "c"]))
    }

    func testAddFavoriteIsIdempotent() {
        repo.addFavorite("cocktail1")
        repo.addFavorite("cocktail1")
        XCTAssertEqual(repo.favoriteIds.count, 1)
    }

    func testRemoveNonExistentIsNoOp() {
        repo.removeFavorite("nonexistent")
        XCTAssertTrue(repo.favoriteIds.isEmpty)
    }

    func testPersistence() {
        repo.addFavorite("cocktail1")
        repo.addFavorite("cocktail2")

        // Create a new repo with the same UserDefaults
        let newRepo = FavoritesRepository(defaults: defaults)
        XCTAssertTrue(newRepo.favoriteIds.contains("cocktail1"))
        XCTAssertTrue(newRepo.favoriteIds.contains("cocktail2"))
    }
}
