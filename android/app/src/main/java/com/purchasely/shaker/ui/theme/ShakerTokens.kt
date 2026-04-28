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

val LightTokens = ShakerTokens(
    dark = false,
    bg = Color(0xFFF3F1F9),
    bgElev = Color(0xFFFFFFFF),
    bgCard = Color(0xFFFFFFFF),
    bgSubtle = Color(0xFFECE9F4),
    indigo = Color(0xFF3C4876),
    indigoText = Color(0xFF3C4876),
    indigoSoft = Color(0xFFE4E6F3),
    accent = Color(0xFF2B79E4),
    accentSoft = Color(0x1A2B79E4),
    orange = Color(0xFFE8723F),
    gold = Color(0xFFF5B93A),
    goldSoft = Color(0xFFFFF4D9),
    green = Color(0xFF23C071),
    danger = Color(0xFFD33A3A),
    text = Color(0xFF1C1E2C),
    textSec = Color(0xFF6A6F88),
    textTer = Color(0xFF9EA2B6),
    hair = Color(0x1F3C4876),
    hairStrong = Color(0x333C4876),
    inputBg = Color(0xFFECE9F4),
    onIndigo = Color.White,
)

val DarkTokens = ShakerTokens(
    dark = true,
    bg = Color(0xFF0F1020),
    bgElev = Color(0xFF1B1D30),
    bgCard = Color(0xFF1B1D30),
    bgSubtle = Color(0xFF13142A),
    indigo = Color(0xFF8A96C9),
    indigoText = Color(0xFFB3BDE4),
    indigoSoft = Color(0x2E8A96C9),
    accent = Color(0xFF4F92F0),
    accentSoft = Color(0x264F92F0),
    orange = Color(0xFFF08B5F),
    gold = Color(0xFFF5B93A),
    goldSoft = Color(0x26F5B93A),
    green = Color(0xFF3ED58B),
    danger = Color(0xFFF56A6A),
    text = Color(0xFFF4F3FB),
    textSec = Color(0xFF9EA2B6),
    textTer = Color(0xFF6E7392),
    hair = Color(0x14FFFFFF),
    hairStrong = Color(0x24FFFFFF),
    inputBg = Color(0x0FFFFFFF),
    onIndigo = Color(0xFF0F1020),
)

val LocalShakerTokens = staticCompositionLocalOf { LightTokens }

object Shaker {
    val tokens: ShakerTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalShakerTokens.current
}
