package com.purchasely.shaker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.purchasely.shaker.ui.navigation.ShakerNavHost
import com.purchasely.shaker.ui.theme.ShakerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShakerTheme {
                ShakerNavHost()
            }
        }
    }
}
