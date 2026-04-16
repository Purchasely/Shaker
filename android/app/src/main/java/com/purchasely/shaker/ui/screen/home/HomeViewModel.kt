package com.purchasely.shaker.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purchasely.shaker.domain.repository.CocktailRepository
import com.purchasely.shaker.domain.repository.PremiumRepository
import com.purchasely.shaker.domain.model.Cocktail
import com.purchasely.shaker.domain.usecase.GetFilteredCocktailsUseCase
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

class HomeViewModel(
    private val repository: CocktailRepository,
    private val premiumRepository: PremiumRepository,
    private val purchaselyWrapper: PurchaselyWrapper,
    private val getFilteredCocktails: GetFilteredCocktailsUseCase
) : ViewModel() {

    private val _cocktails = MutableStateFlow<List<Cocktail>>(emptyList())
    val cocktails: StateFlow<List<Cocktail>> = _cocktails.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val isPremium: StateFlow<Boolean> = premiumRepository.isPremium

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

    private val _hasActiveFilters = MutableStateFlow(false)
    val hasActiveFilters: StateFlow<Boolean> = _hasActiveFilters.asStateFlow()

    private fun updateHasActiveFilters() {
        _hasActiveFilters.value = _selectedSpirits.value.isNotEmpty() ||
                _selectedCategories.value.isNotEmpty() ||
                _selectedDifficulty.value != null
    }

    // Prefetched inline presentation
    private val _inlinePresentation = MutableStateFlow<FetchResult?>(null)
    val inlinePresentation: StateFlow<FetchResult?> = _inlinePresentation.asStateFlow()

    // Prefetched filters presentation
    private val _filtersPresentation = MutableStateFlow<FetchResult?>(null)
    val filtersPresentation: StateFlow<FetchResult?> = _filtersPresentation.asStateFlow()

    private val _isFiltersLoading = MutableStateFlow(false)
    val isFiltersLoading: StateFlow<Boolean> = _isFiltersLoading.asStateFlow()

    // Signal Screen to display filters paywall
    private val _requestPaywallDisplay = MutableSharedFlow<PresentationHandle>()
    val requestPaywallDisplay: SharedFlow<PresentationHandle> = _requestPaywallDisplay.asSharedFlow()

    init {
        _cocktails.value = getFilteredCocktails()
        prefetchPresentations()
    }

    private fun prefetchPresentations() {
        if (isPremium.value) return
        viewModelScope.launch {
            _isFiltersLoading.value = true
            _filtersPresentation.value = purchaselyWrapper.loadPresentation("filters")
            _isFiltersLoading.value = false
        }
        viewModelScope.launch {
            _inlinePresentation.value = purchaselyWrapper.loadPresentation("inline")
        }
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
        val result = _filtersPresentation.value
        if (result is FetchResult.Success) {
            viewModelScope.launch { _requestPaywallDisplay.emit(result.handle) }
        }
    }

    fun toggleSpirit(spirit: String) {
        val current = _selectedSpirits.value.toMutableSet()
        if (current.contains(spirit)) current.remove(spirit) else current.add(spirit)
        _selectedSpirits.value = current
        applyFilters()
        updateHasActiveFilters()
    }

    fun toggleCategory(category: String) {
        val current = _selectedCategories.value.toMutableSet()
        if (current.contains(category)) current.remove(category) else current.add(category)
        _selectedCategories.value = current
        applyFilters()
        updateHasActiveFilters()
    }

    fun selectDifficulty(difficulty: String?) {
        _selectedDifficulty.value = if (_selectedDifficulty.value == difficulty) null else difficulty
        applyFilters()
        updateHasActiveFilters()
    }

    fun clearFilters() {
        _selectedSpirits.value = emptySet()
        _selectedCategories.value = emptySet()
        _selectedDifficulty.value = null
        applyFilters()
        updateHasActiveFilters()
    }

    fun onPaywallDismissed() {
        premiumRepository.refreshPremiumStatus()
    }

    private fun applyFilters() {
        _cocktails.value = getFilteredCocktails(
            query = _searchQuery.value,
            spirits = _selectedSpirits.value,
            categories = _selectedCategories.value,
            difficulty = _selectedDifficulty.value
        )
    }
}
