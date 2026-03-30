package com.purchasely.shaker

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.purchasely.shaker.data.PremiumManager
import com.purchasely.shaker.di.appModule
import io.purchasely.ext.EventListener
import io.purchasely.ext.LogLevel
import io.purchasely.ext.PLYEvent
import io.purchasely.ext.PLYPresentationAction
import io.purchasely.ext.PLYRunningMode
import io.purchasely.ext.Purchasely
import io.purchasely.google.GoogleStore
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ShakerApp : Application() {

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
        Purchasely.Builder(this)
            .apiKey("6cda6b92-d63c-4444-bd55-5a164c989bd4")
            .logLevel(LogLevel.DEBUG)
            .readyToOpenDeeplink(true)
            .runningMode(PLYRunningMode.Full)
            .stores(listOf(GoogleStore()))
            .build()
            .start { isConfigured, error ->
                if (isConfigured) {
                    Log.d(TAG, "[Shaker] Purchasely SDK configured successfully")
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
        Purchasely.eventListener = object : EventListener {
            override fun onEvent(event: PLYEvent) {
                Log.d(TAG, "[Shaker] Event: ${event.name} | Properties: ${event.properties}")
            }
        }

        // PURCHASELY: Intercept paywall button actions before the SDK handles them
        // Use this to customize LOGIN, NAVIGATE, or other CTA behavior app-side
        // Docs: https://docs.purchasely.com/advanced-features/customize-screens/paywall-action-interceptor
        Purchasely.setPaywallActionsInterceptor { info, action, parameters, proceed ->
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
                // PURCHASELY: All other actions — let the SDK handle them normally
                else -> proceed(true)
            }
        }
    }

    companion object {
        private const val TAG = "ShakerApp"
    }
}
