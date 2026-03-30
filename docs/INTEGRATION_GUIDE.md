# Purchasely SDK Integration Guide

> This guide walks through every Purchasely SDK feature demonstrated in the Shaker sample app.
> Code snippets are taken directly from the app — see the referenced files for full context.

## Table of Contents

1. [SDK Initialization](#1-sdk-initialization)
2. [Displaying Paywalls](#2-displaying-paywalls)
3. [Paywall Actions Interceptor](#3-paywall-actions-interceptor)
4. [User Authentication](#4-user-authentication)
5. [Subscription Status](#5-subscription-status)
6. [User Attributes](#6-user-attributes)
7. [Events & Analytics](#7-events--analytics)
8. [Deeplinks](#8-deeplinks)
9. [GDPR & Privacy](#9-gdpr--privacy)
10. [Restore Purchases](#10-restore-purchases)

---

## 1. SDK Initialization

**What it does:** Bootstraps the Purchasely SDK at app launch — configures the API key, running mode, store adapters, and log level. No other SDK method may be called before `start()` completes successfully.

### Android (Kotlin)

Source: `android/app/src/main/java/com/purchasely/shaker/ShakerApp.kt`

```kotlin
Purchasely.Builder(this)
    .apiKey("YOUR_API_KEY")
    .logLevel(LogLevel.DEBUG)
    .readyToOpenDeeplink(true)
    .runningMode(PLYRunningMode.Full)
    .stores(listOf(GoogleStore()))
    .build()
    .start { isConfigured, error ->
        if (isConfigured) {
            premiumManager.refreshPremiumStatus()
        }
        error?.let { Log.e(TAG, "SDK error: ${it.message}") }
    }
```

### iOS (Swift)

Source: `ios/Shaker/AppViewModel.swift`

```swift
Purchasely.start(
    withAPIKey: "YOUR_API_KEY",
    appUserId: nil,
    runningMode: .full,
    storekitSettings: .storeKit2,
    logLevel: .debug
) { [weak self] success, error in
    DispatchQueue.main.async {
        self?.isSDKReady = success
        if success {
            PremiumManager.shared.refreshPremiumStatus()
        }
    }
}

Purchasely.readyToOpenDeeplink(true)
```

### Console Setup

1. Create an application in the [Purchasely Console](https://console.purchasely.io) under **Apps**.
2. Copy the API key from the app settings page.
3. Set the key in `local.properties` (Android) or `Config.xcconfig` (iOS) — never commit it.

### Common Pitfalls

- `start()` is **asynchronous**. Calling `userSubscriptions()`, `fetchPresentation()`, or any other SDK method before the completion fires will produce unexpected results.
- On Android, call `start()` inside `Application.onCreate()`, not inside an Activity, so the SDK is ready before any screen is shown.

---

## 2. Displaying Paywalls

**What it does:** The `fetchPresentation()` + `display()` two-step pattern fetches a paywall configured in the Purchasely Console for a given **placement**, then renders it modally. A `contentId` can be passed to personalise the paywall for a specific content item (e.g. a cocktail recipe).

Shaker uses four placements:

| Placement ID    | Trigger                                   |
|-----------------|-------------------------------------------|
| `onboarding`    | First launch, before the main UI appears  |
| `recipe_detail` | Tapping a locked recipe (with `contentId`)|
| `favorites`     | Tapping the favorites heart when not premium |
| `filters`       | Tapping the filter button when not premium|

### Android (Kotlin) — basic placement

Source: `android/app/src/main/java/com/purchasely/shaker/ui/screen/home/HomeScreen.kt`

```kotlin
Purchasely.fetchPresentation("filters") { presentation, error ->
    if (presentation != null && presentation.type != PLYPresentationType.DEACTIVATED) {
        presentation.display(activity) { result, plan ->
            when (result) {
                PLYProductViewResult.PURCHASED,
                PLYProductViewResult.RESTORED -> premiumManager.refreshPremiumStatus()
                else -> {}
            }
        }
    }
}
```

### Android (Kotlin) — placement with `contentId`

Source: `android/app/src/main/java/com/purchasely/shaker/ui/screen/detail/DetailScreen.kt`

```kotlin
val properties = PLYPresentationProperties(contentId = cocktailId)
Purchasely.fetchPresentation("recipe_detail", properties) { presentation, error ->
    if (presentation != null && presentation.type != PLYPresentationType.DEACTIVATED) {
        presentation.display(activity) { result, plan -> /* handle result */ }
    }
}
```

### iOS (Swift) — basic placement

Source: `ios/Shaker/Screens/Home/HomeScreen.swift`

```swift
Purchasely.fetchPresentation(
    for: "filters",
    fetchCompletion: { presentation, error in
        guard let presentation = presentation, presentation.type != .deactivated else { return }
        DispatchQueue.main.async {
            presentation.display(from: viewController)
        }
    },
    completion: { result, plan in
        switch result {
        case .purchased, .restored:
            PremiumManager.shared.refreshPremiumStatus()
        default: break
        }
    }
)
```

### iOS (Swift) — placement with `contentId`

Source: `ios/Shaker/Screens/Detail/DetailViewModel.swift`

```swift
Purchasely.fetchPresentation(
    for: "recipe_detail",
    contentId: cocktailId,
    fetchCompletion: { presentation, error in
        guard let presentation = presentation, presentation.type != .deactivated else { return }
        DispatchQueue.main.async { presentation.display(from: vc) }
    },
    completion: { result, plan in
        if result == .purchased || result == .restored {
            PremiumManager.shared.refreshPremiumStatus()
        }
    }
)
```

### Console Setup

1. Go to **Placements** and create one placement per trigger point (e.g. `onboarding`, `recipe_detail`).
2. Assign a paywall (screen) to each placement. You can run A/B tests per placement.
3. Activate the placement — a deactivated placement returns `type == .deactivated`, which the app treats as "skip paywall".

### Common Pitfalls

- Always guard against `presentation.type == .deactivated` (or `PLYPresentationType.DEACTIVATED` on Android). This is the normal state when the placement has no active paywall — treat it as a no-op, not an error.
- On iOS, `presentation.display(from:)` must be called on the **main thread**. Wrap it in `DispatchQueue.main.async` if `fetchCompletion` fires on a background thread.
- On Android, pass an `Activity` (not a `Context`) to `presentation.display()`. Cast `LocalContext.current as? Activity` inside Composables.

---

## 3. Paywall Actions Interceptor

**What it does:** Intercepts specific button actions triggered from inside a paywall before they are processed. Use it to implement a custom login flow (`LOGIN` action) or to handle in-paywall navigation links (`NAVIGATE` action). Call `proceed(true)` to let the SDK handle the action, or `proceed(false)` to suppress the default behaviour.

### Android (Kotlin)

Source: `android/app/src/main/java/com/purchasely/shaker/ShakerApp.kt`

```kotlin
Purchasely.setPaywallActionsInterceptor { info, action, parameters, proceed ->
    when (action) {
        PLYPresentationAction.LOGIN -> {
            // Dismiss paywall; user navigates to Settings to log in
            proceed(false)
        }
        PLYPresentationAction.NAVIGATE -> {
            val url = parameters?.url
            if (url != null) {
                val intent = Intent(Intent.ACTION_VIEW, url)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            proceed(false)
        }
        else -> proceed(true)
    }
}
```

### iOS (Swift)

Source: `ios/Shaker/AppViewModel.swift`

```swift
Purchasely.setPaywallActionsInterceptor { action, parameters, info, proceed in
    switch action {
    case .login:
        proceed(false)
    case .navigate:
        if let url = parameters?.url {
            DispatchQueue.main.async { UIApplication.shared.open(url) }
        }
        proceed(false)
    default:
        proceed(true)
    }
}
```

### Console Setup

No special console configuration is required. Add **Login** or **Navigate** buttons to your paywall in the Screen Composer and assign the corresponding action. The interceptor fires automatically when the user taps them.

### Common Pitfalls

- The interceptor is a global singleton — set it once during SDK initialization, not inside individual screens.
- Always call `proceed` (either `true` or `false`). Forgetting to call it will leave the paywall in a frozen state.

---

## 4. User Authentication

**What it does:** Associates the current app user with a Purchasely user ID so that subscriptions are correctly attributed and can be restored across devices. `userLogin()` optionally triggers a subscription refresh via the `refresh` callback. `userLogout()` clears the association.

### Android (Kotlin)

Source: `android/app/src/main/java/com/purchasely/shaker/ui/screen/settings/SettingsViewModel.kt`

```kotlin
// Login
Purchasely.userLogin(userId) { refresh ->
    if (refresh) {
        premiumManager.refreshPremiumStatus()
    }
}

// Logout
Purchasely.userLogout()
premiumManager.refreshPremiumStatus()
```

### iOS (Swift)

Source: `ios/Shaker/Screens/Settings/SettingsViewModel.swift`

```swift
// Login
Purchasely.userLogin(with: userId) { refresh in
    if refresh {
        PremiumManager.shared.refreshPremiumStatus()
    }
}

// Logout
Purchasely.userLogout()
PremiumManager.shared.refreshPremiumStatus()
```

### Console Setup

No special console configuration is required. User IDs appear in the **Users** tab once `userLogin()` has been called at least once.

### Common Pitfalls

- The `refresh` flag in the `userLogin` callback indicates that the SDK detected new subscription data for this user. Always call `refreshPremiumStatus()` when `refresh == true`.
- Call `userLogout()` followed by `refreshPremiumStatus()` so that the local premium state reflects the anonymous (logged-out) state immediately.

---

## 5. Subscription Status

**What it does:** Queries the user's active subscriptions from Purchasely's server. Shaker uses `userSubscriptions()` to determine whether the user is premium, driving paywall gating throughout the app.

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

    override fun onFailure(error: Throwable) {
        Log.e(TAG, "Error checking premium: ${error.message}")
    }
})
```

The first parameter (`false`) controls cache invalidation. Pass `true` to force a fresh server request.

### iOS (Swift)

Source: `ios/Shaker/Data/PremiumManager.swift`

```swift
Purchasely.userSubscriptions(
    success: { [weak self] subscriptions in
        let premium = subscriptions?.contains { subscription in
            switch subscription.status {
            case .autoRenewing, .inGracePeriod, .autoRenewingCanceled, .onHold:
                return true
            default:
                return false
            }
        } ?? false

        DispatchQueue.main.async {
            self?.isPremium = premium
        }
    },
    failure: { error in
        print("Error checking premium: \(error.localizedDescription)")
    }
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

### Console Setup

Subscription statuses are driven by receipt validation — no manual setup is needed. Ensure your in-app products are configured in the **Products** section of the console and mapped to a Purchasely plan.

### Common Pitfalls

- On Android, `subscriptionStatus` is **nullable** and `isExpired()` is a **function** (not a property). Use `?.isExpired() == false` rather than `!isExpired`.
- On iOS, do not treat `autoRenewingCanceled` as non-premium — the user still has access until the billing period ends.

---

## 6. User Attributes

**What it does:** Sends typed key-value attributes to Purchasely for use in audience segmentation, A/B test targeting, and personalisation. Shaker tracks search usage, cocktail view counts, theme preference, and favourite spirit.

### Android (Kotlin)

Sources: `SettingsViewModel.kt`, `HomeViewModel.kt`, `DetailViewModel.kt`

```kotlin
// String attribute
Purchasely.setUserAttribute("user_id", userId)
Purchasely.setUserAttribute("app_theme", "dark")
Purchasely.setUserAttribute("favorite_spirit", "Rum")

// Boolean attribute
Purchasely.setUserAttribute("has_used_search", true)

// Increment a counter
Purchasely.incrementUserAttribute("cocktails_viewed")
```

### iOS (Swift)

Sources: `SettingsViewModel.swift`, `HomeViewModel.swift`, `DetailViewModel.swift`

```swift
// String attribute
Purchasely.setUserAttribute(withStringValue: userId, forKey: "user_id")
Purchasely.setUserAttribute(withStringValue: "dark", forKey: "app_theme")
Purchasely.setUserAttribute(withStringValue: "Rum", forKey: "favorite_spirit")

// Boolean attribute
Purchasely.setUserAttribute(withBoolValue: true, forKey: "has_used_search")

// Increment a counter
Purchasely.incrementUserAttribute(withKey: "cocktails_viewed")
```

### Attributes Tracked in Shaker

| Key                  | Type    | Set when                                  |
|----------------------|---------|-------------------------------------------|
| `user_id`            | String  | User logs in                              |
| `app_theme`          | String  | User changes theme in Settings            |
| `has_used_search`    | Boolean | User types in the search bar              |
| `cocktails_viewed`   | Integer | User opens a recipe detail page           |
| `favorite_spirit`    | String  | User opens a recipe (spirit of the recipe)|

### Console Setup

User attributes are available in **Audiences** for targeting and in the **A/B Test** configuration. No pre-registration is required — attributes are created automatically on first use.

### Common Pitfalls

- On iOS, the method name varies by type: `withStringValue:`, `withBoolValue:`, `withIntValue:`. There is no single generic method.
- `incrementUserAttribute` increments from the last known server value — it is not a local counter. Avoid calling it multiple times for the same event in a single session.

---

## 7. Events & Analytics

**What it does:** The SDK emits named `PLYEvent` objects at key points in the subscription lifecycle (paywall shown, purchase started, purchase completed, etc.). Register a listener/delegate to log these events or forward them to your analytics stack.

### Android (Kotlin)

Source: `android/app/src/main/java/com/purchasely/shaker/ShakerApp.kt`

```kotlin
Purchasely.eventListener = object : EventListener {
    override fun onEvent(event: PLYEvent) {
        Log.d(TAG, "Event: ${event.name} | Properties: ${event.properties}")
    }
}
```

### iOS (Swift)

Source: `ios/Shaker/AppViewModel.swift`

```swift
// Register during SDK init
Purchasely.setEventDelegate(self)

// Implement PLYEventDelegate
extension AppViewModel: PLYEventDelegate {
    func eventTriggered(_ event: PLYEvent, properties: [String: Any]?) {
        print("Event: \(event.name) | Properties: \(properties ?? [:])")
    }
}
```

Note: the `properties` parameter is **optional** (`[String: Any]?`). Always handle the `nil` case.

### Console Setup

No console configuration is required to receive events. To forward events to third-party analytics tools (Amplitude, Mixpanel, etc.) automatically, configure **Integrations** in the Purchasely Console.

### Common Pitfalls

- On Android, assign `eventListener` **after** calling `start()` but in the same initialization block — events fired during SDK startup may otherwise be missed.
- On iOS, `properties` is nullable — do not force-unwrap it.

---

## 8. Deeplinks

**What it does:** Allows Purchasely paywalls to be opened directly from a URL (e.g. from a push notification, web link, or QR code). `readyToOpenDeeplink(true)` signals that the app is ready to handle deeplinks. `isDeeplinkHandled(deeplink:)` processes an incoming URL and returns `true` if the SDK consumed it.

### Android (Kotlin)

Source: `android/app/src/main/java/com/purchasely/shaker/ShakerApp.kt`

```kotlin
// During SDK init — enable deeplink handling
Purchasely.Builder(this)
    // ...
    .readyToOpenDeeplink(true)
    .build()
    .start { /* ... */ }
```

To handle an incoming intent in your Activity:

```kotlin
val uri: Uri? = intent.data
if (uri != null) {
    Purchasely.isDeeplinkHandled(uri)
}
```

### iOS (Swift)

Source: `ios/Shaker/ShakerApp.swift`

```swift
// Enable deeplink readiness during SDK init (AppViewModel.swift)
Purchasely.readyToOpenDeeplink(true)

// Handle incoming URLs at the app entry point
WindowGroup {
    ContentView()
        .onOpenURL { url in
            _ = Purchasely.isDeeplinkHandled(deeplink: url)
        }
}
```

### Console Setup

1. Go to **Campaigns** or **Push Notifications** and generate a deeplink URL pointing to a specific placement or paywall.
2. Configure your app's URL scheme (iOS: add it to `Info.plist`; Android: add an `<intent-filter>` in `AndroidManifest.xml`).

### Common Pitfalls

- `readyToOpenDeeplink(true)` must be called **after** `start()` fires (or at least after the SDK starts initializing). In Shaker it is called in the same `initPurchasely()` block immediately after `start()`.
- `isDeeplinkHandled` returns a `Bool` — check it if you need to apply your own fallback routing for URLs the SDK did not recognise.

---

## 9. GDPR & Privacy

**What it does:** `revokeDataProcessingConsent(for:)` tells the SDK which data processing purposes the user has opted out of. The SDK stops the corresponding data flows immediately. Shaker exposes five toggles in Settings, one per purpose, persisted to `UserDefaults` / `SharedPreferences`.

### Android (Kotlin)

Source: `android/app/src/main/java/com/purchasely/shaker/ui/screen/settings/SettingsViewModel.kt`

```kotlin
private fun applyConsentPreferences() {
    val revoked = mutableSetOf<PLYDataProcessingPurpose>()
    if (!analyticsConsent.value)           revoked.add(PLYDataProcessingPurpose.Analytics)
    if (!identifiedAnalyticsConsent.value) revoked.add(PLYDataProcessingPurpose.IdentifiedAnalytics)
    if (!personalizationConsent.value)     revoked.add(PLYDataProcessingPurpose.Personalization)
    if (!campaignsConsent.value)           revoked.add(PLYDataProcessingPurpose.Campaigns)
    if (!thirdPartyConsent.value)          revoked.add(PLYDataProcessingPurpose.ThirdPartyIntegrations)
    Purchasely.revokeDataProcessingConsent(revoked)
}
```

### iOS (Swift)

Source: `ios/Shaker/Screens/Settings/SettingsViewModel.swift`

```swift
private func applyConsentPreferences() {
    var revoked = Set<PLYDataProcessingPurpose>()
    if !analyticsConsent           { revoked.insert(.analytics) }
    if !identifiedAnalyticsConsent { revoked.insert(.identifiedAnalytics) }
    if !personalizationConsent     { revoked.insert(.personalization) }
    if !campaignsConsent           { revoked.insert(.campaigns) }
    if !thirdPartyConsent          { revoked.insert(.thirdPartyIntegrations) }
    Purchasely.revokeDataProcessingConsent(for: revoked)
}
```

### Data Processing Purposes

| Purpose                   | Controls                                          |
|---------------------------|---------------------------------------------------|
| `Analytics`               | Anonymous usage analytics                         |
| `IdentifiedAnalytics`     | Analytics tied to user identity                   |
| `Personalization`         | Personalised paywall content                      |
| `Campaigns`               | Win-back and retention campaigns                  |
| `ThirdPartyIntegrations`  | Data forwarding to third-party tools              |

### Console Setup

No console configuration is required. If your app is subject to GDPR or other privacy regulations, surface these toggles to users in your Settings / Privacy screen.

### Common Pitfalls

- `applyConsentPreferences()` is called both in `init` (to restore persisted consent) and whenever a toggle changes. Do not forget the `init` call, or revoked consents will be silently reset on every app launch.
- Pass the **revoked** set (purposes the user has opted out of), not the granted set.

---

## 10. Restore Purchases

**What it does:** Triggers a server-side restore of all purchases associated with the current App Store / Google Play account. On success, refreshes the local premium status. Use this to handle the case where a user reinstalls the app or logs in on a new device.

### Android (Kotlin)

Source: `android/app/src/main/java/com/purchasely/shaker/ui/screen/settings/SettingsViewModel.kt`

```kotlin
Purchasely.restoreAllProducts(
    onSuccess = { plan ->
        premiumManager.refreshPremiumStatus()
        _restoreMessage.value = "Purchases restored successfully!"
    },
    onError = { error ->
        _restoreMessage.value = error?.message ?: "No purchases to restore"
    }
)
```

### iOS (Swift)

Source: `ios/Shaker/Screens/Settings/SettingsViewModel.swift`

```swift
Purchasely.restoreAllProducts(
    success: { [weak self] in
        PremiumManager.shared.refreshPremiumStatus()
        DispatchQueue.main.async {
            self?.restoreMessage = "Purchases restored successfully!"
        }
    },
    failure: { [weak self] error in
        DispatchQueue.main.async {
            self?.restoreMessage = error.localizedDescription
        }
    }
)
```

### Console Setup

No special console configuration is required. Apple and Google impose their own UX requirements: on iOS, Apple requires an accessible **Restore Purchases** button, typically placed in Settings or on the paywall itself.

### Common Pitfalls

- On iOS, the `success` closure does **not** receive a `plan` parameter (unlike Android). Both platforms use separate `success`/`failure` closures — not a single completion handler.
- Always call `refreshPremiumStatus()` inside the success handler so that gated UI updates immediately without requiring a screen refresh.
