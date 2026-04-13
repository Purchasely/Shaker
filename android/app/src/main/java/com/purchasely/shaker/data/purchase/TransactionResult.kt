package com.purchasely.shaker.data.purchase

sealed class TransactionResult {
    data object Success : TransactionResult()
    data object Cancelled : TransactionResult()
    data class Error(val message: String?) : TransactionResult()
    data object Idle : TransactionResult()
}
