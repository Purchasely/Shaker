package com.purchasely.shaker.ui.screen.favorites

import androidx.lifecycle.ViewModel
import com.purchasely.shaker.data.CocktailRepository
import com.purchasely.shaker.data.FavoritesRepository
import com.purchasely.shaker.data.PremiumManager
import com.purchasely.shaker.domain.model.Cocktail
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FavoritesViewModel(
    private val cocktailRepository: CocktailRepository,
    private val favoritesRepository: FavoritesRepository,
    private val premiumManager: PremiumManager
) : ViewModel() {

    val favoriteIds: StateFlow<Set<String>> = favoritesRepository.favoriteIds
    val isPremium: StateFlow<Boolean> = premiumManager.isPremium

    fun getFavoriteCocktails(): List<Cocktail> {
        val ids = favoriteIds.value
        return cocktailRepository.loadCocktails().filter { it.id in ids }
    }

    fun removeFavorite(cocktailId: String) {
        favoritesRepository.removeFavorite(cocktailId)
    }

    fun onPaywallDismissed() {
        premiumManager.refreshPremiumStatus()
    }
}
