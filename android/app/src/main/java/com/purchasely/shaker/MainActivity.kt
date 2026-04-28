package com.purchasely.shaker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.purchasely.shaker.data.SettingsRepository
import com.purchasely.shaker.purchasely.PurchaselyWrapper
import com.purchasely.shaker.ui.navigation.ShakerNavHost
import com.purchasely.shaker.ui.theme.ShakerTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val purchaselyWrapper: PurchaselyWrapper by inject()
    private val settingsRepository: SettingsRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleDeepLink(intent)

        setContent {
            val themeMode by settingsRepository.themeModeFlow.collectAsState()
            ShakerTheme(themeMode = themeMode) {
                ShakerNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (purchaselyWrapper.isDeeplinkHandled(uri, this)) {
            Log.d("MainActivity", "[Shaker] Deep link handled by Purchasely: $uri")
        } else {
            Log.d("MainActivity", "[Shaker] Deep link not handled by Purchasely: $uri")
        }
    }
}
