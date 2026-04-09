import UIKit
import Combine

class HomeViewModel: ObservableObject {

    @Published var cocktails: [Cocktail] = []
    @Published var searchQuery = ""

    // Filter state
    @Published var selectedSpirits: Set<String> = []
    @Published var selectedCategories: Set<String> = []
    @Published var selectedDifficulty: String?

    // Prefetched presentations
    @Published var inlinePresentation: FetchResult?
    @Published var filtersPresentation: FetchResult?
    @Published var isFiltersLoading = false

    private let repository = CocktailRepository.shared
    private let wrapper = PurchaselyWrapper.shared
    private var cancellables = Set<AnyCancellable>()

    var availableSpirits: [String] { repository.spirits() }
    var availableCategories: [String] { repository.categories() }
    var availableDifficulties: [String] { repository.difficulties() }

    var hasActiveFilters: Bool {
        !selectedSpirits.isEmpty || !selectedCategories.isEmpty || selectedDifficulty != nil
    }

    /// The UIViewController for the inline embedded presentation, if available.
    var inlineController: UIViewController? {
        guard case .success(let presentation) = inlinePresentation else { return nil }
        return wrapper.getController(presentation: presentation)
    }

    /// The height (dp/points) for the inline presentation, 0 if unknown.
    var inlineHeight: Int {
        inlinePresentation?.height ?? 0
    }

    init() {
        let allCocktails = repository.allCocktails()

        Publishers.CombineLatest4(
            $searchQuery.debounce(for: .milliseconds(200), scheduler: RunLoop.main),
            $selectedSpirits,
            $selectedCategories,
            $selectedDifficulty
        )
        .handleEvents(receiveOutput: { [weak self] query, _, _, _ in
            if !query.isEmpty {
                self?.wrapper.setUserAttribute(true, forKey: "has_used_search")
            }
        })
        .map { query, spirits, categories, difficulty in
            allCocktails.filter { cocktail in
                let matchesQuery = query.isEmpty || cocktail.name.localizedCaseInsensitiveContains(query)
                let matchesSpirit = spirits.isEmpty || spirits.contains(cocktail.spirit)
                let matchesCategory = categories.isEmpty || categories.contains(cocktail.category)
                let matchesDifficulty = difficulty == nil || cocktail.difficulty == difficulty
                return matchesQuery && matchesSpirit && matchesCategory && matchesDifficulty
            }
        }
        .assign(to: &$cocktails)

        cocktails = allCocktails
    }

    func prefetchPresentations(isPremium: Bool) {
        guard !isPremium else { return }

        Task {
            isFiltersLoading = true
            filtersPresentation = await wrapper.loadPresentation(placementId: "filters") { result in
                if case .purchased = result { PremiumManager.shared.refreshPremiumStatus() }
                if case .restored = result { PremiumManager.shared.refreshPremiumStatus() }
            }
            isFiltersLoading = false
        }

        Task {
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
