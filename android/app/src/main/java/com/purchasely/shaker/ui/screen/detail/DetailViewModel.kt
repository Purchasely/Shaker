package com.purchasely.shaker.ui.screen.detail

import androidx.lifecycle.ViewModel
import com.purchasely.shaker.data.CocktailRepository
import com.purchasely.shaker.data.FavoritesRepository
import com.purchasely.shaker.data.PremiumManager
import com.purchasely.shaker.domain.model.Cocktail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class DetailViewModel(
    private val repository: CocktailRepository,
    private val premiumManager: PremiumManager,
    private val favoritesRepository: FavoritesRepository,
    private val cocktailId: String
) : ViewModel() {

    private val _cocktail = MutableStateFlow<Cocktail?>(null)
    val cocktail: StateFlow<Cocktail?> = _cocktail.asStateFlow()

    val isPremium: StateFlow<Boolean> = premiumManager.isPremium

    val favoriteIds: StateFlow<Set<String>> = favoritesRepository.favoriteIds

    init {
        _cocktail.value = repository.getCocktail(cocktailId)
    }

    fun isFavorite(): Boolean = favoritesRepository.isFavorite(cocktailId)

    fun toggleFavorite() {
        favoritesRepository.toggleFavorite(cocktailId)
    }

    fun onPaywallDismissed() {
        premiumManager.refreshPremiumStatus()
    }
}
