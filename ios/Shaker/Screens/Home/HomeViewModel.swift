import UIKit
@preconcurrency import Purchasely

@MainActor
class HomeViewModel: ObservableObject {

    @Published var cocktails: [Cocktail] = []
    @Published var searchQuery = "" {
        didSet {
            if !searchQuery.isEmpty {
                wrapper.setUserAttribute(true, forKey: "has_used_search")
            }
            applyFilters()
        }
    }

    // Filter state
    @Published var selectedSpirits: Set<String> = [] {
        didSet { applyFilters() }
    }
    @Published var selectedCategories: Set<String> = [] {
        didSet { applyFilters() }
    }
    @Published var selectedDifficulty: String? {
        didSet { applyFilters() }
    }

    // Prefetched presentations
    @Published var inlinePresentation: FetchResult?
    @Published var filtersPresentation: FetchResult?
    @Published var isFiltersLoading = false

    private let repository: CocktailRepository
    private let wrapper: PurchaselyWrapping
    private let allCocktails: [Cocktail]

    var availableSpirits: [String] { repository.spirits() }
    var availableCategories: [String] { repository.categories() }
    var availableDifficulties: [String] { repository.difficulties() }

    var hasActiveFilters: Bool {
        !selectedSpirits.isEmpty || !selectedCategories.isEmpty || selectedDifficulty != nil
    }

    /// The PLYPresentationViewController for the inline embedded presentation, if available.
    var inlineController: PLYPresentationViewController? {
        guard case .success(let presentation) = inlinePresentation else { return nil }
        return wrapper.getController(presentation: presentation)
    }

    /// The height (dp/points) for the inline presentation, 0 if unknown.
    var inlineHeight: Int {
        inlinePresentation?.height ?? 0
    }

    init(repository: CocktailRepository = .shared,
         wrapper: PurchaselyWrapping = PurchaselyWrapper.shared) {
        self.repository = repository
        self.wrapper = wrapper
        self.allCocktails = repository.allCocktails()
        cocktails = allCocktails
    }

    private func applyFilters() {
        cocktails = allCocktails.filter { cocktail in
            let matchesQuery = searchQuery.isEmpty || cocktail.name.localizedCaseInsensitiveContains(searchQuery)
            let matchesSpirit = selectedSpirits.isEmpty || selectedSpirits.contains(cocktail.spirit)
            let matchesCategory = selectedCategories.isEmpty || selectedCategories.contains(cocktail.category)
            let matchesDifficulty = selectedDifficulty == nil || cocktail.difficulty == selectedDifficulty
            return matchesQuery && matchesSpirit && matchesCategory && matchesDifficulty
        }
    }

    func prefetchPresentations(isPremium: Bool) {
        guard !isPremium else { return }

        Task { @MainActor in
            isFiltersLoading = true
            filtersPresentation = await wrapper.loadPresentation(placementId: "filters") { result in
                if case .purchased = result { PremiumManager.shared.refreshPremiumStatus() }
                if case .restored = result { PremiumManager.shared.refreshPremiumStatus() }
            }
            isFiltersLoading = false
        }

        Task { @MainActor in
            inlinePresentation = await wrapper.loadPresentation(placementId: "inline") { result in
                if case .purchased = result { PremiumManager.shared.refreshPremiumStatus() }
                if case .restored = result { PremiumManager.shared.refreshPremiumStatus() }
            }
        }
    }

    func displayFiltersPaywall(from viewController: UIViewController?) {
        guard case .success(let presentation) = filtersPresentation else { return }
        wrapper.display(presentation: presentation, from: viewController)
    }

    func toggleSpirit(_ spirit: String) {
        if selectedSpirits.contains(spirit) {
            selectedSpirits.remove(spirit)
        } else {
            selectedSpirits.insert(spirit)
        }
    }

    func toggleCategory(_ category: String) {
        if selectedCategories.contains(category) {
            selectedCategories.remove(category)
        } else {
            selectedCategories.insert(category)
        }
    }

    func selectDifficulty(_ difficulty: String) {
        selectedDifficulty = selectedDifficulty == difficulty ? nil : difficulty
    }

    func clearFilters() {
        selectedSpirits = []
        selectedCategories = []
        selectedDifficulty = nil
    }
}
