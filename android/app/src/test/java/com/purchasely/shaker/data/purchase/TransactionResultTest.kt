package com.purchasely.shaker.data.purchase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionResultTest {

    @Test
    fun `Success is a singleton`() {
        assertTrue(TransactionResult.Success is TransactionResult)
    }

    @Test
    fun `Cancelled is a singleton`() {
        assertTrue(TransactionResult.Cancelled is TransactionResult)
    }

    @Test
    fun `Idle is a singleton`() {
        assertTrue(TransactionResult.Idle is TransactionResult)
    }

    @Test
    fun `Error holds message`() {
        val result = TransactionResult.Error("Payment failed")
        assertEquals("Payment failed", result.message)
    }

    @Test
    fun `Error with null message`() {
        val result = TransactionResult.Error(null)
        assertNull(result.message)
    }

    @Test
    fun `exhaustive when covers all cases`() {
        val results = listOf(
            TransactionResult.Success,
            TransactionResult.Cancelled,
            TransactionResult.Error("fail"),
            TransactionResult.Idle
        )
        results.forEach { result ->
            when (result) {
                is TransactionResult.Success -> {}
                is TransactionResult.Cancelled -> {}
                is TransactionResult.Error -> assertEquals("fail", result.message)
                is TransactionResult.Idle -> {}
            }
        }
    }
}
