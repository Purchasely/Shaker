package com.purchasely.shaker.data

import android.content.Context
import android.content.SharedPreferences

class OnboardingRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("shaker_onboarding", Context.MODE_PRIVATE)

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_COMPLETED, false)
        set(value) { prefs.edit().putBoolean(KEY_COMPLETED, value).apply() }

    companion object {
        private const val KEY_COMPLETED = "onboarding_completed"
    }
}
