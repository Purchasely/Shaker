package com.purchasely.shaker.data

import com.purchasely.shaker.data.storage.KeyValueStore

class OnboardingRepository(private val store: KeyValueStore) {

    var isOnboardingCompleted: Boolean
        get() = store.getBoolean(KEY_COMPLETED)
        set(value) { store.putBoolean(KEY_COMPLETED, value) }

    companion object {
        private const val KEY_COMPLETED = "onboarding_completed"
    }
}
