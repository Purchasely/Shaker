# Purchasely SDK — Best Practices

**Shaker is Purchasely's official demo / reference app.** It is the canonical example of how to integrate the Purchasely SDK on iOS and Android, so the patterns documented here are not just "what works for one team" — they are the recommendations Purchasely shows to its customers.

This document defines the integration standards as currently implemented in Shaker. **Every change to the Purchasely integration must follow these rules and update this document in the same PR** so the reference app stays a trustworthy source. When the SDK evolves (new APIs, deprecations, new patterns), update Shaker first, then propagate the change here so the doc reflects the current shipping app.

External readers integrating Purchasely in their own app can pick what fits — naming, app prefix, log tags, and infra choices (`KeyValueStore`, repository names, etc.) are Shaker-specific. The architectural rules (wrapper boundary, Observer mode flow, `closeAllScreens()`/`processAction(false)` ordering, `success_payment` chain, audience cache invalidation, …) are platform-wide best practices.

---

## 1. Architecture: PurchaselyWrapper

**Rule: Never call the Purchasely SDK directly. All calls go through `PurchaselyWrapper`.**

**Why:**
- Single point of control for all SDK interactions — easy to swap with a stub for removal
- Screens have zero `io.purchasely` / `import Purchasely` imports — clean separation
- Easier to test (mock the wrapper, not the SDK)
- Consistent error handling and result mapping
- Type-safe result types (`FetchResult`, `DisplayResult`) instead of raw SDK enums

**Wrapper responsibilities:**

| Category | Methods |
|----------|---------|
| **Init & Lifecycle** | `initialize()`, `restart()`, `close()`, `closeAllScreens()` |
| **Interceptor** | Internal paywall actions interceptor (LOGIN, NAVIGATE, PURCHASE, RESTORE) |
| **Events** | Internal event listener/delegate |
| **Presentations** | `loadPresentation()`, `display()`, `getView()` (Android) / `getController()` (iOS) |
| **User Attributes** | `setUserAttribute()`, `incrementUserAttribute()` |
| **User Management** | `userLogin()`, `userLogout()`, `anonymousUserId` |
| **Purchases** | `restoreAllProducts()`, `synchronize()`, `signPromotionalOffer()` (iOS) |
| **Consent** | `revokeDataProcessingConsent()` |
| **Info** | `sdkVersion`, `isDeeplinkHandled()` |

**Android (v6) — zero SDK imports outside the `purchasely/` package.** Since the v6 migration the rule is strict and mechanically checkable: only `PurchaselyWrapper` and its sibling files in `purchasely/` may `import io.purchasely`. Everything the rest of the app needs crosses the boundary through app-owned types:

| SDK concept | App-owned type | Where |
|-------------|----------------|-------|
| `PLYPresentation` | `PresentationHandle` (opaque value class) | `purchasely/PresentationHandle.kt` |
| `PLYPresentationType` + fetch errors | `FetchResult` sealed class | `purchasely/FetchResult.kt` |
| `PLYPresentationOutcome` | `DisplayResult` sealed class | `purchasely/DisplayResult.kt` |
| `PLYSubscriptionData` | `SubscriptionInfo` | `purchasely/SubscriptionInfo.kt` |
| `PLYRunningMode` | `PurchaselySdkMode` enum (mapping done in the wrapper) | `data/PurchaselySdkMode.kt` |
| `LogLevel` | `verboseLogging: Boolean` parameter on `initialize()` | — |
| `PLYDataProcessingPurpose` | `ConsentPurpose` enum (mapping in the wrapper) | `domain/model/ConsentPurpose.kt` |

Check it with: `rg -l 'import io\.purchasely' app/src/main` — every hit must be under `purchasely/`.

**iOS (v6) — same strict boundary.** Only the `Purchasely/` group may `import Purchasely`
(`PurchaselyWrapper`, `PresentationHandle`, `EmbeddedScreenBanner`). The same SDK→app mapping
applies:

| SDK concept | App-owned type | Where |
|-------------|----------------|-------|
| `any PLYPresentation` | `PresentationHandle` (opaque struct) | `Purchasely/PresentationHandle.swift` |
| `PLYPresentationType` + fetch errors | `FetchResult` enum | `Purchasely/FetchResult.swift` |
| `PLYPresentationOutcome` | `DisplayResult` enum | `Purchasely/DisplayResult.swift` |
| `PLYSubscription` | `SubscriptionInfo` | `Purchasely/SubscriptionInfo.swift` |
| `PLYRunningMode` | `PurchaselySDKMode` enum (mapping in the wrapper) | `Data/PurchaselySDKMode.swift` |
| `PLYLogger.PLYLogLevel` | `verboseLogging: Bool` parameter on `initialize()` | — |
| `PLYDataProcessingPurpose` | `ConsentPurpose` enum (mapping in the wrapper) | `Model/ConsentPurpose.swift` |

Check it with: `grep -rl 'import Purchasely' Shaker --include='*.swift'` — every hit must be under `Purchasely/`.

---

## 2. Observer Mode: Purchase Flow

**Rule: In Observer mode, purchases and restores are decoupled from the SDK. `PurchaseManager` has zero Purchasely imports.**

### Architecture

```
PurchaselyWrapper                          PurchaseManager
    │                                           │
    │ PURCHASE (observer)                        │
    │   Android: emit PurchaseRequest ───────►   │
    │   iOS: await purchase(productId:) ─────►   │
    │                                           │ Native billing
    │                                           │ (Play Billing / StoreKit 2)
    │                                           │
    │   Android: collect TransactionResult ◄──   │
    │   iOS: return TransactionResult ───────◄   │
    │                                           │
    │ set pendingSuccessfulPurchase = true       │
    │ synchronize()                              │
    │   ├─ Android: fire-and-forget (no cb)      │
    │   └─ iOS: await synchronizeReceipt()       │
    │ processAction(false)                       │
    │ closeAllScreens()                          │
    │                                            │
    │ (premium refresh is deferred until the     │
    │  chained success_payment screen closes)    │
```

**SDK rules (both platforms):**
- `processAction(false)` MUST be called BEFORE `closeAllScreens()` — the interceptor needs to know not to proceed before the paywall tears down.
- `Purchasely.closeAllScreens()` is safe to call at any time, no need to wait for anything.
- `Purchasely.synchronize()` runs in the background — by default you do **not** need to await it before dismissing the paywall.

**Why Shaker awaits `synchronize()` anyway:** Shaker chains a `success_payment` placement after a successful purchase to show a thank-you screen. That placement targets users based on their (now-active) subscription state, so we wait for `synchronize()` to finish before we tear the paywall down — otherwise the `success_payment` fetch can resolve against stale subscription state and show the wrong screen (or get deactivated).

**iOS implementation:** the wrapper exposes a private `synchronizeReceipt() async throws` that wraps `Purchasely.synchronize(success:, failure:)` via `withCheckedThrowingContinuation`. The post-purchase flow is linear:

```swift
do {
    try await synchronizeReceipt()      // wait — needed for success_payment targeting
    PresentationCache.shared.invalidateAll()
    proceed(false)                       // tell interceptor we handled it
    Purchasely.closeAllScreens()         // dismiss
} catch {
    proceed(false)                       // dismiss anyway on sync error
    Purchasely.closeAllScreens()
}
```

**Android:** `Purchasely.synchronize()` is parameterless (fire-and-forget) — there is no callback to await. The wrapper calls `synchronize()`, then `processAction(false)`, then `closeAllScreens()` in-line. The risk of stale state for `success_payment` exists but is mitigated by Android's faster cache refresh.

**Always use `Purchasely.closeAllScreens()`** to force-dismiss the paywall after a successful Observer-mode purchase. Do not use `Purchasely.closeDisplayedPresentation()` for this flow — `closeAllScreens()` is the correct API to chain the `success_payment` placement reliably.

**SDK version requirements for `closeAllScreens()`:**
- **iOS:** Purchasely SDK **5.7.5+** required. The method is `@MainActor`-isolated, so any call from a non-isolated synchronous context (e.g. inside `DispatchQueue.main.async`, `synchronize(success:)` callbacks) must be wrapped:
  ```swift
  Task { @MainActor in Purchasely.closeAllScreens() }
  ```
  Calling `Purchasely.closeAllScreens()` directly from a non-isolated context produces the compile error: *"Call to main actor-isolated class method 'closeAllScreens()' in a synchronous nonisolated context."*
- **Android:** Purchasely SDK **5.7.4+** required. No actor/threading constraint — call directly.

**Android (Kotlin):**
- `SharedFlow<PurchaseRequest>` — wrapper emits, PurchaseManager collects
- `SharedFlow<RestoreRequest>` — wrapper emits, PurchaseManager collects
- `SharedFlow<TransactionResult>` — PurchaseManager emits, wrapper collects
- PurchaseManager takes a `billingClientFactory` lambda (testable, no hardcoded BillingClient)

**iOS (Swift) — pure Swift Concurrency, no Combine:**
- `PurchaseManager` exposes async methods directly — no subjects, no sinks, no `cancellables`:
  - `func purchase(productId: String) async -> TransactionResult`
  - `func restore() async -> TransactionResult`
  - `func purchaseWithPromoOffer(productId:, storeOfferId:) async -> TransactionResult`
- `PurchaselyWrapper` interceptor `.purchase` / `.restore` cases dispatch in one shot:
  ```swift
  Task { @MainActor [weak self] in
      let result = await PurchaseManager.shared.purchase(productId: productId)
      await self?.handleTransactionResult(result, proceed: processAction)
  }
  ```
- `proceed` is captured directly in the running task — no `pendingProcessAction` field.
- The wrapper keeps a single `observerActionTask` guard. If another purchase/restore interceptor action arrives while one is already running, the new action calls `proceed(false)` and is ignored. This prevents overlapping StoreKit flows and double-calling independent interceptor closures.
- `PurchaseManager` uses injected closures for `anonymousUserId` and `signPromotionalOffer` (no wrapper reference). Same as before, but without Combine plumbing around them.

**Types:**

```kotlin
// Android
data class PurchaseRequest(val activity: Activity, val productId: String, val offerToken: String)
data object RestoreRequest
sealed class TransactionResult { Success, Cancelled, Error(message), Idle }
```

```swift
// iOS
enum TransactionResult { case success, cancelled, error(String?), idle }
```

**processAction callback:**
- **Android:** the wrapper stores a single `pendingProcessAction: ((Boolean) -> Unit)?` when emitting a purchase/restore request. When `TransactionResult` arrives, it invokes the callback and nullifies it. **Race guard:** before storing a new `pendingProcessAction`, the wrapper cancels any existing one by calling `pendingProcessAction?.invoke(false)` — prevents orphaning the first callback.
- **iOS:** `proceed` is captured directly inside the task created for that interceptor invocation. The wrapper does not store callbacks, but it does store `observerActionTask` as a concurrency guard: one Observer purchase/restore at a time.

**Interceptor rules:**

| Action | Observer mode (Android) | Observer mode (iOS) | Full mode |
|--------|------------------------|---------------------|-----------|
| PURCHASE | Store processAction, emit `PurchaseRequest` | Guard `observerActionTask`, then `await PurchaseManager.shared.purchase(productId:)` | proceed(true) |
| RESTORE | Store processAction, emit `RestoreRequest` | Guard `observerActionTask`, then `await PurchaseManager.shared.restore()` | proceed(true) |
| LOGIN | proceed(false) | proceed(false) | proceed(false) |
| NAVIGATE | Open URL, proceed(false) | `Task { @MainActor in UIApplication.shared.open(url) }`, proceed(false) | proceed(false) |
| Other | proceed(true) | proceed(true) | proceed(true) |

**TransactionResult handling:**

| Result | Wrapper actions |
|--------|----------------|
| Success | Set `pendingSuccessfulPurchase = true`. **Both platforms:** the order is `processAction(false)` → `closeAllScreens()`. The difference is whether `synchronize()` is awaited first: **Android** fire-and-forget (no callback to await); **iOS** awaits `synchronizeReceipt()` so subscription state is fresh before `loadPresentation`'s post-dismiss logic fetches the `success_payment` placement. The flag is then consumed by `loadPresentation`'s `completion` closure, which chains the `success_payment` placement (`showSuccessPaymentScreen()`) and only then refreshes subscriptions. |
| Cancelled | processAction(false) |
| Error | processAction(false) |
| Idle | ignore |

---

## 3. SDK Initialization

**Rule: `PurchaselyWrapper.initialize()` is the single entry point for SDK setup. The app entry point (ShakerApp/AppViewModel) only calls `wrapper.initialize()` and handles the ready callback.**

**Android:** `ShakerApp.onCreate()` calls `wrapper.initialize(application, apiKey, logLevel)`
**iOS:** `AppViewModel.init()` calls `wrapper.initialize(apiKey:, appUserId:, logLevel:, onReady:)`

The wrapper internally configures:
1. SDK start with API key, running mode, StoreKit settings
2. Event listener/delegate
3. Paywall actions interceptor
4. Deeplink readiness
5. **Android only:** Flow subscriptions for the Observer purchase flow. **iOS:** the interceptor dispatches `Task { @MainActor in await PurchaseManager.shared.purchase(...) }` directly — no Combine subjects, no init-time subscriptions.

**Restart:** When the SDK mode changes, `wrapper.restart()` is called:
- **Android:** `SettingsViewModel` calls `purchaselyWrapper.restart()` directly. `restart()` → `close()` → `initialize()`. `close()` cancels the transaction result collection job, clears any pending process action, then stops the SDK. `initialize()` restarts the collection.
- **iOS:** `SettingsViewModel` posts `.purchaselySdkModeDidChange` notification, wrapper observes it and calls `restart()` internally on the main actor

---

## 4. Presentation Loading: Always build → preload → display

**Rule (v6): Always build the presentation request (`PLYPresentation { … }` on Android,
`PLYPresentationBuilder` on iOS), `preload()` it, then `display()`/`buildView()` the loaded
presentation. Never use the deprecated `fetchPresentation()`/`presentationView()` shortcuts.**

**Why:**
- build + preload + display gives full control over the presentation lifecycle
- You can inspect `presentation.type` before deciding what to do (NORMAL, CLIENT, DEACTIVATED)
- You can handle errors from the fetch step separately from display errors
- The shortcuts hide these steps — unsuitable for reference code

**Pattern (Android, SDK v6):**
```kotlin
// In PurchaselyWrapper — fetch: DSL builder + suspend preload()
suspend fun loadPresentation(placementId: String): FetchResult {
    return try {
        val prepared = PLYPresentation {
            placementId(placementId)
            onDismissed { outcome -> /* log; stays active for callback-less displays */ }
        }
        val presentation = prepared.preload() // suspend, throws on failure
        when (presentation.type) {
            PLYPresentationType.DEACTIVATED -> FetchResult.Deactivated
            PLYPresentationType.CLIENT -> FetchResult.Client(PresentationHandle(presentation))
            else -> FetchResult.Success(PresentationHandle(presentation), presentation.height)
        }
    } catch (e: Exception) {
        FetchResult.Error(e.message)
    }
}

// Modal display — v6 session API: display() is non-suspend and returns a
// PLYPresentationSession; await() suspends until dismissal and returns the outcome
// (throws the PLYError if the screen fails to launch or render).
suspend fun display(handle: PresentationHandle, activity: Activity): DisplayResult =
    try {
        handle.presentation.display(activity).await().toDisplayResult()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "display failed: ${e.message}", e)
        DisplayResult.Cancelled
    }

// Inline/embedded display — callbacks are correct here: buildView() returns the
// View synchronously, the outcome callback fires later.
fun getView(handle: PresentationHandle, context: Context, onResult): View?
```

**Pattern (iOS, SDK v6):**
```swift
// In PurchaselyWrapper — fetch: PLYPresentationBuilder + preload
func loadPresentation(placementId: String, contentId: String?,
                      onResult: @escaping @MainActor (DisplayResult) -> Void) async -> FetchResult {
    // PLYPresentationBuilder.from(placementId:).contentId(...).onClose{}.onDismissed{outcome}
    //   .build().preload { presentation, error in ... }
    // Maps presentation.type to FetchResult and wraps the presentation in PresentationHandle.
}

// Modal display — the loaded presentation displays itself; outcomes arrive through the
// onDismissed callback bound at fetch time (PresentationCache constraint).
func display(handle: PresentationHandle, from viewController: UIViewController?)
```

> **Platform note:** Android's `display(activity).await()` suspends until dismissal because the
> Android v6 SDK exposes a `PLYPresentationSession`; the iOS v6 SDK reports outcomes through the
> builder-bound `onDismissed` instead. Both wrappers normalize to the same app-facing
> `DisplayResult`, so ViewModels are identical across platforms.

> **Why `display().await()` and not the callback overload?** Passing an inline callback to
> `display(activity) { outcome -> ... }` **replaces** the `onDismissed` set on the builder for
> that display. The session API keeps the builder callback active *and* hands the outcome to
> the awaiting coroutine — both observers see the dismissal. It also propagates launch/render
> failures as typed `PLYError`s instead of silently dropping them.

---

## 5. MVVM Pattern: ViewModel Owns Paywall Logic

**Rule: ViewModels decide when and what to show. Screens only provide the Activity and render the UI.**

**Prefetch pattern:** All presentations must be prefetched by the ViewModel on init (if not premium). This ensures paywalls are ready to display instantly when the user interacts.

```kotlin
init {
    prefetchPresentations()
}

private fun prefetchPresentations() {
    if (isPremium.value) return
    viewModelScope.launch {
        _filtersPresentation.value = purchaselyWrapper.loadPresentation("filters")
    }
    viewModelScope.launch {
        _inlinePresentation.value = purchaselyWrapper.loadPresentation("inline")
    }
}
```

**Modal paywall flow — Android (e.g., filters):**
1. ViewModel prefetches the presentation on init, exposes `filtersPresentation: StateFlow<FetchResult?>` and `isFiltersLoading: StateFlow<Boolean>`
2. Screen shows a loader on the icon while loading, the regular icon once ready
3. User taps icon → ViewModel checks if presentation is ready (`FetchResult.Success`)
4. If ready, ViewModel emits the `PresentationHandle` via `SharedFlow<PresentationHandle>`
5. Screen collects the handle, resolves `Activity` from `LocalContext`, calls `wrapper.display(handle, activity)` directly
6. Screen reports the result back to the ViewModel via `viewModel.onPaywallDismissed()`
7. If presentation is still loading or failed, the tap is ignored (loader is visible)

**Why the Screen handles display (not the ViewModel):**
- The SDK requires an `Activity` to display modal paywalls — a framework concern
- The ViewModel cannot hold an Activity reference (lifecycle leak)
- The ViewModel emits a `PresentationHandle` (opaque, SDK-free) via `SharedFlow`
- The Screen — which already has `LocalContext` — resolves the Activity and calls `wrapper.display()`
- `PurchaselyWrapper` is injected in the Screen via `koinInject()` for this purpose

**Embedded paywall flow (e.g., inline banner):**
1. ViewModel prefetches the presentation on init, exposes `inlinePresentation: StateFlow<FetchResult?>`
2. Screen observes the state — when `FetchResult.Success`, passes it to `EmbeddedScreenBanner`
3. If fetch failed or is still loading, nothing is displayed (no crash, no empty space)
4. Use `FetchResult.Success.height` (pixels, convert to dp) for the view height

**Prefetch cache (iOS):** `PurchaselyWrapper.loadPresentation` consults `PresentationCache` (in-memory, keyed by `placementId[/contentId]`). First call fetches over the network, subsequent calls return the cached `FetchResult` instantly. This prevents:
- Duplicate network calls when SwiftUI `.onAppear` fires repeatedly (nav back, sheet dismiss, etc.)
- Accumulation of stale `flowSteps` entries in the SDK's `FlowsManager` for flow placements — a known SDK issue where each fetch appends a new entry and dismissing the only visible step leaves a stuck `PLYWindow`.

**Cache invalidation triggers** (iOS):
- `PLYUserAttributeDelegate.onUserAttributeSet` / `onUserAttributeRemoved` — any attribute change can alter audience targeting
- Successful `Purchasely.synchronize()` — subscription state may have changed
- `wrapper.restart()` — SDK mode change (Full ↔ Observer) resets the session

> **Note:** Invalidation is coarse-grained (`invalidateAll`) because the SDK doesn't expose attribute→audience dependencies. This is the simplest correct approach; native placement-level caching is expected in Purchasely SDK 6.x and the app-side cache should be removed then.

> **Note on `onResult` binding:** The `onResult` closure is captured by the SDK at first fetch. On cache hits, the original binding is reused — subsequent callers' `onResult` closures are ignored. For Shaker this is safe because all closures perform the same work (refresh premium on purchased/restored).

**Android:** Same cache concept applies. The Android SDK doesn't (yet) expose a user-attribute delegate as public API — invalidation is currently tied to explicit `wrapper.synchronize()` and `wrapper.restart()` calls only. When Android gets a delegate (SDK 6.x), wire it up the same way.

---

## 6. EmbeddedScreenBanner: Reusable Inline Paywall

**Rule: Use `EmbeddedScreenBanner` for any inline/embedded paywall display. The presentation must be prefetched by the ViewModel.**

```kotlin
// In Screen — only render when prefetch succeeded
val inlineResult by viewModel.inlinePresentation.collectAsStateWithLifecycle()
if (inlineResult is FetchResult.Success) {
    val heightModifier = if (inlineResult.height > 0) {
        Modifier.height(inlineResult.height.dp)
    } else {
        Modifier.heightIn(max = 200.dp)
    }
    EmbeddedScreenBanner(
        fetchResult = inlineResult as FetchResult.Success,
        onResult = { viewModel.onPaywallDismissed() },
        modifier = Modifier.fillMaxWidth().then(heightModifier)
    )
}
```

**Behavior:**
- Accepts a prefetched `FetchResult.Success` (ViewModel owns the fetch)
- Builds the view via `PurchaselyWrapper.getView()` using `remember`
- Renders via `AndroidView`
- Uses `presentation.height` (dp) for view height
- If height is 0, falls back to `heightIn(max = 200.dp)`
- `onResult` forwards purchase events to the ViewModel
- If fetch failed, the banner is simply not shown (Screen checks for `FetchResult.Success`)

---

## 7. User Attributes

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

**Typed overloads:** The wrapper provides `String`, `Boolean`, `Int`, and `Float`/`Double` overloads matching the SDK.

---

## 8. Handling Presentation Types

Always handle all `FetchResult` variants:

| Type | Action |
|------|--------|
| `Success` | Display or build view normally |
| `Client` | App must build its own paywall UI using plan data from the presentation |
| `Deactivated` | Do nothing — placement is disabled in the Purchasely console |
| `Error` | Log the error, fail gracefully (no crash, no empty screen) |

---

## 9. Error Handling

- **Never crash on SDK errors.** Log and degrade gracefully.
- **Never block the UI** waiting for a presentation. Use coroutines/async-await and show content immediately.
- **Embedded views:** If fetch fails, the banner simply doesn't appear.
- **Modal paywalls:** If fetch fails, the user action degrades gracefully — but **never silently**.
- **Handle every `FetchResult` variant explicitly.** An `else -> {}` on a paywall fetch produces a
  dead button with no diagnosis trail. The reference pattern (see `HomeViewModel.onFilterClick`):
  - `Success` → emit the handle for display
  - `Error` → log with the message **and retry the prefetch** so a transient failure (offline at
    launch) doesn't permanently kill the entry point
  - `Deactivated` / `Client` → log at debug level (expected console-side states)
  - `null` (prefetch in flight) → ignore the tap, the loader is visible

---

## 10. Async: Native Async Patterns

**Rule: Use the platform's native async pattern. Only use callbacks when the SDK doesn't provide an alternative.**

**Android (Kotlin, SDK v6):**
- `loadPresentation()` — uses the v6 DSL (`PLYPresentation { ... }`) + the native suspend `preload()`
- `display()` — uses the v6 session API: `presentation.display(activity).await()`. No more
  `suspendCoroutine` bridging — `display()` is non-suspend (Java-friendly) and returns a
  `PLYPresentationSession` whose `await()` suspends until dismissal and throws typed `PLYError`s.
  The session also exposes `state: StateFlow<PLYPresentationState>` to observe the full
  lifecycle (`Displayed` → `Dismissed`/`Error`) when a single outcome isn't enough.
- `getView()` — keeps callbacks because `buildView()` returns a `View?` synchronously; the callback fires later on purchase events

**iOS (Swift, SDK v6) — Swift 6 / Swift Concurrency throughout, no Combine, no `DispatchQueue.main.async` in production Purchasely code:**
- `loadPresentation()` — `async/await` with `withCheckedContinuation` around the v6
  `PLYPresentationBuilder…build().preload { presentation, error in }`; outcome callbacks
  (`onClose`/`onDismissed`) are bound at fetch time and rebound on cache hits
- `display(handle:from:)` — `@MainActor` synchronous, calls `handle.presentation.display(from:)`;
  dismissal outcomes arrive through the fetch-time `onDismissed`
- `EmbeddedScreenBanner(fetchResult:)` — resolves the presentation's `controller` internally for embedding
- `synchronizeReceipt()` — private `async throws` wrapper around `Purchasely.synchronize(success:, failure:)` via `withCheckedThrowingContinuation`
- `PurchaselyWrapping`, `PurchaselyWrapper`, Purchasely-facing ViewModels, and UI-state managers are `@MainActor` isolated. `PLYEventDelegate` / `PLYUserAttributeDelegate` callbacks stay `nonisolated` and only do thread-safe work (logging + `PresentationCache.invalidateAll()`).
- SDK callbacks that fire on unknown threads hop to the main actor via `Task { @MainActor [weak self] in … }`, never `DispatchQueue.main.async`.
- `NotificationCenter` observer for `.purchaselySdkModeDidChange` dispatches via `Task { @MainActor in self?.restart() }` (not `addObserver(forName:queue: .main)`)
- Use `@preconcurrency import Purchasely` where needed because the current SDK exposes Objective-C class properties/callback types without full Swift 6 concurrency annotations.

---

## 11. Testability

**Rule: All Purchasely integration code must be testable. ViewModels use dependency injection for the wrapper.**

**Protocol (iOS):** `PurchaselyWrapping` protocol abstracts the wrapper. ViewModels accept `PurchaselyWrapping` via init with default `PurchaselyWrapper.shared`. Tests inject `MockPurchaselyWrapper`.

**Mocking (Android):** `PurchaselyWrapper` is injected via Koin constructor. Tests use MockK to mock it with `mockk<PurchaselyWrapper>(relaxed = true)`.

**PurchaseManager testability:**
- **Android:** Constructor takes `billingClientFactory: (PurchasesUpdatedListener) -> BillingClient` — tests inject a mock BillingClient
- **iOS:** Uses injected closures (`anonymousUserIdProvider`, `signPromotionalOfferProvider`) instead of direct wrapper access

**Repository testability (Android):** `FavoritesRepository`, `OnboardingRepository`, `RunningModeRepository`, `SettingsRepository` accept a `KeyValueStore` interface instead of `Context`/`SharedPreferences`. Tests use `InMemoryKeyValueStore` — no Android framework needed.

**Repository testability (iOS):** `FavoritesRepository`, `OnboardingRepository` accept custom `UserDefaults` for test isolation. `CocktailRepository` accepts a `[Cocktail]` array for test data.

---

## 12. Platform-Specific Notes

### Android (Kotlin / Jetpack Compose)

- `PurchaselyWrapper` is a Koin singleton with DI constructor: `PurchaselyWrapper(runningModeRepo, purchaseRequests, restoreRequests, transactionResult, scope)`
- `ShakerApp.onCreate()` calls `wrapper.initialize(application, apiKey, logLevel, onConfigured)` — nothing else. `onConfigured` triggers `premiumRepository.refreshPremiumStatus()`.
- `PremiumManagerImpl` implements `PremiumRepository` interface and injects `PurchaselyWrapper` (no direct SDK calls). Wired via `onTransactionCompleted` callback in AppModule.
- `PLYPresentation` is wrapped in `PresentationHandle` (`@JvmInline value class`). ViewModels emit `SharedFlow<PresentationHandle>` for modal paywalls; Screens resolve Activity and call `wrapper.display(handle, activity)`.
- Repositories (`FavoritesRepository`, `OnboardingRepository`, `RunningModeRepository`, `SettingsRepository`) accept `KeyValueStore` interface — no `Context` dependency. Koin injects `SharedPreferencesKeyValueStore` in production, `InMemoryKeyValueStore` in tests.
- Domain layer (`domain/repository/`) defines interfaces; `data/` contains `*Impl` implementations.
- ViewModels inject repository interfaces, not concrete classes.
- `EmbeddedScreenBanner` uses `koinInject()` for DI in Composables
- `buildView()` returns `PLYPresentationView?` (extends `FrameLayout`)

### iOS (SwiftUI)

- `PurchaselyWrapper` is a `@MainActor` Swift singleton (`PurchaselyWrapper.shared`) conforming to the `@MainActor` `PurchaselyWrapping` protocol
- `AppViewModel.init()` calls `wrapper.initialize(apiKey:, appUserId:, logLevel:, onReady:)` — nothing else
- ViewModels accept `PurchaselyWrapping` via init with default `.shared` and are `@MainActor` isolated when they mutate UI state or call wrapper APIs
- **No Combine in the wrapper or in PurchaseManager.** Pure Swift Concurrency: `async/await`, `Task { @MainActor in … }`, `withCheckedContinuation`/`withCheckedThrowingContinuation` to bridge SDK callbacks
- **No `DispatchQueue.main.async` in production Purchasely code.** Hops to main go through `Task { @MainActor in … }` to keep one consistent concurrency model
- The whole `PurchaselyWrapping` protocol is `@MainActor`, so protocol call sites do not accidentally cross actor boundaries.
- `HomeViewModel` no longer uses Combine for filtering; filtering is synchronous state derivation from `@Published` properties. Tests should not rely on debounce timing for filter assertions.
- `loadPresentation()` is `async` and takes an `onResult` callback for purchase/dismiss events
- `getController()` returns `PLYPresentationViewController?` for embedding via `UIViewControllerRepresentable`
- `EmbeddedScreenBanner` is a `UIViewControllerRepresentable` wrapping the presentation's controller
- Screen resolves a `UIViewController` via `ViewControllerResolver` for modal display
- `presentation.height` is in points (use as `CGFloat` directly in `.frame(height:)`)
- Prefetch is triggered from `onAppear` since `@StateObject` init doesn't have access to `@EnvironmentObject`

---

## 13. Diagnostic & Troubleshooting

When something looks wrong (paywall doesn't close, wrong screen reappears, purchase doesn't unlock premium…), do **not** start patching code. The Purchasely SDK emits a detailed log stream, and Shaker adds its own `[Shaker]` log lines — read them first, the answer is almost always there.

### Log sources

| Prefix | Source | What it tells you |
|--------|--------|-------------------|
| `[Purchasely][YYYY-MM-DD HH:MM:SS.mmm]<Level>` | SDK internal logs | SDK lifecycle (config, fetch, validation, receipt status) |
| `[Purchasely] Event: <NAME>` | SDK analytics events | Every paywall view, purchase, restore, dismiss, error |
| `[Shaker] Event: <NAME> \| Properties: {…}` | App-side mirror of SDK events (via `PLYEventDelegate` / `EventListener`) | Same events, with the full property bag — useful to inspect targeting context |
| `[Shaker] …` | App-side instrumentation in `PurchaselyWrapper` | Local decisions (chain `success_payment`, sync result, observer mode dispatch) |

> `[Purchasely]` is emitted by the SDK and is identical in every integration — that is your grep target (`grep "\[Purchasely\]"`) when debugging any Purchasely-powered app. `[Shaker]` is **specific to this demo app** — in your own integration, replace it with your app's log tag (e.g. `[YourApp]`) but keep the same markers around the same decision points (chain trigger, sync result, fetch outcome) so a teammate can reproduce this diagnostic workflow.

Set the SDK log level to `.debug` (iOS) / `LogLevel.DEBUG` (Android) during development — defaults in Shaker.

### Key SDK events to watch

The SDK fires these named events. Each carries a property bag (placement_id, displayed_presentation, flow_id, step_id, plan, …). They are the source of truth for *what the SDK actually did*.

| Event | Fires when | Useful properties |
|-------|------------|-------------------|
| `APP_CONFIGURED` | After `Purchasely.start(...)` completes successfully | `sdk_version`, `running_mode`, `storekit_version` |
| `APP_STARTED` | After the SDK has finished its full startup (config + initial fetches) | `session_id`, `session_count` |
| `PRESENTATION_LOADED` | A paywall is fetched and ready to render. **Fires once per prefetched placement at startup, plus on every fetch** | `placement_id`, `displayed_presentation`, `internal_presentation_id`, `flow_id`, `display_mode`, `paywall_request_duration_in_ms` |
| `PRESENTATION_VIEWED` | A paywall is on screen | same + `paywall_rendering_time_in_ms`, `display_method` |
| `PRESENTATION_CLOSED` | A paywall is dismissed (any reason) | same + `screen_duration` |
| `PLAN_SELECTED` | User taps a plan | `plan`, `purchasely_plan_id`, `store_product_id` |
| `IN_APP_PURCHASING` | Purchase tap, billing flow opens | `plan` |
| `IN_APP_PURCHASED` | Native purchase succeeds (before validation) | `plan`, `transaction_id` |
| `RECEIPT_CREATED` | SDK builds the receipt payload, about to validate | `receipt_status` |
| `RECEIPT_VALIDATED` | Server validation succeeded | `receipt_status: completed` |
| `RECEIPT_FAILED` | Server validation refused the receipt (sandbox issues, expired, invalid signature) | `error` |
| `IN_APP_PURCHASE_FAILED` | Whole purchase attempt failed (network, receipt, billing) | `error` |
| `IN_APP_RENEWED` | Receipt confirms an active subscription | `running_subscriptions`, `plan` |
| `IN_APP_RESTORED` | Restore flow finds an active receipt | `plan` |
| `IN_APP_DEFERRED` / `IN_APP_NOT_PURCHASED` | Pending / cancelled | `plan` |

### How to read a purchase log trace

Real Shaker log slice for one Observer-mode purchase on the `onboarding` placement (annotated):

```
[Purchasely] Receipt status: transmitting          ← SDK starts validating the receipt
[Purchasely] Successfully retrieved subscriptions.
[Purchasely] Receipt status: completed             ← receipt validated
[Purchasely] Event: RECEIPT_VALIDATED
[Shaker] Event: RECEIPT_VALIDATED | Properties: {  ← Shaker mirrors via PLYEventDelegate
  placement_id: "onboarding",
  flow_id: "onboarding_flow",
  displayed_presentation: "onboarding_step_3",
  plan: "premiumbasicmonthly",
  running_subscriptions: [{ plan, product }],      ← user is now subscribed ✓
  …
}
[Purchasely] Event: IN_APP_RENEWED                 ← subscription confirmed active
[Shaker] Transaction success — synchronized; presentation closed, awaiting success_payment
                                                   ↑ Shaker's own log from
                                                     handleTransactionResult(.success)
[Purchasely] Interceptor executed action purchase. Skipping SDK execution.
                                                   ↑ proceed(false) acknowledged — SDK won't
                                                     try to purchase via its own flow
[Purchasely] Event: PRESENTATION_CLOSED            ← paywall dismissed
[Shaker] Event: PRESENTATION_CLOSED | Properties: { placement_id: "onboarding", … }
[Shaker] loadPresentation completion — placement=onboarding displayResult=cancelled pendingSuccessfulPurchase=true
                                                   ↑ Shaker logs the chain decision: pending=true,
                                                     so success_payment will be triggered next
[Shaker] Chaining success_payment after onboarding
[Purchasely] Successfully retrieved presentation Optional("new_screen").
[Shaker] success_payment fetchCompletion — id=new_screen type=PLYPresentationType(rawValue: 0) error=none
[Purchasely] Event: PRESENTATION_LOADED            ← success_payment paywall loaded
[Purchasely] Event: PRESENTATION_VIEWED            ← shown on screen
```

The trace tells you three things, in order:
1. **Receipt validated** (`RECEIPT_VALIDATED`, `IN_APP_RENEWED`) — purchase succeeded server-side.
2. **Paywall dismissed** (`PRESENTATION_CLOSED`) — `proceed(false)` + `closeAllScreens()` worked.
3. **Chain fired** (`Chaining success_payment` → `PRESENTATION_VIEWED` for the new placement).

If any of those three is missing, you have a defined symptom — see the table below.

### Symptom → likely cause

| Symptom (in logs) | Likely cause | Where to look |
|-------------------|--------------|---------------|
| No `RECEIPT_VALIDATED` event | Receipt failed server-side validation | Check `[Purchasely] Receipt status: …` lines — `failed` / `error` → check StoreKit config, sandbox account, server clock |
| `IN_APP_PURCHASED` but no `IN_APP_RENEWED` | Receipt validated but no active subscription state — server didn't see entitlement | Dashboard → Subscribers → look up the transaction; check store product config |
| `PRESENTATION_CLOSED` never fires after a successful purchase | `closeAllScreens()` not called, or called before `proceed(false)` | iOS: `PurchaselyWrapper.handleTransactionResult(.success)` order. Android: same in Kotlin |
| `[Shaker] loadPresentation completion — … pendingSuccessfulPurchase=false` after a real purchase | The flag was never set (handleTransactionResult didn't run, or wrong mode) | iOS: check interceptor `.purchase` case took the Observer branch. Android: same |
| `[Shaker] Chaining success_payment` fires, but `success_payment fetchCompletion` returns `type=deactivated` or `error=…` | Placement `success_payment` missing / typo / deactivated on dashboard | Dashboard → Placements → `success_payment`. Common gotcha: **typo** in placement_id (we hit `sucess_payment` once — see Shaker history) |
| `success_payment fetchCompletion` returns a presentation, but the rendered paywall is "the onboarding one again" | The flow that hosts the original placement chains a post-purchase step that points to the wrong paywall | The event's `flow_id` and `displayed_presentation` will reveal the chained step. Dashboard → Flows → inspect `<flow_id>` post-purchase branches |
| `IN_APP_RESTORED` but premium UI doesn't update | `userSubscriptions(...)` not called after the chain, or callback's `PremiumManager.shared.updatePremium(...)` not wired | iOS: `refreshAfterSuccessPayment()` in `PurchaselyWrapper`. Android: `PremiumManagerImpl.onTransactionCompleted` |
| `is_fallback_presentation: true` on `PRESENTATION_LOADED` | Audience targeting failed, SDK served the default — usually a stale presentation cache | Trigger an attribute change → `PLYUserAttributeDelegate` invalidates cache. Or call `PresentationCache.shared.invalidateAll()` explicitly |

### Reading event property bags

Every `[Shaker] Event: <NAME> | Properties: {…}` carries the full SDK context. Useful fields when debugging:

- `placement_id` + `internal_placement_id` — which placement the SDK was working on
- `displayed_presentation` + `internal_presentation_id` + `template` — which paywall design was rendered (template ID matches the Console > Paywalls listing)
- `flow_id` + `flow_session_id` + `internal_flow_id` + `step_id` + `from_step_id` — flow position. Useful to detect when a flow continues into a post-purchase step (the bug we hit with `new_screen` chained inside `onboarding_flow`)
- `is_fallback_presentation: true` — SDK fell back to the default paywall instead of resolving via audience targeting
- `display_mode` — `full_screen` / `push` — reveals how the SDK is rendering (e.g. a `push` after `full_screen` indicates a flow step continuation)
- `purchasable_plans` — the plans offered. Empty array on `success_payment` is normal (no purchase action expected)
- `running_subscriptions` (on `IN_APP_RENEWED`) — confirms which entitlement is active after the validation
- `paywall_request_duration_in_ms` + `paywall_rendering_time_in_ms` — performance budget for the paywall

### App-side `[Shaker]` log markers added by the wrapper

These are the lines we **deliberately** print to make the integration debuggable. Don't remove them unless you've stopped using `PurchaselyWrapper`:

| Line | Where | Tells you |
|------|-------|-----------|
| `[Shaker] Transaction success — synchronized; presentation closed, awaiting success_payment` | `handleTransactionResult(.success)` after `synchronizeReceipt()` | Sync finished, dismissal sequence ran |
| `[Shaker] Synchronize failed after transaction: <err>` | `handleTransactionResult(.success)` catch | Sync errored — we dismissed anyway |
| `[Shaker] Transaction cancelled` / `[Shaker] Transaction error: …` | other `handleTransactionResult` cases | Non-success outcomes from Observer mode |
| `[Shaker] loadPresentation completion — placement=<id> displayResult=<r> pendingSuccessfulPurchase=<b>` | `loadPresentation` SDK completion | Snapshot of state when paywall dismissed |
| `[Shaker] Chaining success_payment after <placement>` | Same, just before triggering the chain | Confirms chain decision |
| `[Shaker] success_payment fetchCompletion — id=<id> type=<t> error=<e>` | `showSuccessPaymentScreen` fetch | What the success_payment placement actually resolves to |
| `[Shaker] success_payment placement unavailable: <err>` | Same, no presentation returned | Placement is missing or deactivated |
| `[Shaker] Error refreshing after success_payment: …` | `refreshAfterSuccessPayment` failure | Subscriptions refresh failed — UI may show stale premium state |
| `[Shaker] Event: <NAME> \| Properties: …` | `PLYEventDelegate.eventTriggered` | Every SDK event mirrored to the app log |
| `[Shaker] User attribute set: <k>=<v> (source: …)` / `[Shaker] User attribute removed: …` | `PLYUserAttributeDelegate` | Audience-affecting changes — presentation cache is invalidated here |

### Reading SDK lifecycle logs (startup)

> Every SDK log line is tagged `[Purchasely][YYYY-MM-DD HH:MM:SS.mmm]<Level>` — that tag is the easiest way to slice them out of the console (`grep "\[Purchasely\]"`). App-side mirroring is tagged `[Shaker]`.

Real Shaker startup slice (annotated):

```
[Purchasely] 1 products declared: premium-basic                          ← SDK reads its configured products
[Purchasely] [AppStore][Storekit2] Fetching app store products:          ← StoreKit2 fetches App Store metadata
              premium.basic.nonrenewing,
              com.purchasely.shaker.basic.semester,
              com.purchasely.shaker.basic.monthly
[Purchasely] Successfully retrieved presentation Optional("new_screen")  ← prefetched paywalls (one log per placement)
[Purchasely] Successfully retrieved presentation Optional("inline_2")
[Purchasely] [AppStore][Storekit2] Fetched app store products and found  ← all store products resolved
              premium.basic.nonrenewing,
              com.purchasely.shaker.basic.monthly,
              com.purchasely.shaker.basic.semester
[Purchasely] 1 products available for sale: premium-basic                ← product mapping resolved
[Purchasely] 3 plans available for sale: premiumbasicmonthly,
              premiumbasicyearly,
              premiumbasicsemester
[Purchasely] Event: APP_CONFIGURED                                       ← ✓ Purchasely.start() succeeded
[Purchasely] Event: PRESENTATION_LOADED                                  ← one event per prefetched placement
[Purchasely] Event: PRESENTATION_LOADED
[Purchasely] Event: PRESENTATION_LOADED
[Purchasely] Event: PRESENTATION_LOADED
[Purchasely] Event: PRESENTATION_VIEWED                                  ← the onboarding paywall shown to user
[Purchasely] Event: APP_STARTED                                          ← ✓ initial fetches done, SDK fully ready
[Purchasely] Successfully retrieved subscriptions.                       ← first subscriptions() poll
[Purchasely] Successfully retrieved subscriptions.                       ← (may fire several times — initial sync passes)
[Purchasely] Successfully retrieved subscriptions.
```

**Order matters:**
1. **Products fetch** from the App Store / Play Store (before any paywall can show real prices)
2. **Presentations fetch** in parallel (one `Successfully retrieved presentation Optional("…")` per prefetched placement)
3. **`APP_CONFIGURED`** — SDK marks itself as ready; `onReady`/`onConfigured` fires
4. **`PRESENTATION_LOADED` × N** — one event per prefetched placement; useful to confirm all your placements were resolved
5. **`PRESENTATION_VIEWED`** — first paywall actually shown
6. **`APP_STARTED`** — full startup completed (the SDK considers itself fully bootstrapped)
7. **Initial `userSubscriptions` polls** — SDK refreshes subscription state

**Red flags at startup:**
- `APP_CONFIGURED` never fires → `start(...)` failed. Check API key, network, the `onReady`/`onConfigured` callback's `error` argument.
- `0 products available for sale` → product IDs in the Console don't match any store products. Check Console > Products and store consoles (App Store Connect / Play Console).
- `0 plans available for sale` → plans configured but no store products bound. Console > Products > plan → store binding.
- Paywall `PRESENTATION_LOADED` missing for a placement you expect → placement undefined / deactivated / wrong audience targeting on dashboard.
- `is_fallback_presentation: true` on `PRESENTATION_VIEWED` → audience targeting failed, default paywall served.

### Reading receipt validation logs

Receipt processing has its own log stream — useful when a purchase succeeds locally (StoreKit confirms) but Purchasely doesn't recognise the subscription. **The validation can fail without aborting the StoreKit transaction**, so always check both sides.

Sandbox-failure trace (annotated):

```
[Purchasely] [AppStore][Storekit2][Listener] Transaction verified:       ← StoreKit verified the transaction locally
              com.purchasely.shaker.basic.monthly
[Purchasely] Receipt created.                                            ← receipt payload built
[Purchasely] Event: RECEIPT_CREATED
[Purchasely] Refreshing receipt status for validation.
[Purchasely] Checking receipt status (single attempt).
[Purchasely] Receipt status: verifying                                   ← server-side validation in progress
[Purchasely] Receipt is still being processed (status: verifying)
[Purchasely] Refreshing receipt status for validation.                   ← SDK polls
[Purchasely] Checking receipt status (single attempt).
[Purchasely] Receipt status: failed                                      ← ⛔ server refused
[Purchasely] ⛔️ Receipt validation failed.                               ← human-readable cause
              [Sandbox error] The receipt sent by Apple doesn't
              contain a valid purchase. To force Apple to make a
              new purchase, try the following procedure: …
[Purchasely] Event: RECEIPT_FAILED                                       ← analytics event for the failure
[Purchasely] Event: IN_APP_PURCHASE_FAILED                               ← overall purchase attempt marked failed
[Purchasely] [AppStore][Storekit2][Listener] Transaction verified:       ← StoreKit retries / fires the entitlement again
              com.purchasely.shaker.basic.monthly
[Purchasely] Event: IN_APP_RENEWED                                       ← ✓ eventually recovers
```

**How to read `Receipt status`:** the SDK polls until a terminal status is reached.

| Status | Meaning |
|--------|---------|
| `transmitting` | Receipt being uploaded to Purchasely's server |
| `verifying` | Server validating with Apple / Google |
| `completed` | Validated, entitlement granted ✓ |
| `failed` | Validation refused — see the error message that follows |

**Common `failed` causes (App Store sandbox):**
- Sandbox account not signed in / mismatched
- StoreKit Configuration file used in Xcode (local testing) but receipt sent to real Apple servers
- Receipt from a different bundle ID / environment
- Clock skew (server vs device > a few minutes)
- For real prod issues: check Apple / Google service status before debugging code

### Quick diagnostic checklist

When a teammate says "paywall is broken", ask in this order:
1. **Which platform** and **which placement_id**? (iOS / Android, `onboarding` / `recipe_detail` / …)
2. **Console grep** : `grep -E "\[Purchasely\]|\[Shaker\]"` over the run
3. **First red flag** : missing `APP_CONFIGURED` (config) ? Missing `PRESENTATION_LOADED` (placement/audience) ? `is_fallback_presentation: true` (cache) ?
4. **Dashboard cross-check** : does the placement exist? Is it deactivated? Which paywall is attached? Is it in a flow that chains elsewhere?

---

## Checklist for New Purchasely Integrations

- [ ] All SDK calls go through `PurchaselyWrapper`
- [ ] Screen has zero `io.purchasely` / `import Purchasely` imports
- [ ] Uses `loadPresentation()` + `display()`/`getView()`/`getController()`, never `presentationView()` or `presentationController()`
- [ ] Presentations are prefetched by the ViewModel (Android: `init`, iOS: `onAppear`)
- [ ] Handles all `FetchResult` variants (success, client, deactivated, error)
- [ ] User attributes set from ViewModel, not Screen
- [ ] Modal paywalls (Android): ViewModel prefetches, emits `SharedFlow<PresentationHandle>`, Screen resolves Activity and calls `wrapper.display(handle, activity)`
- [ ] Modal paywalls (iOS): ViewModel prefetches, shows loader while loading, Screen provides ViewController on display
- [ ] Embedded paywalls: ViewModel prefetches, Screen uses `EmbeddedScreenBanner` with prefetched result/controller
- [ ] Uses `presentation.height` (dp/points) for embedded view sizing
- [ ] No crashes on SDK errors — nothing shown if fetch fails
- [ ] SDK init and interceptor are in PurchaselyWrapper.initialize() — NOT in App class
- [ ] Observer mode purchases flow through PurchaseManager — **Android:** via `SharedFlow` subjects; **iOS:** via direct `async` calls (`PurchaseManager.shared.purchase(productId:)`)
- [ ] PurchaseManager has zero Purchasely/SDK imports
- [ ] **iOS:** no `import Combine` in `PurchaselyWrapper` or `PurchaseManager`; no `DispatchQueue.main.async` in production Purchasely code (use `Task { @MainActor in … }`)
- [ ] **iOS:** `PurchaselyWrapping`, `PurchaselyWrapper`, and Purchasely-facing ViewModels are `@MainActor`; SDK delegate callbacks that remain nonisolated do only thread-safe work
- [ ] **iOS:** Observer purchase/restore path guards against overlapping StoreKit flows with a single in-flight task
- [ ] Login/logout, restore, consent, synchronize go through wrapper in ViewModels
- [ ] SDK types (PLYRunningMode, PLYDataProcessingPurpose, etc.) are tolerated as direct imports
- [ ] Android Screens use `collectAsStateWithLifecycle()` (not `collectAsState()`) for lifecycle-aware collection
- [ ] Tests use MockPurchaselyWrapper (iOS) or mockk (Android) — never the real SDK
