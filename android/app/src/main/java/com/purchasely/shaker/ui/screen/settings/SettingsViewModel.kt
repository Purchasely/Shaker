package com.purchasely.shaker.ui.screen.settings

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purchasely.shaker.ShakerApp
import com.purchasely.shaker.data.PurchaselySdkMode
import com.purchasely.shaker.data.PremiumManager
import com.purchasely.shaker.data.RunningModeRepository
import com.purchasely.shaker.purchasely.DisplayResult
import com.purchasely.shaker.purchasely.FetchResult
import com.purchasely.shaker.purchasely.PurchaselyWrapper
import io.purchasely.ext.PLYDataProcessingPurpose
import io.purchasely.ext.PLYPresentation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val context: Context,
    private val premiumManager: PremiumManager,
    private val runningModeRepo: RunningModeRepository,
    private val purchaselyWrapper: PurchaselyWrapper
) : ViewModel() {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PurchaselySdkMode.PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _userId = MutableStateFlow(prefs.getString(KEY_USER_ID, null))
    val userId: StateFlow<String?> = _userId.asStateFlow()

    val isPremium: StateFlow<Boolean> = premiumManager.isPremium

    private val _restoreMessage = MutableStateFlow<String?>(null)
    val restoreMessage: StateFlow<String?> = _restoreMessage.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME, "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _sdkMode = MutableStateFlow(
        PurchaselySdkMode.fromStorage(
            prefs.getString(PurchaselySdkMode.KEY, PurchaselySdkMode.DEFAULT.storageValue)
        )
    )
    val sdkMode: StateFlow<PurchaselySdkMode> = _sdkMode.asStateFlow()

    // Data privacy consent toggles (default: true = consent given)
    private val _analyticsConsent = MutableStateFlow(prefs.getBoolean(KEY_CONSENT_ANALYTICS, true))
    val analyticsConsent: StateFlow<Boolean> = _analyticsConsent.asStateFlow()

    private val _identifiedAnalyticsConsent = MutableStateFlow(prefs.getBoolean(KEY_CONSENT_IDENTIFIED_ANALYTICS, true))
    val identifiedAnalyticsConsent: StateFlow<Boolean> = _identifiedAnalyticsConsent.asStateFlow()

    private val _personalizationConsent = MutableStateFlow(prefs.getBoolean(KEY_CONSENT_PERSONALIZATION, true))
    val personalizationConsent: StateFlow<Boolean> = _personalizationConsent.asStateFlow()

    private val _campaignsConsent = MutableStateFlow(prefs.getBoolean(KEY_CONSENT_CAMPAIGNS, true))
    val campaignsConsent: StateFlow<Boolean> = _campaignsConsent.asStateFlow()

    private val _thirdPartyConsent = MutableStateFlow(prefs.getBoolean(KEY_CONSENT_THIRD_PARTY, true))
    val thirdPartyConsent: StateFlow<Boolean> = _thirdPartyConsent.asStateFlow()

    private val _runningMode = MutableStateFlow(
        if (runningModeRepo.isObserverMode) "observer" else "full"
    )
    val runningMode: StateFlow<String> = _runningMode.asStateFlow()

    private val _anonymousId = MutableStateFlow(purchaselyWrapper.anonymousUserId)
    val anonymousId: StateFlow<String> = _anonymousId.asStateFlow()

    private val _displayMode = MutableStateFlow(prefs.getString(KEY_DISPLAY_MODE, "fullscreen") ?: "fullscreen")
    val displayMode: StateFlow<String> = _displayMode.asStateFlow()

    // Signal Screen to display onboarding paywall
    private var pendingOnboardingPresentation: PLYPresentation? = null
    private val _requestPaywallDisplay = MutableSharedFlow<Unit>()
    val requestPaywallDisplay: SharedFlow<Unit> = _requestPaywallDisplay.asSharedFlow()

    val sdkVersion: String get() = purchaselyWrapper.sdkVersion

    init {
        if (!prefs.contains(PurchaselySdkMode.KEY)) {
            prefs.edit().putString(PurchaselySdkMode.KEY, PurchaselySdkMode.DEFAULT.storageValue).apply()
        }
        applyConsentPreferences()
    }

    fun refreshAnonymousId() {
        _anonymousId.value = purchaselyWrapper.anonymousUserId
    }

    fun login(userId: String) {
        if (userId.isBlank()) return

        // PURCHASELY: Associate this session with an authenticated user ID
        // The callback's `refresh` flag indicates whether subscriptions should be re-fetched
        // Docs: https://docs.purchasely.com/quick-start/sdk-configuration/user-login
        purchaselyWrapper.userLogin(userId) { refresh ->
            if (refresh) {
                premiumManager.refreshPremiumStatus()
            }
            Log.d(TAG, "[Shaker] Logged in as: $userId (refresh: $refresh)")
        }

        _userId.value = userId
        prefs.edit().putString(KEY_USER_ID, userId).apply()

        // PURCHASELY: Store the user ID as a custom attribute for paywall targeting/personalization
        // Docs: https://docs.purchasely.com/advanced-features/user-attributes
        purchaselyWrapper.setUserAttribute("user_id", userId)
    }

    fun logout() {
        // PURCHASELY: Disassociate the current user — clears cached user attributes and subscriptions
        // Docs: https://docs.purchasely.com/quick-start/sdk-configuration/user-login
        purchaselyWrapper.userLogout()
        _userId.value = null
        prefs.edit().remove(KEY_USER_ID).apply()
        premiumManager.refreshPremiumStatus()
        Log.d(TAG, "[Shaker] Logged out")
    }

    fun restorePurchases() {
        _restoreMessage.value = null
        // PURCHASELY: Restore previously purchased products for the current user
        // Required by App Store / Play Store guidelines; call on explicit user request only
        // Docs: https://docs.purchasely.com/quick-start/sdk-implementation/restore-purchases
        purchaselyWrapper.restoreAllProducts(
            onSuccess = { plan ->
                premiumManager.refreshPremiumStatus()
                _restoreMessage.value = "Purchases restored successfully!"
                Log.d(TAG, "[Shaker] Restore success: ${plan?.name}")
            },
            onError = { error ->
                _restoreMessage.value = error?.message ?: "No purchases to restore"
                Log.e(TAG, "[Shaker] Restore error: ${error?.message}")
            }
        )
    }

    fun clearRestoreMessage() {
        _restoreMessage.value = null
    }

    fun onPurchaseCompleted() {
        premiumManager.refreshPremiumStatus()
    }

    fun showOnboardingPaywall() {
        viewModelScope.launch {
            when (val result = purchaselyWrapper.loadPresentation("onboarding")) {
                is FetchResult.Success -> {
                    pendingOnboardingPresentation = result.presentation
                    _requestPaywallDisplay.emit(Unit)
                }
                is FetchResult.Client -> {
                    Log.d(TAG, "[Shaker] CLIENT presentation received for onboarding placement — build custom UI here")
                }
                is FetchResult.Deactivated -> {
                    Log.d(TAG, "[Shaker] Onboarding presentation is deactivated")
                }
                is FetchResult.Error -> {
                    Log.d(TAG, "[Shaker] Onboarding presentation not available: ${result.error?.message}")
                }
            }
        }
    }

    suspend fun displayPendingPaywall(activity: Activity) {
        val presentation = pendingOnboardingPresentation ?: return
        pendingOnboardingPresentation = null
        val result = purchaselyWrapper.display(presentation, activity)
        when (result) {
            is DisplayResult.Purchased, is DisplayResult.Restored -> {
                Log.d(TAG, "[Shaker] Purchased/Restored from onboarding")
                onPurchaseCompleted()
            }
            else -> {}
        }
    }

    fun setDisplayMode(mode: String) {
        _displayMode.value = mode
        prefs.edit().putString(KEY_DISPLAY_MODE, mode).apply()
        Log.d(TAG, "[Shaker] Display mode changed to: $mode")
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME, mode).apply()
        // PURCHASELY: Track the user's preferred theme as a custom attribute for audience segmentation
        // Docs: https://docs.purchasely.com/advanced-features/user-attributes
        purchaselyWrapper.setUserAttribute("app_theme", mode)
    }

    fun setSdkMode(mode: PurchaselySdkMode) {
        if (_sdkMode.value == mode) return

        _sdkMode.value = mode
        prefs.edit().putString(PurchaselySdkMode.KEY, mode.storageValue).apply()
        restartPurchaselySdk(mode)
    }

    fun setAnalyticsConsent(enabled: Boolean) {
        _analyticsConsent.value = enabled
        prefs.edit().putBoolean(KEY_CONSENT_ANALYTICS, enabled).apply()
        applyConsentPreferences()
    }

    fun setIdentifiedAnalyticsConsent(enabled: Boolean) {
        _identifiedAnalyticsConsent.value = enabled
        prefs.edit().putBoolean(KEY_CONSENT_IDENTIFIED_ANALYTICS, enabled).apply()
        applyConsentPreferences()
    }

    fun setPersonalizationConsent(enabled: Boolean) {
        _personalizationConsent.value = enabled
        prefs.edit().putBoolean(KEY_CONSENT_PERSONALIZATION, enabled).apply()
        applyConsentPreferences()
    }

    fun setCampaignsConsent(enabled: Boolean) {
        _campaignsConsent.value = enabled
        prefs.edit().putBoolean(KEY_CONSENT_CAMPAIGNS, enabled).apply()
        applyConsentPreferences()
    }

    fun setThirdPartyConsent(enabled: Boolean) {
        _thirdPartyConsent.value = enabled
        prefs.edit().putBoolean(KEY_CONSENT_THIRD_PARTY, enabled).apply()
        applyConsentPreferences()
    }

    private fun restartPurchaselySdk(mode: PurchaselySdkMode) {
        val app = context.applicationContext as? ShakerApp
        if (app == null) {
            Log.e(TAG, "[Shaker] Could not restart SDK: application context is not ShakerApp")
            return
        }

        app.restartPurchaselySdk()
        Log.d(TAG, "[Shaker] SDK restarted with mode ${mode.storageValue}")
    }

    private fun applyConsentPreferences() {
        val revoked = mutableSetOf<PLYDataProcessingPurpose>()
        if (!_analyticsConsent.value) revoked.add(PLYDataProcessingPurpose.Analytics)
        if (!_identifiedAnalyticsConsent.value) revoked.add(PLYDataProcessingPurpose.IdentifiedAnalytics)
        if (!_personalizationConsent.value) revoked.add(PLYDataProcessingPurpose.Personalization)
        if (!_campaignsConsent.value) revoked.add(PLYDataProcessingPurpose.Campaigns)
        if (!_thirdPartyConsent.value) revoked.add(PLYDataProcessingPurpose.ThirdPartyIntegrations)
        // PURCHASELY: Revoke GDPR data-processing consent for specific purposes
        // Pass the set of revoked purposes; an empty set re-grants all consent
        // Docs: https://docs.purchasely.com/advanced-features/gdpr
        purchaselyWrapper.revokeDataProcessingConsent(revoked)
        Log.d(TAG, "[Shaker] Consent updated — revoked: $revoked")
    }

    companion object {
        private const val TAG = "SettingsViewModel"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_CONSENT_ANALYTICS = "consent_analytics"
        private const val KEY_CONSENT_IDENTIFIED_ANALYTICS = "consent_identified_analytics"
        private const val KEY_CONSENT_PERSONALIZATION = "consent_personalization"
        private const val KEY_CONSENT_CAMPAIGNS = "consent_campaigns"
        private const val KEY_CONSENT_THIRD_PARTY = "consent_third_party"
        private const val KEY_DISPLAY_MODE = "display_mode"
    }
}
