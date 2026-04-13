package com.purchasely.shaker.data.purchase

import android.app.Activity

data class PurchaseRequest(
    val activity: Activity,
    val productId: String,
    val offerToken: String
)
