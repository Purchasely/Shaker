# Purchasely SDK - Complete Feature Inventory

> Generated 2026-03-30 from Context7 docs + local platform guides (`Documentation/platform/android.md`, `Documentation/platform/ios.md`)

---

## 1. SDK Initialization

| Feature | Android API | iOS API |
|---------|------------|---------|
| Start SDK | `Purchasely.Builder(ctx).apiKey().build().start {}` | `Purchasely.start(withAPIKey:appUserId:runningMode:storekitSettings:logLevel:) {}` |
| Set user ID at init | `.userId("id")` on Builder | `appUserId:` parameter |
| Set log level | `.logLevel(LogLevel.DEBUG)` | `logLevel: .debug` |
| Set running mode | `.runningMode(PLYRunningMode.Full / .PaywallObserver)` | `runningMode: .full / .paywallObserver` |
| Set stores | `.stores(listOf(GoogleStore(), HuaweiStore(), AmazonStore()))` | N/A (Apple only) |
| StoreKit version (iOS only) | N/A | `storekitSettings: .storeKit1 / .storeKit2` |

### Running Modes
- **Full** - Purchasely owns the entire purchase flow
- **PaywallObserver** - Purchasely displays paywalls, you handle purchases with your own infrastructure

### Supported Stores
- **Android**: Google Play (`GoogleStore`), Huawei (`HuaweiStore`), Amazon (`AmazonStore`)
- **iOS**: Apple App Store (StoreKit 1 or StoreKit 2)

---

## 2. Displaying Paywalls / In-App Experiences

### 2a. Fetch + Display (Recommended Pattern)

| Feature | Android API | iOS API |
|---------|------------|---------|
| Fetch presentation | `Purchasely.fetchPresentation(placementId, contentId) { presentation, error -> }` | `Purchasely.fetchPresentation(for:, fetchCompletion:, completion:, loadedCompletion:)` |
| Display fetched presentation | `presentation.display(activity)` | `presentation.display(from: viewController)` |
| Get ViewController/Fragment | `presentation.getFragment { result, plan -> }` | `presentation.controller` (UIViewController) |
| Build view (Android) | `presentation.buildView(context, onClose = {}) { result, plan -> }` | N/A |
| Cache and display later | Store `PLYPresentation` reference, call `display()` when needed | Store `PLYPresentation` reference, call `display(from:)` when needed |

### 2b. Direct Display (Convenience)

| Feature | Android API | iOS API |
|---------|------------|---------|
| Display placement | `Purchasely.presentationView(context, PLYPresentationProperties(placementId, contentId, onClose))` | `Purchasely.presentationController(for:, contentId:, completion:)` |
| Display with loading handler | N/A | `Purchasely.presentationController(for:, contentId:, loaded:, completion:)` |

### 2c. Nested/Embedded Views

| Feature | Android API | iOS API |
|---------|------------|---------|
| Embed in layout | `PLYPresentationView` added to `FrameLayout` | UIViewController embedding |
| Jetpack Compose | `AndroidView { Purchasely.presentationView(...) }` | N/A |
| SwiftUI | N/A | `UIViewControllerRepresentable` wrapper |

### 2d. Presentation Types (`PLYPresentationType`)

| Type | Description |
|------|-------------|
| `NORMAL` / `.normal` | Standard paywall - display it |
| `FALLBACK` / `.fallback` | Fallback paywall (network error occurred) - display it |
| `DEACTIVATED` / `.deactivated` | Placement is deactivated - do NOT show anything |
| `CLIENT` / `.client` | Custom/client paywall - display your own UI. Access `presentation.id` and `presentation.plans` |

### 2e. Display Modes / Transitions (Android)

| Type | Description |
|------|-------------|
| `PLYTransitionType.PUSH` | Push transition |
| `PLYTransitionType.FULLSCREEN` | Fullscreen transition |
| `PLYTransitionType.MODAL` | Modal transition |
| `PLYTransitionType.DRAWER` | Drawer with `heightPercentage` |
| `PLYTransitionType.POPIN` | Pop-in with `heightPercentage` |

### 2f. Purchase Results

| Result | Android | iOS |
|--------|---------|-----|
| Purchased | `PLYProductViewResult.PURCHASED` | `.purchased` |
| Restored | `PLYProductViewResult.RESTORED` | `.restored` |
| Cancelled | `PLYProductViewResult.CANCELLED` | `.cancelled` |

### 2g. Close Presentation

| Platform | Behavior |
|----------|----------|
| Android | Must manually handle `onClose` callback and remove view from layout |
| iOS | Auto-closes after purchase/restore. Manual: `paywallCtrl?.dismiss(animated: true)` |

### 2h. Content ID

Both platforms support an optional `contentId` parameter to associate specific content with a purchase (e.g., a specific article or feature being gated).

---

## 3. Paywall Action Interceptor

### Setup

| Android | iOS |
|---------|-----|
| `Purchasely.setPaywallActionsInterceptor { info, action, parameters, processAction -> }` | `Purchasely.setPaywallActionsInterceptor { action, parameters, presentationInfos, proceed in }` |

### Interceptable Actions (`PLYPresentationAction`)

| Action | Description |
|--------|-------------|
| `PURCHASE` / `.purchase` | User tapped a purchase button |
| `RESTORE` / `.restore` | User tapped restore button |
| `LOGIN` / `.login` | User tapped login button |
| `CLOSE` / `.close` | User tapped close button |
| `OPEN_PRESENTATION` / `.openPresentation` | User tapped button to open another presentation |
| `PROMO_CODE` / `.promoCode` | User wants to enter a promo code |

### Parameters Available in Interceptor

- `parameters.plan` - The PLYPlan user selected
  - `.name` - Plan name
  - `.appleProductId` (iOS) / `.store_product_id` / `.productId` (Android) - Store product ID
  - `.basePlanId` (Android) - Google Play base plan ID
- `parameters.offer` / `parameters.subscriptionOffer` - Selected offer/subscription option
- `info.activity` (Android) / `presentationInfos` (iOS) - UI context

### Key Pattern: `processAction(true/false)`

- `processAction(true)` / `proceed(true)` - Let Purchasely handle the action
- `processAction(false)` / `proceed(false)` - You handled it yourself (e.g., RevenueCat purchase)

---

## 4. User Identification

| Feature | Android API | iOS API |
|---------|------------|---------|
| Login | `Purchasely.userLogin("userId") { refresh -> }` | `Purchasely.userLogin(with: "userId") { refresh in }` |
| Logout | `Purchasely.userLogout()` | `Purchasely.userLogout()` |
| Get anonymous ID | `Purchasely.anonymousUserId` | `Purchasely.anonymousUserId` |

The `refresh` callback on login indicates the user has subscriptions from a previous device/install - refresh local entitlements.

---

## 5. Subscription Status & Entitlements

| Feature | Android API | iOS API |
|---------|------------|---------|
| Get subscriptions | `Purchasely.userSubscriptions(onSuccess: { subscriptions -> }, onError: { error -> })` | `Purchasely.userSubscriptions { subscriptions, error in }` (also `success:, failure:` variant) |
| Check entitlement | `subscription.plan?.hasEntitlement("entitlement_id")` | `subscription.plan?.hasEntitlement("entitlement_id")` |

### Subscription Object Properties

- `subscription.plan?.name` - Plan name
- `subscription.product?.name` - Product name
- `subscription.subscriptionSource?.nextRenewalDate` - Next renewal date
- `subscription.status` (iOS) - `PLYSubscriptionStatus` enum

### PLYSubscriptionStatus (iOS)

| Status | Description |
|--------|-------------|
| `.autoRenewing` | Active and will renew |
| `.onHold` | On hold |
| `.inGracePeriod` | In grace period |
| `.autoRenewingCanceled` | Active but will not renew |
| `.deactivated` | Deactivated |
| `.revoked` | Revoked |
| `.paused` | Paused |
| `.unpaid` | Unpaid |
| `.unknown` | Unknown |

---

## 6. Custom User Attributes

| Feature | Android API | iOS API |
|---------|------------|---------|
| Set string | `Purchasely.setUserAttribute("key", "value")` | `Purchasely.setUserAttribute(withStringValue: "value", forKey: "key")` |
| Set int | `Purchasely.setUserAttribute("key", 25)` | `Purchasely.setUserAttribute(withIntValue: 25, forKey: "key")` |
| Set float | `Purchasely.setUserAttribute("key", 4.5f)` | `Purchasely.setUserAttribute(withDoubleValue: 4.5, forKey: "key")` |
| Set boolean | `Purchasely.setUserAttribute("key", true)` | `Purchasely.setUserAttribute(withBoolValue: true, forKey: "key")` |
| Set date | `Purchasely.setUserAttribute("key", Date())` | `Purchasely.setUserAttribute(withDateValue: Date(), forKey: "key")` |
| Increment | `Purchasely.incrementUserAttribute("key")` / `Purchasely.incrementUserAttribute("key", 10)` | `Purchasely.incrementUserAttribute(for: "key")` / `Purchasely.incrementUserAttribute(for: "key", by: 10)` |
| Clear single | `Purchasely.clearUserAttribute("key")` | `Purchasely.clearUserAttribute(for: "key")` |
| Get all | N/A documented | `Purchasely.userAttributes()` -> `[String: Any]` |

### Supported Attribute Types
- String, Int, Float, Bool, Date, Array of Strings

---

## 7. Deeplinks Management

| Feature | Android API | iOS API |
|---------|------------|---------|
| Enable deeplink display | `Purchasely.readyToOpenDeeplink = true` | `Purchasely.readyToOpenDeeplink(true)` |
| Check if handled | `Purchasely.isDeeplinkHandled(uri)` | N/A documented |
| Set default result handler | `Purchasely.setDefaultPresentationResultHandler { result, plan -> }` | `Purchasely.setDefaultPresentationResultHandler { result, plan in }` |

### Supported Deeplink Schemes
- `purchasely://`
- `https://` (with Purchasely domain)

**Important**: The default result handler is required to capture purchase results from deeplink-opened paywalls, since the standard closure callbacks are not invoked.

---

## 8. Event Listeners / Analytics

### SDK/UI Events

| Android | iOS |
|---------|-----|
| `Purchasely.setEventListener(object : EventListener { override fun onEvent(event: PLYEvent) {} })` | `Purchasely.setEventDelegate(self)` + `PLYEventDelegate.eventTriggered(_ event: PLYEvent, properties: [String: Any]?)` |

Each `PLYEvent` has:
- `.name` - Event name string
- `.properties` - Associated properties dictionary

### User Attribute Listener (for surveys)

| Android | iOS |
|---------|-----|
| `Purchasely.setUserAttributeListener(object : UserAttributeListener { ... })` | `Purchasely.setUserAttributesDelegate(self)` + `PLYUserAttributesDelegate` |

Callbacks:
- `onUserAttributeSet(key, value, source)` - source is `.purchasely` (from survey) or `.client` (set by app)
- `onUserAttributeRemoved(key, source)`

---

## 9. Transaction Processing

### Full Mode
- Purchasely automatically handles purchase flow when users tap a purchase button
- Results delivered via completion closure (PURCHASED, RESTORED, CANCELLED)

### PaywallObserver Mode
- Use the Paywall Action Interceptor to capture purchase intents
- Handle purchases with your own infrastructure or RevenueCat
- Call `Purchasely.synchronize()` after a purchase to sync with Purchasely analytics

| Feature | Android API | iOS API |
|---------|------------|---------|
| Synchronize | `Purchasely.synchronize()` | `Purchasely.synchronize()` |

---

## 10. Restore Purchases

- In **Full mode**: handled automatically by `RESTORE` action (just `processAction(true)`)
- In **PaywallObserver mode**: intercept `RESTORE` action, call your own restore, then `Purchasely.synchronize()`

---

## 11. Additional Features

| Feature | Android | iOS |
|---------|---------|-----|
| Promoted In-App Purchases | N/A | Supported via App Store Connect |
| Promo codes | Interceptable via `PROMO_CODE` action | Interceptable via `.promoCode` action |
| A/B testing | Managed in Console, transparent to SDK | Managed in Console, transparent to SDK |
| Audiences/Flows | Managed in Console, transparent to SDK | Managed in Console, transparent to SDK |

---

## Summary: Complete API Surface

### Core Methods (Both Platforms)

1. `start()` / `Builder().build().start()` - Initialize SDK
2. `fetchPresentation()` - Fetch a paywall for a placement
3. `presentation.display()` - Display a fetched paywall
4. `presentationView()` / `presentationController()` - Direct display convenience
5. `setPaywallActionsInterceptor()` - Intercept paywall user actions
6. `userSubscriptions()` - Get active subscriptions
7. `userLogin()` - Set user ID
8. `userLogout()` - Clear user ID
9. `anonymousUserId` - Get anonymous ID
10. `setUserAttribute()` - Set custom user attribute (String/Int/Float/Bool/Date)
11. `incrementUserAttribute()` - Increment numeric attribute
12. `clearUserAttribute()` - Remove an attribute
13. `setEventDelegate()` / `setEventListener()` - Listen for SDK events
14. `setUserAttributeDelegate()` / `setUserAttributeListener()` - Listen for attribute changes
15. `readyToOpenDeeplink` - Enable deeplink display
16. `setDefaultPresentationResultHandler()` - Handle deeplink paywall results
17. `synchronize()` - Sync purchases for PaywallObserver mode
18. `isDeeplinkHandled()` (Android) - Check if a URI is a Purchasely deeplink

### iOS-Only
- `PLYSubscriptionStatus` enum (autoRenewing, onHold, inGracePeriod, etc.)
- `storekitSettings` (.storeKit1 / .storeKit2)
- `presentation.controller` - Get raw UIViewController
- `userAttributes()` - Get all attributes as dictionary
- `Purchasely.restoreAllProducts(success:, failure:)` - Restore all products

### Android-Only
- `PLYTransitionType` display modes (PUSH, FULLSCREEN, MODAL, DRAWER, POPIN)
- `presentation.buildView()` - Build a View for embedding
- `Purchasely.presentationView()` - Get a PLYPresentationView for embedding
- Multiple store support (Google, Huawei, Amazon)
- `presentation.displayMode?.type` / `.heightPercentage`
