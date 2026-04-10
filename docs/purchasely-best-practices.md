# Purchasely SDK — Best Practices

This document defines the integration standards for the Purchasely SDK in Shaker.
All changes to the Purchasely integration must follow and update this document.

---

## 1. Architecture: PurchaselyWrapper

**Rule: Never call the Purchasely SDK directly. All calls go through `PurchaselyWrapper`.**

The only exception is `PremiumManager`, which directly calls `userSubscriptions` — it's already an abstraction layer for subscription status.

**Why:**
- Single point of control for all SDK interactions — easy to swap with a stub for removal
- Screens have zero `io.purchasely` / `import Purchasely` imports — clean separation
- Easier to test (mock the wrapper, not the SDK)
- Consistent error handling and result mapping
- Type-safe result types (`FetchResult`, `DisplayResult`) instead of raw SDK enums

**Wrapper responsibilities:**

| Category | Methods |
|----------|---------|
| **Init & Lifecycle** | `initialize()`, `restart()`, `close()`, `closeDisplayedPresentation()` |
| **Interceptor** | Internal paywall actions interceptor (LOGIN, NAVIGATE, PURCHASE, RESTORE) |
| **Events** | Internal event listener/delegate |
| **Presentations** | `loadPresentation()`, `display()`, `getView()` (Android) / `getController()` (iOS) |
| **User Attributes** | `setUserAttribute()`, `incrementUserAttribute()` |
| **User Management** | `userLogin()`, `userLogout()`, `anonymousUserId` |
| **Purchases** | `restoreAllProducts()`, `synchronize()`, `signPromotionalOffer()` (iOS) |
| **Consent** | `revokeDataProcessingConsent()` |
| **Info** | `sdkVersion`, `isDeeplinkHandled()` |

**Tolerated SDK type imports:** `PLYRunningMode`, `PLYDataProcessingPurpose`, `PLYPresentationAction`, `EventListener`/`PLYEventDelegate`, `PLYOfferSignature`, `LogLevel`/`PLYLogger.PLYLogLevel` — these are enums/types needed for configuration, not SDK call points.

---

## 2. Observer Mode: Reactive Purchase Flow

**Rule: In Observer mode, purchases and restores are decoupled from the SDK via reactive flows. `PurchaseManager` has zero Purchasely imports.**

### Architecture

```
PurchaselyWrapper                          PurchaseManager
    │                                           │
    │ PURCHASE (observer) ──────────────────►   │
    │   emit PurchaseRequest                     │
    │                                           │ Native billing
    │                                           │ (Play Billing / StoreKit 2)
    │   ◄────────────────────────────────────   │
    │   TransactionResult                        │
    │                                           │
    │ synchronize()                              │
    │ processAction(false)                       │
    │ premiumManager.refreshPremiumStatus()      │
```

**Android (Kotlin):**
- `SharedFlow<PurchaseRequest>` — wrapper emits, PurchaseManager collects
- `SharedFlow<RestoreRequest>` — wrapper emits, PurchaseManager collects
- `SharedFlow<TransactionResult>` — PurchaseManager emits, wrapper collects
- PurchaseManager takes a `billingClientFactory` lambda (testable, no hardcoded BillingClient)

**iOS (Swift):**
- `PassthroughSubject<PurchaseRequest, Never>` — wrapper sends, PurchaseManager sinks
- `PassthroughSubject<Void, Never>` — wrapper sends restore trigger
- `PassthroughSubject<TransactionResult, Never>` — PurchaseManager sends, wrapper sinks
- PurchaseManager uses injected closures for `anonymousUserId` and `signPromotionalOffer` (no wrapper reference)

**Types:**

```kotlin
// Android
data class PurchaseRequest(val activity: Activity, val productId: String, val offerToken: String)
data object RestoreRequest
sealed class TransactionResult { Success, Cancelled, Error(message), Idle }
```

```swift
// iOS
struct PurchaseRequest { let productId: String }
enum TransactionResult { case success, cancelled, error(String?), idle }
```

**processAction callback:** The wrapper stores a single `pendingProcessAction: ((Boolean) -> Unit)?` when emitting a purchase/restore request. When TransactionResult arrives, it invokes the callback and nullifies it. Only one purchase at a time (paywall flow is sequential).

**Interceptor rules:**

| Action | Observer mode | Full mode |
|--------|--------------|-----------|
| PURCHASE | Store processAction, emit PurchaseRequest | proceed(true) |
| RESTORE | Store processAction, emit RestoreRequest | proceed(true) |
| LOGIN | proceed(false) | proceed(false) |
| NAVIGATE | Open URL, proceed(false) | Open URL, proceed(false) |
| Other | proceed(true) | proceed(true) |

**TransactionResult handling:**

| Result | Wrapper actions |
|--------|----------------|
| Success | synchronize() → processAction(false) → premiumManager.refreshPremiumStatus() |
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
5. Combine/Flow subscriptions for Observer purchase flow

**Restart:** When the SDK mode changes, `wrapper.restart()` is called:
- **Android:** `SettingsViewModel` calls `purchaselyWrapper.restart()` directly
- **iOS:** `SettingsViewModel` posts `.purchaselySdkModeDidChange` notification, wrapper observes it and calls `restart()` internally

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
suspend fun display(presentation: PLYPresentation, activity: Activity): DisplayResult {
    // Uses presentation.display(activity) { result, plan -> } internally
}

// For inline/embedded display
fun getView(presentation: PLYPresentation, context: Context, onResult): View? {
    // Uses presentation.buildView(context, callback) internally
}
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

**Modal paywall flow (e.g., filters):**
1. ViewModel prefetches the presentation on init, exposes `filtersPresentation: StateFlow<FetchResult?>` and `isFiltersLoading: StateFlow<Boolean>`
2. Screen shows a loader on the icon while loading, the regular icon once ready
3. User taps icon → ViewModel checks if presentation is ready (`FetchResult.Success`)
4. If ready, ViewModel stores it and emits an event via `SharedFlow<Unit>`
5. Screen collects the event, gets the Activity, calls `viewModel.displayPendingPaywall(activity)`
6. ViewModel calls `wrapper.display(presentation, activity)` and handles the result
7. If presentation is still loading or failed, the tap is ignored (loader is visible)

**Why the Activity passes through the ViewModel:**
- The SDK requires an `Activity` to display modal paywalls
- The ViewModel cannot hold an Activity reference (lifecycle leak)
- The Screen provides the Activity on-demand when the ViewModel signals readiness
- This is a standard Android MVVM compromise for SDK interactions

**Embedded paywall flow (e.g., inline banner):**
1. ViewModel prefetches the presentation on init, exposes `inlinePresentation: StateFlow<FetchResult?>`
2. Screen observes the state — when `FetchResult.Success`, passes it to `EmbeddedScreenBanner`
3. If fetch failed or is still loading, nothing is displayed (no crash, no empty space)
4. Use `FetchResult.Success.height` (pixels, convert to dp) for the view height

---

## 6. EmbeddedScreenBanner: Reusable Inline Paywall

**Rule: Use `EmbeddedScreenBanner` for any inline/embedded paywall display. The presentation must be prefetched by the ViewModel.**

```kotlin
// In Screen — only render when prefetch succeeded
val inlineResult by viewModel.inlinePresentation.collectAsState()
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

**iOS (Swift):**
- `loadPresentation()` — uses `async/await` with `withCheckedContinuation` to bridge `fetchPresentation(for:, fetchCompletion:, completion:)`; the `onResult` callback is bound at fetch time via the `completion` closure
- `display()` — synchronous, calls `presentation.display(from:)` on main thread; result delivered through the `onResult` callback from fetch
- `getController()` — returns the presentation's `UIViewController` for embedding

---

## 11. Testability

**Rule: All Purchasely integration code must be testable. ViewModels use dependency injection for the wrapper.**

**Protocol (iOS):** `PurchaselyWrapping` protocol abstracts the wrapper. ViewModels accept `PurchaselyWrapping` via init with default `PurchaselyWrapper.shared`. Tests inject `MockPurchaselyWrapper`.

**Mocking (Android):** `PurchaselyWrapper` is injected via Koin constructor. Tests use MockK to mock it with `mockk<PurchaselyWrapper>(relaxed = true)`.

**PurchaseManager testability:**
- **Android:** Constructor takes `billingClientFactory: (PurchasesUpdatedListener) -> BillingClient` — tests inject a mock BillingClient
- **iOS:** Uses injected closures (`anonymousUserIdProvider`, `signPromotionalOfferProvider`) instead of direct wrapper access

**Repository testability (iOS):** `FavoritesRepository`, `OnboardingRepository` accept custom `UserDefaults` for test isolation. `CocktailRepository` accepts a `[Cocktail]` array for test data.

---

## 12. Platform-Specific Notes

### Android (Kotlin / Jetpack Compose)

- `PurchaselyWrapper` is a Koin singleton with DI constructor: `PurchaselyWrapper(premiumManager, runningModeRepo, purchaseRequests, restoreRequests, transactionResult, scope)`
- `ShakerApp.onCreate()` calls `wrapper.initialize(application, apiKey, logLevel)` — nothing else
- ViewModels inject it via constructor: `class HomeViewModel(..., private val purchaselyWrapper: PurchaselyWrapper)`
- `EmbeddedScreenBanner` uses `koinInject()` for DI in Composables
- `display()` requires an `Activity` — passed from Screen to ViewModel on-demand
- `buildView()` returns `PLYPresentationView?` (extends `FrameLayout`)

### iOS (SwiftUI)

- `PurchaselyWrapper` is a Swift singleton (`PurchaselyWrapper.shared`) conforming to `PurchaselyWrapping`
- `AppViewModel.init()` calls `wrapper.initialize(apiKey:, appUserId:, logLevel:, onReady:)` — nothing else
- ViewModels accept `PurchaselyWrapping` via init with default `.shared`
- Async operations use Swift `async/await` (`withCheckedContinuation` to bridge callbacks)
- `loadPresentation()` is `async` and takes an `onResult` callback for purchase/dismiss events
- `display()` is synchronous — calls `presentation.display(from: viewController)` on main thread
- `getController()` returns `PLYPresentationViewController?` for embedding via `UIViewControllerRepresentable`
- `EmbeddedScreenBanner` is a `UIViewControllerRepresentable` wrapping the presentation's controller
- Screen resolves a `UIViewController` via `ViewControllerResolver` for modal display
- `presentation.height` is in points (use as `CGFloat` directly in `.frame(height:)`)
- Prefetch is triggered from `onAppear` since `@StateObject` init doesn't have access to `@EnvironmentObject`

---

## Checklist for New Purchasely Integrations

- [ ] All SDK calls go through `PurchaselyWrapper` (exception: `PremiumManager`)
- [ ] Screen has zero `io.purchasely` / `import Purchasely` imports
- [ ] Uses `loadPresentation()` + `display()`/`getView()`/`getController()`, never `presentationView()` or `presentationController()`
- [ ] Presentations are prefetched by the ViewModel (Android: `init`, iOS: `onAppear`)
- [ ] Handles all `FetchResult` variants (success, client, deactivated, error)
- [ ] User attributes set from ViewModel, not Screen
- [ ] Modal paywalls: ViewModel prefetches, shows loader while loading, Screen provides Activity/ViewController on display
- [ ] Embedded paywalls: ViewModel prefetches, Screen uses `EmbeddedScreenBanner` with prefetched result/controller
- [ ] Uses `presentation.height` (dp/points) for embedded view sizing
- [ ] No crashes on SDK errors — nothing shown if fetch fails
- [ ] SDK init and interceptor are in PurchaselyWrapper.initialize() — NOT in App class
- [ ] Observer mode purchases flow through PurchaseManager via reactive subjects (not direct wrapper calls)
- [ ] PurchaseManager has zero Purchasely/SDK imports
- [ ] Login/logout, restore, consent, synchronize go through wrapper in ViewModels
- [ ] SDK types (PLYRunningMode, PLYDataProcessingPurpose, etc.) are tolerated as direct imports
- [ ] Tests use MockPurchaselyWrapper (iOS) or mockk (Android) — never the real SDK
