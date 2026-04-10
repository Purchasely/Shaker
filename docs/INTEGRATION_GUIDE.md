# Purchasely SDK Integration Guide

> This guide walks through every Purchasely SDK feature demonstrated in the Shaker sample app.
> Code snippets are taken directly from the app — see the referenced files for full context.
>
> **Architecture note:** Shaker wraps all SDK calls in `PurchaselyWrapper`. ViewModels and Screens never import the Purchasely SDK directly. See `docs/purchasely-best-practices.md` for the full architecture rationale.

## Table of Contents

1. [SDK Initialization](#1-sdk-initialization)
2. [Displaying Paywalls](#2-displaying-paywalls)
3. [Paywall Actions Interceptor](#3-paywall-actions-interceptor)
4. [Observer Mode: Native Purchase Flow](#4-observer-mode-native-purchase-flow)
5. [User Authentication](#5-user-authentication)
6. [Subscription Status](#6-subscription-status)
7. [User Attributes](#7-user-attributes)
8. [Events & Analytics](#8-events--analytics)
9. [Deeplinks](#9-deeplinks)
10. [GDPR & Privacy](#10-gdpr--privacy)
11. [Restore Purchases](#11-restore-purchases)

---

## 1. SDK Initialization

**What it does:** Bootstraps the Purchasely SDK at app launch — configures the API key, running mode, store adapters, and log level. No other SDK method may be called before `start()` completes successfully.

**Architecture:** `PurchaselyWrapper.initialize()` owns the entire init: SDK start, event listener, paywall actions interceptor, and deeplink readiness. The app entry point just calls `initialize()`.

### Android (Kotlin)

Source: `android/app/src/main/java/com/purchasely/shaker/ShakerApp.kt`

```kotlin
// ShakerApp.onCreate() — just Koin init + wrapper.initialize()
val purchaselyWrapper: PurchaselyWrapper by inject()
purchaselyWrapper.initialize(
    application = this,
    apiKey = BuildConfig.PURCHASELY_API_KEY,
    logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.WARN
)
```

Inside `PurchaselyWrapper.initialize()`:

```kotlin
Purchasely.Builder(application)
    .apiKey(apiKey)
    .logLevel(logLevel)
    .readyToOpenDeeplink(true)
    .runningMode(runningModeRepo.runningMode)
    .stores(listOf(GoogleStore()))
    .build()
    .start { isConfigured, error ->
        if (isConfigured) premiumManager.refreshPremiumStatus()
    }

// Event listener and interceptor also configured here
```

### iOS (Swift)

Source: `ios/Shaker/AppViewModel.swift`

```swift
// AppViewModel.init() — just wrapper.initialize() + isSDKReady tracking
wrapper.initialize(
    apiKey: resolvedApiKey,
    appUserId: storedUserId,
    logLevel: sdkLogLevel
) { [weak self] success, error in
    DispatchQueue.main.async {
        self?.isSDKReady = success
        self?.sdkError = success ? nil : error?.localizedDescription
    }
}
```

Inside `PurchaselyWrapper.initialize()`:

```swift
Purchasely.start(withAPIKey: apiKey, appUserId: appUserId,
                  runningMode: selectedMode.runningMode,
                  storekitSettings: .storeKit2, logLevel: logLevel) { success, error in
    if success { PremiumManager.shared.refreshPremiumStatus() }
    onReady(success, error)
}
Purchasely.readyToOpenDeeplink(true)
Purchasely.setEventDelegate(self)
Purchasely.setPaywallActionsInterceptor { [weak self] action, params, info, proceed in
    self?.handlePaywallAction(action: action, parameters: params, info: info, processAction: proceed)
}
```

### Console Setup

1. Create an application in the [Purchasely Console](https://console.purchasely.io) under **Apps**.
2. Copy the API key from the app settings page.
3. Set the key in `local.properties` (Android) or `Config.xcconfig` (iOS) — never commit it.

### Common Pitfalls

- `start()` is **asynchronous**. Calling `userSubscriptions()`, `fetchPresentation()`, or any other SDK method before the completion fires will produce unexpected results.
- On Android, call `initialize()` inside `Application.onCreate()`, not inside an Activity.
- The wrapper's `restart()` method re-initializes the SDK when the running mode changes (Full ↔ Observer).

---

## 2. Displaying Paywalls

**What it does:** The `loadPresentation()` + `display()` two-step pattern fetches a paywall configured in the Purchasely Console for a given **placement**, then renders it modally. A `contentId` can be passed to personalise the paywall for a specific content item (e.g. a cocktail recipe).

Shaker uses four placements:

| Placement ID    | Trigger                                   |
|-----------------|-------------------------------------------|
| `onboarding`    | First launch, before the main UI appears  |
| `recipe_detail` | Tapping a locked recipe (with `contentId`)|
| `favorites`     | Tapping the favorites heart when not premium |
| `filters`       | Tapping the filter button when not premium|

### Android (Kotlin) — via PurchaselyWrapper

Source: `android/app/src/main/java/com/purchasely/shaker/purchasely/PurchaselyWrapper.kt`

```kotlin
// Wrapper provides a suspend API returning type-safe FetchResult
suspend fun loadPresentation(placementId: String, contentId: String? = null): FetchResult
suspend fun display(presentation: PLYPresentation, activity: Activity): DisplayResult
```

ViewModel usage:

```kotlin
// Prefetch in init
viewModelScope.launch {
    _filtersPresentation.value = purchaselyWrapper.loadPresentation("filters")
}

// Display when user taps
val presentation = pendingPresentation ?: return
val result = purchaselyWrapper.display(presentation, activity)
when (result) {
    is DisplayResult.Purchased, is DisplayResult.Restored -> premiumManager.refreshPremiumStatus()
    else -> {}
}
```

### iOS (Swift) — via PurchaselyWrapper

Source: `ios/Shaker/Purchasely/PurchaselyWrapper.swift`

```swift
// Wrapper provides async API with onResult callback for purchase events
@MainActor
func loadPresentation(placementId: String, contentId: String? = nil,
                       onResult: @escaping @MainActor (DisplayResult) -> Void) async -> FetchResult
func display(presentation: PLYPresentation, from viewController: UIViewController?)
```

ViewModel usage:

```swift
// Prefetch
recipeFetchResult = await wrapper.loadPresentation(
    placementId: "recipe_detail", contentId: cocktailId
) { result in
    if case .purchased = result { PremiumManager.shared.refreshPremiumStatus() }
    if case .restored = result { PremiumManager.shared.refreshPremiumStatus() }
}

// Display
guard case .success(let presentation) = recipeFetchResult else { return }
wrapper.display(presentation: presentation, from: viewController)
```

### Console Setup

1. Go to **Placements** and create one placement per trigger point (e.g. `onboarding`, `recipe_detail`).
2. Assign a paywall (screen) to each placement. You can run A/B tests per placement.
3. Activate the placement — a deactivated placement returns `type == .deactivated`, which the app treats as "skip paywall".

### Common Pitfalls

- Always handle `FetchResult.Deactivated` (no-op) and `FetchResult.Error` (log, don't crash).
- On iOS, `presentation.display(from:)` must be called on the **main thread**.
- On Android, pass an `Activity` (not a `Context`) to `display()`.

---

## 3. Paywall Actions Interceptor

**What it does:** Intercepts specific button actions triggered from inside a paywall before they are processed. Use it to implement a custom login flow (`LOGIN` action) or to handle in-paywall navigation links (`NAVIGATE` action).

**Architecture:** The interceptor is configured inside `PurchaselyWrapper.initialize()` — it is **not** exposed to ViewModels. The wrapper handles all actions internally.

### Android (Kotlin)

Source: `android/app/src/main/java/com/purchasely/shaker/purchasely/PurchaselyWrapper.kt` — `handlePaywallAction()`

```kotlin
internal fun handlePaywallAction(info, action, parameters, processAction) {
    when (action) {
        PLYPresentationAction.LOGIN -> processAction(false)
        PLYPresentationAction.NAVIGATE -> {
            parameters?.url?.let { url ->
                val intent = Intent(Intent.ACTION_VIEW, url)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                application?.startActivity(intent)
            }
            processAction(false)
        }
        PLYPresentationAction.PURCHASE -> { /* Observer mode flow — see section 4 */ }
        PLYPresentationAction.RESTORE -> { /* Observer mode flow — see section 4 */ }
        else -> processAction(true)
    }
}
```

### iOS (Swift)

Source: `ios/Shaker/Purchasely/PurchaselyWrapper.swift` — `handlePaywallAction()`

```swift
internal func handlePaywallAction(action, parameters, info, processAction) {
    switch action {
    case .login: processAction(false)
    case .navigate:
        if let url = parameters?.url {
            DispatchQueue.main.async { UIApplication.shared.open(url) }
        }
        processAction(false)
    case .purchase: /* Observer mode flow — see section 4 */
    case .restore: /* Observer mode flow — see section 4 */
    default: processAction(true)
    }
}
```

### Common Pitfalls

- Always call `processAction` (either `true` or `false`). Forgetting to call it will leave the paywall in a frozen state.
- The interceptor is global — set it once during SDK initialization, not inside individual screens.

---

## 4. Observer Mode: Native Purchase Flow

**What it does:** In Observer mode, the app handles purchases natively (Google Play Billing / StoreKit 2) while Purchasely only observes transactions for analytics and paywall display. Shaker uses a **reactive decoupling** pattern where `PurchaseManager` has zero Purchasely SDK imports.

### Architecture

```
PurchaselyWrapper                          PurchaseManager
    │                                           │
    │ Interceptor receives PURCHASE              │
    │   → stores processAction callback          │
    │   → emits PurchaseRequest ──────────────►  │
    │                                           │ Native billing
    │   ◄────────────────────────────────────   │
    │   TransactionResult                        │
    │                                           │
    │ synchronize()                              │
    │ processAction(false)                       │
    │ premiumManager.refreshPremiumStatus()      │
```

### Android (Kotlin)

Source: `android/app/src/main/java/com/purchasely/shaker/data/purchase/`

```kotlin
// Types
data class PurchaseRequest(val activity: Activity, val productId: String, val offerToken: String)
data object RestoreRequest
sealed class TransactionResult { Success, Cancelled, Error(message), Idle }

// PurchaseManager observes SharedFlows (zero Purchasely imports)
class PurchaseManager(
    billingClientFactory: (PurchasesUpdatedListener) -> BillingClient,
    purchaseRequests: SharedFlow<PurchaseRequest>,
    restoreRequests: SharedFlow<RestoreRequest>,
    scope: CoroutineScope
) : PurchasesUpdatedListener {
    val transactionResult: SharedFlow<TransactionResult>
    // Collects requests → launches billing → emits results
}

// PurchaselyWrapper emits requests and observes results
scope.launch {
    purchaseRequests.emit(PurchaseRequest(activity, productId, offerToken))
}
// Collection starts in init{} and restarts on initialize()/restart():
private fun startTransactionCollection() {
    collectionJob?.cancel()
    collectionJob = scope.launch { transactionResult.collect { handleTransactionResult(it) } }
}
```

### iOS (Swift)

Source: `ios/Shaker/Data/purchase/`, `ios/Shaker/Data/PurchaseManager.swift`

```swift
// Types
struct PurchaseRequest { let productId: String }
enum TransactionResult { case success, cancelled, error(String?), idle }

// PurchaseManager uses Combine (zero Purchasely imports)
class PurchaseManager {
    var purchaseSubject = PassthroughSubject<PurchaseRequest, Never>()
    var restoreSubject = PassthroughSubject<Void, Never>()
    let resultSubject = PassthroughSubject<TransactionResult, Never>()
    // Sinks requests → executes StoreKit 2 → sends results
}

// PurchaselyWrapper sends requests and sinks results
PurchaseManager.shared.purchaseSubject.send(PurchaseRequest(productId: productId))
// In init:
PurchaseManager.shared.resultSubject.sink { result in handleTransactionResult(result) }
```

### Common Pitfalls

- In Observer mode, `synchronize()` must be called after every successful purchase so Purchasely can track it. The wrapper handles this automatically on `TransactionResult.Success`.
- The `processAction(false)` callback must be called for **every** outcome (success, cancel, error) — otherwise the paywall freezes.
- `PurchaseManager` must never import or reference the Purchasely SDK — it uses injected closures for `anonymousUserId` and `signPromotionalOffer`.

---

## 5. User Authentication

**What it does:** Associates the current app user with a Purchasely user ID so that subscriptions are correctly attributed and can be restored across devices.

### Android (Kotlin)

Source: `android/app/src/main/java/com/purchasely/shaker/ui/screen/settings/SettingsViewModel.kt`

```kotlin
// Login — via PurchaselyWrapper
purchaselyWrapper.userLogin(userId) { refresh ->
    if (refresh) premiumManager.refreshPremiumStatus()
}
purchaselyWrapper.setUserAttribute("user_id", userId)

// Logout
purchaselyWrapper.userLogout()
premiumManager.refreshPremiumStatus()
```

### iOS (Swift)

Source: `ios/Shaker/Screens/Settings/SettingsViewModel.swift`

```swift
// Login — via PurchaselyWrapping protocol
wrapper.userLogin(userId: userId) { refresh in
    if refresh { PremiumManager.shared.refreshPremiumStatus() }
}
wrapper.setUserAttribute(userId, forKey: "user_id")

// Logout
wrapper.userLogout()
PremiumManager.shared.refreshPremiumStatus()
```

### Common Pitfalls

- The `refresh` flag indicates new subscription data — always call `refreshPremiumStatus()` when `refresh == true`.
- Call `userLogout()` followed by `refreshPremiumStatus()` so the local state reflects the anonymous state.

---

## 6. Subscription Status

**What it does:** Queries the user's active subscriptions from Purchasely's server. Shaker uses `userSubscriptions()` to determine whether the user is premium.

**Note:** `PremiumManager` is the only class that calls the Purchasely SDK directly — it's already an abstraction layer for subscription status.

### Android (Kotlin)

Source: `android/app/src/main/java/com/purchasely/shaker/data/PremiumManager.kt`

```kotlin
Purchasely.userSubscriptions(false, object : SubscriptionsListener {
    override fun onSuccess(subscriptions: List<PLYSubscriptionData>) {
        val premium = subscriptions.any { subscriptionData ->
            subscriptionData.data.subscriptionStatus?.isExpired() == false
        }
        _isPremium.value = premium
    }
    override fun onFailure(error: Throwable) { /* log error */ }
})
```

### iOS (Swift)

Source: `ios/Shaker/Data/PremiumManager.swift`

```swift
Purchasely.userSubscriptions(
    success: { [weak self] subscriptions in
        let premium = subscriptions?.contains { subscription in
            switch subscription.status {
            case .autoRenewing, .inGracePeriod, .autoRenewingCanceled, .onHold: return true
            default: return false
            }
        } ?? false
        DispatchQueue.main.async { self?.isPremium = premium }
    },
    failure: { error in /* log error */ }
)
```

### Subscription Statuses

| Status                  | Meaning                                      | Premium? |
|-------------------------|----------------------------------------------|----------|
| `autoRenewing`          | Active, will renew                           | Yes      |
| `inGracePeriod`         | Billing issue, still has access              | Yes      |
| `autoRenewingCanceled`  | Cancelled but still within paid period       | Yes      |
| `onHold`                | Account on hold (billing issue)              | Yes      |
| `paused`                | Subscription paused by user                  | No       |
| `deactivated`           | Expired or manually revoked                  | No       |
| `revoked`               | Refunded by the store                        | No       |

### Common Pitfalls

- On Android, `subscriptionStatus` is **nullable** and `isExpired()` is a **function**. Use `?.isExpired() == false`.
- On iOS, do not treat `autoRenewingCanceled` as non-premium — the user still has access.

---

## 7. User Attributes

**What it does:** Sends typed key-value attributes to Purchasely for audience segmentation, A/B test targeting, and personalisation.

### Android (Kotlin) — via PurchaselyWrapper

```kotlin
purchaselyWrapper.setUserAttribute("app_theme", "dark")
purchaselyWrapper.setUserAttribute("has_used_search", true)
purchaselyWrapper.incrementUserAttribute("cocktails_viewed")
purchaselyWrapper.setUserAttribute("favorite_spirit", "Rum")
```

### iOS (Swift) — via PurchaselyWrapping protocol

```swift
wrapper.setUserAttribute("dark", forKey: "app_theme")
wrapper.setUserAttribute(true, forKey: "has_used_search")
wrapper.incrementUserAttribute(forKey: "cocktails_viewed")
wrapper.setUserAttribute("Rum", forKey: "favorite_spirit")
```

### Attributes Tracked in Shaker

| Key                  | Type    | Set when                                  |
|----------------------|---------|-------------------------------------------|
| `user_id`            | String  | User logs in                              |
| `app_theme`          | String  | User changes theme in Settings            |
| `has_used_search`    | Boolean | User types in the search bar              |
| `cocktails_viewed`   | Integer | User opens a recipe detail page           |
| `favorite_spirit`    | String  | User opens a recipe (spirit of the recipe)|

### Common Pitfalls

- On iOS, the method name varies by type: `withStringValue:`, `withBoolValue:`, `withIntValue:`. The wrapper unifies this behind `setUserAttribute(_:forKey:)` overloads.
- `incrementUserAttribute` increments from the last known server value — avoid calling it multiple times for the same event.

---

## 8. Events & Analytics

**What it does:** The SDK emits named `PLYEvent` objects at key points. The event listener/delegate is configured inside `PurchaselyWrapper.initialize()`.

### Android (Kotlin)

Source: `android/app/src/main/java/com/purchasely/shaker/purchasely/PurchaselyWrapper.kt`

```kotlin
// Inside initialize()
eventListener = object : EventListener {
    override fun onEvent(event: PLYEvent) {
        Log.d(TAG, "Event: ${event.name} | Properties: ${event.properties}")
    }
}
```

### iOS (Swift)

Source: `ios/Shaker/Purchasely/PurchaselyWrapper.swift`

```swift
// Inside initialize()
Purchasely.setEventDelegate(self)

// PurchaselyWrapper conforms to PLYEventDelegate
extension PurchaselyWrapper: PLYEventDelegate {
    func eventTriggered(_ event: PLYEvent, properties: [String: Any]?) {
        print("Event: \(event.name) | Properties: \(properties ?? [:])")
    }
}
```

### Common Pitfalls

- Set the listener **after** `start()` but in the same initialization block.
- On iOS, `properties` is nullable — do not force-unwrap it.

---

## 9. Deeplinks

**What it does:** Allows Purchasely paywalls to be opened directly from a URL. `readyToOpenDeeplink(true)` is called inside `PurchaselyWrapper.initialize()`.

### Android (Kotlin)

Source: `android/app/src/main/java/com/purchasely/shaker/purchasely/PurchaselyWrapper.kt`

```kotlin
// Inside initialize() — readyToOpenDeeplink is configured in the Builder
Purchasely.Builder(application)
    .readyToOpenDeeplink(true)
    // ...

// Handle incoming intent via wrapper
fun isDeeplinkHandled(deeplink: Uri, activity: Activity?): Boolean
```

### iOS (Swift)

Source: `ios/Shaker/Purchasely/PurchaselyWrapper.swift`

```swift
// Inside initialize()
Purchasely.readyToOpenDeeplink(true)

// Handle incoming URL
@discardableResult
func isDeeplinkHandled(deeplink: URL) -> Bool
```

### Common Pitfalls

- `readyToOpenDeeplink(true)` must be called **after** `start()` fires.
- `isDeeplinkHandled` returns a `Bool` — check it for fallback routing.

---

## 10. GDPR & Privacy

**What it does:** `revokeDataProcessingConsent(for:)` tells the SDK which data processing purposes the user has opted out of. Shaker exposes five toggles in Settings.

### Android (Kotlin) — via PurchaselyWrapper

Source: `android/app/src/main/java/com/purchasely/shaker/ui/screen/settings/SettingsViewModel.kt`

```kotlin
val revoked = mutableSetOf<PLYDataProcessingPurpose>()
if (!analyticsConsent.value) revoked.add(PLYDataProcessingPurpose.Analytics)
if (!personalizationConsent.value) revoked.add(PLYDataProcessingPurpose.Personalization)
// ...
purchaselyWrapper.revokeDataProcessingConsent(revoked)
```

### iOS (Swift) — via PurchaselyWrapping protocol

Source: `ios/Shaker/Screens/Settings/SettingsViewModel.swift`

```swift
var revoked = Set<PLYDataProcessingPurpose>()
if !analyticsConsent { revoked.insert(.analytics) }
if !personalizationConsent { revoked.insert(.personalization) }
// ...
wrapper.revokeDataProcessingConsent(for: revoked)
```

### Data Processing Purposes

| Purpose                   | Controls                                          |
|---------------------------|---------------------------------------------------|
| `Analytics`               | Anonymous usage analytics                         |
| `IdentifiedAnalytics`     | Analytics tied to user identity                   |
| `Personalization`         | Personalised paywall content                      |
| `Campaigns`               | Win-back and retention campaigns                  |
| `ThirdPartyIntegrations`  | Data forwarding to third-party tools              |

### Common Pitfalls

- Pass the **revoked** set (opted-out purposes), not the granted set.
- Call `applyConsentPreferences()` both on init and on toggle changes.

---

## 11. Restore Purchases

**What it does:** Triggers a server-side restore of all purchases. In Full mode, goes through the wrapper. In Observer mode, the reactive flow handles it automatically via `PurchaseManager`.

### Android (Kotlin) — Full mode via PurchaselyWrapper

Source: `android/app/src/main/java/com/purchasely/shaker/ui/screen/settings/SettingsViewModel.kt`

```kotlin
purchaselyWrapper.restoreAllProducts(
    onSuccess = { plan ->
        premiumManager.refreshPremiumStatus()
        _restoreMessage.value = "Purchases restored successfully!"
    },
    onError = { error ->
        _restoreMessage.value = error?.message ?: "No purchases to restore"
    }
)
```

### iOS (Swift) — Full mode via PurchaselyWrapping protocol

Source: `ios/Shaker/Screens/Settings/SettingsViewModel.swift`

```swift
wrapper.restoreAllProducts(
    success: { [weak self] in
        PremiumManager.shared.refreshPremiumStatus()
        DispatchQueue.main.async { self?.restoreMessage = "Purchases restored successfully!" }
    },
    failure: { [weak self] error in
        DispatchQueue.main.async { self?.restoreMessage = error.localizedDescription }
    }
)
```

### Observer mode

In Observer mode, restore from the paywall interceptor flows through the reactive architecture (Section 4): the wrapper emits `RestoreRequest`, `PurchaseManager` queries native transactions, and `TransactionResult` flows back for `synchronize()` + `processAction()`.

### Common Pitfalls

- On iOS, the `success` closure does **not** receive a `plan` parameter (unlike Android).
- Always call `refreshPremiumStatus()` inside the success handler.
