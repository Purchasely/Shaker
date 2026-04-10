package com.purchasely.shaker.purchasely

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayResultTest {

    @Test
    fun `Purchased holds plan name`() {
        val result = DisplayResult.Purchased("Premium Monthly")
        assertTrue(result is DisplayResult.Purchased)
        assertEquals("Premium Monthly", result.planName)
    }

    @Test
    fun `Purchased with null plan name`() {
        val result = DisplayResult.Purchased(null)
        assertNull(result.planName)
    }

    @Test
    fun `Restored holds plan name`() {
        val result = DisplayResult.Restored("Premium Yearly")
        assertTrue(result is DisplayResult.Restored)
        assertEquals("Premium Yearly", result.planName)
    }

    @Test
    fun `Restored with null plan name`() {
        val result = DisplayResult.Restored(null)
        assertNull(result.planName)
    }

    @Test
    fun `Cancelled is a singleton object`() {
        val result = DisplayResult.Cancelled
        assertTrue(result is DisplayResult.Cancelled)
    }

    @Test
    fun `sealed class exhaustive when`() {
        val results = listOf(
            DisplayResult.Purchased("Plan A"),
            DisplayResult.Restored("Plan B"),
            DisplayResult.Cancelled
        )

        results.forEach { result ->
            when (result) {
                is DisplayResult.Purchased -> assertEquals("Plan A", result.planName)
                is DisplayResult.Restored -> assertEquals("Plan B", result.planName)
                is DisplayResult.Cancelled -> {} // OK
            }
        }
    }

    @Test
    fun `Purchased data class equality`() {
        val a = DisplayResult.Purchased("Plan")
        val b = DisplayResult.Purchased("Plan")
        assertEquals(a, b)
    }

    @Test
    fun `Restored data class equality`() {
        val a = DisplayResult.Restored("Plan")
        val b = DisplayResult.Restored("Plan")
        assertEquals(a, b)
    }
}
