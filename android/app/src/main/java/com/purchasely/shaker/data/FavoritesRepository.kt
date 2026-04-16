package com.purchasely.shaker.data

import com.purchasely.shaker.data.storage.KeyValueStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FavoritesRepository(private val store: KeyValueStore) {

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    init {
        _favoriteIds.value = store.getStringSet(KEY_FAVORITES)
    }

    fun isFavorite(cocktailId: String): Boolean = _favoriteIds.value.contains(cocktailId)

    fun toggleFavorite(cocktailId: String) {
        val current = _favoriteIds.value.toMutableSet()
        if (current.contains(cocktailId)) {
            current.remove(cocktailId)
        } else {
            current.add(cocktailId)
        }
        _favoriteIds.value = current
        store.putStringSet(KEY_FAVORITES, current)
    }

    fun addFavorite(cocktailId: String) {
        val current = _favoriteIds.value.toMutableSet()
        current.add(cocktailId)
        _favoriteIds.value = current
        store.putStringSet(KEY_FAVORITES, current)
    }

    fun removeFavorite(cocktailId: String) {
        val current = _favoriteIds.value.toMutableSet()
        current.remove(cocktailId)
        _favoriteIds.value = current
        store.putStringSet(KEY_FAVORITES, current)
    }

    companion object {
        private const val KEY_FAVORITES = "favorite_cocktail_ids"
    }
}
