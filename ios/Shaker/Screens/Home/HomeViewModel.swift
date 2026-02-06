import Foundation
import Combine

class HomeViewModel: ObservableObject {

    @Published var cocktails: [Cocktail] = []
    @Published var searchQuery = ""

    private let repository = CocktailRepository.shared
    private var cancellables = Set<AnyCancellable>()

    init() {
        let allCocktails = repository.allCocktails()

        $searchQuery
            .debounce(for: .milliseconds(200), scheduler: RunLoop.main)
            .map { query in
                if query.isEmpty {
                    return allCocktails
                }
                return allCocktails.filter {
                    $0.name.localizedCaseInsensitiveContains(query)
                }
            }
            .assign(to: &$cocktails)

        cocktails = allCocktails
    }
}
