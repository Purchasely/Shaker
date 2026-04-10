package com.purchasely.shaker

import android.app.Application
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

        purchaselyWrapper.initialize(
            application = this,
            apiKey = BuildConfig.PURCHASELY_API_KEY,
            logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.WARN
        )

    }
}
