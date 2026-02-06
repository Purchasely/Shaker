package com.purchasely.shaker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.purchasely.shaker.ui.navigation.ShakerNavHost
import com.purchasely.shaker.ui.theme.ShakerTheme
import io.purchasely.ext.Purchasely

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleDeepLink(intent)

        setContent {
            ShakerTheme {
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
        if (Purchasely.isDeeplinkHandled(uri, this)) {
            Log.d("MainActivity", "[Shaker] Deep link handled by Purchasely: $uri")
        } else {
            Log.d("MainActivity", "[Shaker] Deep link not handled by Purchasely: $uri")
        }
    }
}
