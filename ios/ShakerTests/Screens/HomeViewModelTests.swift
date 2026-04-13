import XCTest
import Combine
@testable import Shaker

final class HomeViewModelTests: XCTestCase {

    private var mockWrapper: MockPurchaselyWrapper!
    private var cancellables: Set<AnyCancellable>!

    private let testCocktails = [
        testCocktail(id: "1", name: "Mojito", spirit: "Rum", category: "Classic", difficulty: "Easy"),
        testCocktail(id: "2", name: "Margarita", spirit: "Tequila", category: "Classic", difficulty: "Medium"),
        testCocktail(id: "3", name: "Negroni", spirit: "Gin", category: "Bitter", difficulty: "Easy"),
        testCocktail(id: "4", name: "Old Fashioned", spirit: "Whiskey", category: "Classic", difficulty: "Hard"),
        testCocktail(id: "5", name: "Daiquiri", spirit: "Rum", category: "Tropical", difficulty: "Easy")
    ]

    override func setUp() {
        super.setUp()
        mockWrapper = MockPurchaselyWrapper()
        cancellables = []
    }

    override func tearDown() {
        cancellables = nil
        super.tearDown()
    }

    private func createViewModel() -> HomeViewModel {
        let repo = CocktailRepository(cocktails: testCocktails)
        return HomeViewModel(repository: repo, wrapper: mockWrapper)
    }

    // MARK: - Initial state

    func testInitialCocktailsLoaded() {
        let vm = createViewModel()
        XCTAssertEqual(vm.cocktails.count, 5)
    }

    func testAvailableSpirits() {
        let vm = createViewModel()
        let spirits = vm.availableSpirits
        XCTAssertTrue(spirits.contains("Rum"))
        XCTAssertTrue(spirits.contains("Gin"))
        XCTAssertTrue(spirits.contains("Tequila"))
        XCTAssertTrue(spirits.contains("Whiskey"))
    }

    func testAvailableCategories() {
        let vm = createViewModel()
        let categories = vm.availableCategories
        XCTAssertTrue(categories.contains("Classic"))
        XCTAssertTrue(categories.contains("Bitter"))
        XCTAssertTrue(categories.contains("Tropical"))
    }

    func testAvailableDifficulties() {
        let vm = createViewModel()
        let difficulties = vm.availableDifficulties
        XCTAssertTrue(difficulties.contains("Easy"))
        XCTAssertTrue(difficulties.contains("Medium"))
        XCTAssertTrue(difficulties.contains("Hard"))
    }

    func testHasActiveFiltersInitiallyFalse() {
        let vm = createViewModel()
        XCTAssertFalse(vm.hasActiveFilters)
    }

    // MARK: - Spirit filter

    func testToggleSpiritAdds() {
        let vm = createViewModel()
        vm.toggleSpirit("Rum")
        XCTAssertTrue(vm.selectedSpirits.contains("Rum"))
        XCTAssertTrue(vm.hasActiveFilters)
    }

    func testToggleSpiritRemoves() {
        let vm = createViewModel()
        vm.toggleSpirit("Rum")
        vm.toggleSpirit("Rum")
        XCTAssertFalse(vm.selectedSpirits.contains("Rum"))
        XCTAssertFalse(vm.hasActiveFilters)
    }

    // MARK: - Category filter

    func testToggleCategoryAdds() {
        let vm = createViewModel()
        vm.toggleCategory("Classic")
        XCTAssertTrue(vm.selectedCategories.contains("Classic"))
        XCTAssertTrue(vm.hasActiveFilters)
    }

    func testToggleCategoryRemoves() {
        let vm = createViewModel()
        vm.toggleCategory("Classic")
        vm.toggleCategory("Classic")
        XCTAssertFalse(vm.selectedCategories.contains("Classic"))
    }

    // MARK: - Difficulty filter

    func testSelectDifficulty() {
        let vm = createViewModel()
        vm.selectDifficulty("Easy")
        XCTAssertEqual(vm.selectedDifficulty, "Easy")
        XCTAssertTrue(vm.hasActiveFilters)
    }

    func testSelectDifficultyTogglesOff() {
        let vm = createViewModel()
        vm.selectDifficulty("Easy")
        vm.selectDifficulty("Easy")
        XCTAssertNil(vm.selectedDifficulty)
    }

    // MARK: - Clear filters

    func testClearFilters() {
        let vm = createViewModel()
        vm.toggleSpirit("Rum")
        vm.toggleCategory("Classic")
        vm.selectDifficulty("Easy")
        vm.clearFilters()
        XCTAssertTrue(vm.selectedSpirits.isEmpty)
        XCTAssertTrue(vm.selectedCategories.isEmpty)
        XCTAssertNil(vm.selectedDifficulty)
        XCTAssertFalse(vm.hasActiveFilters)
    }

    // MARK: - Filtering logic (via Combine pipeline)

    func testSpiritFilterUpdatesResults() {
        let vm = createViewModel()
        let expectation = expectation(description: "Cocktails filtered")

        vm.toggleSpirit("Rum")

        // Wait for Combine pipeline (debounce on searchQuery, immediate on spirits)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            let filtered = vm.cocktails
            XCTAssertEqual(filtered.count, 2)
            XCTAssertTrue(filtered.allSatisfy { $0.spirit == "Rum" })
            expectation.fulfill()
        }

        wait(for: [expectation], timeout: 1.0)
    }

    func testCategoryFilterUpdatesResults() {
        let vm = createViewModel()
        let expectation = expectation(description: "Cocktails filtered by category")

        vm.toggleCategory("Classic")

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            let filtered = vm.cocktails
            XCTAssertEqual(filtered.count, 3)
            XCTAssertTrue(filtered.allSatisfy { $0.category == "Classic" })
            expectation.fulfill()
        }

        wait(for: [expectation], timeout: 1.0)
    }

    func testDifficultyFilterUpdatesResults() {
        let vm = createViewModel()
        let expectation = expectation(description: "Cocktails filtered by difficulty")

        vm.selectDifficulty("Easy")

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            let filtered = vm.cocktails
            XCTAssertEqual(filtered.count, 3)
            XCTAssertTrue(filtered.allSatisfy { $0.difficulty == "Easy" })
            expectation.fulfill()
        }

        wait(for: [expectation], timeout: 1.0)
    }

    func testSearchFilterUpdatesResults() {
        let vm = createViewModel()
        let expectation = expectation(description: "Cocktails filtered by search")

        vm.searchQuery = "Mojito"

        // 200ms debounce + buffer
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
            let filtered = vm.cocktails
            XCTAssertEqual(filtered.count, 1)
            XCTAssertEqual(filtered.first?.name, "Mojito")
            expectation.fulfill()
        }

        wait(for: [expectation], timeout: 1.0)
    }

    func testSearchSetsUserAttribute() {
        let vm = createViewModel()
        let expectation = expectation(description: "User attribute set")

        vm.searchQuery = "test"

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
            XCTAssertTrue(self.mockWrapper.setBoolAttributeCalls.contains(where: {
                $0.key == "has_used_search" && $0.value == true
            }))
            expectation.fulfill()
        }

        wait(for: [expectation], timeout: 1.0)
    }

    func testCombinedSpiritAndCategoryFilter() {
        let vm = createViewModel()
        let expectation = expectation(description: "Combined filter")

        vm.toggleSpirit("Rum")
        vm.toggleCategory("Classic")

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            let filtered = vm.cocktails
            XCTAssertEqual(filtered.count, 1)
            XCTAssertEqual(filtered.first?.name, "Mojito")
            expectation.fulfill()
        }

        wait(for: [expectation], timeout: 1.0)
    }

    // MARK: - Prefetch

    func testPrefetchCallsWrapper() {
        let vm = createViewModel()
        vm.prefetchPresentations(isPremium: false)

        let expectation = expectation(description: "Prefetch completed")
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            XCTAssertTrue(self.mockWrapper.loadPresentationCalls.contains(where: { $0.placementId == "filters" }))
            XCTAssertTrue(self.mockWrapper.loadPresentationCalls.contains(where: { $0.placementId == "inline" }))
            expectation.fulfill()
        }
        wait(for: [expectation], timeout: 1.0)
    }

    func testPrefetchSkipsWhenPremium() {
        let vm = createViewModel()
        vm.prefetchPresentations(isPremium: true)

        let expectation = expectation(description: "No prefetch")
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            XCTAssertTrue(self.mockWrapper.loadPresentationCalls.isEmpty)
            expectation.fulfill()
        }
        wait(for: [expectation], timeout: 1.0)
    }
}
