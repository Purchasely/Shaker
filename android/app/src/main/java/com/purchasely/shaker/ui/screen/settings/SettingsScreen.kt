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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.purchasely.shaker.R
import com.purchasely.shaker.data.PurchaselySdkMode
import com.purchasely.shaker.domain.model.DisplayMode
import com.purchasely.shaker.domain.model.ThemeMode
import com.purchasely.shaker.purchasely.DisplayResult
import com.purchasely.shaker.purchasely.PurchaselyWrapper
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

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
    val anonymousId by viewModel.anonymousId.collectAsStateWithLifecycle()
    val displayMode by viewModel.displayMode.collectAsStateWithLifecycle()
    val clipboard = LocalClipboard.current
    val clipboardScope = rememberCoroutineScope()
    val context = LocalContext.current
    val purchaselyWrapper: PurchaselyWrapper = koinInject()
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
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Account section
        Text(
            text = stringResource(R.string.account),
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
                        text = stringResource(R.string.logged_in_as),
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
                        stringResource(R.string.logout),
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
                    label = { Text(stringResource(R.string.user_id)) },
                    placeholder = { Text(stringResource(R.string.enter_user_id)) },
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
                    Text(stringResource(R.string.login))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Premium status
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.premium_status), style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (isPremium) stringResource(R.string.active) else stringResource(R.string.free),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isPremium) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.anonymous_id), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copy), modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // Purchases section
        Text(
            text = stringResource(R.string.purchases),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = { viewModel.restorePurchases() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.restore_purchases))
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { viewModel.showOnboardingPaywall() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.show_onboarding))
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // Purchasely SDK section
        Text(
            text = stringResource(R.string.purchasely_sdk),
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
            text = stringResource(R.string.default_mode_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // Data Privacy section
        Text(
            text = stringResource(R.string.data_privacy),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        ConsentToggleRow(
            label = stringResource(R.string.analytics),
            description = stringResource(R.string.analytics_desc),
            checked = analyticsConsent,
            onCheckedChange = { viewModel.setAnalyticsConsent(it) }
        )
        ConsentToggleRow(
            label = stringResource(R.string.identified_analytics),
            description = stringResource(R.string.identified_analytics_desc),
            checked = identifiedAnalyticsConsent,
            onCheckedChange = { viewModel.setIdentifiedAnalyticsConsent(it) }
        )
        ConsentToggleRow(
            label = stringResource(R.string.personalization),
            description = stringResource(R.string.personalization_desc),
            checked = personalizationConsent,
            onCheckedChange = { viewModel.setPersonalizationConsent(it) }
        )
        ConsentToggleRow(
            label = stringResource(R.string.campaigns),
            description = stringResource(R.string.campaigns_desc),
            checked = campaignsConsent,
            onCheckedChange = { viewModel.setCampaignsConsent(it) }
        )
        ConsentToggleRow(
            label = stringResource(R.string.third_party),
            description = stringResource(R.string.third_party_desc),
            checked = thirdPartyConsent,
            onCheckedChange = { viewModel.setThirdPartyConsent(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.technical_processing_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // Appearance section
        Text(
            text = stringResource(R.string.appearance),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        val themes = ThemeMode.entries
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            themes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = themes.size)
                ) {
                    Text(mode.label)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // Display Mode section
        Text(
            text = stringResource(R.string.screen_display_mode),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.display_mode_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        val displayModes = DisplayMode.entries
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            displayModes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = displayMode == mode,
                    onClick = { viewModel.setDisplayMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = displayModes.size)
                ) {
                    Text(mode.label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // About section
        Text(
            text = stringResource(R.string.about),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row {
            Text(stringResource(R.string.version), style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Text(stringResource(R.string.purchasely_sdk), style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = viewModel.sdkVersion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.powered_by_purchasely),
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
