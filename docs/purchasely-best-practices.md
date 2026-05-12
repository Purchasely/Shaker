# Purchasely SDK — Best Practices

This document defines the integration standards for the Purchasely SDK in Shaker.
All changes to the Purchasely integration must follow and update this document.

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

**Tolerated SDK type imports:** `PLYRunningMode`, `PLYDataProcessingPurpose`, `PLYPresentationAction`, `PLYPresentationInfo`, `PLYPresentationActionParameters`, `PLYPresentationViewController`, `EventListener`/`PLYEventDelegate`, `PLYOfferSignature`, `LogLevel`/`PLYLogger.PLYLogLevel` — these are enums/types needed for configuration, interceptor logic, and presentation handling. They are not SDK call points.

> **Android note:** `PLYPresentation` is wrapped in the opaque `PresentationHandle` value class. ViewModels and Screens never import `PLYPresentation` directly — they use `PresentationHandle` exclusively.

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

## 4. Presentation Loading: Always fetch then build/display

**Rule: Always use `Purchasely.fetchPresentation()` followed by `presentation.buildView()` or `presentation.display()`. Never use `Purchasely.presentationView()`.**

**Why:**
- `fetchPresentation` + `buildView`/`display` gives full control over the presentation lifecycle
- You can inspect `presentation.type` before deciding what to do (NORMAL, CLIENT, DEACTIVATED)
- You can handle errors from the fetch step separately from display errors
- `presentationView()` is a convenience shortcut that hides these steps — unsuitable for reference code

**Pattern:**
```kotlin
// In PurchaselyWrapper
suspend fun loadPresentation(placementId: String): FetchResult {
    // Uses suspend Purchasely.fetchPresentation() internally
    // Maps result to FetchResult sealed class
    // Catches exceptions and returns FetchResult.Error
}

// For modal display
suspend fun display(handle: PresentationHandle, activity: Activity): DisplayResult  // Android
func display(presentation: PLYPresentation, from viewController: UIViewController?) // iOS

// For inline/embedded display
fun getView(handle: PresentationHandle, context: Context, onResult): View?          // Android
func getController(presentation: PLYPresentation) -> PLYPresentationViewController? // iOS
```

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
- **Modal paywalls:** If fetch fails, the user action is silently ignored (with a log).

---

## 10. Async: Native Async Patterns

**Rule: Use the platform's native async pattern. Only use callbacks when the SDK doesn't provide an alternative.**

**Android (Kotlin):**
- `loadPresentation()` — uses the native suspend `Purchasely.fetchPresentation()`
- `display()` — wraps the callback-based `display(activity)` with `suspendCoroutine`
- `getView()` — keeps callbacks because `buildView()` returns a `View?` synchronously; the callback fires later on purchase events

**iOS (Swift) — Swift 6 / Swift Concurrency throughout, no Combine, no `DispatchQueue.main.async` in production Purchasely code:**
- `loadPresentation()` — `async/await` with `withCheckedContinuation` to bridge `fetchPresentation(for:, fetchCompletion:, completion:)`; the `onResult` callback is bound at fetch time via the `completion` closure
- `display()` — `@MainActor` synchronous, calls `presentation.display(from:)` directly
- `getController()` — returns the presentation's `UIViewController` for embedding
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
