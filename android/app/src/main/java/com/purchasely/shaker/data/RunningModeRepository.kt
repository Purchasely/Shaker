package com.purchasely.shaker.data

import com.purchasely.shaker.data.storage.KeyValueStore

/**
 * Persists the Full/Observer mode choice. SDK-free on purpose: exposes the
 * app-level [PurchaselySdkMode]; only `PurchaselyWrapper` maps it to the
 * Purchasely `PLYRunningMode`.
 */
class RunningModeRepository(private val store: KeyValueStore) {

    var sdkMode: PurchaselySdkMode
        get() {
            val stored = store.getString(PurchaselySdkMode.KEY, PurchaselySdkMode.DEFAULT.storageValue)
            // fromStorage handles both the current "observer" value and the legacy "paywallObserver" one.
            return PurchaselySdkMode.fromStorage(stored)
        }
        set(value) {
            store.putString(PurchaselySdkMode.KEY, value.storageValue)
        }

    val isObserverMode: Boolean
        get() = sdkMode == PurchaselySdkMode.OBSERVER

}
