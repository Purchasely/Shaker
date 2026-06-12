package com.purchasely.shaker.purchasely

/**
 * App-level view of an active Purchasely subscription. SDK-free on purpose:
 * `PurchaselyWrapper.fetchSubscriptions` maps the SDK's `PLYSubscriptionData`
 * to this type so the data layer never imports `io.purchasely`.
 */
data class SubscriptionInfo(
    val planName: String?,
    val productName: String?,
    val isActive: Boolean,
)
