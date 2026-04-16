package com.purchasely.shaker.ui.screen.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purchasely.shaker.domain.repository.CocktailRepository
import com.purchasely.shaker.domain.repository.FavoritesRepository
import com.purchasely.shaker.domain.repository.PremiumRepository
import com.purchasely.shaker.domain.model.Cocktail
import com.purchasely.shaker.purchasely.FetchResult
import com.purchasely.shaker.purchasely.PresentationHandle
import com.purchasely.shaker.purchasely.PurchaselyWrapper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailViewModel(
    private val repository: CocktailRepository,
    private val premiumRepository: PremiumRepository,
    private val favoritesRepository: FavoritesRepository,
    private val purchaselyWrapper: PurchaselyWrapper,
    private val cocktailId: String
) : ViewModel() {

    private val _cocktail = MutableStateFlow<Cocktail?>(null)
    val cocktail: StateFlow<Cocktail?> = _cocktail.asStateFlow()

    val isPremium: StateFlow<Boolean> = premiumRepository.isPremium

    val favoriteIds: StateFlow<Set<String>> = favoritesRepository.favoriteIds

    // Signal Screen to display recipe paywall
    private val _requestRecipePaywall = MutableSharedFlow<PresentationHandle>()
    val requestRecipePaywall: SharedFlow<PresentationHandle> = _requestRecipePaywall.asSharedFlow()

    // Signal Screen to display favorites paywall
    private val _requestFavoritesPaywall = MutableSharedFlow<PresentationHandle>()
    val requestFavoritesPaywall: SharedFlow<PresentationHandle> = _requestFavoritesPaywall.asSharedFlow()

    init {
        _cocktail.value = repository.getCocktail(cocktailId)
        trackCocktailViewed()
    }

    private fun trackCocktailViewed() {
        // PURCHASELY: Increment a numeric counter attribute each time the user views a cocktail detail
        // Useful for triggering paywalls after N views or segmenting engaged users
        // Docs: https://docs.purchasely.com/advanced-features/user-attributes
        purchaselyWrapper.incrementUserAttribute("cocktails_viewed")
        // PURCHASELY: Track the spirit of the last-viewed cocktail for personalized paywall content
        // Docs: https://docs.purchasely.com/advanced-features/user-attributes
        _cocktail.value?.spirit?.let { spirit ->
            purchaselyWrapper.setUserAttribute("favorite_spirit", spirit)
        }
    }

    fun isFavorite(): Boolean = favoritesRepository.isFavorite(cocktailId)

    fun toggleFavorite() {
        favoritesRepository.toggleFavorite(cocktailId)
    }

    fun showRecipePaywall() {
        viewModelScope.launch {
            val result = purchaselyWrapper.loadPresentation(
                placementId = "recipe_detail",
                contentId = _cocktail.value?.id
            )
            when (result) {
                is FetchResult.Success -> {
                    _requestRecipePaywall.emit(result.handle)
                }
                is FetchResult.Client -> {
                    Log.d("DetailViewModel", "[Shaker] CLIENT presentation received for recipe_detail placement — build custom UI here")
                }
                else -> {}
            }
        }
    }

    fun showFavoritesPaywall() {
        viewModelScope.launch {
            val result = purchaselyWrapper.loadPresentation(placementId = "favorites")
            when (result) {
                is FetchResult.Success -> {
                    _requestFavoritesPaywall.emit(result.handle)
                }
                is FetchResult.Client -> {
                    Log.d("DetailViewModel", "[Shaker] CLIENT presentation received for favorites placement — build custom UI here")
                }
                else -> {}
            }
        }
    }

    fun onPaywallDismissed() {
        premiumRepository.refreshPremiumStatus()
    }
}
