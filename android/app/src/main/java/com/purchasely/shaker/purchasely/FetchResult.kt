package com.purchasely.shaker.purchasely

sealed class FetchResult {
    data class Success(val handle: PresentationHandle, val height: Int) : FetchResult()
    data class Client(val handle: PresentationHandle) : FetchResult()
    data object Deactivated : FetchResult()
    data class Error(val message: String?) : FetchResult()
}
