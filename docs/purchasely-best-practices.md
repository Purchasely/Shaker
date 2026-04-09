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
- `loadPresentation()` — fetch a presentation (suspend, uses native suspend API)
- `display()` — show a modal paywall (suspend, wraps callback)
- `getView()` — build an inline view for embedding (returns `View?`)
- `setUserAttribute()` / `incrementUserAttribute()` — user targeting

---

## 2. Presentation Loading: Always fetch then build/display

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

## 3. MVVM Pattern: ViewModel Owns Paywall Logic

**Rule: ViewModels decide when and what to show. Screens only provide the Activity and render the UI.**

**Modal paywall flow:**
1. User action triggers ViewModel method (e.g., `onFilterClick()`)
2. ViewModel checks premium status
3. If not premium, ViewModel calls `wrapper.loadPresentation()` (suspend, in `viewModelScope`)
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
- `onResult` forwards purchase events to the ViewModel

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

**Typed overloads:** The wrapper provides `String`, `Boolean`, `Int`, and `Float` overloads matching the SDK.

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

## 8. Async: Coroutines over Callbacks

**Rule: Use suspend functions for async operations. Only use callbacks when the SDK doesn't provide a suspend alternative.**

- `loadPresentation()` — uses the native suspend `Purchasely.fetchPresentation()`
- `display()` — wraps the callback-based `display(activity)` with `suspendCoroutine`
- `getView()` — keeps callbacks because `buildView()` returns a `View?` synchronously; the callback fires later on purchase events

---

## 9. Platform-Specific Notes

### Android (Kotlin / Jetpack Compose)

- `PurchaselyWrapper` is a Koin singleton (`single { PurchaselyWrapper() }`)
- ViewModels inject it via constructor: `class HomeViewModel(..., private val purchaselyWrapper: PurchaselyWrapper)`
- `EmbeddedScreenBanner` uses `koinInject()` for DI in Composables
- `display()` requires an `Activity` — passed from Screen to ViewModel on-demand
- `buildView()` returns `PLYPresentationView?` (extends `FrameLayout`)

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
