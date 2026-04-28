package com.purchasely.shaker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ShakerLogo(
    size: Dp = 40.dp,
    color: Color = Color(0xFF3C4876),
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 40f
        fun p(x: Float, y: Float) = Offset(x * s, y * s)

        val cap = Path().apply {
            moveTo(p(13f, 6f).x, p(13f, 6f).y)
            lineTo(p(27f, 6f).x, p(27f, 6f).y)
            lineTo(p(25.5f, 10f).x, p(25.5f, 10f).y)
            lineTo(p(14.5f, 10f).x, p(14.5f, 10f).y)
            close()
        }
        drawPath(cap, color)

        val body = Path().apply {
            moveTo(p(14f, 10f).x, p(14f, 10f).y)
            lineTo(p(26f, 10f).x, p(26f, 10f).y)
            lineTo(p(24.5f, 28f).x, p(24.5f, 28f).y)
            cubicTo(
                p(24.4f, 30f).x, p(24.4f, 30f).y,
                p(22.5f, 31.6f).x, p(22.5f, 31.6f).y,
                p(20.5f, 31.6f).x, p(20.5f, 31.6f).y,
            )
            lineTo(p(19.5f, 31.6f).x, p(19.5f, 31.6f).y)
            cubicTo(
                p(17.5f, 31.6f).x, p(17.5f, 31.6f).y,
                p(15.6f, 30f).x, p(15.6f, 30f).y,
                p(15.5f, 28f).x, p(15.5f, 28f).y,
            )
            close()
        }
        drawPath(body, color)

        drawCircle(Color.White, radius = 2.5f * s, center = p(20f, 18f))
        drawCircle(Color.White.copy(alpha = 0.7f), radius = 1.5f * s, center = p(22f, 23f))
        drawCircle(Color.White.copy(alpha = 0.5f), radius = 1.2f * s, center = p(18f, 25f))
    }
}
