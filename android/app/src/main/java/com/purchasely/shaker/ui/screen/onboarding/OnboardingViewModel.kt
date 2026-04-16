package com.purchasely.shaker.ui.screen.onboarding

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import com.purchasely.shaker.data.PremiumManager
import com.purchasely.shaker.purchasely.DisplayResult
import com.purchasely.shaker.purchasely.FetchResult
import com.purchasely.shaker.purchasely.PresentationHandle
import com.purchasely.shaker.purchasely.PurchaselyWrapper

class OnboardingViewModel(
    private val purchaselyWrapper: PurchaselyWrapper,
    private val premiumManager: PremiumManager
) : ViewModel() {

    private var pendingPresentation: PresentationHandle? = null

    suspend fun loadOnboarding(): FetchResult {
        val result = purchaselyWrapper.loadPresentation("onboarding")
        if (result is FetchResult.Success) {
            pendingPresentation = result.handle
        }
        return result
    }

    suspend fun displayOnboarding(activity: Activity): DisplayResult? {
        val handle = pendingPresentation ?: return null
        pendingPresentation = null
        val result = purchaselyWrapper.display(handle, activity)
        when (result) {
            is DisplayResult.Purchased, is DisplayResult.Restored -> {
                Log.d(TAG, "[Shaker] Purchased/Restored from onboarding")
                premiumManager.refreshPremiumStatus()
            }
            is DisplayResult.Cancelled -> {
                Log.d(TAG, "[Shaker] Onboarding paywall cancelled")
            }
        }
        return result
    }

    companion object {
        private const val TAG = "OnboardingViewModel"
    }
}
