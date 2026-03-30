package com.purchasely.shaker.data

import android.content.Context
import android.content.SharedPreferences
import io.purchasely.ext.PLYRunningMode

class RunningModeRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("shaker_settings", Context.MODE_PRIVATE)

    var runningMode: PLYRunningMode
        get() {
            val stored = prefs.getString(KEY_RUNNING_MODE, "full")
            return if (stored == "observer") PLYRunningMode.PaywallObserver else PLYRunningMode.Full
        }
        set(value) {
            val str = if (value == PLYRunningMode.PaywallObserver) "observer" else "full"
            prefs.edit().putString(KEY_RUNNING_MODE, str).apply()
        }

    val isObserverMode: Boolean
        get() = runningMode == PLYRunningMode.PaywallObserver

    companion object {
        private const val KEY_RUNNING_MODE = "running_mode"
    }
}
