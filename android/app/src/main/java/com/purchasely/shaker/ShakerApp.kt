package com.purchasely.shaker

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.purchasely.shaker.data.PurchaselySdkMode
import com.purchasely.shaker.data.PremiumManager
import com.purchasely.shaker.data.purchase.PurchaseManager
import com.purchasely.shaker.di.appModule
import com.purchasely.shaker.purchasely.PurchaselyWrapper
import io.purchasely.ext.EventListener
import io.purchasely.ext.LogLevel
import io.purchasely.ext.PLYEvent
import io.purchasely.ext.PLYPresentationAction
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ShakerApp : Application() {

    private val purchaselyWrapper: PurchaselyWrapper by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@ShakerApp)
            modules(appModule)
        }

        initPurchasely()
    }

    fun initPurchasely() {
        // PURCHASELY: Initialize the SDK with API key, store, and running mode
        // Call once in Application.onCreate(); Full mode means Purchasely owns the purchase flow
        // Docs: https://docs.purchasely.com/quick-start/sdk-configuration
        val selectedMode = getSdkModeFromStorage()

        purchaselyWrapper.start(
            application = this,
            apiKey = "6cda6b92-d63c-4444-bd55-5a164c989bd4",
            logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.WARN,
            runningMode = selectedMode.runningMode,
            readyToOpenDeeplink = true
        ) { isConfigured, error ->
            if (isConfigured) {
                Log.d(TAG, "[Shaker] Purchasely SDK configured successfully (mode: ${selectedMode.label})")
                val premiumManager: PremiumManager by inject()
                premiumManager.refreshPremiumStatus()
            }
            error?.let {
                Log.e(TAG, "[Shaker] Purchasely configuration error: ${it.message}")
            }
        }

        // PURCHASELY: Subscribe to SDK analytics events (purchases, cancellations, paywall views, etc.)
        // Forward these to your own analytics pipeline or BI tool as needed
        // Docs: https://docs.purchasely.com/advanced-features/events
        purchaselyWrapper.eventListener = object : EventListener {
            override fun onEvent(event: PLYEvent) {
                Log.d(TAG, "[Shaker] Event: ${event.name} | Properties: ${event.properties}")
            }
        }

        // PURCHASELY: Intercept paywall button actions before the SDK handles them
        // Use this to customize LOGIN, NAVIGATE, or other CTA behavior app-side
        // Docs: https://docs.purchasely.com/advanced-features/customize-screens/paywall-action-interceptor
        purchaselyWrapper.setPaywallActionsInterceptor { info, action, parameters, proceed ->
            when (action) {
                // PURCHASELY: LOGIN action — dismiss paywall and redirect user to app login flow
                // Call proceed(false) to prevent the SDK's default behavior
                PLYPresentationAction.LOGIN -> {
                    Log.d(TAG, "[Shaker] Paywall login action intercepted")
                    // Close the paywall and let the user navigate to Settings to log in
                    proceed(false)
                }
                // PURCHASELY: NAVIGATE action — open an external URL from a paywall CTA
                // Call proceed(false) after handling it yourself to suppress SDK's default
                PLYPresentationAction.NAVIGATE -> {
                    val url = parameters?.url
                    if (url != null) {
                        Log.d(TAG, "[Shaker] Paywall navigate action: $url")
                        val intent = Intent(Intent.ACTION_VIEW, url)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                    proceed(false)
                }
                // PURCHASELY: PURCHASE action — in Observer mode, route to native Google Play Billing;
                // in Full mode, let the SDK handle the purchase flow
                // Docs: https://docs.purchasely.com/advanced-features/customize-screens/paywall-action-interceptor
                PLYPresentationAction.PURCHASE -> {
                    if (getSdkModeFromStorage() == PurchaselySdkMode.PAYWALL_OBSERVER) {
                        val plan = parameters?.plan
                        val offer = parameters?.subscriptionOffer
                        val productId = plan?.store_product_id
                        val offerToken = offer?.offerToken
                        val activity = info?.activity
                        if (activity != null && productId != null && offerToken != null) {
                            val purchaseManager: PurchaseManager by inject()
                            val premiumManager: PremiumManager by inject()
                            purchaseManager.purchase(activity, productId, offerToken) { success ->
                                if (success) premiumManager.refreshPremiumStatus()
                                proceed(false)
                            }
                        } else {
                            Log.w(TAG, "[Shaker] Observer mode purchase: missing activity, productId, or offerToken")
                            proceed(false)
                        }
                    } else {
                        proceed(true)
                    }
                }
                // PURCHASELY: RESTORE action — in Observer mode, query Google Play Billing directly;
                // in Full mode, let the SDK handle restore
                PLYPresentationAction.RESTORE -> {
                    if (getSdkModeFromStorage() == PurchaselySdkMode.PAYWALL_OBSERVER) {
                        val purchaseManager: PurchaseManager by inject()
                        val premiumManager: PremiumManager by inject()
                        purchaseManager.restore { success ->
                            if (success) premiumManager.refreshPremiumStatus()
                            proceed(false)
                        }
                    } else {
                        proceed(true)
                    }
                }
                // PURCHASELY: All other actions — let the SDK handle them normally
                else -> proceed(true)
            }
        }
    }

    fun restartPurchaselySdk() {
        Log.d(TAG, "[Shaker] Restarting Purchasely SDK")
        purchaselyWrapper.close()
        initPurchasely()
    }

    private fun getSdkModeFromStorage(): PurchaselySdkMode {
        val prefs = getSharedPreferences(PurchaselySdkMode.PREFERENCES_NAME, MODE_PRIVATE)
        val storedMode = prefs.getString(PurchaselySdkMode.KEY, null)
        val resolvedMode = PurchaselySdkMode.fromStorage(storedMode)

        if (storedMode != resolvedMode.storageValue) {
            prefs.edit().putString(PurchaselySdkMode.KEY, resolvedMode.storageValue).apply()
        }

        return resolvedMode
    }

    companion object {
        private const val TAG = "ShakerApp"
    }
}
