package com.purchasely.shaker.data

import com.purchasely.shaker.data.storage.InMemoryKeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RunningModeRepositoryTest {

    private lateinit var store: InMemoryKeyValueStore

    @Before
    fun setUp() {
        store = InMemoryKeyValueStore()
    }

    @Test
    fun `default mode is OBSERVER (PurchaselySdkMode DEFAULT)`() {
        val repo = RunningModeRepository(store)
        assertEquals(PurchaselySdkMode.OBSERVER, repo.sdkMode)
    }

    @Test
    fun `isObserverMode is true by default`() {
        val repo = RunningModeRepository(store)
        assertTrue(repo.isObserverMode)
    }

    @Test
    fun `setting to OBSERVER persists observer string`() {
        val repo = RunningModeRepository(store)
        repo.sdkMode = PurchaselySdkMode.OBSERVER
        assertEquals("observer", store.getString(PurchaselySdkMode.KEY))
    }

    @Test
    fun `reading legacy paywallObserver from storage maps to OBSERVER`() {
        store.putString(PurchaselySdkMode.KEY, "paywallObserver")
        val repo = RunningModeRepository(store)
        assertEquals(PurchaselySdkMode.OBSERVER, repo.sdkMode)
        assertTrue(repo.isObserverMode)
    }

    @Test
    fun `reading observer value from storage`() {
        store.putString(PurchaselySdkMode.KEY, "observer")
        val repo = RunningModeRepository(store)
        assertEquals(PurchaselySdkMode.OBSERVER, repo.sdkMode)
        assertTrue(repo.isObserverMode)
    }

    @Test
    fun `setting to FULL persists full string`() {
        store.putString(PurchaselySdkMode.KEY, "paywallObserver")
        val repo = RunningModeRepository(store)
        repo.sdkMode = PurchaselySdkMode.FULL
        assertEquals("full", store.getString(PurchaselySdkMode.KEY))
    }

    @Test
    fun `reading FULL from settings storage`() {
        store.putString(PurchaselySdkMode.KEY, "full")
        val repo = RunningModeRepository(store)
        assertEquals(PurchaselySdkMode.FULL, repo.sdkMode)
        assertFalse(repo.isObserverMode)
    }

    @Test
    fun `unknown stored value defaults to OBSERVER`() {
        store.putString(PurchaselySdkMode.KEY, "unknown")
        val repo = RunningModeRepository(store)
        assertEquals(PurchaselySdkMode.OBSERVER, repo.sdkMode)
    }
}
