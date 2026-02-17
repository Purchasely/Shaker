package com.purchasely.shaker.ui.screen.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import com.purchasely.shaker.ShakerApp
import com.purchasely.shaker.data.PurchaselySdkMode
import com.purchasely.shaker.data.PremiumManager
import io.purchasely.ext.PLYDataProcessingPurpose
import io.purchasely.ext.Purchasely
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(
    private val context: Context,
    private val premiumManager: PremiumManager
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

    private val _sdkModeChangeAlert = MutableStateFlow<String?>(null)
    val sdkModeChangeAlert: StateFlow<String?> = _sdkModeChangeAlert.asStateFlow()

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

    init {
        if (!prefs.contains(PurchaselySdkMode.KEY)) {
            prefs.edit().putString(PurchaselySdkMode.KEY, PurchaselySdkMode.DEFAULT.storageValue).apply()
        }
        applyConsentPreferences()
    }

    fun login(userId: String) {
        if (userId.isBlank()) return

        Purchasely.userLogin(userId) { refresh ->
            if (refresh) {
                premiumManager.refreshPremiumStatus()
            }
            Log.d(TAG, "[Shaker] Logged in as: $userId (refresh: $refresh)")
        }

        _userId.value = userId
        prefs.edit().putString(KEY_USER_ID, userId).apply()

        Purchasely.setUserAttribute("user_id", userId)
    }

    fun logout() {
        Purchasely.userLogout()
        _userId.value = null
        prefs.edit().remove(KEY_USER_ID).apply()
        premiumManager.refreshPremiumStatus()
        Log.d(TAG, "[Shaker] Logged out")
    }

    fun restorePurchases() {
        _restoreMessage.value = null
        Purchasely.restoreAllProducts(
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

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME, mode).apply()
        Purchasely.setUserAttribute("app_theme", mode)
    }

    fun setSdkMode(mode: PurchaselySdkMode) {
        if (_sdkMode.value == mode) return

        _sdkMode.value = mode
        prefs.edit().putString(PurchaselySdkMode.KEY, mode.storageValue).apply()
        restartPurchaselySdk(mode)
    }

    fun clearSdkModeChangeAlert() {
        _sdkModeChangeAlert.value = null
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
            _sdkModeChangeAlert.value = "Mode saved (${mode.label}). Please kill and relaunch the app."
            return
        }

        app.restartPurchaselySdk()
        _sdkModeChangeAlert.value =
            "Purchasely SDK switched to ${mode.label}. Please kill and relaunch the app."
        Log.d(TAG, "[Shaker] SDK mode updated to ${mode.storageValue}")
    }

    private fun applyConsentPreferences() {
        val revoked = mutableSetOf<PLYDataProcessingPurpose>()
        if (!_analyticsConsent.value) revoked.add(PLYDataProcessingPurpose.Analytics)
        if (!_identifiedAnalyticsConsent.value) revoked.add(PLYDataProcessingPurpose.IdentifiedAnalytics)
        if (!_personalizationConsent.value) revoked.add(PLYDataProcessingPurpose.Personalization)
        if (!_campaignsConsent.value) revoked.add(PLYDataProcessingPurpose.Campaigns)
        if (!_thirdPartyConsent.value) revoked.add(PLYDataProcessingPurpose.ThirdPartyIntegrations)
        Purchasely.revokeDataProcessingConsent(revoked)
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
    }
}
