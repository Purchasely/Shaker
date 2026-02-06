package com.purchasely.shaker.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FavoritesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("shaker_favorites", Context.MODE_PRIVATE)

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    init {
        _favoriteIds.value = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
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
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
    }

    fun addFavorite(cocktailId: String) {
        val current = _favoriteIds.value.toMutableSet()
        current.add(cocktailId)
        _favoriteIds.value = current
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
    }

    fun removeFavorite(cocktailId: String) {
        val current = _favoriteIds.value.toMutableSet()
        current.remove(cocktailId)
        _favoriteIds.value = current
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
    }

    companion object {
        private const val KEY_FAVORITES = "favorite_cocktail_ids"
    }
}
