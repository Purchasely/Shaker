package com.purchasely.shaker.ui.screen.detail

import androidx.lifecycle.ViewModel
import com.purchasely.shaker.data.CocktailRepository
import com.purchasely.shaker.data.FavoritesRepository
import com.purchasely.shaker.data.PremiumManager
import com.purchasely.shaker.domain.model.Cocktail
import io.purchasely.ext.Purchasely
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
        trackCocktailViewed()
    }

    private fun trackCocktailViewed() {
        // PURCHASELY: Increment a numeric counter attribute each time the user views a cocktail detail
        // Useful for triggering paywalls after N views or segmenting engaged users
        // Docs: https://docs.purchasely.com/advanced-features/user-attributes
        Purchasely.incrementUserAttribute("cocktails_viewed")
        // PURCHASELY: Track the spirit of the last-viewed cocktail for personalized paywall content
        // Docs: https://docs.purchasely.com/advanced-features/user-attributes
        _cocktail.value?.spirit?.let { spirit ->
            Purchasely.setUserAttribute("favorite_spirit", spirit)
        }
    }

    fun isFavorite(): Boolean = favoritesRepository.isFavorite(cocktailId)

    fun toggleFavorite() {
        favoritesRepository.toggleFavorite(cocktailId)
    }

    fun onPaywallDismissed() {
        premiumManager.refreshPremiumStatus()
    }
}
