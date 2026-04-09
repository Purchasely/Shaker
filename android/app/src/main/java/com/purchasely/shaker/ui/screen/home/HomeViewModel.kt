package com.purchasely.shaker.ui.screen.home

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purchasely.shaker.data.CocktailRepository
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

class HomeViewModel(
    private val repository: CocktailRepository,
    private val premiumManager: PremiumManager,
    private val purchaselyWrapper: PurchaselyWrapper
) : ViewModel() {

    private val _cocktails = MutableStateFlow<List<Cocktail>>(emptyList())
    val cocktails: StateFlow<List<Cocktail>> = _cocktails.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val isPremium: StateFlow<Boolean> = premiumManager.isPremium

    // Filter state
    private val _selectedSpirits = MutableStateFlow<Set<String>>(emptySet())
    val selectedSpirits: StateFlow<Set<String>> = _selectedSpirits.asStateFlow()

    private val _selectedCategories = MutableStateFlow<Set<String>>(emptySet())
    val selectedCategories: StateFlow<Set<String>> = _selectedCategories.asStateFlow()

    private val _selectedDifficulty = MutableStateFlow<String?>(null)
    val selectedDifficulty: StateFlow<String?> = _selectedDifficulty.asStateFlow()

    val availableSpirits: List<String> get() = repository.getSpirits()
    val availableCategories: List<String> get() = repository.getCategories()
    val availableDifficulties: List<String> get() = repository.getDifficulties()

    val hasActiveFilters: Boolean
        get() = _selectedSpirits.value.isNotEmpty() ||
                _selectedCategories.value.isNotEmpty() ||
                _selectedDifficulty.value != null

    private var pendingFiltersPresentation: PLYPresentation? = null

    private val _requestPaywallDisplay = MutableSharedFlow<Unit>()
    val requestPaywallDisplay: SharedFlow<Unit> = _requestPaywallDisplay.asSharedFlow()

    init {
        _cocktails.value = repository.loadCocktails()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) {
            purchaselyWrapper.setUserAttribute("has_used_search", true)
        }
        applyFilters()
    }

    fun onFilterClick() {
        if (isPremium.value) return
        viewModelScope.launch {
            when (val result = purchaselyWrapper.loadPresentation("filters")) {
                is FetchResult.Success -> {
                    pendingFiltersPresentation = result.presentation
                    _requestPaywallDisplay.emit(Unit)
                }
                is FetchResult.Client -> {
                    Log.d("HomeViewModel", "[Shaker] CLIENT presentation for filters — build custom UI here")
                }
                else -> {}
            }
        }
    }

    suspend fun displayPendingPaywall(activity: Activity) {
        val presentation = pendingFiltersPresentation ?: return
        pendingFiltersPresentation = null
        val result = purchaselyWrapper.display(presentation, activity)
        when (result) {
            is DisplayResult.Purchased, is DisplayResult.Restored -> {
                Log.d("HomeViewModel", "[Shaker] Purchased/Restored from filters")
                onPaywallDismissed()
            }
            else -> {}
        }
    }

    fun toggleSpirit(spirit: String) {
        val current = _selectedSpirits.value.toMutableSet()
        if (current.contains(spirit)) current.remove(spirit) else current.add(spirit)
        _selectedSpirits.value = current
        applyFilters()
    }

    fun toggleCategory(category: String) {
        val current = _selectedCategories.value.toMutableSet()
        if (current.contains(category)) current.remove(category) else current.add(category)
        _selectedCategories.value = current
        applyFilters()
    }

    fun selectDifficulty(difficulty: String?) {
        _selectedDifficulty.value = if (_selectedDifficulty.value == difficulty) null else difficulty
        applyFilters()
    }

    fun clearFilters() {
        _selectedSpirits.value = emptySet()
        _selectedCategories.value = emptySet()
        _selectedDifficulty.value = null
        applyFilters()
    }

    fun onPaywallDismissed() {
        premiumManager.refreshPremiumStatus()
    }

    private fun applyFilters() {
        val query = _searchQuery.value
        val spirits = _selectedSpirits.value
        val categories = _selectedCategories.value
        val difficulty = _selectedDifficulty.value

        _cocktails.value = repository.loadCocktails().filter { cocktail ->
            val matchesQuery = query.isBlank() || cocktail.name.contains(query, ignoreCase = true)
            val matchesSpirit = spirits.isEmpty() || spirits.contains(cocktail.spirit)
            val matchesCategory = categories.isEmpty() || categories.contains(cocktail.category)
            val matchesDifficulty = difficulty == null || cocktail.difficulty == difficulty
            matchesQuery && matchesSpirit && matchesCategory && matchesDifficulty
        }
    }
}
