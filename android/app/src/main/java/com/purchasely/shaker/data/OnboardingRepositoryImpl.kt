package com.purchasely.shaker.data

import com.purchasely.shaker.data.storage.KeyValueStore
import com.purchasely.shaker.domain.repository.OnboardingRepository

class OnboardingRepositoryImpl(private val store: KeyValueStore) : OnboardingRepository {

    override var isOnboardingCompleted: Boolean
        get() = store.getBoolean(KEY_COMPLETED)
        set(value) { store.putBoolean(KEY_COMPLETED, value) }

    companion object {
        private const val KEY_COMPLETED = "onboarding_completed"
    }
}
