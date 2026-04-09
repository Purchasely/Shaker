package com.purchasely.shaker.purchasely

import android.util.Log
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.koin.compose.koinInject

@Composable
fun EmbeddedScreenBanner(
    placementId: String,
    onResult: (DisplayResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val wrapper: PurchaselyWrapper = koinInject()
    var view by remember { mutableStateOf<View?>(null) }

    LaunchedEffect(placementId) {
        when (val result = wrapper.loadPresentation(placementId)) {
            is FetchResult.Success -> {
                view = wrapper.getView(
                    presentation = result.presentation,
                    context = context,
                    onResult = onResult
                )
            }
            is FetchResult.Client -> {
                Log.d("EmbeddedScreenBanner", "[Shaker] CLIENT presentation for $placementId")
            }
            else -> {
                Log.d("EmbeddedScreenBanner", "[Shaker] Presentation not available for $placementId")
            }
        }
    }

    view?.let { androidView ->
        AndroidView(
            factory = { androidView },
            modifier = modifier
        )
    }
}
