package com.purchasely.shaker.ui.screen.settings

import android.app.Activity
import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.purchasely.shaker.data.PurchaselySdkMode
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel()
) {
    val userId by viewModel.userId.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val restoreMessage by viewModel.restoreMessage.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val sdkMode by viewModel.sdkMode.collectAsStateWithLifecycle()

    val analyticsConsent by viewModel.analyticsConsent.collectAsStateWithLifecycle()
    val identifiedAnalyticsConsent by viewModel.identifiedAnalyticsConsent.collectAsStateWithLifecycle()
    val personalizationConsent by viewModel.personalizationConsent.collectAsStateWithLifecycle()
    val campaignsConsent by viewModel.campaignsConsent.collectAsStateWithLifecycle()
    val thirdPartyConsent by viewModel.thirdPartyConsent.collectAsStateWithLifecycle()
    val runningMode by viewModel.runningMode.collectAsStateWithLifecycle()
    val anonymousId by viewModel.anonymousId.collectAsStateWithLifecycle()
    val displayMode by viewModel.displayMode.collectAsStateWithLifecycle()
    val clipboard = LocalClipboard.current
    val clipboardScope = rememberCoroutineScope()
    val context = LocalContext.current
    var loginInput by remember { mutableStateOf("") }

    // Show restore toast
    LaunchedEffect(restoreMessage) {
        restoreMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearRestoreMessage()
        }
    }

    // Collect paywall display requests from ViewModel
    LaunchedEffect(Unit) {
        viewModel.requestPaywallDisplay.collect {
            val activity = context as? Activity ?: return@collect
            viewModel.displayPendingPaywall(activity)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Account section
        Text(
            text = "Account",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (userId != null) {
            // Logged in state
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Logged in as",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = userId ?: "",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                TextButton(onClick = { viewModel.logout() }) {
                    Text(
                        "Logout",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        } else {
            // Login form
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = loginInput,
                    onValueChange = { loginInput = it },
                    label = { Text("User ID") },
                    placeholder = { Text("Enter any user ID") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        viewModel.login(loginInput)
                        loginInput = ""
                    },
                    enabled = loginInput.isNotBlank()
                ) {
                    Text("Login")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Premium status
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Premium Status", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (isPremium) "Active" else "Free",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isPremium) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Anonymous ID", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = anonymousId,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
            IconButton(onClick = {
                clipboardScope.launch {
                    clipboard.setClipEntry(
                        ClipEntry(ClipData.newPlainText("anonymousId", anonymousId))
                    )
                }
                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // Purchases section
        Text(
            text = "Purchases",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = { viewModel.restorePurchases() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Restore Purchases")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { viewModel.showOnboardingPaywall() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Show Onboarding")
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // Purchasely SDK section
        Text(
            text = "Purchasely SDK",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        val sdkModes = listOf(PurchaselySdkMode.PAYWALL_OBSERVER, PurchaselySdkMode.FULL)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            sdkModes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = sdkMode == mode,
                    onClick = { viewModel.setSdkMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = sdkModes.size)
                ) {
                    Text(mode.label)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Default mode is Paywall Observer.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // Data Privacy section
        Text(
            text = "Data Privacy",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        ConsentToggleRow(
            label = "Analytics",
            description = "Anonymous audience measurement",
            checked = analyticsConsent,
            onCheckedChange = { viewModel.setAnalyticsConsent(it) }
        )
        ConsentToggleRow(
            label = "Identified Analytics",
            description = "User-identified analytics",
            checked = identifiedAnalyticsConsent,
            onCheckedChange = { viewModel.setIdentifiedAnalyticsConsent(it) }
        )
        ConsentToggleRow(
            label = "Personalization",
            description = "Personalized content & offers",
            checked = personalizationConsent,
            onCheckedChange = { viewModel.setPersonalizationConsent(it) }
        )
        ConsentToggleRow(
            label = "Campaigns",
            description = "Promotional campaigns",
            checked = campaignsConsent,
            onCheckedChange = { viewModel.setCampaignsConsent(it) }
        )
        ConsentToggleRow(
            label = "Third-party Integrations",
            description = "External analytics & integrations",
            checked = thirdPartyConsent,
            onCheckedChange = { viewModel.setThirdPartyConsent(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Technical processing required for app operation cannot be disabled.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // Appearance section
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        val themes = listOf("light", "dark", "system")
        val labels = listOf("Light", "Dark", "System")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            themes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = themes.size)
                ) {
                    Text(labels[index])
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // Display Mode section
        Text(
            text = "Screen Display Mode",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "How paywalls are presented on screen",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        val displayModes = listOf("fullscreen", "modal", "drawer", "popin")
        val displayLabels = listOf("Full", "Modal", "Drawer", "Popin")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            displayModes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = displayMode == mode,
                    onClick = { viewModel.setDisplayMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = displayModes.size)
                ) {
                    Text(displayLabels[index], style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // About section
        Text(
            text = "About",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row {
            Text("Version", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Text("Purchasely SDK", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = viewModel.sdkVersion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Powered by Purchasely",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ConsentToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
