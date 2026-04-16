package com.purchasely.shaker.data

import com.purchasely.shaker.data.storage.KeyValueStore
import com.purchasely.shaker.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FavoritesRepositoryImpl(private val store: KeyValueStore) : FavoritesRepository {

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    override val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    init {
        _favoriteIds.value = store.getStringSet(KEY_FAVORITES)
    }

    override fun isFavorite(cocktailId: String): Boolean = _favoriteIds.value.contains(cocktailId)

    override fun toggleFavorite(cocktailId: String) {
        val current = _favoriteIds.value.toMutableSet()
        if (current.contains(cocktailId)) {
            current.remove(cocktailId)
        } else {
            current.add(cocktailId)
        }
        _favoriteIds.value = current
        store.putStringSet(KEY_FAVORITES, current)
    }

    override fun addFavorite(cocktailId: String) {
        val current = _favoriteIds.value.toMutableSet()
        current.add(cocktailId)
        _favoriteIds.value = current
        store.putStringSet(KEY_FAVORITES, current)
    }

    override fun removeFavorite(cocktailId: String) {
        val current = _favoriteIds.value.toMutableSet()
        current.remove(cocktailId)
        _favoriteIds.value = current
        store.putStringSet(KEY_FAVORITES, current)
    }

    companion object {
        private const val KEY_FAVORITES = "favorite_cocktail_ids"
    }
}
