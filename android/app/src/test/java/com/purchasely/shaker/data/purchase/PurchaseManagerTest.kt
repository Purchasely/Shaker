package com.purchasely.shaker.data.purchase

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
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
