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

        fun fromStorage(value: String?): PurchaselySdkMode {
            if (value == "pay" + "wallObserver") return OBSERVER
            return values().firstOrNull { it.storageValue == value } ?: DEFAULT
        }
    }
}
