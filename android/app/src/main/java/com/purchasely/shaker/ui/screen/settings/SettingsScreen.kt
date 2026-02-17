package com.purchasely.shaker.ui.screen.settings

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.purchasely.shaker.data.PurchaselySdkMode
import io.purchasely.ext.PLYPresentationType
import io.purchasely.ext.PLYProductViewResult
import io.purchasely.ext.Purchasely
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel()
) {
    val userId by viewModel.userId.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val restoreMessage by viewModel.restoreMessage.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val sdkMode by viewModel.sdkMode.collectAsState()
    val sdkModeChangeAlert by viewModel.sdkModeChangeAlert.collectAsState()
    val analyticsConsent by viewModel.analyticsConsent.collectAsState()
    val identifiedAnalyticsConsent by viewModel.identifiedAnalyticsConsent.collectAsState()
    val personalizationConsent by viewModel.personalizationConsent.collectAsState()
    val campaignsConsent by viewModel.campaignsConsent.collectAsState()
    val thirdPartyConsent by viewModel.thirdPartyConsent.collectAsState()
    val context = LocalContext.current
    var loginInput by remember { mutableStateOf("") }

    // Show restore toast
    LaunchedEffect(restoreMessage) {
        restoreMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearRestoreMessage()
        }
    }

    if (sdkModeChangeAlert != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearSdkModeChangeAlert() },
            title = { Text("SDK Restart Required") },
            text = { Text(sdkModeChangeAlert ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearSdkModeChangeAlert() }) {
                    Text("OK")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                Column {
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
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { viewModel.logout() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout")
            }
        } else {
            // Login form
            OutlinedTextField(
                value = loginInput,
                onValueChange = { loginInput = it },
                label = { Text("User ID") },
                placeholder = { Text("Enter any user ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.login(loginInput)
                    loginInput = ""
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = loginInput.isNotBlank()
            ) {
                Text("Login")
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
            onClick = {
                val activity = context as? Activity ?: return@OutlinedButton
                Purchasely.fetchPresentation("onboarding") { presentation, error ->
                    if (presentation != null && presentation.type != PLYPresentationType.DEACTIVATED) {
                        presentation.display(activity) { result, plan ->
                            when (result) {
                                PLYProductViewResult.PURCHASED,
                                PLYProductViewResult.RESTORED -> {
                                    Log.d("Settings", "[Shaker] Purchased/Restored from onboarding: ${plan?.name}")
                                    viewModel.onPurchaseCompleted()
                                }
                                else -> {}
                            }
                        }
                    } else {
                        Log.d("Settings", "[Shaker] Onboarding presentation not available: ${error?.message}")
                    }
                }
            },
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
            text = "Default mode is Paywall Observer. Changing mode restarts the SDK.",
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
