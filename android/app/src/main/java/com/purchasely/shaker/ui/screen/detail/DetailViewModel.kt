package com.purchasely.shaker.ui.screen.detail

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purchasely.shaker.data.CocktailRepository
import com.purchasely.shaker.data.FavoritesRepository
import com.purchasely.shaker.data.PremiumManager
import com.purchasely.shaker.domain.model.Cocktail
import com.purchasely.shaker.purchasely.DisplayResult
import com.purchasely.shaker.purchasely.FetchResult
import com.purchasely.shaker.purchasely.PurchaselyWrapper
import io.purchasely.ext.PLYPresentation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailViewModel(
    private val repository: CocktailRepository,
    private val premiumManager: PremiumManager,
    private val favoritesRepository: FavoritesRepository,
    private val purchaselyWrapper: PurchaselyWrapper,
    private val cocktailId: String
) : ViewModel() {

    private val _cocktail = MutableStateFlow<Cocktail?>(null)
    val cocktail: StateFlow<Cocktail?> = _cocktail.asStateFlow()

    val isPremium: StateFlow<Boolean> = premiumManager.isPremium

    val favoriteIds: StateFlow<Set<String>> = favoritesRepository.favoriteIds

    // Signal Screen to display recipe paywall
    private var pendingRecipePresentation: PLYPresentation? = null
    private val _requestRecipePaywall = MutableSharedFlow<Unit>()
    val requestRecipePaywall: SharedFlow<Unit> = _requestRecipePaywall.asSharedFlow()

    // Signal Screen to display favorites paywall
    private var pendingFavoritesPresentation: PLYPresentation? = null
    private val _requestFavoritesPaywall = MutableSharedFlow<Unit>()
    val requestFavoritesPaywall: SharedFlow<Unit> = _requestFavoritesPaywall.asSharedFlow()

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
                    pendingRecipePresentation = result.presentation
                    _requestRecipePaywall.emit(Unit)
                }
                is FetchResult.Client -> {
                    Log.d("DetailViewModel", "[Shaker] CLIENT presentation received for recipe_detail placement — build custom UI here")
                }
                else -> {}
            }
        }
    }

    suspend fun displayPendingRecipePaywall(activity: Activity) {
        val presentation = pendingRecipePresentation ?: return
        pendingRecipePresentation = null
        val result = purchaselyWrapper.display(presentation, activity)
        when (result) {
            is DisplayResult.Purchased -> {
                Log.d("DetailViewModel", "[Shaker] Purchased: ${result.planName}")
                onPaywallDismissed()
            }
            is DisplayResult.Restored -> {
                Log.d("DetailViewModel", "[Shaker] Restored: ${result.planName}")
                onPaywallDismissed()
            }
            is DisplayResult.Cancelled -> {
                Log.d("DetailViewModel", "[Shaker] Cancelled")
            }
        }
    }

    fun showFavoritesPaywall() {
        viewModelScope.launch {
            val result = purchaselyWrapper.loadPresentation(placementId = "favorites")
            when (result) {
                is FetchResult.Success -> {
                    pendingFavoritesPresentation = result.presentation
                    _requestFavoritesPaywall.emit(Unit)
                }
                is FetchResult.Client -> {
                    Log.d("DetailViewModel", "[Shaker] CLIENT presentation received for favorites placement — build custom UI here")
                }
                else -> {}
            }
        }
    }

    suspend fun displayPendingFavoritesPaywall(activity: Activity) {
        val presentation = pendingFavoritesPresentation ?: return
        pendingFavoritesPresentation = null
        val result = purchaselyWrapper.display(presentation, activity)
        when (result) {
            is DisplayResult.Purchased, is DisplayResult.Restored -> {
                Log.d("DetailViewModel", "[Shaker] Purchased/Restored from favorites: ${(result as? DisplayResult.Purchased)?.planName ?: (result as? DisplayResult.Restored)?.planName}")
                onPaywallDismissed()
            }
            else -> {}
        }
    }

    fun onPaywallDismissed() {
        premiumManager.refreshPremiumStatus()
    }
}
