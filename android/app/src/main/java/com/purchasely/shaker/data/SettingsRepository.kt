package com.purchasely.shaker.data

import com.purchasely.shaker.data.storage.KeyValueStore

class SettingsRepository(private val store: KeyValueStore) {

    var userId: String?
        get() = store.getString(KEY_USER_ID)
        set(value) {
            if (value != null) store.putString(KEY_USER_ID, value) else store.remove(KEY_USER_ID)
        }

    var themeMode: String
        get() = store.getString(KEY_THEME, "system") ?: "system"
        set(value) = store.putString(KEY_THEME, value)

    var displayMode: String
        get() = store.getString(KEY_DISPLAY_MODE, "fullscreen") ?: "fullscreen"
        set(value) = store.putString(KEY_DISPLAY_MODE, value)

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
