package com.purchasely.shaker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import com.purchasely.shaker.domain.model.Cocktail
import kotlin.math.cos
import kotlin.math.sin

private enum class Glass { MARTINI, COUPE, ROCKS, HIGHBALL }
private enum class Garnish { MINT, CHERRY, LIME, ORANGE, PINEAPPLE, BEANS, NONE }

private data class CocktailArtSpec(
    val hue: Int,
    val tint1: Color,
    val tint2: Color,
    val glass: Glass,
    val garnish: Garnish,
)

private fun Cocktail.artSpec(): CocktailArtSpec {
    // Map cocktails (by id → exact spec from design, otherwise by spirit/category → sensible fallback)
    return when (id) {
        "virgin-mojito", "mojito", "nojito" -> CocktailArtSpec(120, c("#C7F0C8"), c("#7CCB8F"), Glass.HIGHBALL, Garnish.MINT)
        "shirley-temple", "roy-rogers" -> CocktailArtSpec(350, c("#FDD2D8"), c("#EF6C7A"), Glass.HIGHBALL, Garnish.CHERRY)
        "pina-colada", "virgin-pina-colada" -> CocktailArtSpec(48, c("#FFF1C4"), c("#F2C061"), Glass.HIGHBALL, Garnish.PINEAPPLE)
        "manhattan" -> CocktailArtSpec(28, c("#E6B278"), c("#8E4A1E"), Glass.MARTINI, Garnish.ORANGE)
        "espresso-martini" -> CocktailArtSpec(25, c("#C9A27A"), c("#3B2416"), Glass.MARTINI, Garnish.BEANS)
        "negroni" -> CocktailArtSpec(5, c("#E98672"), c("#9A2E1C"), Glass.ROCKS, Garnish.ORANGE)
        "daiquiri" -> CocktailArtSpec(55, c("#F4F0CC"), c("#C7B970"), Glass.MARTINI, Garnish.LIME)
        "margarita" -> CocktailArtSpec(68, c("#E4F2B8"), c("#9CB949"), Glass.COUPE, Garnish.LIME)
        "old-fashioned", "whiskey-sour" -> CocktailArtSpec(28, c("#E6B278"), c("#8E4A1E"), Glass.ROCKS, Garnish.ORANGE)
        "cosmopolitan" -> CocktailArtSpec(340, c("#F2A0B5"), c("#C8365E"), Glass.MARTINI, Garnish.LIME)
        "virgin-mary" -> CocktailArtSpec(5, c("#F2A090"), c("#B42C16"), Glass.HIGHBALL, Garnish.LIME)
        "arnold-palmer" -> CocktailArtSpec(38, c("#F4D89C"), c("#B86E20"), Glass.HIGHBALL, Garnish.LIME)
        "cinderella" -> CocktailArtSpec(28, c("#F7D6A8"), c("#E08B3A"), Glass.COUPE, Garnish.CHERRY)
        "italian-soda" -> CocktailArtSpec(200, c("#C8E1F2"), c("#6A9CC9"), Glass.HIGHBALL, Garnish.LIME)
        "safe-sex-on-the-beach" -> CocktailArtSpec(18, c("#F7C2A0"), c("#E2753C"), Glass.HIGHBALL, Garnish.ORANGE)
        else -> when (spirit.lowercase()) {
            "rum" -> CocktailArtSpec(48, c("#FFF1C4"), c("#F2C061"), Glass.HIGHBALL, Garnish.LIME)
            "whiskey" -> CocktailArtSpec(28, c("#E6B278"), c("#8E4A1E"), Glass.ROCKS, Garnish.ORANGE)
            "gin" -> CocktailArtSpec(200, c("#C8E1F2"), c("#6A9CC9"), Glass.MARTINI, Garnish.LIME)
            "tequila" -> CocktailArtSpec(68, c("#E4F2B8"), c("#9CB949"), Glass.COUPE, Garnish.LIME)
            "vodka" -> CocktailArtSpec(200, c("#E3ECF5"), c("#ADB9CF"), Glass.MARTINI, Garnish.LIME)
            "non-alcoholic" -> CocktailArtSpec(120, c("#C7F0C8"), c("#7CCB8F"), Glass.HIGHBALL, Garnish.MINT)
            else -> CocktailArtSpec(350, c("#FDD2D8"), c("#EF6C7A"), Glass.HIGHBALL, Garnish.CHERRY)
        }
    }
}

@Composable
fun CocktailArt(
    cocktail: Cocktail,
    modifier: Modifier = Modifier,
) {
    val spec = remember(cocktail.id) { cocktail.artSpec() }
    Box(modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            drawCocktail(spec, size)
        }
    }
}

private fun c(hex: String): Color = Color(android.graphics.Color.parseColor(hex))

private fun hsl(h: Float, s: Float, l: Float): Color {
    val hsv = floatArrayOf(h, s, l)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun DrawScope.drawCocktail(spec: CocktailArtSpec, size: Size) {
    // Scale all coordinates from a 200x200 virtual canvas
    val sx = size.width / 200f
    val sy = size.height / 200f
    fun x(v: Float) = v * sx
    fun y(v: Float) = v * sy
    fun r(v: Float) = v * minOf(sx, sy)

    // Background gradient based on hue
    val bgA = hslRgb(spec.hue.toFloat(), 0.45f, 0.88f)
    val bgB = hslRgb((spec.hue + 20).toFloat(), 0.55f, 0.72f)
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(bgA, bgB),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height),
        )
    )

    // Soft light bokeh
    drawCircle(Color.White.copy(alpha = 0.35f), radius = r(30f), center = Offset(x(40f), y(40f)))
    drawCircle(Color.White.copy(alpha = 0.25f), radius = r(22f), center = Offset(x(165f), y(55f)))
    drawCircle(Color.White.copy(alpha = 0.18f), radius = r(35f), center = Offset(x(170f), y(160f)))

    // Countertop
    val counter = Path().apply {
        moveTo(x(0f), y(160f))
        quadraticBezierTo(x(100f), y(150f), x(200f), y(160f))
        lineTo(x(200f), y(200f))
        lineTo(x(0f), y(200f))
        close()
    }
    drawPath(counter, Color.Black.copy(alpha = 0.08f))

    val liquid = Brush.verticalGradient(
        colors = listOf(spec.tint1, spec.tint2),
        startY = y(0f),
        endY = y(200f),
    )

    when (spec.glass) {
        Glass.MARTINI -> drawMartini(sx, sy, liquid)
        Glass.COUPE -> drawCoupe(sx, sy, liquid)
        Glass.ROCKS -> drawRocks(sx, sy, liquid)
        Glass.HIGHBALL -> drawHighball(sx, sy, liquid)
    }

    when (spec.garnish) {
        Garnish.MINT -> drawMint(sx, sy)
        Garnish.CHERRY -> drawCherry(sx, sy)
        Garnish.LIME -> drawLime(sx, sy)
        Garnish.ORANGE -> drawOrange(sx, sy)
        Garnish.PINEAPPLE -> drawPineapple(sx, sy)
        Garnish.BEANS -> drawBeans(sx, sy)
        Garnish.NONE -> {}
    }
}

private fun hslRgb(h: Float, s: Float, l: Float): Color {
    // HSL → RGB
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val hp = (h % 360f) / 60f
    val xc = c * (1f - kotlin.math.abs(hp % 2f - 1f))
    val (r, g, b) = when {
        hp < 1f -> Triple(c, xc, 0f)
        hp < 2f -> Triple(xc, c, 0f)
        hp < 3f -> Triple(0f, c, xc)
        hp < 4f -> Triple(0f, xc, c)
        hp < 5f -> Triple(xc, 0f, c)
        else -> Triple(c, 0f, xc)
    }
    val m = l - c / 2f
    return Color((r + m).coerceIn(0f, 1f), (g + m).coerceIn(0f, 1f), (b + m).coerceIn(0f, 1f))
}

private fun DrawScope.drawMartini(sx: Float, sy: Float, liquid: Brush) {
    fun px(v: Float) = v * sx
    fun py(v: Float) = v * sy
    val bowl = Path().apply {
        moveTo(px(65f), py(75f))
        lineTo(px(135f), py(75f))
        lineTo(px(102f), py(115f))
        close()
    }
    drawPath(bowl, liquid)

    val glass = Path().apply {
        moveTo(px(60f), py(72f)); lineTo(px(140f), py(72f)); lineTo(px(102f), py(118f)); close()
        moveTo(px(102f), py(118f)); lineTo(px(102f), py(155f))
        moveTo(px(82f), py(158f)); lineTo(px(122f), py(158f))
    }
    drawPath(
        glass,
        Color.White.copy(alpha = 0.85f),
        style = Stroke(width = 2f * sx, cap = StrokeCap.Round),
    )
    drawLine(
        Color.White.copy(alpha = 0.6f),
        Offset(px(72f), py(76f)),
        Offset(px(92f), py(106f)),
        strokeWidth = 2f * sx,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawCoupe(sx: Float, sy: Float, liquid: Brush) {
    fun px(v: Float) = v * sx
    fun py(v: Float) = v * sy

    val bowl = Path().apply {
        moveTo(px(65f), py(78f))
        quadraticBezierTo(px(100f), py(110f), px(135f), py(78f))
        close()
    }
    drawPath(bowl, liquid)

    val glass = Path().apply {
        moveTo(px(60f), py(78f))
        quadraticBezierTo(px(100f), py(120f), px(140f), py(78f))
        moveTo(px(102f), py(120f)); lineTo(px(102f), py(155f))
        moveTo(px(82f), py(158f)); lineTo(px(122f), py(158f))
    }
    drawPath(
        glass,
        Color.White.copy(alpha = 0.85f),
        style = Stroke(width = 2f * sx, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawRocks(sx: Float, sy: Float, liquid: Brush) {
    fun px(v: Float) = v * sx
    fun py(v: Float) = v * sy

    drawRect(
        color = Color.White.copy(alpha = 0.15f),
        topLeft = Offset(px(70f), py(80f)),
        size = Size(px(60f), py(65f)),
    )
    drawRect(
        color = Color.White.copy(alpha = 0.8f),
        topLeft = Offset(px(70f), py(80f)),
        size = Size(px(60f), py(65f)),
        style = Stroke(width = 2f * sx),
    )
    drawRect(
        brush = liquid,
        topLeft = Offset(px(72f), py(105f)),
        size = Size(px(56f), py(38f)),
        alpha = 0.95f,
    )
    // Ice cubes
    rotate(-5f, pivot = Offset(px(88f), py(108f))) {
        drawRect(Color.White.copy(alpha = 0.6f), Offset(px(80f), py(100f)), Size(px(16f), py(16f)))
    }
    rotate(8f, pivot = Offset(px(110f), py(116f))) {
        drawRect(Color.White.copy(alpha = 0.55f), Offset(px(102f), py(108f)), Size(px(16f), py(16f)))
    }
    drawLine(
        Color.White.copy(alpha = 0.55f),
        Offset(px(76f), py(85f)),
        Offset(px(76f), py(138f)),
        strokeWidth = 2f * sx,
    )
}

private fun DrawScope.drawHighball(sx: Float, sy: Float, liquid: Brush) {
    fun px(v: Float) = v * sx
    fun py(v: Float) = v * sy

    drawRect(
        color = Color.White.copy(alpha = 0.15f),
        topLeft = Offset(px(75f), py(55f)),
        size = Size(px(50f), py(100f)),
    )
    drawRect(
        color = Color.White.copy(alpha = 0.8f),
        topLeft = Offset(px(75f), py(55f)),
        size = Size(px(50f), py(100f)),
        style = Stroke(width = 2f * sx),
    )
    drawRect(
        brush = liquid,
        topLeft = Offset(px(77f), py(85f)),
        size = Size(px(46f), py(68f)),
        alpha = 0.92f,
    )
    // Bubbles
    drawCircle(Color.White.copy(alpha = 0.8f), radius = 2.5f * sx, center = Offset(px(92f), py(110f)))
    drawCircle(Color.White.copy(alpha = 0.7f), radius = 1.8f * sx, center = Offset(px(102f), py(125f)))
    drawCircle(Color.White.copy(alpha = 0.7f), radius = 2f * sx, center = Offset(px(112f), py(100f)))
    drawCircle(Color.White.copy(alpha = 0.6f), radius = 1.5f * sx, center = Offset(px(97f), py(140f)))
    // Straw
    drawLine(
        Color(0xFFE85A68),
        Offset(px(112f), py(52f)),
        Offset(px(105f), py(150f)),
        strokeWidth = 3f * sx,
        cap = StrokeCap.Round,
    )
    drawLine(
        Color(0xFFF4F4F4).copy(alpha = 0.6f),
        Offset(px(115f), py(52f)),
        Offset(px(108f), py(150f)),
        strokeWidth = 3f * sx,
        cap = StrokeCap.Round,
    )
    drawLine(
        Color.White.copy(alpha = 0.55f),
        Offset(px(80f), py(60f)),
        Offset(px(80f), py(148f)),
        strokeWidth = 2f * sx,
    )
}

// ── Garnishes ──
private fun DrawScope.drawMint(sx: Float, sy: Float) {
    fun px(v: Float) = v * sx
    fun py(v: Float) = v * sy
    rotate(-20f, pivot = Offset(px(95f), py(58f))) {
        drawOval(
            Color(0xFF4FA265),
            topLeft = Offset(px(87f), py(46f)),
            size = Size(px(16f), py(24f)),
        )
    }
    rotate(15f, pivot = Offset(px(108f), py(52f))) {
        drawOval(
            Color(0xFF5BB673),
            topLeft = Offset(px(99f), py(39f)),
            size = Size(px(18f), py(26f)),
        )
    }
    rotate(-5f, pivot = Offset(px(100f), py(48f))) {
        drawOval(
            Color(0xFF6ECB86),
            topLeft = Offset(px(93f), py(38f)),
            size = Size(px(14f), py(20f)),
        )
    }
}

private fun DrawScope.drawCherry(sx: Float, sy: Float) {
    fun px(v: Float) = v * sx
    fun py(v: Float) = v * sy
    val stem = Path().apply {
        moveTo(px(108f), py(50f))
        quadraticBezierTo(px(112f), py(42f), px(118f), py(46f))
    }
    drawPath(stem, Color(0xFF6A4A2A), style = Stroke(width = 1.5f * sx))
    drawCircle(Color(0xFFC42E3A), radius = 5f * sx, center = Offset(px(106f), py(54f)))
    drawCircle(Color.White.copy(alpha = 0.6f), radius = 1.5f * sx, center = Offset(px(104f), py(53f)))
}

private fun DrawScope.drawLime(sx: Float, sy: Float) {
    fun px(v: Float) = v * sx
    fun py(v: Float) = v * sy
    translate(px(120f), py(52f)) {
        rotate(20f, pivot = Offset.Zero) {
            drawCircle(Color(0xFFB6D84A), radius = 8f * sx)
            drawCircle(Color(0xFF8AA836), radius = 8f * sx, style = Stroke(width = 1f * sx))
            drawCircle(Color(0xFFD9EC8E), radius = 5f * sx)
            drawLine(Color(0xFF8AA836), Offset(-5f * sx, 0f), Offset(5f * sx, 0f), strokeWidth = 0.5f * sx)
            drawLine(Color(0xFF8AA836), Offset(0f, -5f * sx), Offset(0f, 5f * sx), strokeWidth = 0.5f * sx)
        }
    }
}

private fun DrawScope.drawOrange(sx: Float, sy: Float) {
    fun px(v: Float) = v * sx
    fun py(v: Float) = v * sy
    val peel = Path().apply {
        moveTo(px(100f), py(55f))
        quadraticBezierTo(px(110f), py(48f), px(115f), py(58f))
        quadraticBezierTo(px(112f), py(63f), px(105f), py(62f))
        quadraticBezierTo(px(99f), py(60f), px(100f), py(55f))
        close()
    }
    drawPath(peel, Color(0xFFF4A64A))
    val outline = Path().apply {
        moveTo(px(100f), py(55f))
        quadraticBezierTo(px(110f), py(48f), px(115f), py(58f))
    }
    drawPath(outline, Color(0xFFD98230), style = Stroke(width = 1f * sx))
}

private fun DrawScope.drawPineapple(sx: Float, sy: Float) {
    fun px(v: Float) = v * sx
    fun py(v: Float) = v * sy
    val leaves = Path().apply {
        moveTo(px(115f), py(40f))
        lineTo(px(118f), py(55f))
        lineTo(px(122f), py(38f))
        lineTo(px(126f), py(55f))
        lineTo(px(129f), py(42f))
        lineTo(px(127f), py(60f))
        lineTo(px(115f), py(60f))
        close()
    }
    drawPath(leaves, Color(0xFF3E8B3E))
    val body = Path().apply {
        moveTo(px(115f), py(58f))
        quadraticBezierTo(px(122f), py(50f), px(129f), py(58f))
        lineTo(px(128f), py(65f))
        quadraticBezierTo(px(122f), py(68f), px(116f), py(65f))
        close()
    }
    drawPath(body, Color(0xFFF5C94C))
}

private fun DrawScope.drawBeans(sx: Float, sy: Float) {
    fun px(v: Float) = v * sx
    fun py(v: Float) = v * sy
    rotate(-15f, pivot = Offset(px(95f), py(72f))) {
        drawOval(Color(0xFF3B2416), topLeft = Offset(px(92f), py(67f)), size = Size(px(6f), py(10f)))
    }
    rotate(5f, pivot = Offset(px(102f), py(68f))) {
        drawOval(Color(0xFF2F1D10), topLeft = Offset(px(99f), py(63f)), size = Size(px(6f), py(10f)))
    }
    rotate(20f, pivot = Offset(px(109f), py(72f))) {
        drawOval(Color(0xFF3B2416), topLeft = Offset(px(106f), py(67f)), size = Size(px(6f), py(10f)))
    }
}
