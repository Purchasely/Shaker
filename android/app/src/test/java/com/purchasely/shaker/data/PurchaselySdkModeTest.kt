package com.purchasely.shaker.data

import io.purchasely.ext.PLYRunningMode
import org.junit.Assert.assertEquals
import org.junit.Test

class PurchaselySdkModeTest {

    @Test
    fun `fromStorage returns OBSERVER for legacy paywallObserver`() {
        val mode = PurchaselySdkMode.fromStorage("paywallObserver")
        assertEquals(PurchaselySdkMode.OBSERVER, mode)
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
    fun `DEFAULT is OBSERVER`() {
        assertEquals(PurchaselySdkMode.OBSERVER, PurchaselySdkMode.DEFAULT)
    }

    @Test
    fun `storageValue matches expected strings`() {
        assertEquals("observer", PurchaselySdkMode.OBSERVER.storageValue)
        assertEquals("full", PurchaselySdkMode.FULL.storageValue)
    }

    @Test
    fun `label matches expected strings`() {
        assertEquals("Presentation Observer", PurchaselySdkMode.OBSERVER.label)
        assertEquals("Full", PurchaselySdkMode.FULL.label)
    }

    @Test
    fun `runningMode maps correctly`() {
        assertEquals(PLYRunningMode.Observer, PurchaselySdkMode.OBSERVER.runningMode)
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
