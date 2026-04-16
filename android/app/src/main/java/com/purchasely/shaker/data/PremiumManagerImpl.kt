package com.purchasely.shaker.data

import android.util.Log
import com.purchasely.shaker.domain.repository.PremiumRepository
import com.purchasely.shaker.purchasely.PurchaselyWrapper
import io.purchasely.ext.SubscriptionsListener
import io.purchasely.models.PLYSubscriptionData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PremiumManagerImpl(private val wrapper: PurchaselyWrapper) : PremiumRepository {

    private val _isPremium = MutableStateFlow(false)
    override val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    override fun refreshPremiumStatus() {
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
