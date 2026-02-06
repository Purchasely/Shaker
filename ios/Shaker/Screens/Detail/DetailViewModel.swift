import Foundation

class DetailViewModel: ObservableObject {

    @Published var cocktail: Cocktail?

    private let repository = CocktailRepository.shared

    init(cocktailId: String) {
        cocktail = repository.cocktail(byId: cocktailId)
    }
}
