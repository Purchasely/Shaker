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
    fun `default mode is Full`() {
        val repo = RunningModeRepository(store)
        assertEquals(PLYRunningMode.Full, repo.runningMode)
    }

    @Test
    fun `isObserverMode is false when Full`() {
        val repo = RunningModeRepository(store)
        assertFalse(repo.isObserverMode)
    }

    @Test
    fun `setting to PaywallObserver persists observer string`() {
        val repo = RunningModeRepository(store)
        repo.runningMode = PLYRunningMode.PaywallObserver
        assertEquals("observer", store.getString("running_mode"))
    }

    @Test
    fun `reading PaywallObserver from storage`() {
        store.putString("running_mode", "observer")
        val repo = RunningModeRepository(store)
        assertEquals(PLYRunningMode.PaywallObserver, repo.runningMode)
        assertTrue(repo.isObserverMode)
    }

    @Test
    fun `setting to Full persists full string`() {
        store.putString("running_mode", "observer")
        val repo = RunningModeRepository(store)
        repo.runningMode = PLYRunningMode.Full
        assertEquals("full", store.getString("running_mode"))
    }

    @Test
    fun `unknown stored value defaults to Full`() {
        store.putString("running_mode", "unknown")
        val repo = RunningModeRepository(store)
        assertEquals(PLYRunningMode.Full, repo.runningMode)
    }
}
