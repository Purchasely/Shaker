package com.purchasely.shaker.purchasely

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.koin.compose.koinInject

@Composable
fun EmbeddedScreenBanner(
    fetchResult: FetchResult.Success,
    onResult: (DisplayResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val wrapper: PurchaselyWrapper = koinInject()
    val view: View? = remember(fetchResult) {
        wrapper.getView(
            presentation = fetchResult.presentation,
            context = context,
            onResult = onResult
        )
    }

    view?.let { androidView ->
        AndroidView(
            factory = { androidView },
            modifier = modifier
        )
    }
}
