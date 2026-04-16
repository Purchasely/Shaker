package com.purchasely.shaker.ui.screen.settings

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purchasely.shaker.data.PurchaselySdkMode
import com.purchasely.shaker.domain.model.DisplayMode
import com.purchasely.shaker.domain.model.ThemeMode
import com.purchasely.shaker.domain.repository.PremiumRepository
import com.purchasely.shaker.data.RunningModeRepository
import com.purchasely.shaker.data.SettingsRepository
import com.purchasely.shaker.purchasely.DisplayResult
import com.purchasely.shaker.purchasely.FetchResult
import com.purchasely.shaker.purchasely.PresentationHandle
import com.purchasely.shaker.purchasely.PurchaselyWrapper
import io.purchasely.ext.PLYDataProcessingPurpose
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
    private val premiumRepository: PremiumRepository,
    private val runningModeRepo: RunningModeRepository,
    private val purchaselyWrapper: PurchaselyWrapper
) : ViewModel() {

    private val _userId = MutableStateFlow(settingsRepo.userId)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    val isPremium: StateFlow<Boolean> = premiumRepository.isPremium

    private val _restoreMessage = MutableStateFlow<String?>(null)
    val restoreMessage: StateFlow<String?> = _restoreMessage.asStateFlow()

    private val _themeMode = MutableStateFlow(settingsRepo.themeMode)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _sdkMode = MutableStateFlow(
        PurchaselySdkMode.fromStorage(settingsRepo.sdkModeStorage)
    )
    val sdkMode: StateFlow<PurchaselySdkMode> = _sdkMode.asStateFlow()

    // Data privacy consent toggles (default: true = consent given)
    private val _analyticsConsent = MutableStateFlow(settingsRepo.analyticsConsent)
    val analyticsConsent: StateFlow<Boolean> = _analyticsConsent.asStateFlow()

    private val _identifiedAnalyticsConsent = MutableStateFlow(settingsRepo.identifiedAnalyticsConsent)
    val identifiedAnalyticsConsent: StateFlow<Boolean> = _identifiedAnalyticsConsent.asStateFlow()

    private val _personalizationConsent = MutableStateFlow(settingsRepo.personalizationConsent)
    val personalizationConsent: StateFlow<Boolean> = _personalizationConsent.asStateFlow()

    private val _campaignsConsent = MutableStateFlow(settingsRepo.campaignsConsent)
    val campaignsConsent: StateFlow<Boolean> = _campaignsConsent.asStateFlow()

    private val _thirdPartyConsent = MutableStateFlow(settingsRepo.thirdPartyConsent)
    val thirdPartyConsent: StateFlow<Boolean> = _thirdPartyConsent.asStateFlow()

    private val _runningMode = MutableStateFlow(
        if (runningModeRepo.isObserverMode) "observer" else "full"
    )
    val runningMode: StateFlow<String> = _runningMode.asStateFlow()

    private val _anonymousId = MutableStateFlow(purchaselyWrapper.anonymousUserId)
    val anonymousId: StateFlow<String> = _anonymousId.asStateFlow()

    private val _displayMode = MutableStateFlow(settingsRepo.displayMode)
    val displayMode: StateFlow<DisplayMode> = _displayMode.asStateFlow()

    // Signal Screen to display onboarding paywall
    private var pendingOnboardingPresentation: PresentationHandle? = null
    private val _requestPaywallDisplay = MutableSharedFlow<Unit>()
    val requestPaywallDisplay: SharedFlow<Unit> = _requestPaywallDisplay.asSharedFlow()

    val sdkVersion: String get() = purchaselyWrapper.sdkVersion

    init {
        settingsRepo.initSdkModeIfNeeded()
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
                premiumRepository.refreshPremiumStatus()
            }
            Log.d(TAG, "[Shaker] Logged in as: $userId (refresh: $refresh)")
        }

        _userId.value = userId
        settingsRepo.userId = userId

        // PURCHASELY: Store the user ID as a custom attribute for paywall targeting/personalization
        // Docs: https://docs.purchasely.com/advanced-features/user-attributes
        purchaselyWrapper.setUserAttribute("user_id", userId)
    }

    fun logout() {
        // PURCHASELY: Disassociate the current user — clears cached user attributes and subscriptions
        // Docs: https://docs.purchasely.com/quick-start/sdk-configuration/user-login
        purchaselyWrapper.userLogout()
        _userId.value = null
        settingsRepo.userId = null
        premiumRepository.refreshPremiumStatus()
        Log.d(TAG, "[Shaker] Logged out")
    }

    fun restorePurchases() {
        _restoreMessage.value = null
        // PURCHASELY: Restore previously purchased products for the current user
        // Required by App Store / Play Store guidelines; call on explicit user request only
        // Docs: https://docs.purchasely.com/quick-start/sdk-implementation/restore-purchases
        purchaselyWrapper.restoreAllProducts(
            onSuccess = { plan ->
                premiumRepository.refreshPremiumStatus()
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
        premiumRepository.refreshPremiumStatus()
    }

    fun showOnboardingPaywall() {
        viewModelScope.launch {
            when (val result = purchaselyWrapper.loadPresentation("onboarding")) {
                is FetchResult.Success -> {
                    pendingOnboardingPresentation = result.handle
                    _requestPaywallDisplay.emit(Unit)
                }
                is FetchResult.Client -> {
                    Log.d(TAG, "[Shaker] CLIENT presentation received for onboarding placement — build custom UI here")
                }
                is FetchResult.Deactivated -> {
                    Log.d(TAG, "[Shaker] Onboarding presentation is deactivated")
                }
                is FetchResult.Error -> {
                    Log.d(TAG, "[Shaker] Onboarding presentation not available: ${result.message}")
                }
            }
        }
    }

    suspend fun displayPendingPaywall(activity: Activity) {
        val handle = pendingOnboardingPresentation ?: return
        pendingOnboardingPresentation = null
        val result = purchaselyWrapper.display(handle, activity)
        when (result) {
            is DisplayResult.Purchased, is DisplayResult.Restored -> {
                Log.d(TAG, "[Shaker] Purchased/Restored from onboarding")
                onPurchaseCompleted()
            }
            else -> {}
        }
    }

    fun setDisplayMode(mode: DisplayMode) {
        _displayMode.value = mode
        settingsRepo.displayMode = mode
        Log.d(TAG, "[Shaker] Display mode changed to: ${mode.storageValue}")
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        settingsRepo.themeMode = mode
        // PURCHASELY: Track the user's preferred theme as a custom attribute for audience segmentation
        // Docs: https://docs.purchasely.com/advanced-features/user-attributes
        purchaselyWrapper.setUserAttribute("app_theme", mode.storageValue)
    }

    fun setSdkMode(mode: PurchaselySdkMode) {
        if (_sdkMode.value == mode) return

        _sdkMode.value = mode
        settingsRepo.sdkModeStorage = mode.storageValue
        restartPurchaselySdk(mode)
    }

    fun setAnalyticsConsent(enabled: Boolean) {
        _analyticsConsent.value = enabled
        settingsRepo.analyticsConsent = enabled
        applyConsentPreferences()
    }

    fun setIdentifiedAnalyticsConsent(enabled: Boolean) {
        _identifiedAnalyticsConsent.value = enabled
        settingsRepo.identifiedAnalyticsConsent = enabled
        applyConsentPreferences()
    }

    fun setPersonalizationConsent(enabled: Boolean) {
        _personalizationConsent.value = enabled
        settingsRepo.personalizationConsent = enabled
        applyConsentPreferences()
    }

    fun setCampaignsConsent(enabled: Boolean) {
        _campaignsConsent.value = enabled
        settingsRepo.campaignsConsent = enabled
        applyConsentPreferences()
    }

    fun setThirdPartyConsent(enabled: Boolean) {
        _thirdPartyConsent.value = enabled
        settingsRepo.thirdPartyConsent = enabled
        applyConsentPreferences()
    }

    private fun restartPurchaselySdk(mode: PurchaselySdkMode) {
        purchaselyWrapper.restart()
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
    }
}
