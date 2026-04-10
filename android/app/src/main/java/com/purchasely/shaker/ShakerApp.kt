package com.purchasely.shaker

import android.app.Application
import android.util.Log
import com.purchasely.shaker.di.appModule
import com.purchasely.shaker.purchasely.PurchaselyWrapper
import io.purchasely.ext.LogLevel
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
        // PURCHASELY: Initialize the SDK — PurchaselyWrapper now owns init, interceptor, and orchestration
        // Docs: https://docs.purchasely.com/quick-start/sdk-configuration
        purchaselyWrapper.initialize(
            application = this,
            apiKey = "6cda6b92-d63c-4444-bd55-5a164c989bd4",
            logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.WARN
        )
    }

    fun restartPurchaselySdk() {
        Log.d(TAG, "[Shaker] Restarting Purchasely SDK")
        purchaselyWrapper.restart()
    }

    companion object {
        private const val TAG = "ShakerApp"
    }
}
