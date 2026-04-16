package com.purchasely.shaker.ui.screen.favorites

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
import com.purchasely.shaker.purchasely.PresentationHandle
import com.purchasely.shaker.purchasely.PurchaselyWrapper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val cocktailRepository: CocktailRepository,
    private val favoritesRepository: FavoritesRepository,
    private val premiumManager: PremiumManager,
    private val purchaselyWrapper: PurchaselyWrapper
) : ViewModel() {

    val favoriteIds: StateFlow<Set<String>> = favoritesRepository.favoriteIds
    val isPremium: StateFlow<Boolean> = premiumManager.isPremium

    private val _favorites = MutableStateFlow<List<Cocktail>>(emptyList())
    val favorites: StateFlow<List<Cocktail>> = _favorites.asStateFlow()

    // Signal Screen to display favorites paywall
    private var pendingPresentation: PresentationHandle? = null
    private val _requestPaywallDisplay = MutableSharedFlow<Unit>()
    val requestPaywallDisplay: SharedFlow<Unit> = _requestPaywallDisplay.asSharedFlow()

    init {
        viewModelScope.launch {
            favoritesRepository.favoriteIds.collect { ids ->
                _favorites.value = cocktailRepository.loadCocktails().filter { it.id in ids }
            }
        }
    }

    fun removeFavorite(cocktailId: String) {
        favoritesRepository.removeFavorite(cocktailId)
    }

    fun showFavoritesPaywall() {
        viewModelScope.launch {
            when (val result = purchaselyWrapper.loadPresentation("favorites")) {
                is FetchResult.Success -> {
                    pendingPresentation = result.handle
                    _requestPaywallDisplay.emit(Unit)
                }
                is FetchResult.Client -> {
                    Log.d(TAG, "[Shaker] CLIENT presentation received for favorites placement — build custom UI here")
                }
                is FetchResult.Deactivated -> {
                    Log.d(TAG, "[Shaker] Favorites placement is deactivated")
                }
                is FetchResult.Error -> {
                    Log.e(TAG, "[Shaker] Error fetching favorites: ${result.message}")
                }
            }
        }
    }

    suspend fun displayPendingPaywall(activity: Activity) {
        val handle = pendingPresentation ?: return
        pendingPresentation = null
        val result = purchaselyWrapper.display(handle, activity)
        when (result) {
            is DisplayResult.Purchased, is DisplayResult.Restored -> {
                Log.d(TAG, "[Shaker] Purchased/Restored from favorites")
                onPaywallDismissed()
            }
            else -> {}
        }
    }

    fun onPaywallDismissed() {
        premiumManager.refreshPremiumStatus()
    }

    companion object {
        private const val TAG = "FavoritesViewModel"
    }
}
