package com.purchasely.shaker.data

/**
 * App-level SDK mode selection. Deliberately SDK-free: only `PurchaselyWrapper`
 * maps this to the Purchasely `PLYRunningMode` — the rest of the app never
 * imports `io.purchasely`.
 */
enum class PurchaselySdkMode(
    val storageValue: String,
    val label: String,
) {
    OBSERVER(
        storageValue = "observer",
        label = "Observer",
    ),
    FULL(
        storageValue = "full",
        label = "Full",
    );

    companion object {
        const val PREFERENCES_NAME = "shaker_settings"
        const val KEY = "purchasely_sdk_mode"
        val DEFAULT = OBSERVER

        /** Storage value persisted by pre-v6 versions, when OBSERVER was named "paywallObserver". */
        private const val LEGACY_PAYWALL_OBSERVER = "paywallObserver"

        fun fromStorage(value: String?): PurchaselySdkMode {
            if (value == LEGACY_PAYWALL_OBSERVER) return OBSERVER
            return values().firstOrNull { it.storageValue == value } ?: DEFAULT
        }
    }
}
