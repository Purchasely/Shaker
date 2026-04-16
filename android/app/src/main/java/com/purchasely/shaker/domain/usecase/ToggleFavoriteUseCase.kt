package com.purchasely.shaker.domain.usecase

import com.purchasely.shaker.domain.repository.FavoritesRepository

class ToggleFavoriteUseCase(
    private val favoritesRepository: FavoritesRepository
) {
    operator fun invoke(cocktailId: String) {
        favoritesRepository.toggleFavorite(cocktailId)
    }
}
