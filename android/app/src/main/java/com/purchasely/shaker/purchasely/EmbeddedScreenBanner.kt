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
    onCloseRequested: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val wrapper: PurchaselyWrapper = koinInject()

    AndroidView(
        factory = { context ->
            wrapper.getView(
                handle = fetchResult.handle,
                context = context,
                onResult = onResult,
                onCloseRequested = onCloseRequested,
            ) ?: FrameLayout(context)
        },
        modifier = modifier
    )
}
