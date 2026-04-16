package com.purchasely.shaker.ui.screen.onboarding

import android.app.Activity
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purchasely.shaker.R
import com.purchasely.shaker.purchasely.DisplayResult
import com.purchasely.shaker.purchasely.FetchResult
import com.purchasely.shaker.purchasely.PurchaselyWrapper
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun OnboardingScreen(
    showOnboarding: Boolean,
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val purchaselyWrapper: PurchaselyWrapper = koinInject()

    LaunchedEffect(Unit) {
        if (!showOnboarding) {
            onComplete()
            return@LaunchedEffect
        }

        val activity = context as? Activity
        if (activity == null) {
            onComplete()
            return@LaunchedEffect
        }

        when (val fetchResult = viewModel.loadOnboarding()) {
            is FetchResult.Success -> {
                val displayResult = purchaselyWrapper.display(fetchResult.handle, activity)
                when (displayResult) {
                    is DisplayResult.Purchased, is DisplayResult.Restored -> viewModel.onPurchaseCompleted()
                    else -> {}
                }
                onComplete()
            }
            else -> onComplete()
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
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.discover_cocktails),
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
