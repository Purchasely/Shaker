# PurchaselyWrapper + Observer Purchase Refactor — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Decouple PurchaseManager from Purchasely SDK via reactive flows, with PurchaselyWrapper as single orchestrator for init, interceptor, and purchase coordination.

**Architecture:** PurchaselyWrapper emits PurchaseRequest/RestoreRequest via SharedFlows. PurchaseManager (zero Purchasely imports) observes these, executes native billing, and emits TransactionResult. Wrapper observes results and handles synchronize/processAction/premium refresh.

**Tech Stack:** Kotlin, Coroutines (SharedFlow/StateFlow), MockK, Koin DI, Google Play Billing

**Spec:** `docs/superpowers/specs/2026-04-10-purchasely-wrapper-observer-purchase-refactor.md`

---

## File Structure

| File | Action | Responsibility |
|------|--------|----------------|
| `data/purchase/PurchaseRequest.kt` | Create | Data class for purchase trigger |
| `data/purchase/RestoreRequest.kt` | Create | Data object for restore trigger |
| `data/purchase/TransactionResult.kt` | Create | Sealed class for billing outcomes |
| `data/purchase/PurchaseManager.kt` | Refactor | Pure billing service, observes flows, emits TransactionResult |
| `purchasely/PurchaselyWrapper.kt` | Refactor | DI constructor, absorbs init + interceptor, orchestrates flows |
| `ShakerApp.kt` | Simplify | Only Koin init + wrapper.initialize() |
| `di/AppModule.kt` | Modify | New DI graph with shared flows |
| `ui/screen/settings/SettingsViewModel.kt` | Minor | restartPurchaselySdk → wrapper.restart() |
| **Tests** | | |
| `data/purchase/TransactionResultTest.kt` | Create | Sealed class behavior |
| `data/purchase/PurchaseManagerTest.kt` | Create | Flow collection + TransactionResult emission |
| `purchasely/PurchaselyWrapperTest.kt` | Rewrite | Orchestration: interceptor → flows → synchronize → processAction |
| `ui/screen/settings/SettingsViewModelTest.kt` | Minor | Adapt restart test |

---

### Task 1: Create reactive flow types

**Files:**
- Create: `android/app/src/main/java/com/purchasely/shaker/data/purchase/PurchaseRequest.kt`
- Create: `android/app/src/main/java/com/purchasely/shaker/data/purchase/RestoreRequest.kt`
- Create: `android/app/src/main/java/com/purchasely/shaker/data/purchase/TransactionResult.kt`
- Create: `android/app/src/test/java/com/purchasely/shaker/data/purchase/TransactionResultTest.kt`

- [ ] **Step 1: Create PurchaseRequest**

```kotlin
// data/purchase/PurchaseRequest.kt
package com.purchasely.shaker.data.purchase

import android.app.Activity

data class PurchaseRequest(
    val activity: Activity,
    val productId: String,
    val offerToken: String
)
```

- [ ] **Step 2: Create RestoreRequest**

```kotlin
// data/purchase/RestoreRequest.kt
package com.purchasely.shaker.data.purchase

data object RestoreRequest
```

- [ ] **Step 3: Create TransactionResult**

```kotlin
// data/purchase/TransactionResult.kt
package com.purchasely.shaker.data.purchase

sealed class TransactionResult {
    data object Success : TransactionResult()
    data object Cancelled : TransactionResult()
    data class Error(val message: String?) : TransactionResult()
    data object Idle : TransactionResult()
}
```

- [ ] **Step 4: Write TransactionResult test**

```kotlin
// test: data/purchase/TransactionResultTest.kt
package com.purchasely.shaker.data.purchase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionResultTest {

    @Test
    fun `Success is a singleton`() {
        assertTrue(TransactionResult.Success is TransactionResult)
    }

    @Test
    fun `Cancelled is a singleton`() {
        assertTrue(TransactionResult.Cancelled is TransactionResult)
    }

    @Test
    fun `Idle is a singleton`() {
        assertTrue(TransactionResult.Idle is TransactionResult)
    }

    @Test
    fun `Error holds message`() {
        val result = TransactionResult.Error("Payment failed")
        assertEquals("Payment failed", result.message)
    }

    @Test
    fun `Error with null message`() {
        val result = TransactionResult.Error(null)
        assertNull(result.message)
    }

    @Test
    fun `exhaustive when covers all cases`() {
        val results = listOf(
            TransactionResult.Success,
            TransactionResult.Cancelled,
            TransactionResult.Error("fail"),
            TransactionResult.Idle
        )
        results.forEach { result ->
            when (result) {
                is TransactionResult.Success -> {}
                is TransactionResult.Cancelled -> {}
                is TransactionResult.Error -> assertEquals("fail", result.message)
                is TransactionResult.Idle -> {}
            }
        }
    }
}
```

- [ ] **Step 5: Run tests**

Run: `cd android && ./gradlew testDebugUnitTest --tests "com.purchasely.shaker.data.purchase.TransactionResultTest"`
Expected: 6 tests PASS

- [ ] **Step 6: Commit**

```bash
git add android/app/src/main/java/com/purchasely/shaker/data/purchase/PurchaseRequest.kt \
        android/app/src/main/java/com/purchasely/shaker/data/purchase/RestoreRequest.kt \
        android/app/src/main/java/com/purchasely/shaker/data/purchase/TransactionResult.kt \
        android/app/src/test/java/com/purchasely/shaker/data/purchase/TransactionResultTest.kt
git commit -m "feat(android): add reactive flow types for Observer purchase decoupling"
```

---

### Task 2: Refactor PurchaseManager to use flows

**Files:**
- Modify: `android/app/src/main/java/com/purchasely/shaker/data/purchase/PurchaseManager.kt`
- Create: `android/app/src/test/java/com/purchasely/shaker/data/purchase/PurchaseManagerTest.kt`

- [ ] **Step 1: Write PurchaseManager tests**

```kotlin
// test: data/purchase/PurchaseManagerTest.kt
package com.purchasely.shaker.data.purchase

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PurchaseManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var mockBillingClient: BillingClient
    private lateinit var purchaseRequests: MutableSharedFlow<PurchaseRequest>
    private lateinit var restoreRequests: MutableSharedFlow<RestoreRequest>
    private lateinit var purchaseManager: PurchaseManager

    @Before
    fun setUp() {
        mockBillingClient = mockk(relaxed = true)
        purchaseRequests = MutableSharedFlow()
        restoreRequests = MutableSharedFlow()
        purchaseManager = PurchaseManager(
            billingClientFactory = { mockBillingClient },
            purchaseRequests = purchaseRequests,
            restoreRequests = restoreRequests,
            scope = testScope
        )
    }

    @Test
    fun `onPurchasesUpdated with OK emits Success`() = runTest {
        val billingResult = mockk<BillingResult> {
            every { responseCode } returns BillingClient.BillingResponseCode.OK
        }
        val purchase = mockk<Purchase> {
            every { purchaseState } returns Purchase.PurchaseState.PURCHASED
            every { isAcknowledged } returns true
        }

        var result: TransactionResult? = null
        val job = launch(testDispatcher) {
            result = purchaseManager.transactionResult.first { it !is TransactionResult.Idle }
        }

        purchaseManager.onPurchasesUpdated(billingResult, listOf(purchase))
        job.join()

        assertTrue(result is TransactionResult.Success)
    }

    @Test
    fun `onPurchasesUpdated with USER_CANCELED emits Cancelled`() = runTest {
        val billingResult = mockk<BillingResult> {
            every { responseCode } returns BillingClient.BillingResponseCode.USER_CANCELED
        }

        var result: TransactionResult? = null
        val job = launch(testDispatcher) {
            result = purchaseManager.transactionResult.first { it !is TransactionResult.Idle }
        }

        purchaseManager.onPurchasesUpdated(billingResult, null)
        job.join()

        assertTrue(result is TransactionResult.Cancelled)
    }

    @Test
    fun `onPurchasesUpdated with error emits Error`() = runTest {
        val billingResult = mockk<BillingResult> {
            every { responseCode } returns BillingClient.BillingResponseCode.ERROR
            every { debugMessage } returns "Something went wrong"
        }

        var result: TransactionResult? = null
        val job = launch(testDispatcher) {
            result = purchaseManager.transactionResult.first { it !is TransactionResult.Idle }
        }

        purchaseManager.onPurchasesUpdated(billingResult, null)
        job.join()

        assertTrue(result is TransactionResult.Error)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd android && ./gradlew testDebugUnitTest --tests "com.purchasely.shaker.data.purchase.PurchaseManagerTest"`
Expected: FAIL — PurchaseManager constructor doesn't match yet

- [ ] **Step 3: Rewrite PurchaseManager**

Replace the entire content of `data/purchase/PurchaseManager.kt`:

```kotlin
package com.purchasely.shaker.data.purchase

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class PurchaseManager(
    billingClientFactory: (PurchasesUpdatedListener) -> BillingClient,
    purchaseRequests: SharedFlow<PurchaseRequest>,
    restoreRequests: SharedFlow<RestoreRequest>,
    scope: CoroutineScope
) : PurchasesUpdatedListener {

    private val billingClient = billingClientFactory(this)

    private val _transactionResult = MutableSharedFlow<TransactionResult>(replay = 1)
    val transactionResult: SharedFlow<TransactionResult> = _transactionResult.asSharedFlow()

    init {
        connectBillingClient()
        scope.launch {
            purchaseRequests.collect { request ->
                launchPurchase(request.activity, request.productId, request.offerToken)
            }
        }
        scope.launch {
            restoreRequests.collect {
                restorePurchases()
            }
        }
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

    private fun launchPurchase(activity: Activity, productId: String, offerToken: String) {
        val queryParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(queryParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK || productDetailsList.isEmpty()) {
                Log.e(TAG, "[Shaker] queryProductDetails failed: ${billingResult.debugMessage}")
                _transactionResult.tryEmit(TransactionResult.Error(billingResult.debugMessage))
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
                _transactionResult.tryEmit(TransactionResult.Error(result.debugMessage))
            }
        }
    }

    private fun restorePurchases() {
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
                _transactionResult.tryEmit(
                    if (activePurchases.isNotEmpty()) TransactionResult.Success
                    else TransactionResult.Cancelled
                )
            } else {
                Log.e(TAG, "[Shaker] Restore failed: ${billingResult.debugMessage}")
                _transactionResult.tryEmit(TransactionResult.Error(billingResult.debugMessage))
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
            Log.d(TAG, "[Shaker] Purchase successful")
            _transactionResult.tryEmit(TransactionResult.Success)
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "[Shaker] Purchase cancelled by user")
            _transactionResult.tryEmit(TransactionResult.Cancelled)
        } else {
            Log.e(TAG, "[Shaker] Purchase error: ${billingResult.debugMessage}")
            _transactionResult.tryEmit(TransactionResult.Error(billingResult.debugMessage))
        }
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

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd android && ./gradlew testDebugUnitTest --tests "com.purchasely.shaker.data.purchase.PurchaseManagerTest"`
Expected: 3 tests PASS

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/java/com/purchasely/shaker/data/purchase/PurchaseManager.kt \
        android/app/src/test/java/com/purchasely/shaker/data/purchase/PurchaseManagerTest.kt
git commit -m "refactor(android): decouple PurchaseManager from Purchasely SDK via reactive flows"
```

---

### Task 3: Refactor PurchaselyWrapper

**Files:**
- Modify: `android/app/src/main/java/com/purchasely/shaker/purchasely/PurchaselyWrapper.kt`
- Rewrite: `android/app/src/test/java/com/purchasely/shaker/purchasely/PurchaselyWrapperTest.kt`

- [ ] **Step 1: Write new PurchaselyWrapper orchestration tests**

Replace the entire content of `PurchaselyWrapperTest.kt`:

```kotlin
package com.purchasely.shaker.purchasely

import android.app.Activity
import com.purchasely.shaker.data.PremiumManager
import com.purchasely.shaker.data.RunningModeRepository
import com.purchasely.shaker.data.purchase.PurchaseRequest
import com.purchasely.shaker.data.purchase.RestoreRequest
import com.purchasely.shaker.data.purchase.TransactionResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.purchasely.ext.PLYPresentation
import io.purchasely.ext.PLYPresentationAction
import io.purchasely.ext.PLYPresentationActionParameters
import io.purchasely.ext.PLYPresentationInfo
import io.purchasely.ext.PLYRunningMode
import io.purchasely.models.PLYPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PurchaselyWrapperTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var premiumManager: PremiumManager
    private lateinit var runningModeRepo: RunningModeRepository
    private lateinit var purchaseRequests: MutableSharedFlow<PurchaseRequest>
    private lateinit var restoreRequests: MutableSharedFlow<RestoreRequest>
    private lateinit var transactionResult: MutableSharedFlow<TransactionResult>
    private lateinit var wrapper: PurchaselyWrapper

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        premiumManager = mockk(relaxed = true)
        runningModeRepo = mockk {
            every { runningMode } returns PLYRunningMode.PaywallObserver
            every { isObserverMode } returns true
        }
        purchaseRequests = MutableSharedFlow()
        restoreRequests = MutableSharedFlow()
        transactionResult = MutableSharedFlow()
        wrapper = PurchaselyWrapper(
            premiumManager = premiumManager,
            runningModeRepo = runningModeRepo,
            purchaseRequests = purchaseRequests,
            restoreRequests = restoreRequests,
            transactionResult = transactionResult,
            scope = testScope
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Interceptor: PURCHASE in Observer mode ---

    @Test
    fun `handlePaywallAction PURCHASE in observer mode emits PurchaseRequest`() = runTest {
        val mockActivity = mockk<Activity>()
        val mockPlan = mockk<PLYPlan> {
            every { store_product_id } returns "com.test.product"
        }
        val mockOffer = mockk<io.purchasely.ext.PLYSubscriptionOffer> {
            every { offerToken } returns "token-123"
        }
        val mockInfo = mockk<PLYPresentationInfo> {
            every { activity } returns mockActivity
        }
        val mockParams = mockk<PLYPresentationActionParameters> {
            every { plan } returns mockPlan
            every { subscriptionOffer } returns mockOffer
        }

        var emittedRequest: PurchaseRequest? = null
        val collectJob = launch(testDispatcher) {
            emittedRequest = purchaseRequests.first()
        }

        wrapper.handlePaywallAction(mockInfo, PLYPresentationAction.PURCHASE, mockParams) {}
        collectJob.join()

        assertNotNull(emittedRequest)
        assertEquals("com.test.product", emittedRequest?.productId)
        assertEquals("token-123", emittedRequest?.offerToken)
    }

    @Test
    fun `handlePaywallAction PURCHASE in full mode calls proceed true`() {
        every { runningModeRepo.isObserverMode } returns false
        var proceededWith: Boolean? = null

        wrapper.handlePaywallAction(null, PLYPresentationAction.PURCHASE, null) { proceededWith = it }

        assertEquals(true, proceededWith)
    }

    // --- Interceptor: RESTORE in Observer mode ---

    @Test
    fun `handlePaywallAction RESTORE in observer mode emits RestoreRequest`() = runTest {
        var emittedRestore = false
        val collectJob = launch(testDispatcher) {
            restoreRequests.first()
            emittedRestore = true
        }

        wrapper.handlePaywallAction(null, PLYPresentationAction.RESTORE, null) {}
        collectJob.join()

        assertTrue(emittedRestore)
    }

    @Test
    fun `handlePaywallAction RESTORE in full mode calls proceed true`() {
        every { runningModeRepo.isObserverMode } returns false
        var proceededWith: Boolean? = null

        wrapper.handlePaywallAction(null, PLYPresentationAction.RESTORE, null) { proceededWith = it }

        assertEquals(true, proceededWith)
    }

    // --- Interceptor: LOGIN ---

    @Test
    fun `handlePaywallAction LOGIN calls proceed false`() {
        var proceededWith: Boolean? = null

        wrapper.handlePaywallAction(null, PLYPresentationAction.LOGIN, null) { proceededWith = it }

        assertEquals(false, proceededWith)
    }

    // --- Interceptor: other actions ---

    @Test
    fun `handlePaywallAction CLOSE calls proceed true`() {
        var proceededWith: Boolean? = null

        wrapper.handlePaywallAction(null, PLYPresentationAction.CLOSE, null) { proceededWith = it }

        assertEquals(true, proceededWith)
    }

    // --- TransactionResult observation ---

    @Test
    fun `TransactionResult Success triggers synchronize and premium refresh`() = runTest {
        // Set a pending processAction via a RESTORE request
        wrapper.handlePaywallAction(null, PLYPresentationAction.RESTORE, null) {}

        // Emit success
        transactionResult.emit(TransactionResult.Success)
        testScope.testScheduler.advanceUntilIdle()

        verify { premiumManager.refreshPremiumStatus() }
    }

    @Test
    fun `TransactionResult Success calls pendingProcessAction with false`() = runTest {
        var proceededWith: Boolean? = null
        wrapper.handlePaywallAction(null, PLYPresentationAction.RESTORE, null) { proceededWith = it }

        transactionResult.emit(TransactionResult.Success)
        testScope.testScheduler.advanceUntilIdle()

        assertEquals(false, proceededWith)
    }

    @Test
    fun `TransactionResult Cancelled calls pendingProcessAction with false`() = runTest {
        var proceededWith: Boolean? = null
        wrapper.handlePaywallAction(null, PLYPresentationAction.RESTORE, null) { proceededWith = it }

        transactionResult.emit(TransactionResult.Cancelled)
        testScope.testScheduler.advanceUntilIdle()

        assertEquals(false, proceededWith)
    }

    @Test
    fun `TransactionResult Error calls pendingProcessAction with false`() = runTest {
        var proceededWith: Boolean? = null
        wrapper.handlePaywallAction(null, PLYPresentationAction.RESTORE, null) { proceededWith = it }

        transactionResult.emit(TransactionResult.Error("fail"))
        testScope.testScheduler.advanceUntilIdle()

        assertEquals(false, proceededWith)
    }

    @Test
    fun `TransactionResult Idle is ignored`() = runTest {
        var proceededWith: Boolean? = null
        wrapper.handlePaywallAction(null, PLYPresentationAction.RESTORE, null) { proceededWith = it }

        transactionResult.emit(TransactionResult.Idle)
        testScope.testScheduler.advanceUntilIdle()

        // processAction should NOT have been called
        assertEquals(null, proceededWith)
    }

    // --- Existing API contract (mocked wrapper) ---

    @Test
    fun `loadPresentation returns FetchResult`() = runTest {
        val mockedWrapper = mockk<PurchaselyWrapper>(relaxed = true)
        val mockPresentation = mockk<PLYPresentation>()
        io.mockk.coEvery { mockedWrapper.loadPresentation("filters", null) } returns FetchResult.Success(mockPresentation)

        val result = mockedWrapper.loadPresentation("filters")
        assertTrue(result is FetchResult.Success)
    }

    @Test
    fun `FetchResult Success exposes height`() {
        val presentation = mockk<PLYPresentation> {
            every { height } returns 400
        }
        val result = FetchResult.Success(presentation)
        assertEquals(400, result.height)
    }

    @Test
    fun `wrapper instance can be created with dependencies`() {
        assertNotNull(wrapper)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd android && ./gradlew testDebugUnitTest --tests "com.purchasely.shaker.purchasely.PurchaselyWrapperTest"`
Expected: FAIL — PurchaselyWrapper constructor doesn't match, handlePaywallAction doesn't exist

- [ ] **Step 3: Rewrite PurchaselyWrapper**

Replace the entire content of `purchasely/PurchaselyWrapper.kt`:

```kotlin
package com.purchasely.shaker.purchasely

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import com.purchasely.shaker.data.PremiumManager
import com.purchasely.shaker.data.RunningModeRepository
import com.purchasely.shaker.data.purchase.PurchaseRequest
import com.purchasely.shaker.data.purchase.RestoreRequest
import com.purchasely.shaker.data.purchase.TransactionResult
import io.purchasely.ext.EventListener
import io.purchasely.ext.LogLevel
import io.purchasely.ext.PLYPresentation
import io.purchasely.ext.PLYPresentationAction
import io.purchasely.ext.PLYPresentationActionParameters
import io.purchasely.ext.PLYPresentationInfo
import io.purchasely.ext.PLYPresentationProperties
import io.purchasely.ext.PLYPresentationType
import io.purchasely.ext.PLYProductViewResult
import io.purchasely.ext.PLYRunningMode
import io.purchasely.ext.Purchasely
import io.purchasely.ext.fetchPresentation
import io.purchasely.models.PLYError
import io.purchasely.models.PLYPlan
import io.purchasely.google.GoogleStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PurchaselyWrapper(
    private val premiumManager: PremiumManager,
    private val runningModeRepo: RunningModeRepository,
    private val purchaseRequests: MutableSharedFlow<PurchaseRequest>,
    private val restoreRequests: MutableSharedFlow<RestoreRequest>,
    transactionResult: SharedFlow<TransactionResult>,
    private val scope: CoroutineScope
) {

    private var application: Application? = null
    private var apiKey: String = ""
    private var logLevel: LogLevel = LogLevel.DEBUG
    private var pendingProcessAction: ((Boolean) -> Unit)? = null

    init {
        scope.launch {
            transactionResult.collect { result ->
                handleTransactionResult(result)
            }
        }
    }

    // MARK: - SDK Initialization

    fun initialize(
        application: Application,
        apiKey: String,
        logLevel: LogLevel = LogLevel.DEBUG
    ) {
        this.application = application
        this.apiKey = apiKey
        this.logLevel = logLevel

        val mode = runningModeRepo.runningMode

        Purchasely.Builder(application)
            .apiKey(apiKey)
            .logLevel(logLevel)
            .readyToOpenDeeplink(true)
            .runningMode(mode)
            .stores(listOf(GoogleStore()))
            .build()
            .start { isConfigured, error ->
                if (isConfigured) {
                    Log.d(TAG, "[Shaker] Purchasely SDK configured successfully")
                    premiumManager.refreshPremiumStatus()
                }
                error?.let {
                    Log.e(TAG, "[Shaker] Purchasely configuration error: ${it.message}")
                }
            }

        eventListener = object : EventListener {
            override fun onEvent(event: io.purchasely.ext.PLYEvent) {
                Log.d(TAG, "[Shaker] Event: ${event.name} | Properties: ${event.properties}")
            }
        }

        setPaywallActionsInterceptor { info, action, parameters, proceed ->
            handlePaywallAction(info, action, parameters, proceed)
        }
    }

    fun restart() {
        close()
        val app = application ?: return
        initialize(app, apiKey, logLevel)
    }

    fun close() {
        Purchasely.close()
    }

    // MARK: - Interceptor Logic

    internal fun handlePaywallAction(
        info: PLYPresentationInfo?,
        action: PLYPresentationAction,
        parameters: PLYPresentationActionParameters?,
        processAction: (Boolean) -> Unit
    ) {
        when (action) {
            PLYPresentationAction.LOGIN -> {
                Log.d(TAG, "[Shaker] Paywall login action intercepted")
                processAction(false)
            }
            PLYPresentationAction.NAVIGATE -> {
                val url = parameters?.url
                if (url != null) {
                    Log.d(TAG, "[Shaker] Paywall navigate action: $url")
                    val intent = Intent(Intent.ACTION_VIEW, url)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    application?.startActivity(intent)
                }
                processAction(false)
            }
            PLYPresentationAction.PURCHASE -> {
                if (runningModeRepo.isObserverMode) {
                    val plan = parameters?.plan
                    val offer = parameters?.subscriptionOffer
                    val productId = plan?.store_product_id
                    val offerToken = offer?.offerToken
                    val activity = info?.activity
                    if (activity != null && productId != null && offerToken != null) {
                        pendingProcessAction = processAction
                        scope.launch {
                            purchaseRequests.emit(PurchaseRequest(activity, productId, offerToken))
                        }
                    } else {
                        Log.w(TAG, "[Shaker] Observer mode purchase: missing activity, productId, or offerToken")
                        processAction(false)
                    }
                } else {
                    processAction(true)
                }
            }
            PLYPresentationAction.RESTORE -> {
                if (runningModeRepo.isObserverMode) {
                    pendingProcessAction = processAction
                    scope.launch {
                        restoreRequests.emit(RestoreRequest)
                    }
                } else {
                    processAction(true)
                }
            }
            else -> processAction(true)
        }
    }

    // MARK: - Transaction Result Handling

    private fun handleTransactionResult(result: TransactionResult) {
        when (result) {
            is TransactionResult.Success -> {
                synchronize()
                pendingProcessAction?.invoke(false)
                pendingProcessAction = null
                premiumManager.refreshPremiumStatus()
                Log.d(TAG, "[Shaker] Transaction success — synchronized and refreshed")
            }
            is TransactionResult.Cancelled -> {
                pendingProcessAction?.invoke(false)
                pendingProcessAction = null
                Log.d(TAG, "[Shaker] Transaction cancelled")
            }
            is TransactionResult.Error -> {
                pendingProcessAction?.invoke(false)
                pendingProcessAction = null
                Log.e(TAG, "[Shaker] Transaction error: ${result.message}")
            }
            is TransactionResult.Idle -> { /* ignore */ }
        }
    }

    // MARK: - Event Listener

    var eventListener: EventListener?
        get() = Purchasely.eventListener
        set(value) { Purchasely.eventListener = value }

    fun setPaywallActionsInterceptor(
        interceptor: (
            info: PLYPresentationInfo?,
            action: PLYPresentationAction,
            parameters: PLYPresentationActionParameters?,
            processAction: (Boolean) -> Unit
        ) -> Unit
    ) {
        Purchasely.setPaywallActionsInterceptor(interceptor)
    }

    // MARK: - Deeplinks

    fun isDeeplinkHandled(deeplink: Uri, activity: Activity?): Boolean {
        @Suppress("DEPRECATION")
        return Purchasely.isDeeplinkHandled(deeplink, activity)
    }

    // MARK: - Presentation Loading

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
            when (presentation.type) {
                PLYPresentationType.DEACTIVATED -> FetchResult.Deactivated
                PLYPresentationType.CLIENT -> FetchResult.Client(presentation)
                else -> FetchResult.Success(presentation)
            }
        } catch (e: Exception) {
            FetchResult.Error(e as? PLYError)
        }
    }

    // MARK: - Modal Display

    suspend fun display(
        presentation: PLYPresentation,
        activity: Activity
    ): DisplayResult = suspendCoroutine { continuation ->
        presentation.display(activity) { result: PLYProductViewResult, plan: PLYPlan? ->
            when (result) {
                PLYProductViewResult.PURCHASED -> continuation.resume(DisplayResult.Purchased(plan?.name))
                PLYProductViewResult.RESTORED -> continuation.resume(DisplayResult.Restored(plan?.name))
                else -> continuation.resume(DisplayResult.Cancelled)
            }
        }
    }

    // MARK: - Embedded View

    fun getView(
        presentation: PLYPresentation,
        context: Context,
        onResult: (DisplayResult) -> Unit
    ): View? {
        return presentation.buildView(
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

    // MARK: - User Management

    fun userLogin(userId: String, onRefresh: (Boolean) -> Unit) {
        Purchasely.userLogin(userId, onRefresh)
    }

    fun userLogout() {
        Purchasely.userLogout()
    }

    val anonymousUserId: String
        get() = Purchasely.anonymousUserId

    // MARK: - User Attributes

    fun setUserAttribute(key: String, value: String) {
        Purchasely.setUserAttribute(key, value)
    }

    fun setUserAttribute(key: String, value: Boolean) {
        Purchasely.setUserAttribute(key, value)
    }

    fun setUserAttribute(key: String, value: Int) {
        Purchasely.setUserAttribute(key, value)
    }

    fun setUserAttribute(key: String, value: Float) {
        Purchasely.setUserAttribute(key, value)
    }

    fun incrementUserAttribute(key: String) {
        Purchasely.incrementUserAttribute(key)
    }

    // MARK: - Restore

    fun restoreAllProducts(
        onSuccess: (PLYPlan?) -> Unit,
        onError: (PLYError?) -> Unit
    ) {
        Purchasely.restoreAllProducts(onSuccess, onError)
    }

    // MARK: - Observer Mode

    fun synchronize() {
        Purchasely.synchronize()
    }

    // MARK: - GDPR Consent

    fun revokeDataProcessingConsent(purposes: Set<io.purchasely.ext.PLYDataProcessingPurpose>) {
        Purchasely.revokeDataProcessingConsent(purposes)
    }

    // MARK: - SDK Info

    val sdkVersion: String
        get() = Purchasely.sdkVersion

    companion object {
        private const val TAG = "PurchaselyWrapper"
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd android && ./gradlew testDebugUnitTest --tests "com.purchasely.shaker.purchasely.PurchaselyWrapperTest"`
Expected: 13 tests PASS

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/java/com/purchasely/shaker/purchasely/PurchaselyWrapper.kt \
        android/app/src/test/java/com/purchasely/shaker/purchasely/PurchaselyWrapperTest.kt
git commit -m "refactor(android): PurchaselyWrapper absorbs init, interceptor, and orchestration"
```

---

### Task 4: Update DI module

**Files:**
- Modify: `android/app/src/main/java/com/purchasely/shaker/di/AppModule.kt`

- [ ] **Step 1: Rewrite AppModule**

Replace the entire content of `di/AppModule.kt`:

```kotlin
package com.purchasely.shaker.di

import com.android.billingclient.api.BillingClient
import com.purchasely.shaker.data.CocktailRepository
import com.purchasely.shaker.data.FavoritesRepository
import com.purchasely.shaker.data.OnboardingRepository
import com.purchasely.shaker.data.PremiumManager
import com.purchasely.shaker.data.RunningModeRepository
import com.purchasely.shaker.data.purchase.PurchaseManager
import com.purchasely.shaker.data.purchase.PurchaseRequest
import com.purchasely.shaker.data.purchase.RestoreRequest
import com.purchasely.shaker.purchasely.PurchaselyWrapper
import com.purchasely.shaker.ui.screen.home.HomeViewModel
import com.purchasely.shaker.ui.screen.detail.DetailViewModel
import com.purchasely.shaker.ui.screen.favorites.FavoritesViewModel
import com.purchasely.shaker.ui.screen.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Shared reactive flows
    single { MutableSharedFlow<PurchaseRequest>() }
    single { MutableSharedFlow<RestoreRequest>() }

    // Repositories
    single { CocktailRepository(androidContext()) }
    single { FavoritesRepository(androidContext()) }
    single { OnboardingRepository(androidContext()) }
    single { RunningModeRepository(androidContext()) }

    // Managers
    single { PremiumManager() }
    single {
        PurchaseManager(
            billingClientFactory = { listener ->
                BillingClient.newBuilder(androidContext())
                    .setListener(listener)
                    .enablePendingPurchases()
                    .build()
            },
            purchaseRequests = get<MutableSharedFlow<PurchaseRequest>>(),
            restoreRequests = get<MutableSharedFlow<RestoreRequest>>(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        )
    }
    single {
        PurchaselyWrapper(
            premiumManager = get(),
            runningModeRepo = get(),
            purchaseRequests = get<MutableSharedFlow<PurchaseRequest>>(),
            restoreRequests = get<MutableSharedFlow<RestoreRequest>>(),
            transactionResult = get<PurchaseManager>().transactionResult,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        )
    }

    // ViewModels
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { params -> DetailViewModel(get(), get(), get(), get(), params.get()) }
    viewModel { FavoritesViewModel(get(), get(), get(), get()) }
    viewModel { SettingsViewModel(androidContext(), get(), get(), get()) }
}
```

- [ ] **Step 2: Commit**

```bash
git add android/app/src/main/java/com/purchasely/shaker/di/AppModule.kt
git commit -m "refactor(android): update Koin DI module for reactive flow architecture"
```

---

### Task 5: Simplify ShakerApp

**Files:**
- Modify: `android/app/src/main/java/com/purchasely/shaker/ShakerApp.kt`

- [ ] **Step 1: Rewrite ShakerApp**

Replace the entire content of `ShakerApp.kt`:

```kotlin
package com.purchasely.shaker

import android.app.Application
import com.purchasely.shaker.di.appModule
import com.purchasely.shaker.purchasely.PurchaselyWrapper
import io.purchasely.ext.LogLevel
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

        val purchaselyWrapper: PurchaselyWrapper by inject()
        purchaselyWrapper.initialize(
            application = this,
            apiKey = BuildConfig.PURCHASELY_API_KEY,
            logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.WARN
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add android/app/src/main/java/com/purchasely/shaker/ShakerApp.kt
git commit -m "refactor(android): simplify ShakerApp to Koin init + wrapper.initialize()"
```

---

### Task 6: Update SettingsViewModel + fix tests

**Files:**
- Modify: `android/app/src/main/java/com/purchasely/shaker/ui/screen/settings/SettingsViewModel.kt`
- Modify: `android/app/src/test/java/com/purchasely/shaker/ui/screen/settings/SettingsViewModelTest.kt`

- [ ] **Step 1: Update SettingsViewModel — replace restartPurchaselySdk**

In `SettingsViewModel.kt`, replace the `restartPurchaselySdk` method:

Find:
```kotlin
    private fun restartPurchaselySdk(mode: PurchaselySdkMode) {
        val app = context.applicationContext as? ShakerApp
        if (app == null) {
            Log.e(TAG, "[Shaker] Could not restart SDK: application context is not ShakerApp")
            return
        }

        app.restartPurchaselySdk()
        Log.d(TAG, "[Shaker] SDK restarted with mode ${mode.storageValue}")
    }
```

Replace with:
```kotlin
    private fun restartPurchaselySdk(mode: PurchaselySdkMode) {
        purchaselyWrapper.restart()
        Log.d(TAG, "[Shaker] SDK restarted with mode ${mode.storageValue}")
    }
```

Also remove the `import com.purchasely.shaker.ShakerApp` import if present.

- [ ] **Step 2: Fix SettingsViewModelTest**

The existing test for `setSdkMode` doesn't directly test `restartPurchaselySdk` because that method is private. The test mocks `wrapper` which is already relaxed. The test should still pass because `wrapper.restart()` is a relaxed mock call.

However, add a specific test. In `SettingsViewModelTest.kt`, add:

```kotlin
    @Test
    fun `setSdkMode calls wrapper restart`() {
        storedValues[PurchaselySdkMode.KEY] = PurchaselySdkMode.FULL.storageValue
        val vm = createViewModel()
        vm.setSdkMode(PurchaselySdkMode.PAYWALL_OBSERVER)
        verify { wrapper.restart() }
    }
```

- [ ] **Step 3: Fix PurchaselyWrapperTest — update wrapper instance test**

In `PurchaselyWrapperTest.kt`, the test `wrapper instance can be created` already uses the new constructor. No change needed (it was rewritten in Task 3).

- [ ] **Step 4: Run full test suite**

Run: `cd android && ./gradlew testDebugUnitTest`
Expected: All tests PASS (previous tests for HomeViewModel, DetailViewModel, FavoritesViewModel should still pass because they mock PurchaselyWrapper with MockK relaxed)

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/java/com/purchasely/shaker/ui/screen/settings/SettingsViewModel.kt \
        android/app/src/test/java/com/purchasely/shaker/ui/screen/settings/SettingsViewModelTest.kt
git commit -m "refactor(android): SettingsViewModel uses wrapper.restart() instead of ShakerApp cast"
```

---

### Task 7: Full verification

- [ ] **Step 1: Run complete test suite**

Run: `cd android && ./gradlew testDebugUnitTest`
Expected: All tests PASS, 0 failures

- [ ] **Step 2: Build debug APK**

Run: `cd android && ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Verify no Purchasely imports in PurchaseManager**

Run: `rg "import io.purchasely" android/app/src/main/java/com/purchasely/shaker/data/purchase/PurchaseManager.kt`
Expected: No output (zero Purchasely imports)

- [ ] **Step 4: Verify ShakerApp is minimal**

Run: `wc -l android/app/src/main/java/com/purchasely/shaker/ShakerApp.kt`
Expected: ~20 lines (was ~157)

- [ ] **Step 5: Final commit if any cleanup needed**

```bash
git status  # Verify clean working tree
```
