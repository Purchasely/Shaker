# PurchaselyWrapper Refactor — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract all direct Purchasely SDK calls from UI layers into a `PurchaselyWrapper` class, starting with HomeScreen/HomeViewModel.

**Architecture:** Create a `purchasely` package under `com.purchasely.shaker` containing a Koin-injectable `PurchaselyWrapper` class with suspend functions for async SDK operations. ViewModels call the wrapper; Screens have zero `io.purchasely` imports. An `EmbeddedScreenBanner` composable in the same package handles inline paywall rendering.

**Tech Stack:** Kotlin, Jetpack Compose, Koin DI, Coroutines

---

## File Map

| Action | File | Responsibility |
|--------|------|----------------|
| Create | `purchasely/PurchaselyWrapper.kt` | Wraps all Purchasely SDK calls (fetch, display, buildView, attributes) |
| Create | `purchasely/FetchResult.kt` | Sealed class for fetch outcomes |
| Create | `purchasely/DisplayResult.kt` | Sealed class for display/purchase outcomes |
| Create | `purchasely/EmbeddedScreenBanner.kt` | Reusable Composable for inline paywall views |
| Modify | `di/AppModule.kt` | Register PurchaselyWrapper in Koin |
| Modify | `ui/screen/home/HomeViewModel.kt` | Inject PurchaselyWrapper, move paywall logic here |
| Modify | `ui/screen/home/HomeScreen.kt` | Remove all `io.purchasely` imports, use ViewModel + EmbeddedScreenBanner |
| Create | `docs/purchasely-best-practices.md` (project root) | Best practices document |
| Modify | `CLAUDE.md` (project root) | Reference best practices doc, note demo project purpose |

All paths relative to `android/app/src/main/java/com/purchasely/shaker/` unless noted.

---

### Task 1: Create FetchResult sealed class

**Files:**
- Create: `android/app/src/main/java/com/purchasely/shaker/purchasely/FetchResult.kt`

- [ ] **Step 1: Create FetchResult.kt**

```kotlin
package com.purchasely.shaker.purchasely

import io.purchasely.ext.PLYPresentation
import io.purchasely.models.PLYError

sealed class FetchResult {
    data class Success(val presentation: PLYPresentation) : FetchResult()
    data class Client(val presentation: PLYPresentation) : FetchResult()
    data object Deactivated : FetchResult()
    data class Error(val error: PLYError?) : FetchResult()
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd /Users/kevin/Purchasely/Shaker/android && ./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 2: Create DisplayResult sealed class

**Files:**
- Create: `android/app/src/main/java/com/purchasely/shaker/purchasely/DisplayResult.kt`

- [ ] **Step 1: Create DisplayResult.kt**

```kotlin
package com.purchasely.shaker.purchasely

sealed class DisplayResult {
    data class Purchased(val planName: String?) : DisplayResult()
    data class Restored(val planName: String?) : DisplayResult()
    data object Cancelled : DisplayResult()
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd /Users/kevin/Purchasely/Shaker/android && ./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 3: Create PurchaselyWrapper

**Files:**
- Create: `android/app/src/main/java/com/purchasely/shaker/purchasely/PurchaselyWrapper.kt`

- [ ] **Step 1: Create PurchaselyWrapper.kt**

```kotlin
package com.purchasely.shaker.purchasely

import android.app.Activity
import android.content.Context
import android.view.View
import io.purchasely.ext.PLYPresentation
import io.purchasely.ext.PLYPresentationProperties
import io.purchasely.ext.PLYPresentationType
import io.purchasely.ext.PLYProductViewResult
import io.purchasely.ext.Purchasely
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PurchaselyWrapper {

    suspend fun loadPresentation(
        placementId: String,
        contentId: String? = null
    ): FetchResult = suspendCoroutine { continuation ->
        val callback = { presentation: PLYPresentation?, error: io.purchasely.models.PLYError? ->
            when {
                presentation == null -> continuation.resume(FetchResult.Error(error))
                presentation.type == PLYPresentationType.DEACTIVATED -> continuation.resume(FetchResult.Deactivated)
                presentation.type == PLYPresentationType.CLIENT -> continuation.resume(FetchResult.Client(presentation))
                else -> continuation.resume(FetchResult.Success(presentation))
            }
        }
        if (contentId != null) {
            Purchasely.fetchPresentation(
                properties = PLYPresentationProperties(placementId = placementId, contentId = contentId),
                callback
            )
        } else {
            Purchasely.fetchPresentation(placementId, callback)
        }
    }

    suspend fun display(
        presentation: PLYPresentation,
        activity: Activity
    ): DisplayResult = suspendCoroutine { continuation ->
        presentation.display(activity) { result, plan ->
            when (result) {
                PLYProductViewResult.PURCHASED -> continuation.resume(DisplayResult.Purchased(plan?.name))
                PLYProductViewResult.RESTORED -> continuation.resume(DisplayResult.Restored(plan?.name))
                else -> continuation.resume(DisplayResult.Cancelled)
            }
        }
    }

    fun getView(
        presentation: PLYPresentation,
        context: Context,
        onClose: () -> Unit,
        onResult: (DisplayResult) -> Unit
    ): View {
        return presentation.buildView(context, onClose) { result, plan ->
            when (result) {
                PLYProductViewResult.PURCHASED -> onResult(DisplayResult.Purchased(plan?.name))
                PLYProductViewResult.RESTORED -> onResult(DisplayResult.Restored(plan?.name))
                else -> onResult(DisplayResult.Cancelled)
            }
        }
    }

    fun setUserAttribute(key: String, value: Any) {
        Purchasely.setUserAttribute(key, value)
    }

    fun incrementUserAttribute(key: String) {
        Purchasely.incrementUserAttribute(key)
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd /Users/kevin/Purchasely/Shaker/android && ./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 4: Create EmbeddedScreenBanner composable

**Files:**
- Create: `android/app/src/main/java/com/purchasely/shaker/purchasely/EmbeddedScreenBanner.kt`

- [ ] **Step 1: Create EmbeddedScreenBanner.kt**

```kotlin
package com.purchasely.shaker.purchasely

import android.util.Log
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.koin.compose.koinInject

@Composable
fun EmbeddedScreenBanner(
    placementId: String,
    onResult: (DisplayResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val wrapper: PurchaselyWrapper = koinInject()
    var view by remember { mutableStateOf<View?>(null) }

    LaunchedEffect(placementId) {
        when (val result = wrapper.loadPresentation(placementId)) {
            is FetchResult.Success -> {
                view = wrapper.getView(
                    presentation = result.presentation,
                    context = context,
                    onClose = { view = null },
                    onResult = onResult
                )
            }
            is FetchResult.Client -> {
                Log.d("EmbeddedScreenBanner", "[Shaker] CLIENT presentation for $placementId")
            }
            else -> {
                Log.d("EmbeddedScreenBanner", "[Shaker] Presentation not available for $placementId")
            }
        }
    }

    view?.let { androidView ->
        AndroidView(
            factory = { androidView },
            modifier = modifier
        )
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd /Users/kevin/Purchasely/Shaker/android && ./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

---

### Task 5: Register PurchaselyWrapper in Koin

**Files:**
- Modify: `android/app/src/main/java/com/purchasely/shaker/di/AppModule.kt`

- [ ] **Step 1: Add PurchaselyWrapper to Koin module**

Add import and singleton registration:

```kotlin
// Add import
import com.purchasely.shaker.purchasely.PurchaselyWrapper

// In the module block, add:
single { PurchaselyWrapper() }
```

The full updated `appModule` block becomes:
```kotlin
val appModule = module {
    single { CocktailRepository(androidContext()) }
    single { FavoritesRepository(androidContext()) }
    single { OnboardingRepository(androidContext()) }
    single { RunningModeRepository(androidContext()) }
    single { PremiumManager() }
    single { PurchaseManager(androidContext()) }
    single { PurchaselyWrapper() }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { params -> DetailViewModel(get(), get(), get(), params.get()) }
    viewModel { FavoritesViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(androidContext(), get(), get()) }
}
```

Note: `HomeViewModel` now takes 3 params (added `PurchaselyWrapper`).

- [ ] **Step 2: Verify compilation**

Run: `cd /Users/kevin/Purchasely/Shaker/android && ./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: compilation error (HomeViewModel constructor not updated yet — expected)

---

### Task 6: Refactor HomeViewModel

**Files:**
- Modify: `android/app/src/main/java/com/purchasely/shaker/ui/screen/home/HomeViewModel.kt`

- [ ] **Step 1: Rewrite HomeViewModel.kt**

Replace the full file content:

```kotlin
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

    // Paywall: ViewModel fetches, then signals Screen to display (Screen provides Activity)
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
        if (isPremium.value) {
            // Premium user: show filter sheet directly (handled by Screen via return value or state)
            return
        }
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
                Log.d("HomeViewModel", "[Shaker] Purchased/Restored from filters: ${(result as? DisplayResult.Purchased)?.planName ?: (result as? DisplayResult.Restored)?.planName}")
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
```

- [ ] **Step 2: Verify compilation**

Run: `cd /Users/kevin/Purchasely/Shaker/android && ./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: compilation error (HomeScreen still uses old APIs — expected)

---

### Task 7: Refactor HomeScreen

**Files:**
- Modify: `android/app/src/main/java/com/purchasely/shaker/ui/screen/home/HomeScreen.kt`

- [ ] **Step 1: Rewrite HomeScreen.kt**

Replace the full file content. Key changes:
- Zero `io.purchasely` imports
- Filter icon click calls `viewModel.onFilterClick()` for non-premium, or toggles sheet for premium
- `LaunchedEffect` collects `requestPaywallDisplay` to call `viewModel.displayPendingPaywall(activity)`
- `EmbeddedPaywallBanner` replaced by `EmbeddedScreenBanner` from the `purchasely` package
- Removed `EmbeddedPaywallBanner` private composable

```kotlin
package com.purchasely.shaker.ui.screen.home

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.purchasely.shaker.domain.model.Cocktail
import com.purchasely.shaker.purchasely.EmbeddedScreenBanner
import com.purchasely.shaker.ui.components.CocktailImage
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCocktailClick: (String) -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val cocktails by viewModel.cocktails.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val context = LocalContext.current
    var showFilterSheet by remember { mutableStateOf(false) }

    // Collect paywall display requests from ViewModel
    LaunchedEffect(Unit) {
        viewModel.requestPaywallDisplay.collect {
            val activity = context as? Activity ?: return@collect
            viewModel.displayPendingPaywall(activity)
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    onSearch = {},
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = { Text("Search cocktails...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        IconButton(onClick = {
                            if (isPremium) {
                                showFilterSheet = true
                            } else {
                                viewModel.onFilterClick()
                            }
                        }) {
                            if (viewModel.hasActiveFilters) {
                                BadgedBox(badge = { Badge() }) {
                                    Icon(Icons.Default.Tune, contentDescription = "Filters")
                                }
                            } else {
                                Icon(Icons.Default.Tune, contentDescription = "Filters")
                            }
                        }
                    }
                )
            },
            expanded = false,
            onExpandedChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {}

        if (cocktails.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No cocktails found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try a different search or filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (!isPremium) {
                    item(span = { GridItemSpan(2) }) {
                        EmbeddedScreenBanner(
                            placementId = "inline",
                            onResult = { viewModel.onPaywallDismissed() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .padding(vertical = 8.dp)
                        )
                    }
                }
                items(cocktails, key = { it.id }) { cocktail ->
                    CocktailCard(cocktail = cocktail, onClick = { onCocktailClick(cocktail.id) })
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterSheet(
            viewModel = viewModel,
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
private fun CocktailCard(cocktail: Cocktail, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            CocktailImage(
                cocktail = cocktail,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .clip(MaterialTheme.shapes.medium)
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = cocktail.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = cocktail.category.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

- [ ] **Step 2: Verify full compilation**

Run: `cd /Users/kevin/Purchasely/Shaker/android && ./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Verify zero `io.purchasely` imports in HomeScreen**

Run: `rg 'import io\.purchasely' android/app/src/main/java/com/purchasely/shaker/ui/screen/home/HomeScreen.kt`
Expected: no output (0 matches)

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/java/com/purchasely/shaker/purchasely/ \
        android/app/src/main/java/com/purchasely/shaker/di/AppModule.kt \
        android/app/src/main/java/com/purchasely/shaker/ui/screen/home/HomeViewModel.kt \
        android/app/src/main/java/com/purchasely/shaker/ui/screen/home/HomeScreen.kt
git commit -m "refactor(android): extract PurchaselyWrapper, clean HomeScreen/HomeViewModel"
```

---

### Task 8: Create best practices document

**Files:**
- Create: `docs/purchasely-best-practices.md` (at Shaker project root)

- [ ] **Step 1: Write the best practices document**

Create `docs/purchasely-best-practices.md` with comprehensive guidelines covering:
- Architecture: always use a wrapper, never call SDK directly from UI
- Fetching: always use `fetchPresentation()` then `buildView()`/`display()`, never `presentationView()`
- MVVM: ViewModel owns paywall logic, Screen provides Activity when needed
- Embedded vs modal patterns
- User attributes best practices
- Error handling patterns
- Platform-specific notes (Android section now, iOS section placeholder)

Full content:

```markdown
# Purchasely SDK — Best Practices

This document defines the integration standards for the Purchasely SDK in Shaker.
All changes to the Purchasely integration must follow and update this document.

---

## 1. Architecture: PurchaselyWrapper

**Rule: Never call the Purchasely SDK directly from Screens or ViewModels.**

All SDK calls go through `PurchaselyWrapper`, a Koin-injectable class in the `purchasely` package.

**Why:**
- Single point of control for all SDK interactions
- Screens have zero `io.purchasely` imports — clean separation of concerns
- Easier to test (mock the wrapper, not the SDK)
- Consistent error handling and result mapping
- Type-safe result types (`FetchResult`, `DisplayResult`) instead of raw SDK enums

**Wrapper responsibilities:**
- `loadPresentation()` — fetch a presentation (suspend)
- `display()` — show a modal paywall (suspend)
- `getView()` — build an inline view for embedding
- `setUserAttribute()` / `incrementUserAttribute()` — user targeting

---

## 2. Presentation Loading: Always fetch then build/display

**Rule: Always use `Purchasely.fetchPresentation()` followed by `presentation.buildView()` or `presentation.display()`. Never use `Purchasely.presentationView()`.**

**Why:**
- `fetchPresentation` + `buildView`/`display` gives full control over the presentation lifecycle
- You can inspect `presentation.type` before deciding what to do (NORMAL, CLIENT, DEACTIVATED)
- You can handle errors from the fetch step separately from display errors
- `presentationView()` is a convenience shortcut that hides these steps — unsuitable for production code

**Pattern:**
```kotlin
// In PurchaselyWrapper
suspend fun loadPresentation(placementId: String): FetchResult {
    // Uses Purchasely.fetchPresentation() internally
    // Maps result to FetchResult sealed class
}

// For modal display
suspend fun display(presentation: PLYPresentation, activity: Activity): DisplayResult {
    // Uses presentation.display(activity) internally
}

// For inline/embedded display
fun getView(presentation: PLYPresentation, context: Context, ...): View {
    // Uses presentation.buildView(context, onClose) internally
}
```

---

## 3. MVVM Pattern: ViewModel Owns Paywall Logic

**Rule: ViewModels decide when and what to show. Screens only provide the Activity and render the UI.**

**Modal paywall flow:**
1. User action triggers ViewModel method (e.g., `onFilterClick()`)
2. ViewModel checks premium status
3. If not premium, ViewModel calls `wrapper.loadPresentation()` (suspend, in viewModelScope)
4. On success, ViewModel stores the presentation and emits an event via `SharedFlow<Unit>`
5. Screen collects the event, gets the Activity, calls `viewModel.displayPendingPaywall(activity)`
6. ViewModel calls `wrapper.display(presentation, activity)` and handles the result

**Why the Activity passes through the ViewModel:**
- The SDK requires an `Activity` to display modal paywalls
- The ViewModel cannot hold an Activity reference (lifecycle leak)
- The Screen provides the Activity on-demand when the ViewModel signals readiness
- This is a standard Android MVVM compromise for SDK interactions

**Embedded paywall flow:**
1. Use the `EmbeddedScreenBanner` composable from the `purchasely` package
2. Pass `placementId` and an `onResult` callback
3. The composable handles fetch + buildView internally via `LaunchedEffect`
4. The `onResult` callback notifies the ViewModel of purchase results

---

## 4. EmbeddedScreenBanner: Reusable Inline Paywall

**Rule: Use `EmbeddedScreenBanner` for any inline/embedded paywall display.**

```kotlin
EmbeddedScreenBanner(
    placementId = "inline",
    onResult = { viewModel.onPaywallDismissed() },
    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
)
```

**Behavior:**
- Fetches the presentation via `PurchaselyWrapper.loadPresentation()`
- Builds the view via `PurchaselyWrapper.getView()`
- Renders via `AndroidView`
- Handles CLIENT and DEACTIVATED types gracefully (logs, no crash)
- `onClose` hides the banner; `onResult` forwards purchase events

---

## 5. User Attributes

**Rule: Set user attributes through `PurchaselyWrapper`, always from the ViewModel layer.**

```kotlin
// In ViewModel
purchaselyWrapper.setUserAttribute("has_used_search", true)
purchaselyWrapper.incrementUserAttribute("cocktails_viewed")
purchaselyWrapper.setUserAttribute("favorite_spirit", "gin")
```

**When to set attributes:**
- On meaningful user actions (search, view detail, add favorite)
- On preference changes (theme, user ID)
- Never on every recomposition — only on actual state changes

---

## 6. Handling Presentation Types

Always handle all `FetchResult` variants:

| Type | Action |
|------|--------|
| `Success` | Display or build view normally |
| `Client` | App must build its own paywall UI using plan data from the presentation |
| `Deactivated` | Do nothing — placement is disabled in the Purchasely console |
| `Error` | Log the error, fail gracefully (no crash, no empty screen) |

---

## 7. Error Handling

- **Never crash on SDK errors.** Log and degrade gracefully.
- **Never block the UI** waiting for a presentation. Use coroutines (suspend) and show content immediately.
- **Embedded views:** If fetch fails, the banner simply doesn't appear.
- **Modal paywalls:** If fetch fails, the user action is silently ignored (with a log).

---

## 8. Platform-Specific Notes

### Android (Kotlin / Jetpack Compose)

- `PurchaselyWrapper` is a Koin singleton (`single { PurchaselyWrapper() }`)
- ViewModels inject it via constructor: `class HomeViewModel(..., private val purchaselyWrapper: PurchaselyWrapper)`
- `EmbeddedScreenBanner` uses `koinInject()` for DI in Composables
- Async operations use Kotlin coroutines (`suspend` functions with `suspendCoroutine`)
- `display()` requires an `Activity` — passed from Screen to ViewModel on-demand

### iOS (SwiftUI)

_To be defined — will follow the same architectural principles adapted to SwiftUI/Combine patterns._

---

## Checklist for New Purchasely Integrations

- [ ] All SDK calls go through `PurchaselyWrapper`
- [ ] Screen has zero `io.purchasely` imports
- [ ] Uses `loadPresentation()` + `display()`/`getView()`, never `presentationView()`
- [ ] Handles all `FetchResult` variants (Success, Client, Deactivated, Error)
- [ ] User attributes set from ViewModel, not Screen
- [ ] Modal paywalls: ViewModel fetches, Screen provides Activity
- [ ] Embedded paywalls: uses `EmbeddedScreenBanner`
- [ ] No crashes on SDK errors
```

- [ ] **Step 2: Commit**

```bash
git add docs/purchasely-best-practices.md
git commit -m "docs: add Purchasely SDK best practices guide"
```

---

### Task 9: Update CLAUDE.md

**Files:**
- Modify: `CLAUDE.md` (Shaker project root)

- [ ] **Step 1: Update About section**

Change the first paragraph of the About section to emphasize demo/reference purpose:

From:
```
Shaker is a cocktail discovery app demonstrating a production-quality Purchasely SDK integration.
```

To:
```
Shaker is a **reference demo application** showcasing the best way to integrate the Purchasely SDK on iOS and Android. It serves as the canonical example for SDK integration patterns — all code must follow the best practices defined in `docs/purchasely-best-practices.md`.
```

- [ ] **Step 2: Add best practices reference to Conventions section**

Add at the end of the Conventions section:

```markdown
- **Purchasely best practices**: All SDK integration changes must follow `docs/purchasely-best-practices.md`. Update the doc when patterns change.
```

- [ ] **Step 3: Add PurchaselyWrapper to Key Components**

Add to the Key Components section:

```markdown
- **PurchaselyWrapper**: Koin singleton wrapping all Purchasely SDK calls. ViewModels use this exclusively — Screens never import `io.purchasely`. See `docs/purchasely-best-practices.md`.
- **EmbeddedScreenBanner**: Reusable Composable for inline paywall display. Uses PurchaselyWrapper internally.
```

- [ ] **Step 4: Update Repository Structure**

Add `purchasely/` to the Android directory tree:

```
│   │   ├── data/            # CocktailRepository, FavoritesRepository, PremiumManager, OnboardingRepository
│   │   ├── di/              # Koin DI modules
│   │   ├── domain/model/    # Data models (Cocktail, Ingredient)
│   │   ├── purchasely/      # PurchaselyWrapper, EmbeddedScreenBanner, result types
│   │   └── ui/              # Compose UI (screens, navigation, theme, components)
```

- [ ] **Step 5: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: update CLAUDE.md with PurchaselyWrapper architecture and best practices ref"
```
