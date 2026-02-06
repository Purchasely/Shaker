package com.purchasely.shaker.ui.screen.onboarding

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purchasely.shaker.data.PremiumManager
import io.purchasely.ext.PLYPresentation
import io.purchasely.ext.PLYPresentationType
import io.purchasely.ext.PLYProductViewResult
import io.purchasely.ext.Purchasely
import io.purchasely.models.PLYError
import org.koin.compose.koinInject

@Composable
fun OnboardingScreen(
    showOnboarding: Boolean,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val premiumManager: PremiumManager = koinInject()

    LaunchedEffect(Unit) {
        Purchasely.fetchPresentation("onboarding") { presentation: PLYPresentation?, error: PLYError? ->
            if (showOnboarding && presentation != null && presentation.type != PLYPresentationType.DEACTIVATED) {
                val activity = context as? Activity
                if (activity != null) {
                    presentation.display(activity) { result, plan ->
                        when (result) {
                            PLYProductViewResult.PURCHASED,
                            PLYProductViewResult.RESTORED -> {
                                Log.d(TAG, "[Shaker] Purchased/Restored from onboarding: ${plan?.name}")
                                premiumManager.refreshPremiumStatus()
                            }
                            PLYProductViewResult.CANCELLED -> {
                                Log.d(TAG, "[Shaker] Onboarding paywall cancelled")
                            }
                            else -> {}
                        }
                        onComplete()
                    }
                } else {
                    onComplete()
                }
            } else {
                if (error != null) {
                    Log.e(TAG, "[Shaker] Error fetching onboarding: ${error.message}")
                }
                onComplete()
            }
        }
    }

    SplashContent()
}

@Composable
private fun SplashContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🍸",
                fontSize = 72.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Shaker",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Discover cocktails",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private const val TAG = "OnboardingScreen"
