package com.purchasely.shaker.purchasely

import io.purchasely.ext.PLYPresentation
import io.purchasely.models.PLYError

sealed class FetchResult {
    data class Success(val presentation: PLYPresentation) : FetchResult()
    data class Client(val presentation: PLYPresentation) : FetchResult()
    data object Deactivated : FetchResult()
    data class Error(val error: PLYError?) : FetchResult()
}
