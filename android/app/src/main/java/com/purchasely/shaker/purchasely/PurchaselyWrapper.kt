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
import io.purchasely.ext.PLYPresentationAction
import io.purchasely.ext.PLYPresentationActionParameters
import io.purchasely.ext.PLYPresentationInfo
import io.purchasely.ext.PLYPresentationProperties
import io.purchasely.ext.PLYPresentationType
import io.purchasely.ext.PLYProductViewResult
import io.purchasely.ext.Purchasely
import io.purchasely.ext.fetchPresentation
import io.purchasely.models.PLYError
import io.purchasely.models.PLYPlan
import io.purchasely.google.GoogleStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
    private val transactionResult: SharedFlow<TransactionResult>,
    private val scope: CoroutineScope
) {

    private var application: Application? = null
    private var apiKey: String = ""
    private var logLevel: LogLevel = LogLevel.DEBUG
    private var pendingProcessAction: ((Boolean) -> Unit)? = null
    private var collectionJob: Job? = null

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

        startTransactionCollection()
    }

    fun restart() {
        close()
        val app = application ?: return
        initialize(app, apiKey, logLevel)
    }

    fun close() {
        collectionJob?.cancel()
        collectionJob = null
        pendingProcessAction?.invoke(false)
        pendingProcessAction = null
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
                        pendingProcessAction?.invoke(false)
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
                    pendingProcessAction?.invoke(false)
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

    // MARK: - Modal Display

    suspend fun display(
        handle: PresentationHandle,
        activity: Activity
    ): DisplayResult = suspendCoroutine { continuation ->
        handle.presentation.display(activity) { result: PLYProductViewResult, plan: PLYPlan? ->
            when (result) {
                PLYProductViewResult.PURCHASED -> continuation.resume(DisplayResult.Purchased(plan?.name))
                PLYProductViewResult.RESTORED -> continuation.resume(DisplayResult.Restored(plan?.name))
                else -> continuation.resume(DisplayResult.Cancelled)
            }
        }
    }

    // MARK: - Embedded View

    fun getView(
        handle: PresentationHandle,
        context: Context,
        onResult: (DisplayResult) -> Unit
    ): View? {
        return handle.presentation.buildView(
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
