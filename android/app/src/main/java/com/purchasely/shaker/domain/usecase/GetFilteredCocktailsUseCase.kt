package com.purchasely.shaker.domain.usecase

import com.purchasely.shaker.domain.model.Cocktail
import com.purchasely.shaker.domain.model.CocktailMood
import com.purchasely.shaker.domain.repository.CocktailRepository

class GetFilteredCocktailsUseCase(
    private val repository: CocktailRepository
) {
    operator fun invoke(
        query: String = "",
        spirits: Set<String> = emptySet(),
        categories: Set<String> = emptySet(),
        difficulty: String? = null,
        mood: CocktailMood? = null,
    ): List<Cocktail> {
        return repository.loadCocktails().filter { cocktail ->
            val matchesQuery = query.isBlank() || cocktail.name.contains(query, ignoreCase = true)
            val matchesSpirit = spirits.isEmpty() || spirits.contains(cocktail.spirit)
            val matchesCategory = categories.isEmpty() || categories.contains(cocktail.category)
            val matchesDifficulty = difficulty == null || cocktail.difficulty == difficulty
            val matchesMood = mood == null || mood.matches(cocktail)
            matchesQuery && matchesSpirit && matchesCategory && matchesDifficulty && matchesMood
        }
    }
}
