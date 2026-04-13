package com.purchasely.shaker.data

import io.purchasely.ext.PLYRunningMode
import org.junit.Assert.assertEquals
import org.junit.Test

class PurchaselySdkModeTest {

    @Test
    fun `fromStorage returns PAYWALL_OBSERVER for paywallObserver`() {
        val mode = PurchaselySdkMode.fromStorage("paywallObserver")
        assertEquals(PurchaselySdkMode.PAYWALL_OBSERVER, mode)
    }

    @Test
    fun `fromStorage returns FULL for full`() {
        val mode = PurchaselySdkMode.fromStorage("full")
        assertEquals(PurchaselySdkMode.FULL, mode)
    }

    @Test
    fun `fromStorage returns DEFAULT for null`() {
        val mode = PurchaselySdkMode.fromStorage(null)
        assertEquals(PurchaselySdkMode.DEFAULT, mode)
    }

    @Test
    fun `fromStorage returns DEFAULT for unknown value`() {
        val mode = PurchaselySdkMode.fromStorage("unknown")
        assertEquals(PurchaselySdkMode.DEFAULT, mode)
    }

    @Test
    fun `DEFAULT is PAYWALL_OBSERVER`() {
        assertEquals(PurchaselySdkMode.PAYWALL_OBSERVER, PurchaselySdkMode.DEFAULT)
    }

    @Test
    fun `storageValue matches expected strings`() {
        assertEquals("paywallObserver", PurchaselySdkMode.PAYWALL_OBSERVER.storageValue)
        assertEquals("full", PurchaselySdkMode.FULL.storageValue)
    }

    @Test
    fun `label matches expected strings`() {
        assertEquals("Paywall Observer", PurchaselySdkMode.PAYWALL_OBSERVER.label)
        assertEquals("Full", PurchaselySdkMode.FULL.label)
    }

    @Test
    fun `runningMode maps correctly`() {
        assertEquals(PLYRunningMode.PaywallObserver, PurchaselySdkMode.PAYWALL_OBSERVER.runningMode)
        assertEquals(PLYRunningMode.Full, PurchaselySdkMode.FULL.runningMode)
    }

    @Test
    fun `PREFERENCES_NAME constant`() {
        assertEquals("shaker_settings", PurchaselySdkMode.PREFERENCES_NAME)
    }

    @Test
    fun `KEY constant`() {
        assertEquals("purchasely_sdk_mode", PurchaselySdkMode.KEY)
    }

    @Test
    fun `roundtrip fromStorage with storageValue`() {
        PurchaselySdkMode.values().forEach { mode ->
            assertEquals(mode, PurchaselySdkMode.fromStorage(mode.storageValue))
        }
    }
}
