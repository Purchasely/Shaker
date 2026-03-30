package com.purchasely.shaker.data.purchase

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.QueryProductDetailsParams
import android.content.Context
import io.purchasely.ext.Purchasely

class PurchaseManager(context: Context) : PurchasesUpdatedListener {

    private var onPurchaseResult: ((Boolean) -> Unit)? = null

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    init {
        connectBillingClient()
    }

    private fun connectBillingClient() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "[Shaker] BillingClient connected")
                } else {
                    Log.e(TAG, "[Shaker] BillingClient connection failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "[Shaker] BillingClient disconnected, reconnecting...")
                connectBillingClient()
            }
        })
    }

    /**
     * Launch a purchase flow using the offerToken from the Purchasely interceptor parameters.
     * In Observer mode, the interceptor provides `parameters.subscriptionOffer?.offerToken`.
     * We must first query ProductDetails (required by BillingClient), then launch the flow.
     */
    fun purchase(
        activity: Activity,
        productId: String,
        offerToken: String,
        onResult: (Boolean) -> Unit
    ) {
        onPurchaseResult = onResult

        // BillingFlowParams requires ProductDetails — query it first
        val queryParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(queryParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK || productDetailsList.isEmpty()) {
                Log.e(TAG, "[Shaker] queryProductDetails failed: ${billingResult.debugMessage}")
                onPurchaseResult?.invoke(false)
                onPurchaseResult = null
                return@queryProductDetailsAsync
            }

            val productDetails = productDetailsList.first()

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .setOfferToken(offerToken)
                            .build()
                    )
                )
                .build()

            val result = billingClient.launchBillingFlow(activity, flowParams)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.e(TAG, "[Shaker] Launch billing flow failed: ${result.debugMessage}")
                onPurchaseResult?.invoke(false)
                onPurchaseResult = null
            }
        }
    }

    /**
     * Restore purchases by querying existing subscriptions and in-app products.
     */
    fun restore(onResult: (Boolean) -> Unit) {
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(subsParams) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val activePurchases = purchases.filter {
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                activePurchases.forEach { acknowledgePurchase(it) }

                Log.d(TAG, "[Shaker] Restored ${activePurchases.size} purchases")
                Purchasely.synchronize()
                onResult(activePurchases.isNotEmpty())
            } else {
                Log.e(TAG, "[Shaker] Restore failed: ${billingResult.debugMessage}")
                onResult(false)
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    acknowledgePurchase(purchase)
                }
            }
            Log.d(TAG, "[Shaker] Purchase successful, synchronizing with Purchasely...")
            Purchasely.synchronize()
            onPurchaseResult?.invoke(true)
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "[Shaker] Purchase cancelled by user")
            onPurchaseResult?.invoke(false)
        } else {
            Log.e(TAG, "[Shaker] Purchase error: ${billingResult.debugMessage}")
            onPurchaseResult?.invoke(false)
        }
        onPurchaseResult = null
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) return

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "[Shaker] Purchase acknowledged: ${purchase.orderId}")
            } else {
                Log.e(TAG, "[Shaker] Acknowledge failed: ${billingResult.debugMessage}")
            }
        }
    }

    companion object {
        private const val TAG = "PurchaseManager"
    }
}
