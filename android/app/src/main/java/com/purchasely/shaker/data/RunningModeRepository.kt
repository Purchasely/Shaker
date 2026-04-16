package com.purchasely.shaker.data

import com.purchasely.shaker.data.storage.KeyValueStore
import io.purchasely.ext.PLYRunningMode

class RunningModeRepository(private val store: KeyValueStore) {

    var runningMode: PLYRunningMode
        get() {
            val stored = store.getString(KEY_RUNNING_MODE, PurchaselySdkMode.DEFAULT.storageValue)
            // Support legacy "observer" value from previous versions
            val mode = if (stored == LEGACY_OBSERVER) {
                PurchaselySdkMode.PAYWALL_OBSERVER
            } else {
                PurchaselySdkMode.fromStorage(stored)
            }
            return mode.runningMode
        }
        set(value) {
            val mode = PurchaselySdkMode.entries.firstOrNull { it.runningMode == value }
                ?: PurchaselySdkMode.DEFAULT
            store.putString(KEY_RUNNING_MODE, mode.storageValue)
        }

    val isObserverMode: Boolean
        get() = runningMode == PLYRunningMode.PaywallObserver

    companion object {
        private const val KEY_RUNNING_MODE = "running_mode"
        /** Legacy storage value from before PurchaselySdkMode unification */
        private const val LEGACY_OBSERVER = "observer"
    }
}
