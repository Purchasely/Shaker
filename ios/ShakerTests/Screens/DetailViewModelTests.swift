import XCTest
@testable import Shaker

final class DetailViewModelTests: XCTestCase {

    private var mockWrapper: MockPurchaselyWrapper!

    private let mojito = testCocktail(id: "mojito", name: "Mojito", spirit: "Rum")
    private let margarita = testCocktail(id: "margarita", name: "Margarita", spirit: "Tequila")

    override func setUp() {
        super.setUp()
        mockWrapper = MockPurchaselyWrapper()
    }

    private func createViewModel(cocktailId: String = "mojito") -> DetailViewModel {
        let repo = CocktailRepository(cocktails: [mojito, margarita])
        return DetailViewModel(cocktailId: cocktailId, repository: repo, wrapper: mockWrapper)
    }

    // MARK: - Cocktail loading

    func testLoadsCocktailById() {
        let vm = createViewModel()
        XCTAssertEqual(vm.cocktail?.name, "Mojito")
    }

    func testNilCocktailWhenNotFound() {
        let vm = createViewModel(cocktailId: "nonexistent")
        XCTAssertNil(vm.cocktail)
    }

    // MARK: - User attribute tracking

    func testTracksCocktailsViewedOnInit() {
        _ = createViewModel()
        XCTAssertTrue(mockWrapper.incrementAttributeCalls.contains("cocktails_viewed"))
    }

    func testTracksFavoriteSpiritOnInit() {
        _ = createViewModel()
        XCTAssertTrue(mockWrapper.setStringAttributeCalls.contains(where: {
            $0.key == "favorite_spirit" && $0.value == "Rum"
        }))
    }

    func testDoesNotTrackSpiritWhenCocktailNotFound() {
        _ = createViewModel(cocktailId: "nonexistent")
        XCTAssertFalse(mockWrapper.setStringAttributeCalls.contains(where: {
            $0.key == "favorite_spirit"
        }))
    }

    func testTracksDifferentSpirits() {
        let repo = CocktailRepository(cocktails: [mojito, margarita])
        _ = DetailViewModel(cocktailId: "margarita", repository: repo, wrapper: mockWrapper)
        XCTAssertTrue(mockWrapper.setStringAttributeCalls.contains(where: {
            $0.key == "favorite_spirit" && $0.value == "Tequila"
        }))
    }

    // MARK: - Prefetch presentations

    func testPrefetchRecipePresentation() {
        let vm = createViewModel()
        vm.prefetchRecipePresentation(contentId: "mojito")

        let expectation = expectation(description: "Prefetch recipe")
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            XCTAssertTrue(self.mockWrapper.loadPresentationCalls.contains(where: {
                $0.placementId == "recipe_detail" && $0.contentId == "mojito"
            }))
            expectation.fulfill()
        }
        wait(for: [expectation], timeout: 1.0)
    }

    func testPrefetchFavoritesPresentation() {
        let vm = createViewModel()
        vm.prefetchFavoritesPresentation()

        let expectation = expectation(description: "Prefetch favorites")
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            XCTAssertTrue(self.mockWrapper.loadPresentationCalls.contains(where: {
                $0.placementId == "favorites"
            }))
            expectation.fulfill()
        }
        wait(for: [expectation], timeout: 1.0)
    }
}
