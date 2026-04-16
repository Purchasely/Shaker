package com.purchasely.shaker.data

import com.purchasely.shaker.data.storage.InMemoryKeyValueStore
import io.purchasely.ext.PLYRunningMode
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
    fun `default mode is PaywallObserver (PurchaselySdkMode DEFAULT)`() {
        val repo = RunningModeRepository(store)
        assertEquals(PLYRunningMode.PaywallObserver, repo.runningMode)
    }

    @Test
    fun `isObserverMode is true by default`() {
        val repo = RunningModeRepository(store)
        assertTrue(repo.isObserverMode)
    }

    @Test
    fun `setting to PaywallObserver persists paywallObserver string`() {
        val repo = RunningModeRepository(store)
        repo.runningMode = PLYRunningMode.PaywallObserver
        assertEquals("paywallObserver", store.getString("running_mode"))
    }

    @Test
    fun `reading paywallObserver from storage`() {
        store.putString("running_mode", "paywallObserver")
        val repo = RunningModeRepository(store)
        assertEquals(PLYRunningMode.PaywallObserver, repo.runningMode)
        assertTrue(repo.isObserverMode)
    }

    @Test
    fun `legacy observer value migrates to PaywallObserver`() {
        store.putString("running_mode", "observer")
        val repo = RunningModeRepository(store)
        assertEquals(PLYRunningMode.PaywallObserver, repo.runningMode)
        assertTrue(repo.isObserverMode)
    }

    @Test
    fun `setting to Full persists full string`() {
        store.putString("running_mode", "paywallObserver")
        val repo = RunningModeRepository(store)
        repo.runningMode = PLYRunningMode.Full
        assertEquals("full", store.getString("running_mode"))
    }

    @Test
    fun `reading Full from storage`() {
        store.putString("running_mode", "full")
        val repo = RunningModeRepository(store)
        assertEquals(PLYRunningMode.Full, repo.runningMode)
        assertFalse(repo.isObserverMode)
    }

    @Test
    fun `unknown stored value defaults to PaywallObserver`() {
        store.putString("running_mode", "unknown")
        val repo = RunningModeRepository(store)
        assertEquals(PLYRunningMode.PaywallObserver, repo.runningMode)
    }
}
