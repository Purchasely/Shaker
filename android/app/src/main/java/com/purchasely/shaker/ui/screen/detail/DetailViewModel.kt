package com.purchasely.shaker.ui.screen.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purchasely.shaker.domain.repository.CocktailRepository
import com.purchasely.shaker.domain.repository.FavoritesRepository
import com.purchasely.shaker.domain.repository.PremiumRepository
import com.purchasely.shaker.domain.model.Cocktail
import com.purchasely.shaker.domain.usecase.ToggleFavoriteUseCase
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
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val cocktailId: String
) : ViewModel() {

    private val _cocktail = MutableStateFlow<Cocktail?>(null)
    val cocktail: StateFlow<Cocktail?> = _cocktail.asStateFlow()

    val isPremium: StateFlow<Boolean> = premiumRepository.isPremium

    val favoriteIds: StateFlow<Set<String>> = favoritesRepository.favoriteIds

    // Signal Screen to display recipe presentation
    private val _requestRecipePresentation = MutableSharedFlow<PresentationHandle>()
    val requestRecipePresentation: SharedFlow<PresentationHandle> = _requestRecipePresentation.asSharedFlow()

    // Signal Screen to display favorites presentation
    private val _requestFavoritesPresentation = MutableSharedFlow<PresentationHandle>()
    val requestFavoritesPresentation: SharedFlow<PresentationHandle> = _requestFavoritesPresentation.asSharedFlow()

    init {
        _cocktail.value = repository.getCocktail(cocktailId)
        trackCocktailViewed()
    }

    private fun trackCocktailViewed() {
        // PURCHASELY: Increment a numeric counter attribute each time the user views a cocktail detail
        // Useful for triggering presentations after N views or segmenting engaged users
        // Docs: https://docs.purchasely.com/advanced-features/user-attributes
        purchaselyWrapper.incrementUserAttribute("cocktails_viewed")
        // PURCHASELY: Track the spirit of the last-viewed cocktail for personalized presentation content
        // Docs: https://docs.purchasely.com/advanced-features/user-attributes
        _cocktail.value?.spirit?.let { spirit ->
            purchaselyWrapper.setUserAttribute("favorite_spirit", spirit)
        }
    }

    fun isFavorite(): Boolean = favoritesRepository.isFavorite(cocktailId)

    fun toggleFavorite() {
        toggleFavoriteUseCase(cocktailId)
    }

    fun showRecipePresentation() {
        viewModelScope.launch {
            val result = purchaselyWrapper.loadPresentation(
                placementId = "recipe_detail",
                contentId = _cocktail.value?.id
            )
            when (result) {
                is FetchResult.Success -> {
                    _requestRecipePresentation.emit(result.handle)
                }
                is FetchResult.Client -> {
                    Log.d("DetailViewModel", "[Shaker] CLIENT presentation received for recipe_detail placement — build custom UI here")
                }
                else -> {}
            }
        }
    }

    fun showFavoritesPresentation() {
        viewModelScope.launch {
            val result = purchaselyWrapper.loadPresentation(placementId = "favorites")
            when (result) {
                is FetchResult.Success -> {
                    _requestFavoritesPresentation.emit(result.handle)
                }
                is FetchResult.Client -> {
                    Log.d("DetailViewModel", "[Shaker] CLIENT presentation received for favorites placement — build custom UI here")
                }
                else -> {}
            }
        }
    }

    fun onPresentationDismissed() {
        premiumRepository.refreshPremiumStatus()
    }
}
