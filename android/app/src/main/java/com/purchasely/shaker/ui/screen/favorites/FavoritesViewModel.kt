package com.purchasely.shaker.ui.screen.favorites

import androidx.lifecycle.ViewModel
import com.purchasely.shaker.data.CocktailRepository
import com.purchasely.shaker.domain.model.Cocktail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FavoritesViewModel(private val repository: CocktailRepository) : ViewModel() {

    private val _favorites = MutableStateFlow<List<Cocktail>>(emptyList())
    val favorites: StateFlow<List<Cocktail>> = _favorites.asStateFlow()
}
