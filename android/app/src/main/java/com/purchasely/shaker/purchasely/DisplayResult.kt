package com.purchasely.shaker.purchasely

sealed class DisplayResult {
    data class Purchased(val planName: String?) : DisplayResult()
    data class Restored(val planName: String?) : DisplayResult()
    data object Cancelled : DisplayResult()
}
