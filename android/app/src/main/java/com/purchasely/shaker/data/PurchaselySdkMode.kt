package com.purchasely.shaker.data

import io.purchasely.ext.PLYRunningMode

enum class PurchaselySdkMode(
    val storageValue: String,
    val label: String,
    val runningMode: PLYRunningMode
) {
    OBSERVER(
        storageValue = "observer",
        label = "Presentation Observer",
        runningMode = PLYRunningMode.Observer
    ),
    FULL(
        storageValue = "full",
        label = "Full",
        runningMode = PLYRunningMode.Full
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
