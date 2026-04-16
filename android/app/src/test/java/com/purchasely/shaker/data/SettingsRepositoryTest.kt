package com.purchasely.shaker.data

import com.purchasely.shaker.data.storage.InMemoryKeyValueStore
import com.purchasely.shaker.domain.model.DisplayMode
import com.purchasely.shaker.domain.model.ThemeMode
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
    fun `themeMode defaults to SYSTEM`() {
        assertEquals(ThemeMode.SYSTEM, repo.themeMode)
    }

    @Test
    fun `themeMode round-trips`() {
        repo.themeMode = ThemeMode.DARK
        assertEquals(ThemeMode.DARK, repo.themeMode)
    }

    @Test
    fun `themeMode stores raw string value`() {
        repo.themeMode = ThemeMode.LIGHT
        assertEquals("light", store.getString("theme_mode"))
    }

    @Test
    fun `themeMode falls back to SYSTEM for unknown value`() {
        store.putString("theme_mode", "unknown")
        assertEquals(ThemeMode.SYSTEM, repo.themeMode)
    }

    @Test
    fun `displayMode defaults to FULLSCREEN`() {
        assertEquals(DisplayMode.FULLSCREEN, repo.displayMode)
    }

    @Test
    fun `displayMode round-trips`() {
        repo.displayMode = DisplayMode.MODAL
        assertEquals(DisplayMode.MODAL, repo.displayMode)
    }

    @Test
    fun `displayMode stores raw string value`() {
        repo.displayMode = DisplayMode.DRAWER
        assertEquals("drawer", store.getString("display_mode"))
    }

    @Test
    fun `displayMode falls back to FULLSCREEN for unknown value`() {
        store.putString("display_mode", "unknown")
        assertEquals(DisplayMode.FULLSCREEN, repo.displayMode)
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
