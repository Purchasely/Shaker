package com.purchasely.shaker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.purchasely.shaker.domain.model.ThemeMode

private fun colorSchemeFor(tokens: ShakerTokens) = if (tokens.dark) {
    darkColorScheme(
        primary = tokens.accent,
        onPrimary = Color.White,
        primaryContainer = tokens.indigo,
        onPrimaryContainer = tokens.onIndigo,
        secondary = tokens.indigo,
        onSecondary = tokens.onIndigo,
        background = tokens.bg,
        onBackground = tokens.text,
        surface = tokens.bgCard,
        onSurface = tokens.text,
        surfaceVariant = tokens.bgSubtle,
        onSurfaceVariant = tokens.textSec,
        outline = tokens.hairStrong,
        outlineVariant = tokens.hair,
        error = tokens.danger,
    )
} else {
    lightColorScheme(
        primary = tokens.accent,
        onPrimary = Color.White,
        primaryContainer = tokens.indigo,
        onPrimaryContainer = Color.White,
        secondary = tokens.indigo,
        onSecondary = Color.White,
        background = tokens.bg,
        onBackground = tokens.text,
        surface = tokens.bgCard,
        onSurface = tokens.text,
        surfaceVariant = tokens.bgSubtle,
        onSurfaceVariant = tokens.textSec,
        outline = tokens.hairStrong,
        outlineVariant = tokens.hair,
        error = tokens.danger,
    )
}

private val ShakerTypography = Typography(
    displayLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp),
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
)

@Composable
fun ShakerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val tokens = if (useDark) DarkTokens else LightTokens
    CompositionLocalProvider(LocalShakerTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorSchemeFor(tokens),
            typography = ShakerTypography,
            content = content,
        )
    }
}
