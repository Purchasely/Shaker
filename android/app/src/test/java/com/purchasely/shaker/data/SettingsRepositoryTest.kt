package com.purchasely.shaker.data

import com.purchasely.shaker.data.storage.InMemoryKeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {

    private lateinit var store: InMemoryKeyValueStore
    private lateinit var repo: SettingsRepository

    @Before
    fun setUp() {
        store = InMemoryKeyValueStore()
        repo = SettingsRepository(store)
    }

    @Test
    fun `userId defaults to null`() {
        assertNull(repo.userId)
    }

    @Test
    fun `userId round-trips`() {
        repo.userId = "kevin"
        assertEquals("kevin", repo.userId)
    }

    @Test
    fun `setting userId to null removes it`() {
        repo.userId = "kevin"
        repo.userId = null
        assertNull(repo.userId)
        assertFalse(store.contains("user_id"))
    }

    @Test
    fun `themeMode defaults to system`() {
        assertEquals("system", repo.themeMode)
    }

    @Test
    fun `themeMode round-trips`() {
        repo.themeMode = "dark"
        assertEquals("dark", repo.themeMode)
    }

    @Test
    fun `displayMode defaults to fullscreen`() {
        assertEquals("fullscreen", repo.displayMode)
    }

    @Test
    fun `displayMode round-trips`() {
        repo.displayMode = "embedded"
        assertEquals("embedded", repo.displayMode)
    }

    @Test
    fun `sdkModeStorage defaults to DEFAULT storageValue`() {
        assertEquals(PurchaselySdkMode.DEFAULT.storageValue, repo.sdkModeStorage)
    }

    @Test
    fun `sdkModeStorage round-trips`() {
        repo.sdkModeStorage = PurchaselySdkMode.FULL.storageValue
        assertEquals(PurchaselySdkMode.FULL.storageValue, repo.sdkModeStorage)
    }

    @Test
    fun `consent booleans default to true`() {
        assertTrue(repo.analyticsConsent)
        assertTrue(repo.identifiedAnalyticsConsent)
        assertTrue(repo.personalizationConsent)
        assertTrue(repo.campaignsConsent)
        assertTrue(repo.thirdPartyConsent)
    }

    @Test
    fun `consent booleans round-trip`() {
        repo.analyticsConsent = false
        repo.identifiedAnalyticsConsent = false
        repo.personalizationConsent = false
        repo.campaignsConsent = false
        repo.thirdPartyConsent = false

        assertFalse(repo.analyticsConsent)
        assertFalse(repo.identifiedAnalyticsConsent)
        assertFalse(repo.personalizationConsent)
        assertFalse(repo.campaignsConsent)
        assertFalse(repo.thirdPartyConsent)
    }

    @Test
    fun `initSdkModeIfNeeded sets default when key missing`() {
        repo.initSdkModeIfNeeded()
        assertTrue(store.contains(PurchaselySdkMode.KEY))
        assertEquals(PurchaselySdkMode.DEFAULT.storageValue, repo.sdkModeStorage)
    }

    @Test
    fun `initSdkModeIfNeeded does not overwrite existing value`() {
        repo.sdkModeStorage = PurchaselySdkMode.FULL.storageValue
        repo.initSdkModeIfNeeded()
        assertEquals(PurchaselySdkMode.FULL.storageValue, repo.sdkModeStorage)
    }
}
