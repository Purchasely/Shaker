package com.purchasely.shaker

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.purchasely.shaker.data.PurchaselySdkMode
import com.purchasely.shaker.data.PremiumManager
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

    private fun initPurchasely() {
        val selectedMode = getSdkModeFromStorage()

        Purchasely.Builder(this)
            .apiKey("6cda6b92-d63c-4444-bd55-5a164c989bd4")
            .logLevel(LogLevel.DEBUG)
            .readyToOpenDeeplink(true)
            .runningMode(selectedMode.runningMode)
            .stores(listOf(GoogleStore()))
            .build()
            .start { isConfigured, error ->
                if (isConfigured) {
                    Log.d(TAG, "[Shaker] Purchasely SDK configured successfully (mode: ${selectedMode.label})")
                    val premiumManager: PremiumManager by inject()
                    premiumManager.refreshPremiumStatus()
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

        Purchasely.setPaywallActionsInterceptor { info, action, parameters, proceed ->
            when (action) {
                PLYPresentationAction.LOGIN -> {
                    Log.d(TAG, "[Shaker] Paywall login action intercepted")
                    // Close the paywall and let the user navigate to Settings to log in
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
                else -> proceed(true)
            }
        }
    }

    fun restartPurchaselySdk() {
        Log.d(TAG, "[Shaker] Restarting Purchasely SDK")
        Purchasely.close()
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
