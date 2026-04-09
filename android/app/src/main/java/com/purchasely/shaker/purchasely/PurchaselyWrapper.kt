package com.purchasely.shaker.purchasely

import android.app.Activity
import android.content.Context
import android.view.View
import io.purchasely.ext.PLYPresentation
import io.purchasely.ext.PLYPresentationProperties
import io.purchasely.ext.PLYPresentationType
import io.purchasely.ext.PLYProductViewResult
import io.purchasely.ext.Purchasely
import io.purchasely.ext.fetchPresentation
import io.purchasely.models.PLYError
import io.purchasely.models.PLYPlan
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PurchaselyWrapper {

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
}
