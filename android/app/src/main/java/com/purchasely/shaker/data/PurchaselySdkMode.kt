package com.purchasely.shaker.data

import io.purchasely.ext.PLYRunningMode

enum class PurchaselySdkMode(
    val storageValue: String,
    val label: String,
    val runningMode: PLYRunningMode
) {
    PAYWALL_OBSERVER(
        storageValue = "paywallObserver",
        label = "Paywall Observer",
        runningMode = PLYRunningMode.PaywallObserver
    ),
    FULL(
        storageValue = "full",
        label = "Full",
        runningMode = PLYRunningMode.Full
    );

    companion object {
        const val PREFERENCES_NAME = "shaker_settings"
        const val KEY = "purchasely_sdk_mode"
        val DEFAULT = PAYWALL_OBSERVER

        fun fromStorage(value: String?): PurchaselySdkMode {
            return values().firstOrNull { it.storageValue == value } ?: DEFAULT
        }
    }
}
