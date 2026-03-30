# Purchasely PaywallObserver Mode - Complete API Reference

## 1. SDK Initialization

### iOS (Swift)

```swift
import Purchasely

Purchasely.start(
    withAPIKey: "YOUR_API_KEY",
    appUserId: nil,                    // Optional: set if known
    runningMode: .paywallObserver,     // PaywallObserver mode
    storekitSettings: .storeKit2,      // .storeKit1 or .storeKit2
    logLevel: .debug                   // .error for production
) { (success, error) in
    if success {
        print("Purchasely SDK configured successfully")
    }
}
```

**Running mode enum values:** `.full`, `.paywallObserver`

### Android (Kotlin)

```kotlin
import io.purchasely.ext.Purchasely
import io.purchasely.ext.PLYRunningMode
import io.purchasely.ext.LogLevel
import io.purchasely.google.GoogleStore

Purchasely.Builder(applicationContext)
    .apiKey("YOUR_API_KEY")
    .userId(null)                              // Optional
    .stores(listOf(GoogleStore()))
    .logLevel(LogLevel.DEBUG)                  // LogLevel.ERROR for production
    .runningMode(PLYRunningMode.PaywallObserver)
    .build()
    .start { isConfigured, error ->
        if (isConfigured) {
            // SDK ready
        }
    }
```

**Running mode enum values:** `PLYRunningMode.Full`, `PLYRunningMode.PaywallObserver`

---

## 2. Paywall Action Interceptor

In PaywallObserver mode, you MUST set up the interceptor to handle purchase and restore actions yourself.

### iOS Signature

```swift
Purchasely.setPaywallActionsInterceptor { [weak self] (action, parameters, presentationInfos, proceed) in
    // action: PLYPresentationAction enum
    // parameters: PLYPresentationActionParameters? (nullable)
    // presentationInfos: PLYPresentationInfo?
    // proceed: (Bool) -> Void
}
```

**Action enum cases (iOS):** `.purchase`, `.restore`, `.login`, `.close`, `.openPresentation`, `.promoCode`, `.navigate`

**Parameters properties (iOS):**
- `parameters?.plan` -> `PLYPlan?`
- `parameters?.plan?.appleProductId` -> `String?`
- `parameters?.plan?.name` -> `String?`
- `parameters?.plan?.promoOffers` -> `[PLYPromoOffer]`
- `parameters?.promoOffer` -> `PLYPromoOffer?`
- `parameters?.url` -> `String?`
- `parameters?.offer?.storeOfferId` -> `String?`

**PresentationInfos properties (iOS):**
- `presentationInfos?.controller` -> `UIViewController?`

### Android Signature

```kotlin
Purchasely.setPaywallActionsInterceptor { info, action, parameters, processAction ->
    // info: PLYPresentationInfo? (contains info.activity)
    // action: PLYPresentationAction enum
    // parameters: PLYPresentationActionParameters
    // processAction: (Boolean) -> Unit
}
```

**Action enum values (Android):** `PLYPresentationAction.PURCHASE`, `.RESTORE`, `.LOGIN`, `.CLOSE`, `.OPEN_PRESENTATION`, `.PROMO_CODE`

**Parameters properties (Android):**
- `parameters.plan` -> `PLYPlan?`
- `parameters.plan?.productId` -> `String?`
- `parameters.plan?.name` -> `String?`
- `parameters.plan?.basePlanId` -> `String?`
- `parameters.offer?.vendorId` -> `String?`
- `parameters.offer?.storeOfferId` -> `String?`
- `parameters.subscriptionOffer?.subscriptionId` -> `String?`
- `parameters.subscriptionOffer?.basePlanId` -> `String?`
- `parameters.subscriptionOffer?.offerId` -> `String?`
- `parameters.subscriptionOffer?.offerToken` -> `String?`
- `parameters.subscriptionOffer?.subscriptionOption` -> RevenueCat SubscriptionOption
- `parameters.url` -> `String?`

**Info properties (Android):**
- `info?.activity` -> `Activity?`

---

## 3. Synchronize

Call `Purchasely.synchronize()` after every successful purchase or restore to sync receipts with Purchasely for analytics.

### iOS

```swift
Purchasely.synchronize()
```

### Android

```kotlin
Purchasely.synchronize()
```

**When to call:** After EVERY successful purchase and restore, before or after calling `proceed(false)` / `processAction(false)`.

---

## 4. Complete PaywallObserver Implementation - In-House Infrastructure

### iOS (Swift)

```swift
Purchasely.setPaywallActionsInterceptor { [weak self] (action, parameters, presentationInfos, proceed) in
    switch action {
    case .purchase:
        guard let plan = parameters?.plan,
              let appleProductId = plan.appleProductId else {
            proceed(false)
            return
        }

        // Use your own purchase system
        let success = MyPurchaseSystem.purchase(appleProductId)
        if success {
            Purchasely.synchronize()
        }
        proceed(false) // We handled the purchase ourselves

    case .restore:
        MyPurchaseSystem.restorePurchases()
        Purchasely.synchronize()
        proceed(false)

    default:
        proceed(true)
    }
}
```

### Android (Kotlin)

```kotlin
Purchasely.setPaywallActionsInterceptor { info, action, parameters, processAction ->
    when (action) {
        PLYPresentationAction.PURCHASE -> {
            val plan = parameters.plan
            val offerId = parameters.offer?.vendorId ?: plan?.basePlanId
            // For Google Play: use offerToken for BillingClient
            val offerToken = parameters.subscriptionOffer?.offerToken

            yourPurchaseManager.purchase(
                productId = plan?.productId,
                offerId = offerId,
                onSuccess = { purchase ->
                    Purchasely.synchronize()
                    processAction(true)
                },
                onError = { error ->
                    processAction(false)
                }
            )
        }
        PLYPresentationAction.RESTORE -> {
            yourPurchaseManager.restore { success ->
                if (success) {
                    Purchasely.synchronize()
                }
                processAction(success)
            }
        }
        else -> processAction(true)
    }
}
```

---

## 5. Complete PaywallObserver Implementation - RevenueCat

### iOS (Swift)

```swift
import RevenueCat

Purchasely.setPaywallActionsInterceptor { [weak self] (action, parameters, presentationInfos, proceed) in
    switch action {
    case .purchase:
        guard let plan = parameters?.plan,
              let appleProductId = plan.appleProductId else {
            return
        }

        Purchases.shared.getOfferings { (offerings, error) in
            if let packages = offerings?.current?.availablePackages {
                if let package = packages.first(where: {
                    $0.storeProduct.productIdentifier == appleProductId
                }) {
                    Purchases.shared.purchase(package: package) { (transaction, customerInfo, error, userCancelled) in
                        Purchasely.synchronize()
                        proceed(false)

                        if customerInfo.entitlements["your_entitlement_id"]?.isActive == true {
                            // Unlock premium content
                        }
                    }
                }
            }
        }

    case .restore:
        Purchases.shared.restorePurchases { customerInfo, error in
            Purchasely.synchronize()
            proceed(false)
        }

    default:
        proceed(true)
    }
}
```

### Android (Kotlin)

```kotlin
Purchasely.setPaywallActionsInterceptor { info, action, parameters, processAction ->
    when (action) {
        PLYPresentationAction.PURCHASE -> {
            val plan = parameters.plan
            val subscriptionOption = parameters.subscriptionOffer?.subscriptionOption

            if (subscriptionOption != null) {
                Purchases.sharedInstance.purchase(
                    PurchaseParams.Builder(info.activity, subscriptionOption).build(),
                    object : PurchaseCallback {
                        override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                            Purchasely.synchronize()
                            processAction(true)
                        }
                        override fun onError(error: PurchasesError, userCancelled: Boolean) {
                            processAction(false)
                        }
                    }
                )
            }
        }
        PLYPresentationAction.RESTORE -> {
            Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    Purchasely.synchronize()
                    processAction(true)
                }
                override fun onError(error: PurchasesError) {
                    processAction(false)
                }
            })
        }
        else -> processAction(true)
    }
}
```

---

## 6. Promotional Offers (Apple Only)

### Signing a Promotional Offer

```swift
Purchasely.signPromotionalOffer(
    storeProductId: "storeProductId",    // Apple product ID
    storeOfferId: "storeOfferId"         // Store offer ID
) { signature in
    // signature contains: .identifier, .keyIdentifier, .nonce, .signature, .timestamp
} failure: { error in
    // Handle error
}
```

### Full Interceptor with Promotional Offer (StoreKit 1)

```swift
Purchasely.setPaywallActionsInterceptor { [weak self] (action, parameters, presentationInfos, proceed) in
    switch action {
    case .purchase:
        guard let plan = parameters?.plan,
              let appleProductId = plan.appleProductId else {
            proceed(false)
            return
        }

        let offer = parameters?.promoOffer

        // Sign the offer if present
        Purchasely.signPromotionalOffer(
            storeProductId: appleProductId,
            storeOfferId: offer?.storeOfferId
        ) { signature in
            // Build payment with signature
            let payment = SKMutablePayment(product: skProduct)
            payment.applicationUsername = Purchasely.anonymousUserId.lowercased() // MANDATORY: lowercase

            if let signature = signature, #available(iOS 12.2, *) {
                let paymentDiscount = SKPaymentDiscount(
                    identifier: signature.identifier,
                    keyIdentifier: signature.keyIdentifier,
                    nonce: signature.nonce,
                    signature: signature.signature,
                    timestamp: NSNumber(value: signature.timestamp)
                )
                payment.paymentDiscount = paymentDiscount
            }

            SKPaymentQueue.default().add(payment)
            Purchasely.synchronize()
            proceed(false)
        } failure: { error in
            proceed(false)
        }

    default:
        proceed(true)
    }
}
```

### Full Interceptor with Promotional Offer (StoreKit 2)

```swift
if #available(iOS 15.0, *) {
    Purchasely.signPromotionalOffer(
        storeProductId: plan.appleProductId,
        storeOfferId: plan.promoOffers.first?.storeOfferId,
        success: { promoOfferSignature in
            Task {
                do {
                    let products = try await Product.products(for: ["storeProductId"])
                    var options: Set<Product.PurchaseOption> = [
                        .simulatesAskToBuyInSandbox(true) // true for testing only
                    ]

                    let userId = Purchasely.anonymousUserId.lowercased()
                    options.insert(.appAccountToken(userId))

                    if let decodedSignature = Data(base64Encoded: promoOfferSignature.signature) {
                        let offerOption: Product.PurchaseOption = .promotionalOffer(
                            offerID: promoOfferSignature.identifier,
                            keyID: promoOfferSignature.keyIdentifier,
                            nonce: promoOfferSignature.nonce,
                            signature: decodedSignature,
                            timestamp: Int(promoOfferSignature.timestamp)
                        )
                        options.insert(offerOption)
                    }

                    if let product = products.first {
                        let purchaseResult = try await product.purchase(options: options)
                    }
                }
            }
        },
        failure: { error in }
    )
}
```

### Purchasing a Promotional Offer via Purchasely SDK (Full mode)

```swift
// Get the plan
Purchasely.plan(with: "planId") { plan in
    // Find the offer
    let promoOffer = plan.promoOffers.first(where: { $0.vendorId == promoOfferVendorId })

    // Purchase with offer
    Purchasely.purchaseWithPromotionalOffer(
        plan: plan,
        contentId: nil,
        storeOfferId: promoOffer.storeOfferId
    ) {
        // Success
    } failure: { error in
        // Failure
    }
} failure: { error in
    // Plan retrieval failed
}
```

---

## 7. Google Play Offer Parameters (Android)

When intercepting purchases on Android, the `parameters` object provides Google Play-specific offer details:

```kotlin
// From parameters directly
val plan = parameters.plan
val productId = plan?.productId              // Google Play product ID
val basePlanId = plan?.basePlanId            // Base plan ID

// From the offer object
val offerVendorId = parameters.offer?.vendorId
val storeOfferId = parameters.offer?.storeOfferId

// From subscriptionOffer (Google Play specific, useful for BillingClient)
val subscriptionId = parameters.subscriptionOffer?.subscriptionId
val offerBasePlanId = parameters.subscriptionOffer?.basePlanId
val offerId = parameters.subscriptionOffer?.offerId
val offerToken = parameters.subscriptionOffer?.offerToken      // Use with BillingClient
val subscriptionOption = parameters.subscriptionOffer?.subscriptionOption  // RevenueCat only
```

---

## 8. Key Rules

1. **Always call `Purchasely.synchronize()`** after successful purchase/restore -- this syncs receipts for analytics
2. **Call `proceed(false)` / `processAction(false)`** when YOU handle the transaction (PaywallObserver mode)
3. **Call `proceed(true)` / `processAction(true)`** to let Purchasely handle it (Full mode, or non-purchase actions)
4. **For iOS promotional offers:** `anonymousUserId` must be **lowercased** when used as `applicationUsername` (SK1) or `appAccountToken` (SK2)
5. **For Android:** use `offerToken` from `parameters.subscriptionOffer` when working with Google Play BillingClient directly
6. **Paywall visibility control (cross-platform SDKs):** `hidePresentation()`, `showPresentation()`, `closePresentation()`, `onProcessAction(bool)`
