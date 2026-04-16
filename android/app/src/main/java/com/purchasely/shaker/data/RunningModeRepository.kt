package com.purchasely.shaker.data

import com.purchasely.shaker.data.storage.KeyValueStore
import io.purchasely.ext.PLYRunningMode

class RunningModeRepository(private val store: KeyValueStore) {

    var runningMode: PLYRunningMode
        get() {
            val stored = store.getString(KEY_RUNNING_MODE, "full")
            return if (stored == "observer") PLYRunningMode.PaywallObserver else PLYRunningMode.Full
        }
        set(value) {
            val str = if (value == PLYRunningMode.PaywallObserver) "observer" else "full"
            store.putString(KEY_RUNNING_MODE, str)
        }

    val isObserverMode: Boolean
        get() = runningMode == PLYRunningMode.PaywallObserver

    companion object {
        private const val KEY_RUNNING_MODE = "running_mode"
    }
}
