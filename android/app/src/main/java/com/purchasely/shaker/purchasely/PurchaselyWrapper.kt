package com.purchasely.shaker.purchasely

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import com.purchasely.shaker.data.PurchaselySdkMode
import com.purchasely.shaker.data.RunningModeRepository
import com.purchasely.shaker.domain.model.ConsentPurpose
import com.purchasely.shaker.data.purchase.PurchaseRequest
import com.purchasely.shaker.data.purchase.RestoreRequest
import com.purchasely.shaker.data.purchase.TransactionResult
import io.purchasely.ext.EventListener
import io.purchasely.ext.LogLevel
import io.purchasely.ext.PLYDataProcessingPurpose
import io.purchasely.ext.PLYInterceptResult
import io.purchasely.ext.PLYInterceptorInfo
import io.purchasely.ext.PLYRunningMode
import io.purchasely.ext.Purchasely
import io.purchasely.ext.SubscriptionsListener
import io.purchasely.ext.interceptAction
import io.purchasely.models.PLYSubscriptionData
import io.purchasely.ext.presentation.PLYPresentation
import io.purchasely.ext.presentation.PLYPresentationAction
import io.purchasely.ext.presentation.PLYPresentationOutcome
import io.purchasely.ext.presentation.PLYPresentationType
import io.purchasely.ext.presentation.PLYPurchaseResult
import io.purchasely.ext.presentation.display
import io.purchasely.ext.presentation.preload
import io.purchasely.google.GoogleStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class PurchaselyWrapper(
    private val runningModeRepo: RunningModeRepository,
    private val purchaseRequests: MutableSharedFlow<PurchaseRequest>,
    private val restoreRequests: MutableSharedFlow<RestoreRequest>,
    private val transactionResult: SharedFlow<TransactionResult>,
    private val scope: CoroutineScope
) {

    var onTransactionCompleted: (() -> Unit)? = null

    private var application: Application? = null
    private var apiKey: String = ""
    private var logLevel: LogLevel = LogLevel.DEBUG
    private var onConfiguredCallback: (() -> Unit)? = null
    private var pendingResult: ((PLYInterceptResult) -> Unit)? = null
    private var collectionJob: Job? = null
    private var pendingObserverAction: ObserverAction? = null

    // PURCHASELY: In Observer mode, the app handles Billing itself, so the SDK-side
    // PLYPresentationOutcome still defaults to CANCELLED when we close the paywall.
    // Keep the app-owned successful result here and merge it when mapping outcomes.
    private var pendingSuccessfulObserverResult: DisplayResult? = null

    private sealed class ObserverAction {
        data class Purchase(val planName: String?) : ObserverAction()
        data object Restore : ObserverAction()
    }

    init {
        startTransactionCollection()
    }

    private fun startTransactionCollection() {
        collectionJob?.cancel()
        collectionJob = scope.launch {
            transactionResult.collect { result ->
                handleTransactionResult(result)
            }
        }
    }

    // MARK: - SDK Initialization

    fun initialize(
        application: Application,
        apiKey: String,
        verboseLogging: Boolean = true,
        onConfigured: (() -> Unit)? = null
    ) {
        this.application = application
        this.apiKey = apiKey
        // The caller stays SDK-free: a boolean is mapped to the SDK LogLevel here.
        this.logLevel = if (verboseLogging) LogLevel.DEBUG else LogLevel.WARN

        this.onConfiguredCallback = onConfigured

        // Map the app-level mode to the SDK type — the rest of the app never sees PLYRunningMode.
        val mode = when (runningModeRepo.sdkMode) {
            PurchaselySdkMode.OBSERVER -> PLYRunningMode.Observer
            PurchaselySdkMode.FULL -> PLYRunningMode.Full
        }

        // PURCHASELY (v6): use the Kotlin DSL entrypoint. `Purchasely { ... }` configures
        // and starts the SDK in one call — no .build()/.start() chain. For Java callers,
        // fall back to the fluent Purchasely.Builder(...).build().start { ... }.
        Purchasely {
            context(application)
            apiKey(apiKey)
            logLevel(logLevel)
            allowDeeplink(true)
            runningMode(mode)
            stores(listOf(GoogleStore()))
            onInitialized { error ->
                if (error == null) {
                    Log.d(TAG, "[Shaker] Purchasely SDK configured successfully")
                    onConfigured?.invoke()
                } else {
                    Log.e(TAG, "[Shaker] Purchasely configuration error: ${error.message}")
                }
            }
        }

        eventListener = object : EventListener {
            override fun onEvent(event: io.purchasely.ext.PLYEvent) {
                Log.d(TAG, "[Shaker] Event: ${event.name} | Properties: ${event.properties}")
            }
        }

        registerActionInterceptors()

        startTransactionCollection()
    }

    fun restart() {
        close()
        val app = application ?: return
        initialize(app, apiKey, logLevel == LogLevel.DEBUG, onConfiguredCallback)
    }

    fun close() {
        collectionJob?.cancel()
        collectionJob = null
        // A pending Observer-mode action was already claimed by the app; resolve it
        // as handled so the SDK does not fall back to its default purchase/restore flow.
        pendingResult?.invoke(PLYInterceptResult.SUCCESS)
        pendingResult = null
        pendingObserverAction = null
        pendingSuccessfulObserverResult = null
        Purchasely.removeAllActionInterceptors()
        Purchasely.close()
    }

    // MARK: - Interceptor Logic

    private fun registerActionInterceptors() {
        Purchasely.interceptAction<PLYPresentationAction.Login> { _, _ ->
            Log.d(TAG, "[Shaker] Presentation login action intercepted")
            PLYInterceptResult.SUCCESS
        }

        Purchasely.interceptAction<PLYPresentationAction.Navigate> { _, navigate ->
            val url = navigate.url
            if (url != null) {
                Log.d(TAG, "[Shaker] Presentation navigate action: $url")
                val intent = Intent(Intent.ACTION_VIEW, url)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                application?.startActivity(intent)
            }
            PLYInterceptResult.SUCCESS
        }

        Purchasely.interceptAction<PLYPresentationAction.Purchase> { info, purchase ->
            handlePurchase(info, purchase)
        }

        Purchasely.interceptAction<PLYPresentationAction.Restore> { _, _ ->
            handleRestore()
        }
    }

    internal suspend fun handlePurchase(
        info: PLYInterceptorInfo?,
        purchase: PLYPresentationAction.Purchase
    ): PLYInterceptResult {
        if (!runningModeRepo.isObserverMode) {
            return PLYInterceptResult.NOT_HANDLED
        }

        val plan = purchase.plan
        val offer = purchase.subscriptionOffer
        val productId = plan.store_product_id
        val offerToken = offer?.offerToken
        val activity = info?.activity

        return if (activity != null && productId != null && offerToken != null) {
            awaitPendingResult { resultCallback ->
                pendingObserverAction = ObserverAction.Purchase(plan.name)
                pendingResult = resultCallback
                scope.launch {
                    purchaseRequests.emit(PurchaseRequest(activity, productId, offerToken))
                }
            }
        } else {
            Log.w(TAG, "[Shaker] Observer mode purchase: missing activity, productId, or offerToken")
            // Observer mode owns purchases. Returning NOT_HANDLED here would let the
            // SDK launch its default flow after the app failed to provide native inputs.
            PLYInterceptResult.FAILED
        }
    }

    internal suspend fun handleRestore(): PLYInterceptResult {
        if (!runningModeRepo.isObserverMode) {
            return PLYInterceptResult.NOT_HANDLED
        }
        return awaitPendingResult { resultCallback ->
            pendingObserverAction = ObserverAction.Restore
            pendingResult = resultCallback
            scope.launch {
                restoreRequests.emit(RestoreRequest)
            }
        }
    }

    /**
     * Bridges the legacy "processAction" callback style to v6's suspend interceptor:
     * the interceptor lambda suspends until the pending result callback is invoked
     * from [handleTransactionResult] with the outcome reported by the host app.
     */
    private suspend fun awaitPendingResult(
        register: ((PLYInterceptResult) -> Unit) -> Unit
    ): PLYInterceptResult = suspendCancellableCoroutine { continuation ->
        // Cancel any previously-pending continuation before installing a new one.
        // The previous Observer-mode action was already claimed by the app, so mark
        // it handled to block the SDK's default purchase/restore fallback.
        pendingResult?.invoke(PLYInterceptResult.SUCCESS)
        pendingObserverAction = null
        pendingSuccessfulObserverResult = null
        val callback: (PLYInterceptResult) -> Unit = { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        register(callback)
        continuation.invokeOnCancellation {
            if (pendingResult === callback) {
                pendingResult = null
                pendingObserverAction = null
            }
        }
    }

    // MARK: - Transaction Result Handling

    private fun handleTransactionResult(result: TransactionResult) {
        when (result) {
            is TransactionResult.Success -> {
                val observerResult = when (val action = pendingObserverAction) {
                    is ObserverAction.Purchase -> DisplayResult.Purchased(action.planName)
                    ObserverAction.Restore -> DisplayResult.Restored(null)
                    null -> null
                }
                synchronize()
                if (observerResult == null) {
                    pendingResult?.invoke(PLYInterceptResult.SUCCESS)
                    pendingResult = null
                    pendingObserverAction = null
                    Log.d(TAG, "[Shaker] Transaction success without pending observer action")
                    onTransactionCompleted?.invoke()
                    return
                }
                // PURCHASELY: Defer onTransactionCompleted to the success_payment chain.
                // closeAllScreens() forces the presentation to dismiss; display()'s callback
                // then merges pendingSuccessfulObserverResult with the SDK's CANCELLED outcome.
                pendingSuccessfulObserverResult = observerResult
                pendingResult?.invoke(PLYInterceptResult.SUCCESS)
                pendingResult = null
                pendingObserverAction = null
                Purchasely.closeAllScreens()
                Log.d(TAG, "[Shaker] Transaction success — synchronized; awaiting success_payment dismissal")
            }
            is TransactionResult.Cancelled -> {
                // Observer mode owns the transaction, so we must block the SDK's default
                // purchase/restore flow even on cancellation (the v5 equivalent of
                // processAction(false)). NOT_HANDLED would map to processAction(true) and
                // let the SDK launch its own purchase. See docs/paywall-observer-reference.md.
                pendingResult?.invoke(PLYInterceptResult.SUCCESS)
                pendingResult = null
                pendingObserverAction = null
                pendingSuccessfulObserverResult = null
                Log.d(TAG, "[Shaker] Transaction cancelled")
            }
            is TransactionResult.Error -> {
                pendingResult?.invoke(PLYInterceptResult.FAILED)
                pendingResult = null
                pendingObserverAction = null
                pendingSuccessfulObserverResult = null
                Log.e(TAG, "[Shaker] Transaction error: ${result.message}")
            }
            is TransactionResult.Idle -> { /* ignore */ }
        }
    }

    // MARK: - Event Listener

    var eventListener: EventListener?
        get() = Purchasely.eventListener
        set(value) { Purchasely.eventListener = value }

    // MARK: - Deeplinks

    fun isDeeplinkHandled(deeplink: Uri, activity: Activity?): Boolean {
        return Purchasely.handleDeeplink(deeplink, activity)
    }

    // MARK: - Presentation Loading

    suspend fun loadPresentation(
        placementId: String? = null,
        screenId: String? = null,
        contentId: String? = null,
        useDemoChrome: Boolean = false,
    ): FetchResult {
        return try {
            val requestedPlacementId = placementId
            val requestedScreenId = screenId
            val requestedContentId = contentId
            val prepared = PLYPresentation {
                requestedPlacementId?.let { placementId(it) }
                requestedScreenId?.let { screenId(it) }
                requestedContentId?.let { contentId(it) }
                if (useDemoChrome) {
                    backgroundColor(0xFF101820.toInt())
                    progressColor(0xFFFFC857.toInt())
                    displayCloseButton(true)
                    displayBackButton(true)
                }
                onPresented { presentation, error ->
                    Log.d(
                        TAG,
                        "[Shaker] Presentation onPresented screenId=${presentation?.screenId}, error=${error?.message}"
                    )
                }
                onCloseRequested {
                    Log.d(TAG, "[Shaker] Presentation close requested")
                }
                onDismissed { outcome: PLYPresentationOutcome ->
                    val appResult = outcome.toDisplayResult()
                    Log.d(
                        TAG,
                        "[Shaker] Presentation dismissed: sdkResult=${outcome.purchaseResult}, " +
                            "appResult=$appResult, plan=${outcome.plan?.name}, " +
                            "reason=${outcome.closeReason}, error=${outcome.error?.message}"
                    )
                }
            }
            val presentation = prepared.preload()

            val handle = PresentationHandle(presentation)
            when (presentation.type) {
                PLYPresentationType.DEACTIVATED -> FetchResult.Deactivated
                PLYPresentationType.CLIENT -> FetchResult.Client(handle)
                else -> FetchResult.Success(handle, presentation.height)
            }
        } catch (e: Exception) {
            FetchResult.Error(e.message)
        }
    }

    suspend fun loadPresentationByScreenId(
        screenId: String,
        contentId: String? = null,
    ): FetchResult = loadPresentation(
        screenId = screenId,
        contentId = contentId,
        useDemoChrome = true,
    )

    suspend fun displayPreparedPresentation(
        placementId: String,
        activity: Activity,
    ): DisplayResult = suspendCancellableCoroutine { continuation ->
        val requestedPlacementId = placementId
        PLYPresentation {
            placementId(requestedPlacementId)
            onPresented { presentation, error ->
                Log.d(
                    TAG,
                    "[Shaker] Prepared display triggered screenId=${presentation?.screenId}, error=${error?.message}"
                )
            }
            onCloseRequested {
                Log.d(TAG, "[Shaker] Prepared presentation close requested")
            }
        }.display(
            context = activity,
            presentation = { presentation ->
                Log.d(TAG, "[Shaker] Prepared presentation loaded: ${presentation.screenId}")
            },
            callback = { outcome ->
                if (continuation.isActive) continuation.resume(outcome.toDisplayResult())
            }
        )
    }

    // MARK: - Modal Display

    suspend fun display(
        handle: PresentationHandle,
        activity: Activity
    ): DisplayResult {
        // PURCHASELY (v6): display() is non-suspend and returns a PLYPresentationSession;
        // session.await() suspends until the screen is dismissed and returns the outcome
        // (throwing the PLYError if the screen fails to launch or render). No callback is
        // passed, so the builder-set onDismissed stays active — an inline callback would
        // replace it for this display.
        val initial: DisplayResult = try {
            handle.presentation.display(activity).await().toDisplayResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "[Shaker] Presentation display failed: ${e.message}", e)
            return DisplayResult.Cancelled
        }

        // PURCHASELY: After the presentation closes, if a purchase succeeded — either
        // reported directly by the SDK (Full mode) or normalized from the app-side
        // Observer-mode Billing result — chain a "success_payment" placement, then
        // refresh subscriptions when that screen closes.
        val purchaseHappened = initial is DisplayResult.Purchased
            || initial is DisplayResult.Restored

        if (purchaseHappened) {
            pendingSuccessfulObserverResult = null
            showSuccessPaymentScreen(activity)
        }

        return initial
    }

    private suspend fun showSuccessPaymentScreen(activity: Activity) {
        when (val fetchResult = loadPresentation(SUCCESS_PAYMENT_PLACEMENT)) {
            is FetchResult.Success -> {
                // PURCHASELY (v6): display the success_payment screen and await its dismissal.
                // A failure to render must not block the post-purchase refresh below.
                try {
                    fetchResult.handle.presentation.display(activity).await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "[Shaker] success_payment display failed: ${e.message}", e)
                }
                // PURCHASELY: After the success_payment screen closes, refresh subscriptions
                // via the wrapper without forcing the cache. The wrapper -> PremiumManager
                // wiring (AppModule) routes onTransactionCompleted to userSubscriptions(false, ...).
                onTransactionCompleted?.invoke()
            }
            else -> {
                // No success_payment placement (deactivated, client, error) — still refresh
                Log.d(TAG, "[Shaker] success_payment placement unavailable: $fetchResult")
                onTransactionCompleted?.invoke()
            }
        }
    }

    // MARK: - Embedded View

    fun getView(
        handle: PresentationHandle,
        context: Context,
        onResult: (DisplayResult) -> Unit,
        onCloseRequested: () -> Unit = {},
    ): View? {
        handle.presentation.onCloseRequested = {
            Log.d(TAG, "[Shaker] Embedded presentation close requested")
            onCloseRequested()
        }
        return handle.presentation.buildView(context) { outcome ->
            val displayResult = outcome.toDisplayResult()
            Log.d(
                TAG,
                "[Shaker] Embedded presentation dismissed: sdkResult=${outcome.purchaseResult}, " +
                    "appResult=$displayResult, plan=${outcome.plan?.name}, " +
                    "reason=${outcome.closeReason}, error=${outcome.error?.message}"
            )
            if (displayResult is DisplayResult.Purchased || displayResult is DisplayResult.Restored) {
                pendingSuccessfulObserverResult = null
            }
            onResult(displayResult)
        }
    }

    private fun PLYPresentationOutcome.toDisplayResult(): DisplayResult =
        when (purchaseResult) {
            PLYPurchaseResult.PURCHASED -> DisplayResult.Purchased(plan?.name)
            PLYPurchaseResult.RESTORED -> DisplayResult.Restored(plan?.name)
            else -> pendingSuccessfulObserverResult ?: DisplayResult.Cancelled
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

    // MARK: - Subscriptions

    /**
     * Fetches the user's subscriptions mapped to the SDK-free [SubscriptionInfo] model,
     * so callers (data layer) never depend on `io.purchasely` types.
     *
     * @param invalidateCache pass false to use cached data, true to force a network refresh.
     */
    fun fetchSubscriptions(
        invalidateCache: Boolean,
        onSuccess: (List<SubscriptionInfo>) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        Purchasely.userSubscriptions(invalidateCache, object : SubscriptionsListener {
            override fun onSuccess(subscriptions: List<PLYSubscriptionData>) {
                onSuccess(
                    subscriptions.map { subscription ->
                        SubscriptionInfo(
                            planName = subscription.plan.name,
                            productName = subscription.product.name,
                            isActive = subscription.data.subscriptionStatus?.isExpired() == false,
                        )
                    }
                )
            }

            override fun onFailure(error: Throwable) {
                onError(error)
            }
        })
    }

    // MARK: - Restore

    fun restoreAllProducts(
        onSuccess: (String?) -> Unit,
        onError: (String?) -> Unit
    ) {
        Purchasely.restoreAllProducts(
            { plan -> onSuccess(plan?.name) },
            { error -> onError(error?.message) }
        )
    }

    // MARK: - Observer Mode

    fun synchronize() {
        Purchasely.synchronize()
    }

    // MARK: - GDPR Consent

    fun revokeDataProcessingConsent(purposes: Set<ConsentPurpose>) {
        val sdkPurposes = purposes.mapTo(mutableSetOf()) { purpose ->
            when (purpose) {
                ConsentPurpose.ANALYTICS -> PLYDataProcessingPurpose.Analytics
                ConsentPurpose.IDENTIFIED_ANALYTICS -> PLYDataProcessingPurpose.IdentifiedAnalytics
                ConsentPurpose.PERSONALIZATION -> PLYDataProcessingPurpose.Personalization
                ConsentPurpose.CAMPAIGNS -> PLYDataProcessingPurpose.Campaigns
                ConsentPurpose.THIRD_PARTY_INTEGRATIONS -> PLYDataProcessingPurpose.ThirdPartyIntegrations
            }
        }
        Purchasely.revokeDataProcessingConsent(sdkPurposes)
    }

    // MARK: - SDK Info

    val sdkVersion: String
        get() = Purchasely.sdkVersion

    companion object {
        private const val TAG = "PurchaselyWrapper"
        private const val SUCCESS_PAYMENT_PLACEMENT = "success_payment"
    }
}
