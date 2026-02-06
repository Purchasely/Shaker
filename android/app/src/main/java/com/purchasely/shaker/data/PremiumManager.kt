package com.purchasely.shaker.data

import android.util.Log
import io.purchasely.ext.Purchasely
import io.purchasely.ext.SubscriptionsListener
import io.purchasely.models.PLYSubscriptionData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PremiumManager {

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    fun refreshPremiumStatus() {
        Purchasely.userSubscriptions(false, object : SubscriptionsListener {
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
