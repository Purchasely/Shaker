package com.purchasely.shaker.purchasely

import android.app.Activity
import com.purchasely.shaker.data.PurchaselySdkMode
import com.purchasely.shaker.data.RunningModeRepository
import com.purchasely.shaker.data.purchase.PurchaseRequest
import com.purchasely.shaker.data.purchase.RestoreRequest
import com.purchasely.shaker.data.purchase.TransactionResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.purchasely.ext.PLYInterceptResult
import io.purchasely.ext.PLYInterceptorInfo
import io.purchasely.ext.presentation.PLYPresentation
import io.purchasely.ext.presentation.PLYPresentationAction
import io.purchasely.ext.presentation.PLYPresentationOutcome
import io.purchasely.ext.presentation.PLYPurchaseResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

    // Wrapper gets a standalone scope so its long-lived collectionJob does not
    // keep runTest's scope busy after each test.
    private lateinit var wrapperScope: CoroutineScope

    private lateinit var onTransactionCompletedCallback: (() -> Unit)
    private lateinit var runningModeRepo: RunningModeRepository
    private lateinit var purchaseRequests: MutableSharedFlow<PurchaseRequest>
    private lateinit var restoreRequests: MutableSharedFlow<RestoreRequest>
    private lateinit var transactionResult: MutableSharedFlow<TransactionResult>
    private lateinit var wrapper: PurchaselyWrapper

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        wrapperScope = CoroutineScope(testDispatcher + Job())
        onTransactionCompletedCallback = mockk(relaxed = true)
        runningModeRepo = mockk {
            every { sdkMode } returns PurchaselySdkMode.OBSERVER
            every { isObserverMode } returns true
        }
        purchaseRequests = MutableSharedFlow()
        restoreRequests = MutableSharedFlow()
        transactionResult = MutableSharedFlow()
        wrapper = PurchaselyWrapper(
            runningModeRepo = runningModeRepo,
            purchaseRequests = purchaseRequests,
            restoreRequests = restoreRequests,
            transactionResult = transactionResult,
            scope = wrapperScope
        ).also {
            it.onTransactionCompleted = onTransactionCompletedCallback
        }
    }

    @After
    fun tearDown() {
        wrapperScope.cancel()
        Dispatchers.resetMain()
    }

    // --- Interceptor: PURCHASE in Observer mode ---

    @Test
    fun `handlePurchase in observer mode emits PurchaseRequest`() = runTest(testDispatcher) {
        val mockActivity = mockk<Activity>()
        val mockPlan = mockk<io.purchasely.models.PLYPlan> {
            every { store_product_id } returns "com.test.product"
            every { name } returns "Monthly"
        }
        val mockOffer = mockk<io.purchasely.ext.presentation.PLYSubscriptionOffer> {
            every { offerToken } returns "token-123"
        }
        val mockInfo = mockk<PLYInterceptorInfo> {
            every { activity } returns mockActivity
        }
        val purchase = mockk<PLYPresentationAction.Purchase> {
            every { plan } returns mockPlan
            every { subscriptionOffer } returns mockOffer
            every { offer } returns null
        }

        var emittedRequest: PurchaseRequest? = null
        val collectJob = launch(testDispatcher) {
            emittedRequest = purchaseRequests.first()
        }

        // handlePurchase suspends until pendingResult is resolved. Run it in a
        // child coroutine so the test can keep observing the emitted request.
        val interceptJob = async(testDispatcher) {
            wrapper.handlePurchase(mockInfo, purchase)
        }
        collectJob.join()

        assertNotNull(emittedRequest)
        assertEquals("com.test.product", emittedRequest?.productId)
        assertEquals("token-123", emittedRequest?.offerToken)

        // Clean up: resolve the suspending interceptor so the async coroutine completes.
        transactionResult.emit(TransactionResult.Cancelled)
        interceptJob.await()
    }

    @Test
    fun `handlePurchase in full mode returns NOT_HANDLED`() = runTest(testDispatcher) {
        every { runningModeRepo.isObserverMode } returns false
        val purchase = mockk<PLYPresentationAction.Purchase>(relaxed = true)
        val result = wrapper.handlePurchase(null, purchase)
        assertEquals(PLYInterceptResult.NOT_HANDLED, result)
    }

    @Test
    fun `handlePurchase in observer mode returns FAILED when required purchase data is missing`() = runTest(testDispatcher) {
        val mockPlan = mockk<io.purchasely.models.PLYPlan> {
            every { store_product_id } returns null
        }
        val purchase = mockk<PLYPresentationAction.Purchase> {
            every { plan } returns mockPlan
            every { subscriptionOffer } returns null
        }

        val result = wrapper.handlePurchase(null, purchase)

        assertEquals(PLYInterceptResult.FAILED, result)
    }

    // --- Interceptor: RESTORE in Observer mode ---

    @Test
    fun `handleRestore in observer mode emits RestoreRequest`() = runTest(testDispatcher) {
        var emittedRestore = false
        val collectJob = launch(testDispatcher) {
            restoreRequests.first()
            emittedRestore = true
        }

        val interceptJob = async(testDispatcher) {
            wrapper.handleRestore()
        }
        collectJob.join()

        assertTrue(emittedRestore)

        transactionResult.emit(TransactionResult.Cancelled)
        interceptJob.await()
    }

    @Test
    fun `starting a second observer action blocks SDK fallback for previous pending action`() = runTest(testDispatcher) {
        val firstSubscriber = launch(testDispatcher) { restoreRequests.first() }
        val firstIntercept = wrapperScope.async { wrapper.handleRestore() }
        firstSubscriber.join()

        val secondSubscriber = launch(testDispatcher) { restoreRequests.first() }
        val secondIntercept = wrapperScope.async { wrapper.handleRestore() }
        secondSubscriber.join()

        assertEquals(PLYInterceptResult.SUCCESS, firstIntercept.await())

        transactionResult.emit(TransactionResult.Cancelled)
        assertEquals(PLYInterceptResult.SUCCESS, secondIntercept.await())
    }

    @Test
    fun `handleRestore in full mode returns NOT_HANDLED`() = runTest(testDispatcher) {
        every { runningModeRepo.isObserverMode } returns false
        val result = wrapper.handleRestore()
        assertEquals(PLYInterceptResult.NOT_HANDLED, result)
    }

    // --- TransactionResult observation ---

    @Test
    fun `TransactionResult Success defers onTransactionCompleted to success_payment chain`() = runTest(testDispatcher) {
        val subscriber = launch(testDispatcher) { restoreRequests.first() }
        val interceptJob = wrapperScope.async { wrapper.handleRestore() }
        subscriber.join()

        transactionResult.emit(TransactionResult.Success)
        val result = interceptJob.await()

        // PURCHASELY: onTransactionCompleted is no longer invoked synchronously at
        // TransactionResult.Success — it is deferred to after the success_payment
        // screen closes (chained by display() once pendingSuccessfulPurchase is consumed).
        verify(exactly = 0) { onTransactionCompletedCallback.invoke() }
        assertEquals(PLYInterceptResult.SUCCESS, result)
    }

    @Test
    fun `observer purchase success maps cancelled SDK outcome to purchased display result`() = runTest(testDispatcher) {
        val mockActivity = mockk<Activity>()
        val mockPlan = mockk<io.purchasely.models.PLYPlan> {
            every { store_product_id } returns "com.test.product"
            every { name } returns "Monthly"
        }
        val mockOffer = mockk<io.purchasely.ext.presentation.PLYSubscriptionOffer> {
            every { offerToken } returns "token-123"
        }
        val mockInfo = mockk<PLYInterceptorInfo> {
            every { activity } returns mockActivity
        }
        val purchase = mockk<PLYPresentationAction.Purchase> {
            every { plan } returns mockPlan
            every { subscriptionOffer } returns mockOffer
            every { offer } returns null
        }
        val subscriber = launch(testDispatcher) { purchaseRequests.first() }
        val interceptJob = wrapperScope.async { wrapper.handlePurchase(mockInfo, purchase) }
        subscriber.join()

        transactionResult.emit(TransactionResult.Success)
        assertEquals(PLYInterceptResult.SUCCESS, interceptJob.await())

        val mapped = wrapper.mapOutcomeForTest(
            PLYPresentationOutcome(purchaseResult = PLYPurchaseResult.CANCELLED)
        )

        assertEquals(DisplayResult.Purchased("Monthly"), mapped)
    }

    @Test
    fun `TransactionResult Cancelled resolves pendingResult with SUCCESS`() = runTest(testDispatcher) {
        val subscriber = launch(testDispatcher) { restoreRequests.first() }
        val interceptJob = wrapperScope.async { wrapper.handleRestore() }
        subscriber.join()

        transactionResult.emit(TransactionResult.Cancelled)
        // Observer mode resolves a cancelled transaction with SUCCESS to block the SDK's
        // default purchase/restore flow (v6 equivalent of v5 processAction(false)).
        assertEquals(PLYInterceptResult.SUCCESS, interceptJob.await())
    }

    @Test
    fun `TransactionResult Error resolves pendingResult with FAILED`() = runTest(testDispatcher) {
        val subscriber = launch(testDispatcher) { restoreRequests.first() }
        val interceptJob = wrapperScope.async { wrapper.handleRestore() }
        subscriber.join()

        transactionResult.emit(TransactionResult.Error("fail"))
        assertEquals(PLYInterceptResult.FAILED, interceptJob.await())
    }

    // --- Existing API contract ---

    @Test
    fun `loadPresentation returns FetchResult via mocked wrapper`() = runTest {
        val mockedWrapper = mockk<PurchaselyWrapper>(relaxed = true)
        val mockPresentation = mockk<PLYPresentation>()
        val handle = PresentationHandle(mockPresentation)
        io.mockk.coEvery { mockedWrapper.loadPresentation("filters", null) } returns FetchResult.Success(handle, 300)
        val result = mockedWrapper.loadPresentation("filters")
        assertTrue(result is FetchResult.Success)
    }

    @Test
    fun `FetchResult Success exposes height`() {
        val presentation = mockk<PLYPresentation>()
        val handle = PresentationHandle(presentation)
        val result = FetchResult.Success(handle, 400)
        assertEquals(400, result.height)
    }

    @Test
    fun `wrapper instance can be created with dependencies`() {
        assertNotNull(wrapper)
    }

    private fun PurchaselyWrapper.mapOutcomeForTest(outcome: PLYPresentationOutcome): DisplayResult {
        val method = PurchaselyWrapper::class.java.getDeclaredMethod(
            "toDisplayResult",
            PLYPresentationOutcome::class.java
        )
        method.isAccessible = true
        return method.invoke(this, outcome) as DisplayResult
    }
}
