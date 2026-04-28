package com.purchasely.shaker.ui.screen.settings

import android.app.Activity
import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.purchasely.shaker.data.PurchaselySdkMode
import com.purchasely.shaker.domain.model.DisplayMode
import com.purchasely.shaker.domain.model.ThemeMode
import com.purchasely.shaker.purchasely.DisplayResult
import com.purchasely.shaker.purchasely.PurchaselyWrapper
import com.purchasely.shaker.ui.theme.Shaker
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val tokens = Shaker.tokens
    val userId by viewModel.userId.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val restoreMessage by viewModel.restoreMessage.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val sdkMode by viewModel.sdkMode.collectAsStateWithLifecycle()
    val displayMode by viewModel.displayMode.collectAsStateWithLifecycle()
    val anonymousId by viewModel.anonymousId.collectAsStateWithLifecycle()

    val analyticsConsent by viewModel.analyticsConsent.collectAsStateWithLifecycle()
    val identifiedAnalyticsConsent by viewModel.identifiedAnalyticsConsent.collectAsStateWithLifecycle()
    val personalizationConsent by viewModel.personalizationConsent.collectAsStateWithLifecycle()
    val campaignsConsent by viewModel.campaignsConsent.collectAsStateWithLifecycle()
    val thirdPartyConsent by viewModel.thirdPartyConsent.collectAsStateWithLifecycle()

    val clipboard = LocalClipboard.current
    val clipboardScope = rememberCoroutineScope()
    val context = LocalContext.current
    val purchaselyWrapper: PurchaselyWrapper = koinInject()
    var loginInput by remember { mutableStateOf("") }

    LaunchedEffect(restoreMessage) {
        restoreMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearRestoreMessage()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.requestPaywallDisplay.collect { handle ->
            val activity = context as? Activity ?: return@collect
            val result = purchaselyWrapper.display(handle, activity)
            when (result) {
                is DisplayResult.Purchased, is DisplayResult.Restored -> viewModel.onPurchaseCompleted()
                else -> {}
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            "Settings",
            color = tokens.text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )

        SectionHeader("Account")
        SettingsCard {
            if (userId != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(tokens.indigo),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            (userId ?: "").take(2).uppercase(),
                            color = tokens.onIndigo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Logged in as", color = tokens.textSec, fontSize = 12.sp)
                        Text(userId ?: "", color = tokens.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.clickable { viewModel.logout() },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Logout, null, tint = tokens.danger, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Logout", color = tokens.danger, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(tokens.bgSubtle)
                            .border(1.dp, tokens.hair, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (loginInput.isEmpty()) {
                            Text("User ID", color = tokens.textSec, fontSize = 15.sp)
                        }
                        BasicTextField(
                            value = loginInput,
                            onValueChange = { loginInput = it },
                            singleLine = true,
                            textStyle = TextStyle(color = tokens.text, fontSize = 15.sp),
                            cursorBrush = SolidColor(tokens.indigo),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.size(10.dp))
                    val enabled = loginInput.isNotBlank()
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (enabled) tokens.accent else tokens.bgSubtle)
                            .clickable(enabled = enabled) {
                                viewModel.login(loginInput)
                                loginInput = ""
                            }
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Login",
                            color = if (enabled) Color.White else tokens.textSec,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
            Divider()
            RowBetween {
                Text("Premium status", color = tokens.text, fontSize = 15.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (isPremium) tokens.goldSoft else tokens.indigoSoft)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        if (isPremium) "PRO" else "FREE",
                        color = tokens.indigoText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Divider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Anonymous ID", color = tokens.textSec, fontSize = 13.sp)
                    Text(anonymousId, color = tokens.text, fontSize = 11.sp, maxLines = 1)
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(tokens.inputBg)
                        .clickable {
                            clipboardScope.launch {
                                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("anonymousId", anonymousId)))
                            }
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.ContentCopy, null, tint = tokens.textSec, modifier = Modifier.size(16.dp))
                }
            }
        }

        SectionHeader("Purchases")
        Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlineButton(label = "Restore purchases") { viewModel.restorePurchases() }
            OutlineButton(label = "Show onboarding") { viewModel.showOnboardingPaywall() }
        }
        Spacer(Modifier.height(16.dp))

        SectionHeader("Purchasely SDK")
        Segmented(
            options = listOf(PurchaselySdkMode.PAYWALL_OBSERVER.label, PurchaselySdkMode.FULL.label),
            active = sdkMode.label,
            onSelect = { label ->
                val mode = PurchaselySdkMode.entries.firstOrNull { it.label == label } ?: PurchaselySdkMode.DEFAULT
                viewModel.setSdkMode(mode)
            },
        )
        Text(
            "Default mode is Paywall Observer — Shaker observes purchases but uses its own paywall UI.",
            color = tokens.textSec,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(16.dp))

        SectionHeader("Data privacy")
        SettingsCard {
            ToggleRow("Analytics", "Anonymous audience measurement", analyticsConsent) { viewModel.setAnalyticsConsent(it) }
            Divider()
            ToggleRow("Identified analytics", "User-identified analytics", identifiedAnalyticsConsent) { viewModel.setIdentifiedAnalyticsConsent(it) }
            Divider()
            ToggleRow("Personalization", "Personalized content & offers", personalizationConsent) { viewModel.setPersonalizationConsent(it) }
            Divider()
            ToggleRow("Campaigns", "Promotional campaigns", campaignsConsent) { viewModel.setCampaignsConsent(it) }
            Divider()
            ToggleRow("Third-party integrations", "External analytics & integrations", thirdPartyConsent, last = true) { viewModel.setThirdPartyConsent(it) }
        }
        Text(
            "Technical processing required for app operation cannot be disabled.",
            color = tokens.textSec,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )

        SectionHeader("Appearance")
        Segmented(
            options = ThemeMode.entries.map { it.label },
            active = themeMode.label,
            onSelect = { label ->
                ThemeMode.entries.firstOrNull { it.label == label }?.let(viewModel::setThemeMode)
            },
        )
        Spacer(Modifier.height(16.dp))

        SectionHeader("Screen display mode")
        Text(
            "How paywalls are presented on screen",
            color = tokens.textSec,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(8.dp))
        Segmented(
            options = DisplayMode.entries.map { it.label },
            active = displayMode.label,
            onSelect = { label ->
                DisplayMode.entries.firstOrNull { it.label == label }?.let(viewModel::setDisplayMode)
            },
        )
        Spacer(Modifier.height(16.dp))

        SectionHeader("About")
        SettingsCard {
            RowBetween {
                Text("Version", color = tokens.text, fontSize = 15.sp)
                Text("1.0.0", color = tokens.textSec, fontSize = 15.sp)
            }
            Divider()
            RowBetween {
                Text("Purchasely SDK", color = tokens.text, fontSize = 15.sp)
                Text(viewModel.sdkVersion, color = tokens.textSec, fontSize = 15.sp)
            }
        }
        Text(
            "Powered by Purchasely",
            color = tokens.textTer,
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        )
        Spacer(Modifier.height(60.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    val tokens = Shaker.tokens
    Text(
        title,
        color = tokens.indigoText,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    val tokens = Shaker.tokens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.bgCard)
            .border(1.dp, tokens.hair, RoundedCornerShape(16.dp)),
    ) { content() }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Shaker.tokens.hair))
}

@Composable
private fun RowBetween(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    on: Boolean,
    last: Boolean = false,
    onToggle: (Boolean) -> Unit,
) {
    val tokens = Shaker.tokens
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = tokens.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = tokens.textSec, fontSize = 13.sp)
        }
        ShakerToggle(on = on, onToggle = { onToggle(!on) })
    }
}

@Composable
private fun ShakerToggle(on: Boolean, onToggle: () -> Unit) {
    val tokens = Shaker.tokens
    Box(
        modifier = Modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (on) tokens.indigo else if (tokens.dark) Color(0xFF3D3F54) else Color(0xFFE0DEE9))
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(start = if (on) 22.dp else 2.dp)
                .size(27.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
private fun Segmented(options: List<String>, active: String, onSelect: (String) -> Unit) {
    val tokens = Shaker.tokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tokens.inputBg)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { o ->
            val selected = o == active
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) tokens.indigoSoft else Color.Transparent)
                    .clickable { onSelect(o) },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selected) {
                        Icon(Icons.Default.Check, null, tint = tokens.indigoText, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(6.dp))
                    }
                    Text(
                        o,
                        color = if (selected) tokens.indigoText else tokens.textSec,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun OutlineButton(label: String, onClick: () -> Unit) {
    val tokens = Shaker.tokens
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(100.dp))
            .border(1.5.dp, tokens.indigoText, RoundedCornerShape(100.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = tokens.indigoText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
