package com.purchasely.shaker.data

import com.purchasely.shaker.data.storage.KeyValueStore
import com.purchasely.shaker.domain.model.DisplayMode
import com.purchasely.shaker.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(private val store: KeyValueStore) {

    private val _themeMode = MutableStateFlow(ThemeMode.fromStorage(store.getString(KEY_THEME)))
    val themeModeFlow: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    var userId: String?
        get() = store.getString(KEY_USER_ID)
        set(value) {
            if (value != null) store.putString(KEY_USER_ID, value) else store.remove(KEY_USER_ID)
        }

    var themeMode: ThemeMode
        get() = _themeMode.value
        set(value) {
            store.putString(KEY_THEME, value.storageValue)
            _themeMode.value = value
        }

    var displayMode: DisplayMode
        get() = DisplayMode.fromStorage(store.getString(KEY_DISPLAY_MODE))
        set(value) = store.putString(KEY_DISPLAY_MODE, value.storageValue)

    var sdkModeStorage: String
        get() = store.getString(PurchaselySdkMode.KEY, PurchaselySdkMode.DEFAULT.storageValue)
            ?: PurchaselySdkMode.DEFAULT.storageValue
        set(value) = store.putString(PurchaselySdkMode.KEY, value)

    var analyticsConsent: Boolean
        get() = store.getBoolean(KEY_CONSENT_ANALYTICS, true)
        set(value) = store.putBoolean(KEY_CONSENT_ANALYTICS, value)

    var identifiedAnalyticsConsent: Boolean
        get() = store.getBoolean(KEY_CONSENT_IDENTIFIED_ANALYTICS, true)
        set(value) = store.putBoolean(KEY_CONSENT_IDENTIFIED_ANALYTICS, value)

    var personalizationConsent: Boolean
        get() = store.getBoolean(KEY_CONSENT_PERSONALIZATION, true)
        set(value) = store.putBoolean(KEY_CONSENT_PERSONALIZATION, value)

    var campaignsConsent: Boolean
        get() = store.getBoolean(KEY_CONSENT_CAMPAIGNS, true)
        set(value) = store.putBoolean(KEY_CONSENT_CAMPAIGNS, value)

    var thirdPartyConsent: Boolean
        get() = store.getBoolean(KEY_CONSENT_THIRD_PARTY, true)
        set(value) = store.putBoolean(KEY_CONSENT_THIRD_PARTY, value)

    fun initSdkModeIfNeeded() {
        if (!store.contains(PurchaselySdkMode.KEY)) {
            store.putString(PurchaselySdkMode.KEY, PurchaselySdkMode.DEFAULT.storageValue)
        }
    }

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_DISPLAY_MODE = "display_mode"
        private const val KEY_CONSENT_ANALYTICS = "consent_analytics"
        private const val KEY_CONSENT_IDENTIFIED_ANALYTICS = "consent_identified_analytics"
        private const val KEY_CONSENT_PERSONALIZATION = "consent_personalization"
        private const val KEY_CONSENT_CAMPAIGNS = "consent_campaigns"
        private const val KEY_CONSENT_THIRD_PARTY = "consent_third_party"
    }
}
