package com.purchasely.shaker.purchasely

import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.koin.compose.koinInject

@Composable
fun EmbeddedScreenBanner(
    fetchResult: FetchResult.Success,
    onResult: (DisplayResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val wrapper: PurchaselyWrapper = koinInject()

    AndroidView(
        factory = { context ->
            wrapper.getView(
                presentation = fetchResult.presentation,
                context = context,
                onResult = onResult
            ) ?: FrameLayout(context)
        },
        modifier = modifier
    )
}
