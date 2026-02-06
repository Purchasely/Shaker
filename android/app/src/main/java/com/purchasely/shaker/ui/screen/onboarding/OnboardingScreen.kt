package com.purchasely.shaker.ui.screen.onboarding

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.purchasely.shaker.data.PremiumManager
import io.purchasely.ext.PLYPresentation
import io.purchasely.ext.PLYPresentationType
import io.purchasely.ext.PLYProductViewResult
import io.purchasely.ext.Purchasely
import io.purchasely.models.PLYError
import org.koin.compose.koinInject

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val premiumManager: PremiumManager = koinInject()

    LaunchedEffect(Unit) {
        Purchasely.fetchPresentation("onboarding") { presentation: PLYPresentation?, error: PLYError? ->
            if (presentation != null && presentation.type != PLYPresentationType.DEACTIVATED) {
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
                    Log.w(TAG, "[Shaker] No activity context for onboarding display")
                    onComplete()
                }
            } else {
                if (error != null) {
                    Log.e(TAG, "[Shaker] Error fetching onboarding: ${error.message}")
                } else {
                    Log.d(TAG, "[Shaker] Onboarding presentation not available, skipping")
                }
                onComplete()
            }
        }
    }
}

private const val TAG = "OnboardingScreen"
