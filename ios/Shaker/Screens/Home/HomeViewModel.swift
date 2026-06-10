import UIKit

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
    @Published var selectedMood: CocktailMood? {
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
        !selectedSpirits.isEmpty || !selectedCategories.isEmpty
            || selectedDifficulty != nil || selectedMood != nil
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
            let matchesMood = selectedMood == nil || selectedMood!.matches(cocktail)
            return matchesQuery && matchesSpirit && matchesCategory && matchesDifficulty && matchesMood
        }
    }

    // MARK: - Mood-based discovery

    /// Selecting a mood filters the catalog by tags and reports the preference
    /// to Purchasely for audience targeting. Mirrors the Android `selectMood`.
    func selectMood(_ mood: CocktailMood?) {
        let newMood = selectedMood == mood ? nil : mood
        selectedMood = newMood
        if let newMood {
            // PURCHASELY: track the user's drinking mood as a custom attribute. Console-side
            // audiences can target e.g. preferred_mood == "zero_proof" with a dedicated screen.
            // Docs: https://docs.purchasely.com/advanced-features/user-attributes
            wrapper.setUserAttribute(newMood.key, forKey: "preferred_mood")
        }
    }

    // MARK: - Surprise me

    /// "Surprise me" — picks a random cocktail from the currently filtered list.
    /// Returns its id for navigation, or nil when the list is empty.
    /// Mirrors the Android `onSurpriseMe`.
    func surpriseMe() -> String? {
        guard let pick = cocktails.randomElement() else { return nil }
        // PURCHASELY: count how often the user asks for a surprise. A console campaign can
        // trigger a dedicated screen after N uses (engaged-user segmentation).
        wrapper.incrementUserAttribute(forKey: "surprise_me_count")
        return pick.id
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
        switch filtersPresentation {
        case .success(let handle):
            wrapper.display(handle: handle, from: viewController)
        case .error(let error):
            // PURCHASELY: the prefetch failed (e.g. offline at launch). Log and retry it so
            // the paywall becomes available instead of leaving the button dead forever.
            print("[Shaker] Filters paywall unavailable (\(error?.localizedDescription ?? "unknown")), retrying prefetch")
            retryFiltersPrefetch()
        case .deactivated, .client:
            print("[Shaker] Filters paywall not displayable: \(String(describing: filtersPresentation))")
        case nil:
            print("[Shaker] Filters paywall still loading")
        }
    }

    private func retryFiltersPrefetch() {
        guard !isFiltersLoading else { return }
        Task { @MainActor in
            isFiltersLoading = true
            filtersPresentation = await wrapper.loadPresentation(placementId: "filters") { result in
                if case .purchased = result { PremiumManager.shared.refreshPremiumStatus() }
                if case .restored = result { PremiumManager.shared.refreshPremiumStatus() }
            }
            isFiltersLoading = false
        }
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
        selectedMood = nil
    }
}
