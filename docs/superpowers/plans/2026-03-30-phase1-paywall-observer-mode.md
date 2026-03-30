# Phase 1: PaywallObserver Mode — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a running mode toggle (Full / PaywallObserver) to both Android and iOS, with complete native purchase handling via StoreKit 2 and Google Play Billing when in Observer mode, including promotional offer support and `synchronize()` calls.

**Architecture:** A new `RunningModeRepository` persists the user's choice. A new `PurchaseManager` encapsulates native store purchases (StoreKit 2 on iOS, Google Play Billing on Android). The app re-initializes the SDK when the mode changes. The paywall actions interceptor branches on mode: Full → `proceed(true)`, Observer → native purchase + `synchronize()` + `proceed(false)`.

**Tech Stack:** StoreKit 2 (iOS 16+), Google Play Billing Library 7.x (Android), Purchasely SDK 5.6.x

---

## File Map

### Android — New Files
| File | Responsibility |
|------|---------------|
| `data/RunningModeRepository.kt` | Persists running mode to SharedPreferences |
| `data/PurchaseManager.kt` (new, separate from PremiumManager) | Google Play Billing: purchase, restore, acknowledge |

### Android — Modified Files
| File | Changes |
|------|---------|
| `ShakerApp.kt` | Extract SDK init to reusable method, branch interceptor on mode |
| `di/AppModule.kt` | Register new repositories |
| `ui/screen/settings/SettingsViewModel.kt` | Add running mode state + toggle |
| `ui/screen/settings/SettingsScreen.kt` | Add running mode picker UI |
| `app/build.gradle.kts` | Add Google Play Billing dependency |

### iOS — New Files
| File | Responsibility |
|------|---------------|
| `Data/RunningModeRepository.swift` | Persists running mode to UserDefaults |
| `Data/PurchaseManager.swift` (new, separate from PremiumManager) | StoreKit 2: purchase, restore, promo offers |

### iOS — Modified Files
| File | Changes |
|------|---------|
| `AppViewModel.swift` | Extract SDK init, branch interceptor on mode, re-init on change |
| `Screens/Settings/SettingsViewModel.swift` | Add running mode state + toggle |
| `Screens/Settings/SettingsScreen.swift` | Add running mode picker UI |

---

## Task 1: Android — RunningModeRepository

**Files:**
- Create: `android/app/src/main/java/com/purchasely/shaker/data/RunningModeRepository.kt`
- Modify: `android/app/src/main/java/com/purchasely/shaker/di/AppModule.kt`

- [ ] **Step 1: Create RunningModeRepository**

```kotlin
package com.purchasely.shaker.data

import android.content.Context
import android.content.SharedPreferences
import io.purchasely.ext.PLYRunningMode

class RunningModeRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("shaker_settings", Context.MODE_PRIVATE)

    var runningMode: PLYRunningMode
        get() {
            val stored = prefs.getString(KEY_RUNNING_MODE, "full")
            return if (stored == "observer") PLYRunningMode.PaywallObserver else PLYRunningMode.Full
        }
        set(value) {
            val str = if (value == PLYRunningMode.PaywallObserver) "observer" else "full"
            prefs.edit().putString(KEY_RUNNING_MODE, str).apply()
        }

    val isObserverMode: Boolean
        get() = runningMode == PLYRunningMode.PaywallObserver

    companion object {
        private const val KEY_RUNNING_MODE = "running_mode"
    }
}
```

- [ ] **Step 2: Register in Koin**

In `AppModule.kt`, add the import and singleton:

```kotlin
// Add import:
import com.purchasely.shaker.data.RunningModeRepository

// Add to module block, after OnboardingRepository:
single { RunningModeRepository(androidContext()) }
```

- [ ] **Step 3: Build to verify**

Run: `cd android && ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/java/com/purchasely/shaker/data/RunningModeRepository.kt android/app/src/main/java/com/purchasely/shaker/di/AppModule.kt
git commit -m "feat(android): add RunningModeRepository for Full/Observer toggle"
```

---

## Task 2: Android — PurchaseManager with Google Play Billing

**Files:**
- Create: `android/app/src/main/java/com/purchasely/shaker/data/purchase/PurchaseManager.kt`
- Modify: `android/app/build.gradle.kts`
- Modify: `android/gradle/libs.versions.toml`
- Modify: `android/app/src/main/java/com/purchasely/shaker/di/AppModule.kt`

- [ ] **Step 1: Add Google Play Billing dependency**

In `android/gradle/libs.versions.toml`, add:

```toml
# Under [versions]:
billing = "7.1.1"

# Under [libraries]:
google-billing = { group = "com.android.billingclient", name = "billing-ktx", version.ref = "billing" }
```

In `android/app/build.gradle.kts`, add to dependencies:

```kotlin
// Google Play Billing (for PaywallObserver mode)
implementation(libs.google.billing)
```

- [ ] **Step 2: Create PurchaseManager**

```kotlin
package com.purchasely.shaker.data.purchase

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.AcknowledgePurchaseParams
import android.content.Context
import io.purchasely.ext.Purchasely

class PurchaseManager(context: Context) : PurchasesUpdatedListener {

    private var onPurchaseResult: ((Boolean) -> Unit)? = null

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    init {
        connectBillingClient()
    }

    private fun connectBillingClient() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "[Shaker] BillingClient connected")
                } else {
                    Log.e(TAG, "[Shaker] BillingClient connection failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "[Shaker] BillingClient disconnected, reconnecting...")
                connectBillingClient()
            }
        })
    }

    /**
     * Launch a purchase flow using the offerToken from the Purchasely interceptor parameters.
     * In Observer mode, the interceptor provides `parameters.subscriptionOffer?.offerToken`.
     * We must first query ProductDetails (required by BillingClient), then launch the flow.
     */
    fun purchase(
        activity: Activity,
        productId: String,
        offerToken: String,
        onResult: (Boolean) -> Unit
    ) {
        onPurchaseResult = onResult

        // BillingFlowParams requires ProductDetails — query it first
        val queryParams = com.android.billingclient.api.QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    com.android.billingclient.api.QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(queryParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK || productDetailsList.isEmpty()) {
                Log.e(TAG, "[Shaker] queryProductDetails failed: ${billingResult.debugMessage}")
                onPurchaseResult?.invoke(false)
                onPurchaseResult = null
                return@queryProductDetailsAsync
            }

            val productDetails = productDetailsList.first()

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .setOfferToken(offerToken)
                            .build()
                    )
                )
                .build()

            val result = billingClient.launchBillingFlow(activity, flowParams)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.e(TAG, "[Shaker] Launch billing flow failed: ${result.debugMessage}")
                onPurchaseResult?.invoke(false)
                onPurchaseResult = null
            }
        }
    }

    /**
     * Restore purchases by querying existing subscriptions and in-app products.
     */
    fun restore(onResult: (Boolean) -> Unit) {
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(subsParams) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val activePurchases = purchases.filter {
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                activePurchases.forEach { acknowledgePurchase(it) }

                Log.d(TAG, "[Shaker] Restored ${activePurchases.size} purchases")
                Purchasely.synchronize()
                onResult(activePurchases.isNotEmpty())
            } else {
                Log.e(TAG, "[Shaker] Restore failed: ${billingResult.debugMessage}")
                onResult(false)
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    acknowledgePurchase(purchase)
                }
            }
            Log.d(TAG, "[Shaker] Purchase successful, synchronizing with Purchasely...")
            Purchasely.synchronize()
            onPurchaseResult?.invoke(true)
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "[Shaker] Purchase cancelled by user")
            onPurchaseResult?.invoke(false)
        } else {
            Log.e(TAG, "[Shaker] Purchase error: ${billingResult.debugMessage}")
            onPurchaseResult?.invoke(false)
        }
        onPurchaseResult = null
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) return

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "[Shaker] Purchase acknowledged: ${purchase.orderId}")
            } else {
                Log.e(TAG, "[Shaker] Acknowledge failed: ${billingResult.debugMessage}")
            }
        }
    }

    companion object {
        private const val TAG = "PurchaseManager"
    }
}
```

- [ ] **Step 3: Register PurchaseManager in Koin**

In `AppModule.kt`, add:

```kotlin
// Add import:
import com.purchasely.shaker.data.purchase.PurchaseManager

// Add to module block:
single { PurchaseManager(androidContext()) }
```

- [ ] **Step 4: Build to verify**

Run: `cd android && ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add android/app/build.gradle.kts android/gradle/libs.versions.toml android/app/src/main/java/com/purchasely/shaker/data/purchase/PurchaseManager.kt android/app/src/main/java/com/purchasely/shaker/di/AppModule.kt
git commit -m "feat(android): add PurchaseManager for native Google Play Billing"
```

---

## Task 3: Android — Update ShakerApp to Support Mode Switching

**Files:**
- Modify: `android/app/src/main/java/com/purchasely/shaker/ShakerApp.kt`

- [ ] **Step 1: Refactor ShakerApp to support re-initialization and mode-aware interceptor**

Replace the entire file content:

```kotlin
package com.purchasely.shaker

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.purchasely.shaker.data.PremiumManager
import com.purchasely.shaker.data.RunningModeRepository
import com.purchasely.shaker.data.purchase.PurchaseManager
import com.purchasely.shaker.di.appModule
import io.purchasely.ext.EventListener
import io.purchasely.ext.LogLevel
import io.purchasely.ext.PLYEvent
import io.purchasely.ext.PLYPresentationAction
import io.purchasely.ext.Purchasely
import io.purchasely.google.GoogleStore
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ShakerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@ShakerApp)
            modules(appModule)
        }

        initPurchasely()
    }

    /**
     * Initialize (or re-initialize) the Purchasely SDK with the current running mode.
     * Called on app launch and whenever the user toggles the running mode in Settings.
     */
    fun initPurchasely() {
        val runningModeRepo: RunningModeRepository by inject()
        val currentMode = runningModeRepo.runningMode

        Log.d(TAG, "[Shaker] Initializing Purchasely SDK in ${currentMode.name} mode")

        Purchasely.Builder(this)
            .apiKey("6cda6b92-d63c-4444-bd55-5a164c989bd4")
            .logLevel(LogLevel.DEBUG)
            .readyToOpenDeeplink(true)
            .runningMode(currentMode)
            .stores(listOf(GoogleStore()))
            .build()
            .start { isConfigured, error ->
                if (isConfigured) {
                    Log.d(TAG, "[Shaker] Purchasely SDK configured successfully (${currentMode.name})")
                    val premiumManager: PremiumManager by inject()
                    premiumManager.refreshPremiumStatus()
                }
                error?.let {
                    Log.e(TAG, "[Shaker] Purchasely configuration error: ${it.message}")
                }
            }

        Purchasely.eventListener = object : EventListener {
            override fun onEvent(event: PLYEvent) {
                Log.d(TAG, "[Shaker] Event: ${event.name} | Properties: ${event.properties}")
            }
        }

        setupInterceptor()

        // Synchronize on launch when in Observer mode to catch external transactions
        if (runningModeRepo.isObserverMode) {
            Purchasely.synchronize()
            Log.d(TAG, "[Shaker] Observer mode: synchronize() called on launch")
        }
    }

    private fun setupInterceptor() {
        val runningModeRepo: RunningModeRepository by inject()

        Purchasely.setPaywallActionsInterceptor { info, action, parameters, proceed ->
            when (action) {
                PLYPresentationAction.LOGIN -> {
                    Log.d(TAG, "[Shaker] Paywall login action intercepted")
                    proceed(false)
                }
                PLYPresentationAction.NAVIGATE -> {
                    val url = parameters?.url
                    if (url != null) {
                        Log.d(TAG, "[Shaker] Paywall navigate action: $url")
                        val intent = Intent(Intent.ACTION_VIEW, url)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                    proceed(false)
                }
                PLYPresentationAction.PURCHASE -> {
                    if (runningModeRepo.isObserverMode) {
                        // Observer mode: handle purchase natively via Google Play Billing
                        val activity = info?.activity
                        val offerToken = parameters?.subscriptionOffer?.offerToken

                        if (activity != null && offerToken != null) {
                            Log.d(TAG, "[Shaker] Observer mode: launching native purchase flow")
                            val purchaseManager: PurchaseManager by inject()
                            purchaseManager.purchase(activity, parameters.plan?.productId ?: "", offerToken) { success ->
                                if (success) {
                                    Log.d(TAG, "[Shaker] Observer mode: native purchase successful")
                                    val premiumManager: PremiumManager by inject()
                                    premiumManager.refreshPremiumStatus()
                                }
                                proceed(false) // We handled it ourselves
                            }
                        } else {
                            Log.e(TAG, "[Shaker] Observer mode: missing activity or offerToken")
                            proceed(false)
                        }
                    } else {
                        // Full mode: let Purchasely handle the purchase
                        proceed(true)
                    }
                }
                PLYPresentationAction.RESTORE -> {
                    if (runningModeRepo.isObserverMode) {
                        // Observer mode: restore via Google Play Billing
                        Log.d(TAG, "[Shaker] Observer mode: restoring purchases natively")
                        val purchaseManager: PurchaseManager by inject()
                        purchaseManager.restore { success ->
                            val premiumManager: PremiumManager by inject()
                            premiumManager.refreshPremiumStatus()
                            proceed(false) // We handled it ourselves
                        }
                    } else {
                        proceed(true)
                    }
                }
                else -> proceed(true)
            }
        }
    }

    companion object {
        private const val TAG = "ShakerApp"
    }
}
```

- [ ] **Step 2: Build to verify**

Run: `cd android && ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/java/com/purchasely/shaker/ShakerApp.kt
git commit -m "feat(android): mode-aware SDK init with Observer purchase/restore in interceptor"
```

---

## Task 4: Android — Settings UI for Running Mode Toggle

**Files:**
- Modify: `android/app/src/main/java/com/purchasely/shaker/ui/screen/settings/SettingsViewModel.kt`
- Modify: `android/app/src/main/java/com/purchasely/shaker/ui/screen/settings/SettingsScreen.kt`

- [ ] **Step 1: Add running mode to SettingsViewModel**

In `SettingsViewModel.kt`, add the import, constructor parameter, state, and method:

Add import at top:
```kotlin
import com.purchasely.shaker.data.RunningModeRepository
import io.purchasely.ext.PLYRunningMode
```

Change the constructor to accept `RunningModeRepository`:
```kotlin
class SettingsViewModel(
    private val context: Context,
    private val premiumManager: PremiumManager,
    private val runningModeRepo: RunningModeRepository
) : ViewModel() {
```

Add state after `_thirdPartyConsent`:
```kotlin
    private val _runningMode = MutableStateFlow(
        if (runningModeRepo.isObserverMode) "observer" else "full"
    )
    val runningMode: StateFlow<String> = _runningMode.asStateFlow()
```

Add method after `setThirdPartyConsent`:
```kotlin
    fun setRunningMode(mode: String) {
        _runningMode.value = mode
        runningModeRepo.runningMode = if (mode == "observer") PLYRunningMode.PaywallObserver else PLYRunningMode.Full

        // Re-initialize the SDK with the new mode
        val app = context.applicationContext as ShakerApp
        app.initPurchasely()

        Log.d(TAG, "[Shaker] Running mode changed to: $mode — SDK re-initialized")
    }
```

Add import for ShakerApp:
```kotlin
import com.purchasely.shaker.ShakerApp
```

- [ ] **Step 2: Update Koin module for new constructor**

In `AppModule.kt`, update the SettingsViewModel line:
```kotlin
    viewModel { SettingsViewModel(androidContext(), get(), get()) }
```

- [ ] **Step 3: Add running mode picker to SettingsScreen**

In `SettingsScreen.kt`, add the state collection after `thirdPartyConsent`:
```kotlin
    val runningMode by viewModel.runningMode.collectAsState()
```

Add the running mode section UI **after the Purchases section** and **before the Data Privacy section** (after the "Show Onboarding" button and the HorizontalDivider). Insert this block:

```kotlin
        // SDK Mode section
        Text(
            text = "SDK Mode",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (runningMode == "observer")
                "PaywallObserver — Your app handles purchases natively"
            else
                "Full — Purchasely SDK handles purchases",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        val modes = listOf("full", "observer")
        val modeLabels = listOf("Full", "Observer")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = runningMode == mode,
                    onClick = { viewModel.setRunningMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size)
                ) {
                    Text(modeLabels[index])
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
```

- [ ] **Step 4: Build to verify**

Run: `cd android && ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/java/com/purchasely/shaker/ui/screen/settings/SettingsViewModel.kt android/app/src/main/java/com/purchasely/shaker/ui/screen/settings/SettingsScreen.kt android/app/src/main/java/com/purchasely/shaker/di/AppModule.kt
git commit -m "feat(android): add running mode toggle (Full/Observer) in Settings"
```

---

## Task 5: iOS — RunningModeRepository

**Files:**
- Create: `ios/Shaker/Data/RunningModeRepository.swift`

- [ ] **Step 1: Create RunningModeRepository**

```swift
import Foundation
import Purchasely

class RunningModeRepository {

    static let shared = RunningModeRepository()

    private let key = "running_mode"

    private init() {}

    var runningMode: PLYRunningMode {
        get {
            let stored = UserDefaults.standard.string(forKey: key) ?? "full"
            return stored == "observer" ? .paywallObserver : .full
        }
        set {
            let str = newValue == .paywallObserver ? "observer" : "full"
            UserDefaults.standard.set(str, forKey: key)
        }
    }

    var isObserverMode: Bool {
        runningMode == .paywallObserver
    }
}
```

- [ ] **Step 2: Regenerate Xcode project**

Run: `cd ios && xcodegen generate && pod install`
Expected: Project generated, Pod installation complete

- [ ] **Step 3: Commit**

```bash
git add ios/Shaker/Data/RunningModeRepository.swift
git commit -m "feat(ios): add RunningModeRepository for Full/Observer toggle"
```

---

## Task 6: iOS — PurchaseManager with StoreKit 2

**Files:**
- Create: `ios/Shaker/Data/PurchaseManager.swift` (note: this is a NEW file, not the existing PremiumManager.swift)

**Important:** The file name `PurchaseManager.swift` must NOT conflict with the existing `PremiumManager.swift`. They are different classes with different responsibilities.

- [ ] **Step 1: Create PurchaseManager**

```swift
import Foundation
import StoreKit
import Purchasely

@available(iOS 15.0, *)
class PurchaseManager {

    static let shared = PurchaseManager()

    private init() {}

    /// Purchase a product natively via StoreKit 2.
    /// Called from the paywall interceptor when in Observer mode.
    func purchase(productId: String) async throws -> Transaction {
        let products = try await Product.products(for: [productId])
        guard let product = products.first else {
            throw PurchaseError.productNotFound
        }

        // Use Purchasely anonymous user ID (lowercased) as the app account token
        let userId = Purchasely.anonymousUserId.lowercased()

        var options: Set<Product.PurchaseOption> = []
        if let uuid = UUID(uuidString: userId) {
            options.insert(.appAccountToken(uuid))
        }

        let result = try await product.purchase(options: options)

        switch result {
        case .success(let verification):
            let transaction = try checkVerified(verification)
            await transaction.finish()
            Purchasely.synchronize()
            print("[Shaker] Observer mode: native purchase successful, synchronized")
            return transaction
        case .userCancelled:
            throw PurchaseError.cancelled
        case .pending:
            throw PurchaseError.pending
        @unknown default:
            throw PurchaseError.unknown
        }
    }

    /// Purchase a product with a promotional offer via StoreKit 2.
    /// Uses Purchasely.signPromotionalOffer() to generate the required signature.
    func purchaseWithPromoOffer(
        productId: String,
        storeOfferId: String
    ) async throws -> Transaction {
        let products = try await Product.products(for: [productId])
        guard let product = products.first else {
            throw PurchaseError.productNotFound
        }

        // Sign the promotional offer via Purchasely backend
        let signature = try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<PLYOfferSignature, Error>) in
            Purchasely.signPromotionalOffer(
                storeProductId: productId,
                storeOfferId: storeOfferId,
                success: { signature in
                    continuation.resume(returning: signature)
                },
                failure: { error in
                    continuation.resume(throwing: error)
                }
            )
        }

        let userId = Purchasely.anonymousUserId.lowercased()
        var options: Set<Product.PurchaseOption> = []
        if let uuid = UUID(uuidString: userId) {
            options.insert(.appAccountToken(uuid))
        }

        // Add promotional offer to purchase options
        if let decodedSignature = Data(base64Encoded: signature.signature) {
            let offerOption: Product.PurchaseOption = .promotionalOffer(
                offerID: signature.identifier,
                keyID: signature.keyIdentifier,
                nonce: signature.nonce,
                signature: decodedSignature,
                timestamp: Int(signature.timestamp)
            )
            options.insert(offerOption)
        }

        let result = try await product.purchase(options: options)

        switch result {
        case .success(let verification):
            let transaction = try checkVerified(verification)
            await transaction.finish()
            Purchasely.synchronize()
            print("[Shaker] Observer mode: promo offer purchase successful, synchronized")
            return transaction
        case .userCancelled:
            throw PurchaseError.cancelled
        case .pending:
            throw PurchaseError.pending
        @unknown default:
            throw PurchaseError.unknown
        }
    }

    /// Restore all completed transactions via StoreKit 2.
    func restoreAllTransactions() async throws {
        var restoredCount = 0
        for await result in Transaction.currentEntitlements {
            if let transaction = try? checkVerified(result) {
                await transaction.finish()
                restoredCount += 1
            }
        }
        Purchasely.synchronize()
        print("[Shaker] Observer mode: restored \(restoredCount) transactions, synchronized")
    }

    private func checkVerified<T>(_ result: VerificationResult<T>) throws -> T {
        switch result {
        case .unverified(_, let error):
            throw error
        case .verified(let value):
            return value
        }
    }

    enum PurchaseError: LocalizedError {
        case productNotFound
        case cancelled
        case pending
        case unknown

        var errorDescription: String? {
            switch self {
            case .productNotFound: return "Product not found in the App Store"
            case .cancelled: return "Purchase cancelled"
            case .pending: return "Purchase pending approval"
            case .unknown: return "Unknown purchase error"
            }
        }
    }
}
```

- [ ] **Step 2: Regenerate Xcode project**

Run: `cd ios && xcodegen generate && pod install`
Expected: Project generated, Pod installation complete

- [ ] **Step 3: Commit**

```bash
git add ios/Shaker/Data/PurchaseManager.swift
git commit -m "feat(ios): add PurchaseManager for native StoreKit 2 purchases"
```

---

## Task 7: iOS — Update AppViewModel for Mode-Aware SDK Init

**Files:**
- Modify: `ios/Shaker/AppViewModel.swift`

- [ ] **Step 1: Refactor AppViewModel to support re-initialization and mode-aware interceptor**

Replace the entire file content:

```swift
import Foundation
import UIKit
import Purchasely
import StoreKit

class AppViewModel: ObservableObject {

    @Published var isSDKReady = false
    @Published var sdkError: String?

    init() {
        initPurchasely()
    }

    /// Initialize (or re-initialize) the Purchasely SDK with the current running mode.
    /// Called on app launch and whenever the user toggles the running mode in Settings.
    func initPurchasely() {
        let mode = RunningModeRepository.shared.runningMode
        let modeName = mode == .paywallObserver ? "PaywallObserver" : "Full"
        print("[Shaker] Initializing Purchasely SDK in \(modeName) mode")

        Purchasely.start(
            withAPIKey: "6cda6b92-d63c-4444-bd55-5a164c989bd4",
            appUserId: nil,
            runningMode: mode,
            storekitSettings: .storeKit2,
            logLevel: .debug
        ) { [weak self] success, error in
            DispatchQueue.main.async {
                self?.isSDKReady = success
                if success {
                    print("[Shaker] Purchasely SDK configured successfully (\(modeName))")
                    PremiumManager.shared.refreshPremiumStatus()
                } else {
                    self?.sdkError = error?.localizedDescription
                    print("[Shaker] Purchasely configuration error: \(error?.localizedDescription ?? "unknown")")
                }
            }
        }

        Purchasely.readyToOpenDeeplink(true)

        Purchasely.setEventDelegate(self)

        setupInterceptor()

        // Synchronize on launch when in Observer mode to catch external transactions
        if RunningModeRepository.shared.isObserverMode {
            Purchasely.synchronize()
            print("[Shaker] Observer mode: synchronize() called on launch")
        }
    }

    private func setupInterceptor() {
        Purchasely.setPaywallActionsInterceptor { action, parameters, info, proceed in
            let isObserver = RunningModeRepository.shared.isObserverMode

            switch action {
            case .login:
                print("[Shaker] Paywall login action intercepted")
                proceed(false)

            case .navigate:
                if let url = parameters?.url {
                    print("[Shaker] Paywall navigate action: \(url)")
                    DispatchQueue.main.async {
                        UIApplication.shared.open(url)
                    }
                }
                proceed(false)

            case .purchase:
                if isObserver {
                    // Observer mode: handle purchase natively via StoreKit 2
                    guard let plan = parameters?.plan,
                          let appleProductId = plan.appleProductId else {
                        print("[Shaker] Observer mode: missing plan or productId")
                        proceed(false)
                        return
                    }

                    // Check if there's a promotional offer
                    if let promoOffer = parameters?.promoOffer,
                       let storeOfferId = promoOffer.storeOfferId {
                        print("[Shaker] Observer mode: purchasing with promo offer \(storeOfferId)")
                        if #available(iOS 15.0, *) {
                            Task {
                                do {
                                    _ = try await PurchaseManager.shared.purchaseWithPromoOffer(
                                        productId: appleProductId,
                                        storeOfferId: storeOfferId
                                    )
                                    PremiumManager.shared.refreshPremiumStatus()
                                } catch {
                                    print("[Shaker] Observer mode: promo purchase error: \(error.localizedDescription)")
                                }
                                proceed(false)
                            }
                        } else {
                            proceed(false)
                        }
                    } else {
                        // Standard purchase without promotional offer
                        print("[Shaker] Observer mode: launching native purchase for \(appleProductId)")
                        if #available(iOS 15.0, *) {
                            Task {
                                do {
                                    _ = try await PurchaseManager.shared.purchase(productId: appleProductId)
                                    PremiumManager.shared.refreshPremiumStatus()
                                } catch {
                                    print("[Shaker] Observer mode: purchase error: \(error.localizedDescription)")
                                }
                                proceed(false)
                            }
                        } else {
                            proceed(false)
                        }
                    }
                } else {
                    // Full mode: let Purchasely handle the purchase
                    proceed(true)
                }

            case .restore:
                if isObserver {
                    // Observer mode: restore via StoreKit 2
                    print("[Shaker] Observer mode: restoring purchases natively")
                    if #available(iOS 15.0, *) {
                        Task {
                            do {
                                try await PurchaseManager.shared.restoreAllTransactions()
                                PremiumManager.shared.refreshPremiumStatus()
                            } catch {
                                print("[Shaker] Observer mode: restore error: \(error.localizedDescription)")
                            }
                            proceed(false)
                        }
                    } else {
                        proceed(false)
                    }
                } else {
                    proceed(true)
                }

            default:
                proceed(true)
            }
        }
    }
}

extension AppViewModel: PLYEventDelegate {
    func eventTriggered(_ event: PLYEvent, properties: [String: Any]?) {
        print("[Shaker] Event: \(event.name) | Properties: \(properties ?? [:])")
    }
}
```

- [ ] **Step 2: Build to verify**

Run: Build in Xcode (Cmd+B) targeting the simulator
Expected: Build succeeds

- [ ] **Step 3: Commit**

```bash
git add ios/Shaker/AppViewModel.swift
git commit -m "feat(ios): mode-aware SDK init with Observer purchase/restore/promo in interceptor"
```

---

## Task 8: iOS — Settings UI for Running Mode Toggle

**Files:**
- Modify: `ios/Shaker/Screens/Settings/SettingsViewModel.swift`
- Modify: `ios/Shaker/Screens/Settings/SettingsScreen.swift`

- [ ] **Step 1: Add running mode to SettingsViewModel**

In `SettingsViewModel.swift`, add a published property and method.

Add after the `thirdPartyConsent` property:
```swift
    @Published var runningMode: String
```

Add to `init()`, after the consent initialization:
```swift
        runningMode = RunningModeRepository.shared.isObserverMode ? "observer" : "full"
```

Add after `setThirdPartyConsent`:
```swift
    func setRunningMode(_ mode: String) {
        runningMode = mode
        RunningModeRepository.shared.runningMode = mode == "observer" ? .paywallObserver : .full
        print("[Shaker] Running mode changed to: \(mode)")
    }
```

- [ ] **Step 2: Add running mode picker to SettingsScreen**

In `SettingsScreen.swift`, add the `@EnvironmentObject` for AppViewModel at the top of the struct:

```swift
    @EnvironmentObject private var appViewModel: AppViewModel
```

Add the SDK Mode section **after the Purchases section** and **before the Data Privacy section**. Insert after the closing brace of `Section("Purchases")`:

```swift
            // SDK Mode section
            Section {
                Picker("Running Mode", selection: $viewModel.runningMode) {
                    Text("Full").tag("full")
                    Text("Observer").tag("observer")
                }
                .pickerStyle(.segmented)
                .onChange(of: viewModel.runningMode) { newValue in
                    viewModel.setRunningMode(newValue)
                    appViewModel.initPurchasely()
                }
            } header: {
                Text("SDK Mode")
            } footer: {
                Text(viewModel.runningMode == "observer"
                    ? "PaywallObserver — Your app handles purchases natively via StoreKit 2."
                    : "Full — Purchasely SDK handles the entire purchase flow.")
            }
```

- [ ] **Step 3: Build to verify**

Run: Build in Xcode (Cmd+B) targeting the simulator
Expected: Build succeeds

- [ ] **Step 4: Commit**

```bash
git add ios/Shaker/Screens/Settings/SettingsViewModel.swift ios/Shaker/Screens/Settings/SettingsScreen.swift
git commit -m "feat(ios): add running mode toggle (Full/Observer) in Settings"
```

---

## Task 9: Verify & Smoke Test

- [ ] **Step 1: Android build**

Run: `cd android && ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: iOS build**

Run: `cd ios && xcodegen generate && pod install && xcodebuild build -workspace Shaker.xcworkspace -scheme Shaker -destination 'platform=iOS Simulator,name=iPhone 16' -quiet`
Expected: BUILD SUCCEEDED

- [ ] **Step 3: Update CLAUDE.md with new components**

Add to the "Key Components" section in `CLAUDE.md`:

```markdown
- **RunningModeRepository**: Persists Full/Observer mode choice. Toggle in Settings re-initializes SDK.
- **PurchaseManager**: Native purchase handling (StoreKit 2 / Google Play Billing) used only in Observer mode. Calls `synchronize()` after every transaction.
```

- [ ] **Step 4: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: update CLAUDE.md with PaywallObserver mode components"
```
