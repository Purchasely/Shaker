package com.purchasely.shaker.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ShakerTokens(
    val dark: Boolean,
    val bg: Color,
    val bgElev: Color,
    val bgCard: Color,
    val bgSubtle: Color,
    val indigo: Color,
    val indigoText: Color,
    val indigoSoft: Color,
    val accent: Color,
    val accentSoft: Color,
    val orange: Color,
    val gold: Color,
    val goldSoft: Color,
    val green: Color,
    val danger: Color,
    val text: Color,
    val textSec: Color,
    val textTer: Color,
    val hair: Color,
    val hairStrong: Color,
    val inputBg: Color,
    val onIndigo: Color,
)

// "Cocktail lounge" palette: deep bottle green as primary (token still named
// `indigo` to avoid a mechanical rename across every screen), burnished gold
// accents, warm cream backgrounds in light mode and charred green in dark mode.
val LightTokens = ShakerTokens(
    dark = false,
    bg = Color(0xFFF7F3EA),
    bgElev = Color(0xFFFFFFFF),
    bgCard = Color(0xFFFFFDF8),
    bgSubtle = Color(0xFFEFE9DB),
    indigo = Color(0xFF1E3B2F),
    indigoText = Color(0xFF1E3B2F),
    indigoSoft = Color(0xFFDFE8DF),
    accent = Color(0xFFB05A2E),
    accentSoft = Color(0x1AB05A2E),
    orange = Color(0xFFC96F3B),
    gold = Color(0xFFC9921E),
    goldSoft = Color(0xFFF6EDD4),
    green = Color(0xFF2E8B57),
    danger = Color(0xFFC23B3B),
    text = Color(0xFF20251F),
    textSec = Color(0xFF6B7066),
    textTer = Color(0xFF9CA095),
    hair = Color(0x1F1E3B2F),
    hairStrong = Color(0x331E3B2F),
    inputBg = Color(0xFFEFE9DB),
    onIndigo = Color(0xFFF7F3EA),
)

val DarkTokens = ShakerTokens(
    dark = true,
    bg = Color(0xFF0E120F),
    bgElev = Color(0xFF1A211B),
    bgCard = Color(0xFF1A211B),
    bgSubtle = Color(0xFF141A15),
    indigo = Color(0xFF9BC4A8),
    indigoText = Color(0xFFBCD9C6),
    indigoSoft = Color(0x2E9BC4A8),
    accent = Color(0xFFE08A52),
    accentSoft = Color(0x26E08A52),
    orange = Color(0xFFE08A52),
    gold = Color(0xFFD8A93E),
    goldSoft = Color(0x26D8A93E),
    green = Color(0xFF4FBF82),
    danger = Color(0xFFE96A6A),
    text = Color(0xFFF2F4EE),
    textSec = Color(0xFF9FA89D),
    textTer = Color(0xFF6F7870),
    hair = Color(0x14FFFFFF),
    hairStrong = Color(0x24FFFFFF),
    inputBg = Color(0x0FFFFFFF),
    onIndigo = Color(0xFF0E120F),
)

val LocalShakerTokens = staticCompositionLocalOf { LightTokens }

object Shaker {
    val tokens: ShakerTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalShakerTokens.current
}
