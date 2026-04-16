# Android Architecture Cleanup — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve Shaker Android architecture: decouple ViewModels from Purchasely SDK types, extract SharedPreferences behind abstractions, add missing Repository interfaces and UseCases, fix Compose reactivity bugs, and harden type-safety.

**Architecture:** Three priority tiers — P1 (quick wins, immediate impact on code quality), P2 (structural refactors for Clean Architecture), P3 (long-term polish). Each task is independently mergeable. P1 tasks should be done before P2, which should be done before P3.

**Tech Stack:** Kotlin, Jetpack Compose, Koin 4.0, kotlinx.coroutines/Flow, MockK, JUnit 4, Turbine

**Base path:** `app/src/main/java/com/purchasely/shaker/` (abbreviated as `shaker/` below)
**Test path:** `app/src/test/java/com/purchasely/shaker/` (abbreviated as `test/` below)

---

## P1 — Quick Wins

---

### Task 1: Wrap PLYPresentation in opaque PresentationHandle

**Why:** 4 ViewModels import `io.purchasely.ext.PLYPresentation` directly, coupling the presentation layer to the SDK. `FetchResult.Success` and `FetchResult.Client` also expose `PLYPresentation`. Wrapping it in an opaque handle eliminates all `io.purchasely` imports from ViewModels.

**Files:**
- Create: `shaker/purchasely/PresentationHandle.kt`
- Modify: `shaker/purchasely/FetchResult.kt`
- Modify: `shaker/purchasely/PurchaselyWrapper.kt:256-267,271-286`
- Modify: `shaker/purchasely/EmbeddedScreenBanner.kt:19-23`
- Modify: `shaker/ui/screen/home/HomeViewModel.kt:13,67,100,105-108`
- Modify: `shaker/ui/screen/detail/DetailViewModel.kt:14,39,44,79,90-93,113,125-128`
- Modify: `shaker/ui/screen/favorites/FavoritesViewModel.kt:14,33,49,66-69`
- Modify: `shaker/ui/screen/settings/SettingsViewModel.kt:16,81,159,175-178`
- Modify: `test/purchasely/PurchaselyWrapperTest.kt` (if it references PLYPresentation)

- [ ] **Step 1: Create PresentationHandle**

```kotlin
// shaker/purchasely/PresentationHandle.kt
package com.purchasely.shaker.purchasely

import io.purchasely.ext.PLYPresentation

/**
 * Opaque wrapper around PLYPresentation.
 * ViewModels hold this handle — only PurchaselyWrapper unwraps it.
 */
@JvmInline
value class PresentationHandle internal constructor(
    @PublishedApi internal val presentation: PLYPresentation
)
```

- [ ] **Step 2: Update FetchResult to use PresentationHandle**

Replace `FetchResult.kt` entirely:

```kotlin
package com.purchasely.shaker.purchasely

sealed class FetchResult {
    data class Success(val handle: PresentationHandle, val height: Int) : FetchResult()
    data class Client(val handle: PresentationHandle) : FetchResult()
    data object Deactivated : FetchResult()
    data class Error(val message: String?) : FetchResult()
}
```

Note: `PLYError` is also removed — we extract the message string at the boundary. `height` is extracted from `presentation.height` at creation time.

- [ ] **Step 3: Update PurchaselyWrapper.loadPresentation() to return PresentationHandle**

In `PurchaselyWrapper.kt`, update `loadPresentation`:

```kotlin
suspend fun loadPresentation(
    placementId: String,
    contentId: String? = null
): FetchResult {
    return try {
        val presentation = if (contentId != null) {
            Purchasely.fetchPresentation(
                properties = PLYPresentationProperties(placementId = placementId, contentId = contentId)
            )
        } else {
            Purchasely.fetchPresentation(placementId = placementId)
        }
        val handle = PresentationHandle(presentation)
        when (presentation.type) {
            PLYPresentationType.DEACTIVATED -> FetchResult.Deactivated
            PLYPresentationType.CLIENT -> FetchResult.Client(handle)
            else -> FetchResult.Success(handle, presentation.height)
        }
    } catch (e: Exception) {
        FetchResult.Error((e as? PLYError)?.message)
    }
}
```

- [ ] **Step 4: Update PurchaselyWrapper.display() to accept PresentationHandle**

```kotlin
suspend fun display(
    handle: PresentationHandle,
    activity: Activity
): DisplayResult = suspendCoroutine { continuation ->
    handle.presentation.display(activity) { result: PLYProductViewResult, plan: PLYPlan? ->
        when (result) {
            PLYProductViewResult.PURCHASED -> continuation.resume(DisplayResult.Purchased(plan?.name))
            PLYProductViewResult.RESTORED -> continuation.resume(DisplayResult.Restored(plan?.name))
            else -> continuation.resume(DisplayResult.Cancelled)
        }
    }
}
```

- [ ] **Step 5: Update PurchaselyWrapper.getView() to accept PresentationHandle**

```kotlin
fun getView(
    handle: PresentationHandle,
    context: Context,
    onResult: (DisplayResult) -> Unit
): View? {
    return handle.presentation.buildView(
        context = context,
        callback = { result: PLYProductViewResult, plan: PLYPlan? ->
            when (result) {
                PLYProductViewResult.PURCHASED -> onResult(DisplayResult.Purchased(plan?.name))
                PLYProductViewResult.RESTORED -> onResult(DisplayResult.Restored(plan?.name))
                else -> onResult(DisplayResult.Cancelled)
            }
        }
    )
}
```

- [ ] **Step 6: Update EmbeddedScreenBanner**

```kotlin
@Composable
fun EmbeddedScreenBanner(
    fetchResult: FetchResult.Success,
    onResult: (DisplayResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val wrapper: PurchaselyWrapper = koinInject()

    AndroidView(
        factory = { context ->
            wrapper.getView(
                handle = fetchResult.handle,
                context = context,
                onResult = onResult
            ) ?: FrameLayout(context)
        },
        modifier = modifier
    )
}
```

- [ ] **Step 7: Update all ViewModels — replace `PLYPresentation` with `PresentationHandle`**

In **HomeViewModel.kt**, replace:
- Remove `import io.purchasely.ext.PLYPresentation`
- `private var pendingFiltersPresentation: PLYPresentation? = null` → `private var pendingFiltersPresentation: PresentationHandle? = null`
- In `onFilterClick()`: `pendingFiltersPresentation = result.presentation` → `pendingFiltersPresentation = result.handle`
- In `displayPendingPaywall()`: `purchaselyWrapper.display(presentation, activity)` → `purchaselyWrapper.display(presentation, activity)` (variable name stays, type changes)

In **DetailViewModel.kt**, replace:
- Remove `import io.purchasely.ext.PLYPresentation`
- `private var pendingRecipePresentation: PLYPresentation? = null` → `PresentationHandle?`
- `private var pendingFavoritesPresentation: PLYPresentation? = null` → `PresentationHandle?`
- `pendingRecipePresentation = result.presentation` → `result.handle`
- `pendingFavoritesPresentation = result.presentation` → `result.handle`

In **FavoritesViewModel.kt**, replace:
- Remove `import io.purchasely.ext.PLYPresentation`
- `private var pendingPresentation: PLYPresentation? = null` → `PresentationHandle?`
- `pendingPresentation = result.presentation` → `result.handle`

In **SettingsViewModel.kt**, replace:
- Remove `import io.purchasely.ext.PLYPresentation`
- `private var pendingOnboardingPresentation: PLYPresentation? = null` → `PresentationHandle?`
- `pendingOnboardingPresentation = result.presentation` → `result.handle`

- [ ] **Step 8: Run tests and fix any compilation errors**

Run: `./gradlew testDebugUnitTest`

Update test files that reference `PLYPresentation` via `FetchResult` to use `PresentationHandle` mocks instead. Since `PresentationHandle` is an inline value class wrapping `PLYPresentation`, existing mocks of `PLYPresentation` can be wrapped: `PresentationHandle(mockPresentation)`.

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "refactor(android): wrap PLYPresentation in opaque PresentationHandle

Removes all io.purchasely imports from ViewModels. SDK types are now
contained within PurchaselyWrapper and PresentationHandle."
```

---

### Task 2: Route PremiumManager through PurchaselyWrapper

**Why:** `PremiumManager.kt:20` calls `Purchasely.userSubscriptions()` directly, bypassing the wrapper abstraction. This makes PremiumManager untestable without static mocking and violates the encapsulation pattern.

**Files:**
- Modify: `shaker/purchasely/PurchaselyWrapper.kt` (add method)
- Modify: `shaker/data/PremiumManager.kt` (inject wrapper, remove SDK imports)
- Modify: `shaker/di/AppModule.kt:32` (pass wrapper to PremiumManager)
- Modify: `test/data/PremiumManagerTest.kt` (if exists) or create test

- [ ] **Step 1: Write failing test for PremiumManager with injected wrapper**

```kotlin
// test/data/PremiumManagerTest.kt
package com.purchasely.shaker.data

import com.purchasely.shaker.purchasely.PurchaselyWrapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.purchasely.ext.SubscriptionsListener
import io.purchasely.models.PLYSubscriptionData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumManagerTest {
    private val wrapper = mockk<PurchaselyWrapper>(relaxed = true)
    private val manager = PremiumManager(wrapper)

    @Test
    fun `refreshPremiumStatus sets isPremium true when active subscription exists`() = runTest {
        val listenerSlot = slot<SubscriptionsListener>()
        every { wrapper.userSubscriptions(any(), capture(listenerSlot)) } answers {
            val mockSub = mockk<PLYSubscriptionData>(relaxed = true) {
                every { data.subscriptionStatus?.isExpired() } returns false
            }
            listenerSlot.captured.onSuccess(listOf(mockSub))
        }

        manager.refreshPremiumStatus()
        assertTrue(manager.isPremium.first())
    }

    @Test
    fun `refreshPremiumStatus sets isPremium false when no subscriptions`() = runTest {
        val listenerSlot = slot<SubscriptionsListener>()
        every { wrapper.userSubscriptions(any(), capture(listenerSlot)) } answers {
            listenerSlot.captured.onSuccess(emptyList())
        }

        manager.refreshPremiumStatus()
        assertFalse(manager.isPremium.first())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.purchasely.shaker.data.PremiumManagerTest"`

Expected: FAIL — `PremiumManager` constructor doesn't accept `PurchaselyWrapper`.

- [ ] **Step 3: Add userSubscriptions to PurchaselyWrapper**

Add to `PurchaselyWrapper.kt` after the `synchronize()` method:

```kotlin
// MARK: - Subscription Status

fun userSubscriptions(invalidateCache: Boolean, listener: SubscriptionsListener) {
    Purchasely.userSubscriptions(invalidateCache, listener)
}
```

Add import `import io.purchasely.ext.SubscriptionsListener` if not already present.

- [ ] **Step 4: Refactor PremiumManager to accept PurchaselyWrapper**

Replace `PremiumManager.kt` entirely:

```kotlin
package com.purchasely.shaker.data

import android.util.Log
import com.purchasely.shaker.purchasely.PurchaselyWrapper
import io.purchasely.ext.SubscriptionsListener
import io.purchasely.models.PLYSubscriptionData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PremiumManager(
    private val wrapper: PurchaselyWrapper
) {

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    fun refreshPremiumStatus() {
        wrapper.userSubscriptions(false, object : SubscriptionsListener {
            override fun onSuccess(subscriptions: List<PLYSubscriptionData>) {
                val premium = subscriptions.any { subscriptionData ->
                    subscriptionData.data.subscriptionStatus?.isExpired() == false
                }
                _isPremium.value = premium
                Log.d(TAG, "[Shaker] Premium status: $premium")
            }

            override fun onFailure(error: Throwable) {
                Log.e(TAG, "[Shaker] Error checking premium: ${error.message}")
            }
        })
    }

    companion object {
        private const val TAG = "PremiumManager"
    }
}
```

- [ ] **Step 5: Fix circular dependency in DI**

`PurchaselyWrapper` already takes `PremiumManager` in its constructor, and now `PremiumManager` takes `PurchaselyWrapper`. This is a circular dependency.

Break it: `PurchaselyWrapper` should NOT take `PremiumManager`. Instead, make `PurchaselyWrapper.initialize()` accept a callback for post-init actions, and call `premiumManager.refreshPremiumStatus()` from the DI wiring or `ShakerApp`.

Update `PurchaselyWrapper` constructor — remove `premiumManager`:

```kotlin
class PurchaselyWrapper(
    private val runningModeRepo: RunningModeRepository,
    private val purchaseRequests: MutableSharedFlow<PurchaseRequest>,
    private val restoreRequests: MutableSharedFlow<RestoreRequest>,
    private val transactionResult: SharedFlow<TransactionResult>,
    private val scope: CoroutineScope
) {
```

Update `initialize()` to accept an `onConfigured` callback:

```kotlin
fun initialize(
    application: Application,
    apiKey: String,
    logLevel: LogLevel = LogLevel.DEBUG,
    onConfigured: (() -> Unit)? = null
) {
    // ... existing code ...
    .start { isConfigured, error ->
        if (isConfigured) {
            Log.d(TAG, "[Shaker] Purchasely SDK configured successfully")
            onConfigured?.invoke()
        }
        // ...
    }
```

Update `handleTransactionResult` — remove `premiumManager.refreshPremiumStatus()` calls, replace with a `var onTransactionCompleted: (() -> Unit)? = null` callback:

```kotlin
var onTransactionCompleted: (() -> Unit)? = null

private fun handleTransactionResult(result: TransactionResult) {
    when (result) {
        is TransactionResult.Success -> {
            synchronize()
            pendingProcessAction?.invoke(false)
            pendingProcessAction = null
            onTransactionCompleted?.invoke()
            Log.d(TAG, "[Shaker] Transaction success — synchronized and refreshed")
        }
        // ... rest unchanged, remove premiumManager references ...
    }
}
```

- [ ] **Step 6: Update AppModule.kt**

```kotlin
val appModule = module {
    single { CocktailRepository(androidContext()) }
    single { FavoritesRepository(androidContext()) }
    single { OnboardingRepository(androidContext()) }
    single { RunningModeRepository(androidContext()) }
    single(named("purchaseRequests")) { MutableSharedFlow<PurchaseRequest>() }
    single(named("restoreRequests")) { MutableSharedFlow<RestoreRequest>() }
    single(named("appScope")) { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    single {
        PurchaseManager(
            billingClientFactory = { listener ->
                BillingClient.newBuilder(androidContext())
                    .setListener(listener)
                    .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder()
                            .enableOneTimeProducts()
                            .build()
                    )
                    .build()
            },
            purchaseRequests = get<MutableSharedFlow<PurchaseRequest>>(named("purchaseRequests")),
            restoreRequests = get<MutableSharedFlow<RestoreRequest>>(named("restoreRequests")),
            scope = get(named("appScope"))
        )
    }
    single {
        PurchaselyWrapper(
            runningModeRepo = get(),
            purchaseRequests = get(named("purchaseRequests")),
            restoreRequests = get(named("restoreRequests")),
            transactionResult = get<PurchaseManager>().transactionResult,
            scope = get(named("appScope"))
        )
    }
    single {
        PremiumManager(wrapper = get()).also { pm ->
            get<PurchaselyWrapper>().onTransactionCompleted = { pm.refreshPremiumStatus() }
        }
    }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { params -> DetailViewModel(get(), get(), get(), get(), params.get()) }
    viewModel { FavoritesViewModel(get(), get(), get(), get()) }
    viewModel { SettingsViewModel(androidContext(), get(), get(), get()) }
}
```

- [ ] **Step 7: Update ShakerApp.kt to pass onConfigured callback**

Find where `initialize()` is called and pass `premiumManager.refreshPremiumStatus()` as the callback.

- [ ] **Step 8: Run tests**

Run: `./gradlew testDebugUnitTest`
Expected: ALL PASS

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "refactor(android): route PremiumManager through PurchaselyWrapper

Removes direct Purchasely SDK calls from PremiumManager. Breaks circular
dependency by using callbacks for post-init and transaction events."
```

---

### Task 3: Extract SharedPreferences behind KeyValueStore interface

**Why:** 4 repositories and SettingsViewModel directly depend on `android.content.SharedPreferences`/`Context`, making them untestable without Android framework and blocking any future KMP migration.

**Files:**
- Create: `shaker/data/storage/KeyValueStore.kt`
- Create: `shaker/data/storage/SharedPreferencesKeyValueStore.kt`
- Modify: `shaker/data/FavoritesRepository.kt`
- Modify: `shaker/data/OnboardingRepository.kt`
- Modify: `shaker/data/RunningModeRepository.kt`
- Modify: `shaker/di/AppModule.kt`
- Modify: tests for repositories

- [ ] **Step 1: Create KeyValueStore interface**

```kotlin
// shaker/data/storage/KeyValueStore.kt
package com.purchasely.shaker.data.storage

interface KeyValueStore {
    fun getString(key: String, default: String? = null): String?
    fun putString(key: String, value: String?)
    fun getBoolean(key: String, default: Boolean = false): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getStringSet(key: String, default: Set<String> = emptySet()): Set<String>
    fun putStringSet(key: String, value: Set<String>)
    fun contains(key: String): Boolean
    fun remove(key: String)
}
```

- [ ] **Step 2: Create SharedPreferencesKeyValueStore**

```kotlin
// shaker/data/storage/SharedPreferencesKeyValueStore.kt
package com.purchasely.shaker.data.storage

import android.content.SharedPreferences

class SharedPreferencesKeyValueStore(
    private val prefs: SharedPreferences
) : KeyValueStore {

    override fun getString(key: String, default: String?): String? =
        prefs.getString(key, default)

    override fun putString(key: String, value: String?) =
        prefs.edit().putString(key, value).apply()

    override fun getBoolean(key: String, default: Boolean): Boolean =
        prefs.getBoolean(key, default)

    override fun putBoolean(key: String, value: Boolean) =
        prefs.edit().putBoolean(key, value).apply()

    override fun getStringSet(key: String, default: Set<String>): Set<String> =
        prefs.getStringSet(key, default) ?: default

    override fun putStringSet(key: String, value: Set<String>) =
        prefs.edit().putStringSet(key, value).apply()

    override fun contains(key: String): Boolean = prefs.contains(key)

    override fun remove(key: String) = prefs.edit().remove(key).apply()
}
```

- [ ] **Step 3: Create InMemoryKeyValueStore for tests**

```kotlin
// test/data/storage/InMemoryKeyValueStore.kt
package com.purchasely.shaker.data.storage

class InMemoryKeyValueStore : KeyValueStore {
    private val store = mutableMapOf<String, Any?>()

    override fun getString(key: String, default: String?): String? =
        store[key] as? String ?: default

    override fun putString(key: String, value: String?) { store[key] = value }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        store[key] as? Boolean ?: default

    override fun putBoolean(key: String, value: Boolean) { store[key] = value }

    override fun getStringSet(key: String, default: Set<String>): Set<String> =
        @Suppress("UNCHECKED_CAST")
        (store[key] as? Set<String>) ?: default

    override fun putStringSet(key: String, value: Set<String>) { store[key] = value }

    override fun contains(key: String): Boolean = store.containsKey(key)

    override fun remove(key: String) { store.remove(key) }
}
```

- [ ] **Step 4: Refactor FavoritesRepository**

```kotlin
package com.purchasely.shaker.data

import com.purchasely.shaker.data.storage.KeyValueStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FavoritesRepository(private val store: KeyValueStore) {

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    init {
        _favoriteIds.value = store.getStringSet(KEY_FAVORITES)
    }

    fun isFavorite(cocktailId: String): Boolean = _favoriteIds.value.contains(cocktailId)

    fun toggleFavorite(cocktailId: String) {
        val current = _favoriteIds.value.toMutableSet()
        if (current.contains(cocktailId)) current.remove(cocktailId) else current.add(cocktailId)
        _favoriteIds.value = current
        store.putStringSet(KEY_FAVORITES, current)
    }

    fun addFavorite(cocktailId: String) {
        val current = _favoriteIds.value.toMutableSet()
        current.add(cocktailId)
        _favoriteIds.value = current
        store.putStringSet(KEY_FAVORITES, current)
    }

    fun removeFavorite(cocktailId: String) {
        val current = _favoriteIds.value.toMutableSet()
        current.remove(cocktailId)
        _favoriteIds.value = current
        store.putStringSet(KEY_FAVORITES, current)
    }

    companion object {
        private const val KEY_FAVORITES = "favorite_cocktail_ids"
    }
}
```

- [ ] **Step 5: Refactor OnboardingRepository**

```kotlin
package com.purchasely.shaker.data

import com.purchasely.shaker.data.storage.KeyValueStore

class OnboardingRepository(private val store: KeyValueStore) {

    var isOnboardingCompleted: Boolean
        get() = store.getBoolean(KEY_COMPLETED)
        set(value) { store.putBoolean(KEY_COMPLETED, value) }

    companion object {
        private const val KEY_COMPLETED = "onboarding_completed"
    }
}
```

- [ ] **Step 6: Refactor RunningModeRepository**

```kotlin
package com.purchasely.shaker.data

import com.purchasely.shaker.data.storage.KeyValueStore
import io.purchasely.ext.PLYRunningMode

class RunningModeRepository(private val store: KeyValueStore) {

    var runningMode: PLYRunningMode
        get() {
            val stored = store.getString(KEY_RUNNING_MODE, "full")
            return if (stored == "observer") PLYRunningMode.PaywallObserver else PLYRunningMode.Full
        }
        set(value) {
            val str = if (value == PLYRunningMode.PaywallObserver) "observer" else "full"
            store.putString(KEY_RUNNING_MODE, str)
        }

    val isObserverMode: Boolean
        get() = runningMode == PLYRunningMode.PaywallObserver

    companion object {
        private const val KEY_RUNNING_MODE = "running_mode"
    }
}
```

- [ ] **Step 7: Update AppModule.kt DI**

Replace repository registrations:

```kotlin
single(named("favorites")) {
    SharedPreferencesKeyValueStore(
        androidContext().getSharedPreferences("shaker_favorites", Context.MODE_PRIVATE)
    ) as KeyValueStore
}
single(named("onboarding")) {
    SharedPreferencesKeyValueStore(
        androidContext().getSharedPreferences("shaker_onboarding", Context.MODE_PRIVATE)
    ) as KeyValueStore
}
single(named("settings")) {
    SharedPreferencesKeyValueStore(
        androidContext().getSharedPreferences("shaker_settings", Context.MODE_PRIVATE)
    ) as KeyValueStore
}
single { FavoritesRepository(get(named("favorites"))) }
single { OnboardingRepository(get(named("onboarding"))) }
single { RunningModeRepository(get(named("settings"))) }
```

Add imports:
```kotlin
import android.content.Context
import com.purchasely.shaker.data.storage.KeyValueStore
import com.purchasely.shaker.data.storage.SharedPreferencesKeyValueStore
```

- [ ] **Step 8: Update FavoritesRepositoryTest to use InMemoryKeyValueStore**

Replace SharedPreferences mocking with:

```kotlin
private val store = InMemoryKeyValueStore()
private val repo = FavoritesRepository(store)
```

- [ ] **Step 9: Update OnboardingRepositoryTest similarly**

```kotlin
private val store = InMemoryKeyValueStore()
private val repo = OnboardingRepository(store)
```

- [ ] **Step 10: Run tests**

Run: `./gradlew testDebugUnitTest`
Expected: ALL PASS

- [ ] **Step 11: Commit**

```bash
git add -A
git commit -m "refactor(android): extract SharedPreferences behind KeyValueStore interface

FavoritesRepository, OnboardingRepository, RunningModeRepository now
accept KeyValueStore instead of Context. InMemoryKeyValueStore for tests."
```

---

### Task 4: Extract SettingsRepository from SettingsViewModel

**Why:** `SettingsViewModel` directly manages `SharedPreferences` for ~10 keys (userId, theme, display mode, consent toggles). This logic belongs in a Repository. The ViewModel should only orchestrate UI state.

**Files:**
- Create: `shaker/data/SettingsRepository.kt`
- Modify: `shaker/ui/screen/settings/SettingsViewModel.kt`
- Modify: `shaker/di/AppModule.kt`
- Modify: `test/ui/screen/settings/SettingsViewModelTest.kt`

- [ ] **Step 1: Create SettingsRepository**

```kotlin
// shaker/data/SettingsRepository.kt
package com.purchasely.shaker.data

import com.purchasely.shaker.data.storage.KeyValueStore

class SettingsRepository(private val store: KeyValueStore) {

    var userId: String?
        get() = store.getString(KEY_USER_ID)
        set(value) {
            if (value != null) store.putString(KEY_USER_ID, value)
            else store.remove(KEY_USER_ID)
        }

    var themeMode: String
        get() = store.getString(KEY_THEME, "system") ?: "system"
        set(value) = store.putString(KEY_THEME, value)

    var displayMode: String
        get() = store.getString(KEY_DISPLAY_MODE, "fullscreen") ?: "fullscreen"
        set(value) = store.putString(KEY_DISPLAY_MODE, value)

    var sdkModeStorage: String
        get() = store.getString(PurchaselySdkMode.KEY, PurchaselySdkMode.DEFAULT.storageValue)
            ?: PurchaselySdkMode.DEFAULT.storageValue
        set(value) = store.putString(PurchaselySdkMode.KEY, value)

    var analyticsConsent: Boolean
        get() = store.getBoolean(KEY_CONSENT_ANALYTICS, true)
        set(value) = store.putBoolean(KEY_CONSENT_ANALYTICS, value)

    var identifiedAnalyticsConsent: Boolean
        get() = store.getBoolean(KEY_CONSENT_IDENTIFIED_ANALYTICS, true)
        set(value) = store.putBoolean(KEY_CONSENT_IDENTIFIED_ANALYTICS, value)

    var personalizationConsent: Boolean
        get() = store.getBoolean(KEY_CONSENT_PERSONALIZATION, true)
        set(value) = store.putBoolean(KEY_CONSENT_PERSONALIZATION, value)

    var campaignsConsent: Boolean
        get() = store.getBoolean(KEY_CONSENT_CAMPAIGNS, true)
        set(value) = store.putBoolean(KEY_CONSENT_CAMPAIGNS, value)

    var thirdPartyConsent: Boolean
        get() = store.getBoolean(KEY_CONSENT_THIRD_PARTY, true)
        set(value) = store.putBoolean(KEY_CONSENT_THIRD_PARTY, value)

    fun initSdkModeIfNeeded() {
        if (!store.contains(PurchaselySdkMode.KEY)) {
            store.putString(PurchaselySdkMode.KEY, PurchaselySdkMode.DEFAULT.storageValue)
        }
    }

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_DISPLAY_MODE = "display_mode"
        private const val KEY_CONSENT_ANALYTICS = "consent_analytics"
        private const val KEY_CONSENT_IDENTIFIED_ANALYTICS = "consent_identified_analytics"
        private const val KEY_CONSENT_PERSONALIZATION = "consent_personalization"
        private const val KEY_CONSENT_CAMPAIGNS = "consent_campaigns"
        private const val KEY_CONSENT_THIRD_PARTY = "consent_third_party"
    }
}
```

- [ ] **Step 2: Refactor SettingsViewModel to use SettingsRepository**

Replace constructor and remove `Context`/`SharedPreferences`:

```kotlin
class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
    private val premiumManager: PremiumManager,
    private val runningModeRepo: RunningModeRepository,
    private val purchaselyWrapper: PurchaselyWrapper
) : ViewModel() {

    private val _userId = MutableStateFlow(settingsRepo.userId)
    // ...
    private val _themeMode = MutableStateFlow(settingsRepo.themeMode)
    // ...
    private val _sdkMode = MutableStateFlow(PurchaselySdkMode.fromStorage(settingsRepo.sdkModeStorage))
    // ...
    private val _analyticsConsent = MutableStateFlow(settingsRepo.analyticsConsent)
    // ... (same pattern for all consent toggles)
    private val _displayMode = MutableStateFlow(settingsRepo.displayMode)

    init {
        settingsRepo.initSdkModeIfNeeded()
        applyConsentPreferences()
    }
```

Replace all `prefs.edit().putXxx(...).apply()` with `settingsRepo.xxx = value` in each setter method.

Remove `import android.content.Context`, `import android.content.SharedPreferences`.

- [ ] **Step 3: Update AppModule.kt**

```kotlin
single { SettingsRepository(get(named("settings"))) }
viewModel { SettingsViewModel(get<SettingsRepository>(), get(), get(), get()) }
```

Remove `androidContext()` from SettingsViewModel constructor.

- [ ] **Step 4: Update SettingsViewModelTest**

Replace `SharedPreferences` mock with `InMemoryKeyValueStore`:

```kotlin
private val store = InMemoryKeyValueStore()
private val settingsRepo = SettingsRepository(store)
private val viewModel = SettingsViewModel(settingsRepo, mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
```

- [ ] **Step 5: Run tests**

Run: `./gradlew testDebugUnitTest`
Expected: ALL PASS

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor(android): extract SettingsRepository from SettingsViewModel

SettingsViewModel no longer depends on Context or SharedPreferences.
All persistent settings go through SettingsRepository + KeyValueStore."
```

---

### Task 5: Create OnboardingViewModel

**Why:** `OnboardingScreen.kt:36-37` directly injects `PremiumManager` and `PurchaselyWrapper` via `koinInject()`, placing business logic (fetch presentation, display, refresh premium) in a Composable. This violates MVVM.

**Files:**
- Create: `shaker/ui/screen/onboarding/OnboardingViewModel.kt`
- Modify: `shaker/ui/screen/onboarding/OnboardingScreen.kt`
- Modify: `shaker/di/AppModule.kt`

- [ ] **Step 1: Create OnboardingViewModel**

```kotlin
// shaker/ui/screen/onboarding/OnboardingViewModel.kt
package com.purchasely.shaker.ui.screen.onboarding

import android.util.Log
import androidx.lifecycle.ViewModel
import com.purchasely.shaker.data.PremiumManager
import com.purchasely.shaker.purchasely.DisplayResult
import com.purchasely.shaker.purchasely.FetchResult
import com.purchasely.shaker.purchasely.PresentationHandle
import com.purchasely.shaker.purchasely.PurchaselyWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OnboardingViewModel(
    private val purchaselyWrapper: PurchaselyWrapper,
    private val premiumManager: PremiumManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var pendingPresentation: PresentationHandle? = null

    suspend fun loadOnboarding(): FetchResult {
        _isLoading.value = true
        val result = purchaselyWrapper.loadPresentation("onboarding")
        if (result is FetchResult.Success) {
            pendingPresentation = result.handle
        }
        _isLoading.value = false
        return result
    }

    suspend fun displayOnboarding(activity: android.app.Activity): DisplayResult? {
        val handle = pendingPresentation ?: return null
        pendingPresentation = null
        val result = purchaselyWrapper.display(handle, activity)
        when (result) {
            is DisplayResult.Purchased, is DisplayResult.Restored -> {
                Log.d(TAG, "[Shaker] Purchased/Restored from onboarding")
                premiumManager.refreshPremiumStatus()
            }
            is DisplayResult.Cancelled -> {
                Log.d(TAG, "[Shaker] Onboarding paywall cancelled")
            }
        }
        return result
    }

    companion object {
        private const val TAG = "OnboardingViewModel"
    }
}
```

- [ ] **Step 2: Simplify OnboardingScreen**

```kotlin
@Composable
fun OnboardingScreen(
    showOnboarding: Boolean,
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (!showOnboarding) {
            onComplete()
            return@LaunchedEffect
        }

        val activity = context as? Activity
        if (activity == null) {
            onComplete()
            return@LaunchedEffect
        }

        when (val result = viewModel.loadOnboarding()) {
            is FetchResult.Success -> {
                viewModel.displayOnboarding(activity)
                onComplete()
            }
            else -> {
                onComplete()
            }
        }
    }

    SplashContent()
}
```

Remove imports: `com.purchasely.shaker.data.PremiumManager`, `com.purchasely.shaker.purchasely.PurchaselyWrapper`, `org.koin.compose.koinInject`.
Add imports: `org.koin.androidx.compose.koinViewModel`, `com.purchasely.shaker.purchasely.FetchResult`.

- [ ] **Step 3: Register in AppModule**

```kotlin
viewModel { OnboardingViewModel(get(), get()) }
```

- [ ] **Step 4: Run tests**

Run: `./gradlew testDebugUnitTest`
Expected: ALL PASS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor(android): create OnboardingViewModel

Moves business logic out of OnboardingScreen Composable into a ViewModel.
OnboardingScreen is now a thin UI shell."
```

---

### Task 6: Fix Compose reactivity bugs

**Why:** Three bugs: (1) `hasActiveFilters` is a plain `val get()` not observable by Compose, (2) `FavoritesScreen` calls `getFavoriteCocktails()` during composition (not reactive), (3) `DetailScreen:67` has unsafe `context as Activity` cast.

**Files:**
- Modify: `shaker/ui/screen/home/HomeViewModel.kt:50-53`
- Modify: `shaker/ui/screen/home/HomeScreen.kt:109`
- Modify: `shaker/ui/screen/favorites/FavoritesViewModel.kt:37-39`
- Modify: `shaker/ui/screen/favorites/FavoritesScreen.kt:50`
- Modify: `shaker/ui/screen/detail/DetailScreen.kt:67`

- [ ] **Step 1: Convert hasActiveFilters to StateFlow**

In `HomeViewModel.kt`, replace the property (lines 50-53):

```kotlin
// Remove:
// val hasActiveFilters: Boolean
//     get() = _selectedSpirits.value.isNotEmpty() ||
//             _selectedCategories.value.isNotEmpty() ||
//             _selectedDifficulty.value != null

// Add:
private val _hasActiveFilters = MutableStateFlow(false)
val hasActiveFilters: StateFlow<Boolean> = _hasActiveFilters.asStateFlow()

private fun updateHasActiveFilters() {
    _hasActiveFilters.value = _selectedSpirits.value.isNotEmpty() ||
            _selectedCategories.value.isNotEmpty() ||
            _selectedDifficulty.value != null
}
```

Call `updateHasActiveFilters()` at the end of `toggleSpirit()`, `toggleCategory()`, `selectDifficulty()`, and `clearFilters()`.

- [ ] **Step 2: Update HomeScreen to collect hasActiveFilters**

In `HomeScreen.kt`, add:

```kotlin
val hasActiveFilters by viewModel.hasActiveFilters.collectAsStateWithLifecycle()
```

Replace `viewModel.hasActiveFilters` (line 109) with `hasActiveFilters`.

- [ ] **Step 3: Make FavoritesViewModel expose reactive favorites list**

In `FavoritesViewModel.kt`, replace `getFavoriteCocktails()` with a reactive `StateFlow`:

```kotlin
// Remove:
// fun getFavoriteCocktails(): List<Cocktail> { ... }

// Add in init or as a derived flow:
private val _favorites = MutableStateFlow<List<Cocktail>>(emptyList())
val favorites: StateFlow<List<Cocktail>> = _favorites.asStateFlow()

init {
    viewModelScope.launch {
        favoriteIds.collect { ids ->
            _favorites.value = cocktailRepository.loadCocktails().filter { it.id in ids }
        }
    }
}
```

- [ ] **Step 4: Update FavoritesScreen to collect favorites**

In `FavoritesScreen.kt` line 50, replace:

```kotlin
// Remove:
// val favorites = viewModel.getFavoriteCocktails()

// Add:
val favorites by viewModel.favorites.collectAsStateWithLifecycle()
```

Remove the `favoriteIds` collection since it's no longer needed directly in the Screen (the ViewModel handles it).

- [ ] **Step 5: Fix unsafe Activity cast in DetailScreen**

In `DetailScreen.kt:67`, replace:

```kotlin
// Remove:
// val window = (context as Activity).window

// Add:
val activity = context as? Activity ?: return@DisposableEffect onDispose {}
val window = activity.window
```

And update the `onDispose` accordingly.

- [ ] **Step 6: Run tests**

Run: `./gradlew testDebugUnitTest`
Expected: ALL PASS. Update `HomeViewModelTest` if it references `hasActiveFilters` as a boolean property. Update `FavoritesViewModelTest` to test `favorites` StateFlow instead of `getFavoriteCocktails()`.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "fix(android): fix Compose reactivity bugs

- hasActiveFilters is now a StateFlow (observable by Compose)
- FavoritesScreen favorites list is now reactive via StateFlow
- DetailScreen: safe Activity cast prevents potential crash"
```

---

### Task 7: Extract hardcoded strings to strings.xml

**Why:** ~50 hardcoded UI strings in English across all screens. No `stringResource()` usage. Bad practice for i18n, accessibility, and as a sample app reference.

**Files:**
- Create: `app/src/main/res/values/strings.xml` (expand existing or create)
- Modify: all Screen composables

- [ ] **Step 1: Create/expand strings.xml**

```xml
<resources>
    <string name="app_name">Shaker</string>

    <!-- Home -->
    <string name="search_cocktails">Search cocktails…</string>
    <string name="search">Search</string>
    <string name="filters">Filters</string>
    <string name="no_cocktails_found">No cocktails found</string>
    <string name="try_different_search">Try a different search or filter.</string>

    <!-- Detail -->
    <string name="ingredients">Ingredients</string>
    <string name="instructions">Instructions</string>
    <string name="unlock_full_recipe">Unlock Full Recipe</string>
    <string name="back">Back</string>
    <string name="add_to_favorites">Add to favorites</string>
    <string name="remove_from_favorites">Remove from favorites</string>

    <!-- Favorites -->
    <string name="no_favorites_yet">No favorites yet</string>
    <string name="favorites_hint">Tap the heart icon on a cocktail to save it here.</string>
    <string name="unlock_favorites">Unlock Favorites</string>

    <!-- Settings -->
    <string name="account">Account</string>
    <string name="logged_in_as">Logged in as</string>
    <string name="logout">Logout</string>
    <string name="user_id">User ID</string>
    <string name="enter_user_id">Enter any user ID</string>
    <string name="login">Login</string>
    <string name="premium_status">Premium Status</string>
    <string name="active">Active</string>
    <string name="free">Free</string>
    <string name="anonymous_id">Anonymous ID</string>
    <string name="copied">Copied!</string>
    <string name="copy">Copy</string>
    <string name="purchases">Purchases</string>
    <string name="restore_purchases">Restore Purchases</string>
    <string name="show_onboarding">Show Onboarding</string>
    <string name="purchasely_sdk">Purchasely SDK</string>
    <string name="default_mode_hint">Default mode is Paywall Observer.</string>
    <string name="data_privacy">Data Privacy</string>
    <string name="analytics">Analytics</string>
    <string name="analytics_desc">Anonymous audience measurement</string>
    <string name="identified_analytics">Identified Analytics</string>
    <string name="identified_analytics_desc">User-identified analytics</string>
    <string name="personalization">Personalization</string>
    <string name="personalization_desc">Personalized content &amp; offers</string>
    <string name="campaigns">Campaigns</string>
    <string name="campaigns_desc">Promotional campaigns</string>
    <string name="third_party">Third-party Integrations</string>
    <string name="third_party_desc">External analytics &amp; integrations</string>
    <string name="technical_processing_note">Technical processing required for app operation cannot be disabled.</string>
    <string name="appearance">Appearance</string>
    <string name="light">Light</string>
    <string name="dark">Dark</string>
    <string name="system">System</string>
    <string name="screen_display_mode">Screen Display Mode</string>
    <string name="display_mode_desc">How paywalls are presented on screen</string>
    <string name="full">Full</string>
    <string name="modal">Modal</string>
    <string name="drawer">Drawer</string>
    <string name="popin">Popin</string>
    <string name="about">About</string>
    <string name="version">Version</string>
    <string name="powered_by_purchasely">Powered by Purchasely</string>
    <string name="purchases_restored">Purchases restored successfully!</string>
    <string name="no_purchases_to_restore">No purchases to restore</string>

    <!-- Onboarding -->
    <string name="discover_cocktails">Discover cocktails</string>

    <!-- Navigation -->
    <string name="home">Home</string>
    <string name="favorites">Favorites</string>
    <string name="settings">Settings</string>

    <!-- Filters -->
    <string name="spirits">Spirits</string>
    <string name="categories">Categories</string>
    <string name="difficulty">Difficulty</string>
    <string name="clear_filters">Clear Filters</string>
</resources>
```

- [ ] **Step 2: Replace hardcoded strings in HomeScreen**

Replace `"Search cocktails..."` → `stringResource(R.string.search_cocktails)`, etc.
Add `import androidx.compose.ui.res.stringResource` and `import com.purchasely.shaker.R`.

- [ ] **Step 3: Replace hardcoded strings in DetailScreen, FavoritesScreen, SettingsScreen, OnboardingScreen, FilterSheet, Navigation**

Same pattern for each screen. Also update Navigation.kt bottom nav labels.

- [ ] **Step 4: Run build to verify no compilation errors**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor(android): extract all hardcoded strings to strings.xml

Replaces ~50 inline string literals with stringResource() calls.
Enables future i18n and improves accessibility."
```

---

## P2 — Structural Refactors

---

### Task 8: Create Repository interfaces in domain

**Why:** All repositories are concrete classes in `data/`. Domain has no interfaces. Dependency Inversion Principle is violated — ViewModels depend on concrete implementations, not abstractions.

**Files:**
- Create: `shaker/domain/repository/CocktailRepository.kt` (interface)
- Create: `shaker/domain/repository/FavoritesRepository.kt` (interface)
- Create: `shaker/domain/repository/OnboardingRepository.kt` (interface)
- Create: `shaker/domain/repository/SettingsRepository.kt` (interface)
- Create: `shaker/domain/repository/PremiumRepository.kt` (interface)
- Rename: `shaker/data/CocktailRepository.kt` → `CocktailRepositoryImpl.kt`
- Rename: `shaker/data/FavoritesRepository.kt` → `FavoritesRepositoryImpl.kt`
- Rename: `shaker/data/OnboardingRepository.kt` → `OnboardingRepositoryImpl.kt`
- Rename: `shaker/data/SettingsRepository.kt` → `SettingsRepositoryImpl.kt`
- Rename: `shaker/data/PremiumManager.kt` → `PremiumManagerImpl.kt`
- Modify: `shaker/di/AppModule.kt` (bind interface → impl)
- Modify: All ViewModels (import interface, not impl)

- [ ] **Step 1: Create domain interfaces**

```kotlin
// shaker/domain/repository/CocktailRepository.kt
package com.purchasely.shaker.domain.repository

import com.purchasely.shaker.domain.model.Cocktail

interface CocktailRepository {
    fun loadCocktails(): List<Cocktail>
    fun getCocktail(id: String): Cocktail?
    fun getSpirits(): List<String>
    fun getCategories(): List<String>
    fun getDifficulties(): List<String>
}
```

```kotlin
// shaker/domain/repository/FavoritesRepository.kt
package com.purchasely.shaker.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface FavoritesRepository {
    val favoriteIds: StateFlow<Set<String>>
    fun isFavorite(cocktailId: String): Boolean
    fun toggleFavorite(cocktailId: String)
    fun addFavorite(cocktailId: String)
    fun removeFavorite(cocktailId: String)
}
```

```kotlin
// shaker/domain/repository/OnboardingRepository.kt
package com.purchasely.shaker.domain.repository

interface OnboardingRepository {
    var isOnboardingCompleted: Boolean
}
```

```kotlin
// shaker/domain/repository/PremiumRepository.kt
package com.purchasely.shaker.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface PremiumRepository {
    val isPremium: StateFlow<Boolean>
    fun refreshPremiumStatus()
}
```

- [ ] **Step 2: Rename data implementations**

Rename classes (not files yet — update class names first):
- `CocktailRepository` → `CocktailRepositoryImpl : CocktailRepository`
- `FavoritesRepository` → `FavoritesRepositoryImpl : FavoritesRepository`
- `OnboardingRepository` → `OnboardingRepositoryImpl : OnboardingRepository`
- `PremiumManager` → `PremiumManagerImpl : PremiumRepository`

Each implementation adds `: InterfaceName` to its declaration.

- [ ] **Step 3: Rename files to match new class names**

Rename source files to `*Impl.kt`.

- [ ] **Step 4: Update AppModule.kt bindings**

```kotlin
single<CocktailRepository> { CocktailRepositoryImpl(androidContext()) }
single<FavoritesRepository> { FavoritesRepositoryImpl(get(named("favorites"))) }
single<OnboardingRepository> { OnboardingRepositoryImpl(get(named("onboarding"))) }
single<PremiumRepository> {
    PremiumManagerImpl(wrapper = get()).also { pm ->
        get<PurchaselyWrapper>().onTransactionCompleted = { pm.refreshPremiumStatus() }
    }
}
```

- [ ] **Step 5: Update ViewModel imports**

All ViewModels: replace `import com.purchasely.shaker.data.XxxRepository` with `import com.purchasely.shaker.domain.repository.XxxRepository`.

Replace `PremiumManager` → `PremiumRepository` in all ViewModel constructors.

- [ ] **Step 6: Update test imports**

All test files: update to reference `*Impl` for concrete classes, or mock the interfaces directly.

- [ ] **Step 7: Run tests**

Run: `./gradlew testDebugUnitTest`
Expected: ALL PASS

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "refactor(android): add Repository interfaces in domain layer

All repositories now have interfaces in domain/ and implementations in data/.
ViewModels depend on abstractions, not concrete classes."
```

---

### Task 9: Type-safe settings enums

**Why:** `themeMode`, `displayMode` are stringly-typed (`"light"`, `"dark"`, `"fullscreen"`, `"modal"`). `RunningModeRepository` uses its own string mapping that duplicates `PurchaselySdkMode`. Risk of typos and no compile-time safety.

**Files:**
- Create: `shaker/domain/model/ThemeMode.kt`
- Create: `shaker/domain/model/DisplayMode.kt`
- Modify: `shaker/data/SettingsRepository.kt` (use enums)
- Modify: `shaker/ui/screen/settings/SettingsViewModel.kt` (StateFlow<ThemeMode>, StateFlow<DisplayMode>)
- Modify: `shaker/ui/screen/settings/SettingsScreen.kt`
- Modify: `shaker/data/RunningModeRepository.kt` (use PurchaselySdkMode consistently)

- [ ] **Step 1: Create ThemeMode enum**

```kotlin
// shaker/domain/model/ThemeMode.kt
package com.purchasely.shaker.domain.model

enum class ThemeMode(val storageValue: String, val label: String) {
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
    SYSTEM("system", "System");

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}
```

- [ ] **Step 2: Create DisplayMode enum**

```kotlin
// shaker/domain/model/DisplayMode.kt
package com.purchasely.shaker.domain.model

enum class DisplayMode(val storageValue: String, val label: String) {
    FULLSCREEN("fullscreen", "Full"),
    MODAL("modal", "Modal"),
    DRAWER("drawer", "Drawer"),
    POPIN("popin", "Popin");

    companion object {
        fun fromStorage(value: String?): DisplayMode =
            entries.firstOrNull { it.storageValue == value } ?: FULLSCREEN
    }
}
```

- [ ] **Step 3: Update SettingsRepository**

```kotlin
var themeMode: ThemeMode
    get() = ThemeMode.fromStorage(store.getString(KEY_THEME))
    set(value) = store.putString(KEY_THEME, value.storageValue)

var displayMode: DisplayMode
    get() = DisplayMode.fromStorage(store.getString(KEY_DISPLAY_MODE))
    set(value) = store.putString(KEY_DISPLAY_MODE, value.storageValue)
```

- [ ] **Step 4: Update SettingsViewModel StateFlows**

```kotlin
private val _themeMode = MutableStateFlow(settingsRepo.themeMode)
val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

private val _displayMode = MutableStateFlow(settingsRepo.displayMode)
val displayMode: StateFlow<DisplayMode> = _displayMode.asStateFlow()

fun setThemeMode(mode: ThemeMode) {
    _themeMode.value = mode
    settingsRepo.themeMode = mode
    purchaselyWrapper.setUserAttribute("app_theme", mode.storageValue)
}

fun setDisplayMode(mode: DisplayMode) {
    _displayMode.value = mode
    settingsRepo.displayMode = mode
}
```

- [ ] **Step 5: Update SettingsScreen to use enums**

Replace the string-based segmented button rows with enum-based ones:

```kotlin
val themes = ThemeMode.entries
SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
    themes.forEachIndexed { index, mode ->
        SegmentedButton(
            selected = themeMode == mode,
            onClick = { viewModel.setThemeMode(mode) },
            shape = SegmentedButtonDefaults.itemShape(index = index, count = themes.size)
        ) {
            Text(stringResource(mode.labelRes))
        }
    }
}
```

(Or keep `mode.label` if not yet using string resources for enum labels.)

- [ ] **Step 6: Unify RunningModeRepository on PurchaselySdkMode**

In `RunningModeRepository`, replace the manual string mapping:

```kotlin
var runningMode: PLYRunningMode
    get() = PurchaselySdkMode.fromStorage(store.getString(KEY_RUNNING_MODE, PurchaselySdkMode.DEFAULT.storageValue)).runningMode
    set(value) {
        val mode = PurchaselySdkMode.entries.firstOrNull { it.runningMode == value } ?: PurchaselySdkMode.DEFAULT
        store.putString(KEY_RUNNING_MODE, mode.storageValue)
    }
```

- [ ] **Step 7: Run tests, update assertions**

Run: `./gradlew testDebugUnitTest`
Update SettingsViewModelTest assertions for enum types.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "refactor(android): type-safe settings with ThemeMode and DisplayMode enums

Replaces stringly-typed settings with compile-time safe enums.
Unifies RunningModeRepository on PurchaselySdkMode."
```

---

### Task 10: Remove Activity parameter from ViewModel methods

**Why:** All 4 ViewModels have `displayPendingPaywall(activity: Activity)` methods — ViewModels should not reference Android framework classes. The Activity resolution should happen at the Screen/Composable level.

**Files:**
- Modify: `shaker/ui/screen/home/HomeViewModel.kt`
- Modify: `shaker/ui/screen/home/HomeScreen.kt`
- Modify: `shaker/ui/screen/detail/DetailViewModel.kt`
- Modify: `shaker/ui/screen/detail/DetailScreen.kt`
- Modify: `shaker/ui/screen/favorites/FavoritesViewModel.kt`
- Modify: `shaker/ui/screen/favorites/FavoritesScreen.kt`
- Modify: `shaker/ui/screen/settings/SettingsViewModel.kt`
- Modify: `shaker/ui/screen/settings/SettingsScreen.kt`

- [ ] **Step 1: Refactor the ViewModel→Screen paywall display pattern**

The current pattern is:
1. ViewModel fetches presentation, stores pending `PresentationHandle`
2. ViewModel emits `SharedFlow<Unit>` to signal Screen
3. Screen collects signal, gets Activity, calls `viewModel.displayPendingPaywall(activity)`

New pattern — the ViewModel exposes the `PresentationHandle` directly and the Screen calls the wrapper:

Actually, a cleaner approach is to have the ViewModel expose a `SharedFlow<PresentationHandle>` instead of `SharedFlow<Unit>`, and the Screen handles display:

In **HomeViewModel.kt**:

```kotlin
// Replace:
// private var pendingFiltersPresentation: PresentationHandle? = null
// private val _requestPaywallDisplay = MutableSharedFlow<Unit>()
// val requestPaywallDisplay: SharedFlow<Unit>

// With:
private val _requestPaywallDisplay = MutableSharedFlow<PresentationHandle>()
val requestPaywallDisplay: SharedFlow<PresentationHandle> = _requestPaywallDisplay.asSharedFlow()

// In onFilterClick():
fun onFilterClick() {
    if (isPremium.value) return
    val result = _filtersPresentation.value
    if (result is FetchResult.Success) {
        viewModelScope.launch { _requestPaywallDisplay.emit(result.handle) }
    }
}

// Remove displayPendingPaywall(activity) entirely
```

In **HomeScreen.kt**:

```kotlin
LaunchedEffect(Unit) {
    viewModel.requestPaywallDisplay.collect { handle ->
        val activity = context as? Activity ?: return@collect
        val result = purchaselyWrapper.display(handle, activity)
        when (result) {
            is DisplayResult.Purchased, is DisplayResult.Restored -> viewModel.onPaywallDismissed()
            else -> {}
        }
    }
}
```

The Screen needs `PurchaselyWrapper` injected via `koinInject()` — this is acceptable because display is inherently a UI-layer concern (needs Activity).

- [ ] **Step 2: Apply same pattern to DetailViewModel/Screen**

Two SharedFlows: `requestRecipePaywall: SharedFlow<PresentationHandle>` and `requestFavoritesPaywall: SharedFlow<PresentationHandle>`.

Remove `displayPendingRecipePaywall(activity)` and `displayPendingFavoritesPaywall(activity)`.

- [ ] **Step 3: Apply same pattern to FavoritesViewModel/Screen**

Same refactor.

- [ ] **Step 4: Apply same pattern to SettingsViewModel/Screen**

Same refactor.

- [ ] **Step 5: Remove `import android.app.Activity` from all ViewModels**

Verify no ViewModel imports `android.app.Activity` anymore.

- [ ] **Step 6: Run tests**

Run: `./gradlew testDebugUnitTest`
Update ViewModel tests: remove tests that called `displayPendingPaywall(activity)`, verify SharedFlow emits `PresentationHandle`.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor(android): remove Activity from ViewModel signatures

Paywall display (Activity-dependent) is now handled in Screen composables.
ViewModels emit PresentationHandle via SharedFlow, Screens handle display."
```

---

### Task 11: Introduce UseCases for key operations

**Why:** Business logic lives directly in ViewModels (filtering, premium checking, presentation loading). UseCases make this logic testable independently and reusable across ViewModels.

**Files:**
- Create: `shaker/domain/usecase/GetFilteredCocktailsUseCase.kt`
- Create: `shaker/domain/usecase/ToggleFavoriteUseCase.kt`
- Create: `shaker/domain/usecase/LoadPresentationUseCase.kt`
- Create: tests for each UseCase
- Modify: `shaker/ui/screen/home/HomeViewModel.kt`
- Modify: `shaker/ui/screen/detail/DetailViewModel.kt`
- Modify: `shaker/ui/screen/favorites/FavoritesViewModel.kt`
- Modify: `shaker/di/AppModule.kt`

- [ ] **Step 1: Create GetFilteredCocktailsUseCase**

```kotlin
// shaker/domain/usecase/GetFilteredCocktailsUseCase.kt
package com.purchasely.shaker.domain.usecase

import com.purchasely.shaker.domain.model.Cocktail
import com.purchasely.shaker.domain.repository.CocktailRepository

class GetFilteredCocktailsUseCase(
    private val repository: CocktailRepository
) {
    operator fun invoke(
        query: String = "",
        spirits: Set<String> = emptySet(),
        categories: Set<String> = emptySet(),
        difficulty: String? = null
    ): List<Cocktail> {
        return repository.loadCocktails().filter { cocktail ->
            val matchesQuery = query.isBlank() || cocktail.name.contains(query, ignoreCase = true)
            val matchesSpirit = spirits.isEmpty() || spirits.contains(cocktail.spirit)
            val matchesCategory = categories.isEmpty() || categories.contains(cocktail.category)
            val matchesDifficulty = difficulty == null || cocktail.difficulty == difficulty
            matchesQuery && matchesSpirit && matchesCategory && matchesDifficulty
        }
    }
}
```

- [ ] **Step 2: Write test for GetFilteredCocktailsUseCase**

```kotlin
// test/domain/usecase/GetFilteredCocktailsUseCaseTest.kt
package com.purchasely.shaker.domain.usecase

import com.purchasely.shaker.domain.repository.CocktailRepository
import com.purchasely.shaker.testCocktail
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class GetFilteredCocktailsUseCaseTest {

    private val repo = mockk<CocktailRepository>()
    private val useCase = GetFilteredCocktailsUseCase(repo)

    private val cocktails = listOf(
        testCocktail(id = "1", name = "Mojito", spirit = "rum", category = "classic", difficulty = "easy"),
        testCocktail(id = "2", name = "Margarita", spirit = "tequila", category = "classic", difficulty = "medium"),
        testCocktail(id = "3", name = "Old Fashioned", spirit = "whiskey", category = "classic", difficulty = "easy")
    )

    init {
        every { repo.loadCocktails() } returns cocktails
    }

    @Test
    fun `no filters returns all cocktails`() {
        assertEquals(3, useCase().size)
    }

    @Test
    fun `filter by spirit`() {
        val result = useCase(spirits = setOf("rum"))
        assertEquals(1, result.size)
        assertEquals("Mojito", result[0].name)
    }

    @Test
    fun `filter by query`() {
        val result = useCase(query = "mar")
        assertEquals(1, result.size)
        assertEquals("Margarita", result[0].name)
    }

    @Test
    fun `combined filters`() {
        val result = useCase(spirits = setOf("whiskey"), difficulty = "easy")
        assertEquals(1, result.size)
        assertEquals("Old Fashioned", result[0].name)
    }
}
```

- [ ] **Step 3: Create ToggleFavoriteUseCase**

```kotlin
// shaker/domain/usecase/ToggleFavoriteUseCase.kt
package com.purchasely.shaker.domain.usecase

import com.purchasely.shaker.domain.repository.FavoritesRepository

class ToggleFavoriteUseCase(
    private val favoritesRepository: FavoritesRepository
) {
    operator fun invoke(cocktailId: String) {
        favoritesRepository.toggleFavorite(cocktailId)
    }
}
```

- [ ] **Step 4: Update HomeViewModel to use GetFilteredCocktailsUseCase**

Replace the `applyFilters()` method:

```kotlin
class HomeViewModel(
    private val repository: CocktailRepository,
    private val premiumRepository: PremiumRepository,
    private val purchaselyWrapper: PurchaselyWrapper,
    private val getFilteredCocktails: GetFilteredCocktailsUseCase
) : ViewModel() {
    // ...
    private fun applyFilters() {
        _cocktails.value = getFilteredCocktails(
            query = _searchQuery.value,
            spirits = _selectedSpirits.value,
            categories = _selectedCategories.value,
            difficulty = _selectedDifficulty.value
        )
    }
}
```

- [ ] **Step 5: Register UseCases in AppModule**

```kotlin
factory { GetFilteredCocktailsUseCase(get()) }
factory { ToggleFavoriteUseCase(get()) }
```

Update ViewModel registrations to include new dependencies.

- [ ] **Step 6: Run tests**

Run: `./gradlew testDebugUnitTest`
Expected: ALL PASS

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor(android): introduce UseCases for key business operations

GetFilteredCocktailsUseCase and ToggleFavoriteUseCase extract business
logic from ViewModels into independently testable units."
```

---

## P3 — Long-term Polish

---

### Task 12: Type-safe Navigation with @Serializable destinations

**Why:** Current navigation uses string-based routes (`"detail/{cocktailId}"`). Navigation Compose 2.8+ supports `@Serializable` data classes as destinations, providing compile-time safety.

**Files:**
- Modify: `shaker/ui/navigation/Navigation.kt`
- Modify: `gradle/libs.versions.toml` (verify navigation-compose >= 2.8)

- [ ] **Step 1: Verify navigation-compose version supports type-safe API**

`libs.versions.toml:6` shows `navigation-compose = "2.8.5"` — supports `@Serializable` routes.

- [ ] **Step 2: Replace sealed Screen class with @Serializable objects**

```kotlin
import kotlinx.serialization.Serializable

@Serializable data object Home
@Serializable data object Favorites
@Serializable data object Settings
@Serializable data class Detail(val cocktailId: String)
```

- [ ] **Step 3: Update NavHost to use typed destinations**

```kotlin
NavHost(
    navController = navController,
    startDestination = Home
) {
    composable<Home> {
        HomeScreen(onCocktailClick = { id -> navController.navigate(Detail(id)) })
    }
    composable<Favorites> {
        FavoritesScreen(onCocktailClick = { id -> navController.navigate(Detail(id)) })
    }
    composable<Settings> {
        SettingsScreen()
    }
    composable<Detail> { backStackEntry ->
        val detail: Detail = backStackEntry.toRoute()
        DetailScreen(cocktailId = detail.cocktailId, onBack = { navController.popBackStack() })
    }
}
```

- [ ] **Step 4: Update bottom nav items**

```kotlin
data class BottomNavItem(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Home, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Favorites, "Favorites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    BottomNavItem(Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)
```

- [ ] **Step 5: Run build and test navigation**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor(android): migrate to type-safe Navigation Compose routes

Replaces string-based routes with @Serializable data classes/objects.
Compile-time safe navigation arguments."
```

---

### Task 13: Add Compose UI tests

**Why:** Zero `androidTest` files. For a reference app, basic UI smoke tests would validate the golden paths and serve as examples for clients.

**Files:**
- Create: `app/src/androidTest/java/com/purchasely/shaker/ui/HomeScreenTest.kt`
- Create: `app/src/androidTest/java/com/purchasely/shaker/ui/FavoritesScreenTest.kt`
- Create: `app/src/androidTest/java/com/purchasely/shaker/ui/NavigationTest.kt`
- Modify: `app/build.gradle.kts` (add `androidTestImplementation` for Compose testing)

- [ ] **Step 1: Add Compose test dependencies**

In `libs.versions.toml`, add:
```toml
compose-ui-test = { group = "androidx.compose.ui", name = "ui-test-junit4" }
compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
```

In `build.gradle.kts`:
```kotlin
androidTestImplementation(platform(libs.compose.bom))
androidTestImplementation(libs.compose.ui.test)
debugImplementation(libs.compose.ui.test.manifest)
```

- [ ] **Step 2: Create HomeScreenTest**

```kotlin
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_displaysSearchBar() {
        composeTestRule.setContent {
            ShakerTheme {
                // Setup with test ViewModel/DI
            }
        }
        composeTestRule.onNodeWithText("Search cocktails").assertIsDisplayed()
    }
}
```

- [ ] **Step 3: Create basic navigation test**

Verify that tapping bottom nav items switches screens.

- [ ] **Step 4: Run instrumented tests**

Run: `./gradlew connectedDebugAndroidTest` (requires emulator)

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "test(android): add Compose UI smoke tests

Basic UI tests for HomeScreen search bar and bottom navigation.
Validates golden paths for the reference app."
```

---

## Summary

| Priority | Tasks | Estimated effort |
|----------|-------|------------------|
| **P1** | Tasks 1-7 (PresentationHandle, PremiumManager wrapper, KeyValueStore, SettingsRepository, OnboardingViewModel, Compose fixes, strings.xml) | 2-3 days |
| **P2** | Tasks 8-11 (Repository interfaces, type-safe settings, Activity removal, UseCases) | 3-5 days |
| **P3** | Tasks 12-13 (type-safe nav, UI tests) | 1-2 days |

**Dependency order:** Tasks within each priority tier are independent and can be parallelized. P1 should be completed before P2 (P2 builds on P1 refactors). P3 is independent.
