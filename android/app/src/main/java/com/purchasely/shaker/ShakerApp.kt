package com.purchasely.shaker

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.purchasely.shaker.data.PremiumManager
import com.purchasely.shaker.data.RunningModeRepository
import com.purchasely.shaker.data.purchase.PurchaseManager
import com.purchasely.shaker.di.appModule
import io.purchasely.ext.EventListener
import io.purchasely.ext.LogLevel
import io.purchasely.ext.PLYEvent
import io.purchasely.ext.PLYPresentationAction
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

    /**
     * Initialize (or re-initialize) the Purchasely SDK with the current running mode.
     * Called on app launch and whenever the user toggles the running mode in Settings.
     */
    fun initPurchasely() {
        val runningModeRepo: RunningModeRepository by inject()
        val currentMode = runningModeRepo.runningMode

        Log.d(TAG, "[Shaker] Initializing Purchasely SDK in ${currentMode.name} mode")

        Purchasely.Builder(this)
            .apiKey("6cda6b92-d63c-4444-bd55-5a164c989bd4")
            .logLevel(LogLevel.DEBUG)
            .readyToOpenDeeplink(true)
            .runningMode(currentMode)
            .stores(listOf(GoogleStore()))
            .build()
            .start { isConfigured, error ->
                if (isConfigured) {
                    Log.d(TAG, "[Shaker] Purchasely SDK configured successfully (${currentMode.name})")
                    val premiumManager: PremiumManager by inject()
                    premiumManager.refreshPremiumStatus()
                    // Track additional user attributes for demo
                    Purchasely.setUserAttribute("last_open_date", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).format(java.util.Date()))
                    Purchasely.incrementUserAttribute("session_count")
                }
                error?.let {
                    Log.e(TAG, "[Shaker] Purchasely configuration error: ${it.message}")
                }
            }

        Purchasely.eventListener = object : EventListener {
            override fun onEvent(event: PLYEvent) {
                Log.d(TAG, "[Shaker] Event: ${event.name} | Properties: ${event.properties}")
            }
        }

        setupInterceptor()

        // Synchronize on launch when in Observer mode to catch external transactions
        if (runningModeRepo.isObserverMode) {
            Purchasely.synchronize()
            Log.d(TAG, "[Shaker] Observer mode: synchronize() called on launch")
        }
    }

    private fun setupInterceptor() {
        val runningModeRepo: RunningModeRepository by inject()

        Purchasely.setPaywallActionsInterceptor { info, action, parameters, proceed ->
            when (action) {
                PLYPresentationAction.LOGIN -> {
                    Log.d(TAG, "[Shaker] Paywall login action intercepted")
                    proceed(false)
                }
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
                PLYPresentationAction.PURCHASE -> {
                    if (runningModeRepo.isObserverMode) {
                        // Observer mode: handle purchase natively via Google Play Billing
                        val activity = info?.activity
                        val offerToken = parameters?.subscriptionOffer?.offerToken

                        if (activity != null && offerToken != null) {
                            Log.d(TAG, "[Shaker] Observer mode: launching native purchase flow")
                            val purchaseManager: PurchaseManager by inject()
                            purchaseManager.purchase(activity, parameters.plan?.store_product_id ?: "", offerToken) { success ->
                                if (success) {
                                    Log.d(TAG, "[Shaker] Observer mode: native purchase successful")
                                    val premiumManager: PremiumManager by inject()
                                    premiumManager.refreshPremiumStatus()
                                }
                                proceed(false) // We handled it ourselves
                            }
                        } else {
                            Log.e(TAG, "[Shaker] Observer mode: missing activity or offerToken")
                            proceed(false)
                        }
                    } else {
                        // Full mode: let Purchasely handle the purchase
                        proceed(true)
                    }
                }
                PLYPresentationAction.RESTORE -> {
                    if (runningModeRepo.isObserverMode) {
                        // Observer mode: restore via Google Play Billing
                        Log.d(TAG, "[Shaker] Observer mode: restoring purchases natively")
                        val purchaseManager: PurchaseManager by inject()
                        purchaseManager.restore { success ->
                            val premiumManager: PremiumManager by inject()
                            premiumManager.refreshPremiumStatus()
                            proceed(false) // We handled it ourselves
                        }
                    } else {
                        proceed(true)
                    }
                }
                else -> proceed(true)
            }
        }
    }

    companion object {
        private const val TAG = "ShakerApp"
    }
}
