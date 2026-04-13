package com.purchasely.shaker.data.purchase

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class PurchaseManager(
    billingClientFactory: (PurchasesUpdatedListener) -> BillingClient,
    purchaseRequests: SharedFlow<PurchaseRequest>,
    restoreRequests: SharedFlow<RestoreRequest>,
    scope: CoroutineScope
) : PurchasesUpdatedListener {

    private val billingClient = billingClientFactory(this)

    private val _transactionResult = MutableSharedFlow<TransactionResult>(replay = 1)
    val transactionResult: SharedFlow<TransactionResult> = _transactionResult.asSharedFlow()

    init {
        connectBillingClient()
        scope.launch {
            purchaseRequests.collect { request ->
                launchPurchase(request.activity, request.productId, request.offerToken)
            }
        }
        scope.launch {
            restoreRequests.collect {
                restorePurchases()
            }
        }
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

    private fun launchPurchase(activity: Activity, productId: String, offerToken: String) {
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
                _transactionResult.tryEmit(TransactionResult.Error(billingResult.debugMessage))
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
                _transactionResult.tryEmit(TransactionResult.Error(result.debugMessage))
            }
        }
    }

    private fun restorePurchases() {
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
                _transactionResult.tryEmit(
                    if (activePurchases.isNotEmpty()) TransactionResult.Success
                    else TransactionResult.Cancelled
                )
            } else {
                Log.e(TAG, "[Shaker] Restore failed: ${billingResult.debugMessage}")
                _transactionResult.tryEmit(TransactionResult.Error(billingResult.debugMessage))
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
            Log.d(TAG, "[Shaker] Purchase successful")
            _transactionResult.tryEmit(TransactionResult.Success)
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "[Shaker] Purchase cancelled by user")
            _transactionResult.tryEmit(TransactionResult.Cancelled)
        } else {
            Log.e(TAG, "[Shaker] Purchase error: ${billingResult.debugMessage}")
            _transactionResult.tryEmit(TransactionResult.Error(billingResult.debugMessage))
        }
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
