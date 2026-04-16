package com.purchasely.shaker.data

import com.purchasely.shaker.data.storage.InMemoryKeyValueStore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnboardingRepositoryTest {

    private lateinit var store: InMemoryKeyValueStore

    @Before
    fun setUp() {
        store = InMemoryKeyValueStore()
    }

    @Test
    fun `initial state is false`() {
        val repo = OnboardingRepository(store)
        assertFalse(repo.isOnboardingCompleted)
    }

    @Test
    fun `setting to true persists`() {
        val repo = OnboardingRepository(store)
        repo.isOnboardingCompleted = true
        assertTrue(repo.isOnboardingCompleted)
        assertTrue(store.getBoolean("onboarding_completed"))
    }

    @Test
    fun `setting to false persists`() {
        store.putBoolean("onboarding_completed", true)
        val repo = OnboardingRepository(store)
        repo.isOnboardingCompleted = false
        assertFalse(repo.isOnboardingCompleted)
        assertFalse(store.getBoolean("onboarding_completed"))
    }

    @Test
    fun `reads stored value on creation`() {
        store.putBoolean("onboarding_completed", true)
        val repo = OnboardingRepository(store)
        assertTrue(repo.isOnboardingCompleted)
    }
}
