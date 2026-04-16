package com.purchasely.shaker.purchasely

import android.app.Activity
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

    private lateinit var onTransactionCompletedCallback: (() -> Unit)
    private lateinit var runningModeRepo: RunningModeRepository
    private lateinit var purchaseRequests: MutableSharedFlow<PurchaseRequest>
    private lateinit var restoreRequests: MutableSharedFlow<RestoreRequest>
    private lateinit var transactionResult: MutableSharedFlow<TransactionResult>
    private lateinit var wrapper: PurchaselyWrapper

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        onTransactionCompletedCallback = mockk(relaxed = true)
        runningModeRepo = mockk {
            every { runningMode } returns PLYRunningMode.PaywallObserver
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
            scope = testScope
        ).also {
            it.onTransactionCompleted = onTransactionCompletedCallback
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Interceptor: PURCHASE in Observer mode ---

    @Test
    fun `handlePaywallAction PURCHASE in observer mode emits PurchaseRequest`() = runTest {
        val mockActivity = mockk<Activity>()
        val mockPlan = mockk<io.purchasely.models.PLYPlan> {
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
    fun `TransactionResult Success triggers onTransactionCompleted callback`() = runTest {
        wrapper.handlePaywallAction(null, PLYPresentationAction.RESTORE, null) {}
        transactionResult.emit(TransactionResult.Success)
        testScope.testScheduler.advanceUntilIdle()
        verify { onTransactionCompletedCallback.invoke() }
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
        assertEquals(null, proceededWith)
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
}
