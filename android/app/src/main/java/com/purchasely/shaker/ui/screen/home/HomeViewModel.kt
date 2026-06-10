package com.purchasely.shaker.ui.screen.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purchasely.shaker.domain.repository.CocktailRepository
import com.purchasely.shaker.domain.repository.PremiumRepository
import com.purchasely.shaker.domain.model.Cocktail
import com.purchasely.shaker.domain.model.CocktailMood
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

    private val _selectedMood = MutableStateFlow<CocktailMood?>(null)
    val selectedMood: StateFlow<CocktailMood?> = _selectedMood.asStateFlow()

    val availableSpirits: List<String> get() = repository.getSpirits()
    val availableCategories: List<String> get() = repository.getCategories()
    val availableDifficulties: List<String> get() = repository.getDifficulties()

    private val _hasActiveFilters = MutableStateFlow(false)
    val hasActiveFilters: StateFlow<Boolean> = _hasActiveFilters.asStateFlow()

    private fun updateHasActiveFilters() {
        _hasActiveFilters.value = _selectedSpirits.value.isNotEmpty() ||
                _selectedCategories.value.isNotEmpty() ||
                _selectedDifficulty.value != null ||
                _selectedMood.value != null
    }

    // Prefetched inline presentation
    private val _inlinePresentation = MutableStateFlow<FetchResult?>(null)
    val inlinePresentation: StateFlow<FetchResult?> = _inlinePresentation.asStateFlow()

    // Prefetched filters presentation
    private val _filtersPresentation = MutableStateFlow<FetchResult?>(null)
    val filtersPresentation: StateFlow<FetchResult?> = _filtersPresentation.asStateFlow()

    private val _isFiltersLoading = MutableStateFlow(false)
    val isFiltersLoading: StateFlow<Boolean> = _isFiltersLoading.asStateFlow()

    // Signal Screen to display filters presentation
    private val _requestPresentationDisplay = MutableSharedFlow<PresentationHandle>()
    val requestPresentationDisplay: SharedFlow<PresentationHandle> = _requestPresentationDisplay.asSharedFlow()

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
        when (val result = _filtersPresentation.value) {
            is FetchResult.Success -> {
                viewModelScope.launch { _requestPresentationDisplay.emit(result.handle) }
            }
            is FetchResult.Error -> {
                // PURCHASELY: the prefetch failed (e.g. offline at launch). Retry it so the
                // paywall becomes available instead of leaving the button dead forever.
                Log.w(TAG, "[Shaker] Filters paywall unavailable (${result.message}), retrying prefetch")
                retryFiltersPrefetch()
            }
            is FetchResult.Deactivated, is FetchResult.Client -> {
                // Placement disabled in the console or client-rendered — nothing to display.
                Log.d(TAG, "[Shaker] Filters paywall not displayable: $result")
            }
            null -> {
                // Prefetch still in flight — the screen shows the loader; ignore the tap.
                Log.d(TAG, "[Shaker] Filters paywall still loading")
            }
        }
    }

    private fun retryFiltersPrefetch() {
        if (_isFiltersLoading.value) return
        viewModelScope.launch {
            _isFiltersLoading.value = true
            _filtersPresentation.value = purchaselyWrapper.loadPresentation("filters")
            _isFiltersLoading.value = false
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
        _selectedMood.value = null
        applyFilters()
        updateHasActiveFilters()
    }

    /**
     * Mood-based discovery. Selecting a mood filters the catalog by tags and
     * reports the preference to Purchasely for audience targeting.
     */
    fun selectMood(mood: CocktailMood?) {
        val newMood = if (_selectedMood.value == mood) null else mood
        _selectedMood.value = newMood
        if (newMood != null) {
            // PURCHASELY: track the user's drinking mood as a custom attribute. Console-side
            // audiences can target e.g. preferred_mood == "zero_proof" with a dedicated screen.
            // Docs: https://docs.purchasely.com/advanced-features/user-attributes
            purchaselyWrapper.setUserAttribute("preferred_mood", newMood.key)
        }
        applyFilters()
        updateHasActiveFilters()
    }

    /**
     * "Surprise me" — picks a random cocktail from the currently filtered list.
     * Returns its id for navigation, or null when the list is empty.
     */
    fun onSurpriseMe(): String? {
        val candidates = _cocktails.value.ifEmpty { return null }
        // PURCHASELY: count how often the user asks for a surprise. A console campaign can
        // trigger a dedicated screen after N uses (engaged-user segmentation).
        purchaselyWrapper.incrementUserAttribute("surprise_me_count")
        return candidates.random().id
    }

    fun onPresentationDismissed() {
        premiumRepository.refreshPremiumStatus()
    }

    private fun applyFilters() {
        _cocktails.value = getFilteredCocktails(
            query = _searchQuery.value,
            spirits = _selectedSpirits.value,
            categories = _selectedCategories.value,
            difficulty = _selectedDifficulty.value,
            mood = _selectedMood.value,
        )
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}
