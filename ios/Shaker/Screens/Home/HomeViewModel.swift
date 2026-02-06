import Foundation
import Combine

class HomeViewModel: ObservableObject {

    @Published var cocktails: [Cocktail] = []
    @Published var searchQuery = ""

    // Filter state
    @Published var selectedSpirits: Set<String> = []
    @Published var selectedCategories: Set<String> = []
    @Published var selectedDifficulty: String?

    private let repository = CocktailRepository.shared
    private var cancellables = Set<AnyCancellable>()

    var availableSpirits: [String] { repository.spirits() }
    var availableCategories: [String] { repository.categories() }
    var availableDifficulties: [String] { repository.difficulties() }

    var hasActiveFilters: Bool {
        !selectedSpirits.isEmpty || !selectedCategories.isEmpty || selectedDifficulty != nil
    }

    init() {
        let allCocktails = repository.allCocktails()

        Publishers.CombineLatest4(
            $searchQuery.debounce(for: .milliseconds(200), scheduler: RunLoop.main),
            $selectedSpirits,
            $selectedCategories,
            $selectedDifficulty
        )
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
