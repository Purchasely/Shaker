package com.purchasely.shaker.data

import android.util.Log
import io.purchasely.ext.Purchasely
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PremiumManager {

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    fun refreshPremiumStatus() {
        Purchasely.userSubscriptions(
            onSuccess = { subscriptions ->
                val premium = subscriptions.any { subscription ->
                    subscription.plan?.hasEntitlement(ENTITLEMENT_ID) == true
                }
                _isPremium.value = premium
                Log.d(TAG, "[Shaker] Premium status: $premium")
            },
            onError = { error ->
                Log.e(TAG, "[Shaker] Error checking premium: ${error.message}")
            }
        )
    }

    companion object {
        private const val TAG = "PremiumManager"
        const val ENTITLEMENT_ID = "SHAKER_PREMIUM"
    }
}
