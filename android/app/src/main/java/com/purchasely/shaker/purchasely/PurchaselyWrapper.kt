package com.purchasely.shaker.purchasely

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.Uri
import android.view.View
import io.purchasely.ext.EventListener
import io.purchasely.ext.LogLevel
import io.purchasely.ext.PLYPresentation
import io.purchasely.ext.PLYPresentationProperties
import io.purchasely.ext.PLYPresentationType
import io.purchasely.ext.PLYProductViewResult
import io.purchasely.ext.PLYRunningMode
import io.purchasely.ext.Purchasely
import io.purchasely.ext.fetchPresentation
import io.purchasely.models.PLYError
import io.purchasely.models.PLYPlan
import io.purchasely.google.GoogleStore
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PurchaselyWrapper {

    // MARK: - SDK Initialization

    fun start(
        application: Application,
        apiKey: String,
        logLevel: LogLevel = LogLevel.DEBUG,
        runningMode: PLYRunningMode = PLYRunningMode.Full,
        readyToOpenDeeplink: Boolean = true,
        onStarted: (Boolean, PLYError?) -> Unit
    ) {
        Purchasely.Builder(application)
            .apiKey(apiKey)
            .logLevel(logLevel)
            .readyToOpenDeeplink(readyToOpenDeeplink)
            .runningMode(runningMode)
            .stores(listOf(GoogleStore()))
            .build()
            .start { isConfigured, error ->
                onStarted(isConfigured, error)
            }
    }

    fun close() {
        Purchasely.close()
    }

    var eventListener: EventListener?
        get() = Purchasely.eventListener
        set(value) { Purchasely.eventListener = value }

    fun setPaywallActionsInterceptor(
        interceptor: (
            info: io.purchasely.ext.PLYPresentationInfo?,
            action: io.purchasely.ext.PLYPresentationAction,
            parameters: io.purchasely.ext.PLYPresentationActionParameters?,
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
}
