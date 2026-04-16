package com.purchasely.shaker.data

import android.util.Log
import com.purchasely.shaker.purchasely.PurchaselyWrapper
import io.purchasely.ext.SubscriptionsListener
import io.purchasely.models.PLYSubscriptionData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PremiumManager(private val wrapper: PurchaselyWrapper) {

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    fun refreshPremiumStatus() {
        // PURCHASELY: Fetch the current user's active subscriptions to determine premium access
        // Pass false to use cached data; true forces a network refresh
        // Docs: https://docs.purchasely.com/advanced-features/subscription-status
        wrapper.userSubscriptions(false, object : SubscriptionsListener {
            override fun onSuccess(subscriptions: List<PLYSubscriptionData>) {
                val premium = subscriptions.any { subscriptionData ->
                    subscriptionData.data.subscriptionStatus?.isExpired() == false
                }
                _isPremium.value = premium
                Log.d(TAG, "[Shaker] Premium status: $premium")
            }

            override fun onFailure(error: Throwable) {
                Log.e(TAG, "[Shaker] Error checking premium: ${error.message}")
            }
        })
    }

    companion object {
        private const val TAG = "PremiumManager"
    }
}
