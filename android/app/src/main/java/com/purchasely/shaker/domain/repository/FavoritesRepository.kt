package com.purchasely.shaker.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface FavoritesRepository {
    val favoriteIds: StateFlow<Set<String>>
    fun isFavorite(cocktailId: String): Boolean
    fun toggleFavorite(cocktailId: String)
    fun addFavorite(cocktailId: String)
    fun removeFavorite(cocktailId: String)
}
