package com.akustom15.mint.library.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.akustom15.mint.library.security.Security
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Centralized billing manager for premium icon requests.
 * Reusable across all icon packs using the Mint library.
 *
 * @param base64PublicKey the app's RSA public key from Google Play Console. When
 * set, every purchase is signature-verified before credits are granted, so a
 * spoofed/fake purchase (e.g. via patched billing) is rejected. If left empty,
 * verification is skipped (logs a warning) to avoid blocking apps that haven't
 * configured the key yet.
 */
class MintBillingManager(
    private val context: Context,
    private val products: List<MintPremiumProduct>,
    private val base64PublicKey: String = ""
) {
    companion object {
        private const val TAG = "MintBillingManager"
    }

    sealed class BillingState {
        object Initializing : BillingState()
        object Ready : BillingState()
        data class PurchaseSuccess(val productId: String, val orderId: String) : BillingState()
        data class Error(val message: String) : BillingState()
    }

    private val _billingState = MutableStateFlow<BillingState>(BillingState.Initializing)
    val billingState: StateFlow<BillingState> = _billingState

    private val billingClient by lazy {
        BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                Log.d(TAG, "Purchase successful. Purchases: ${purchases?.size ?: 0}")
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        processPremiumPurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User canceled purchase")
                _billingState.value = BillingState.Error("Purchase canceled")
            }
            else -> {
                Log.e(TAG, "Purchase error: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                _billingState.value = BillingState.Error("Purchase error: ${billingResult.debugMessage}")
            }
        }
    }

    fun initialize() {
        Log.d(TAG, "Initializing MintBillingManager...")
        _billingState.value = BillingState.Initializing

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        Log.d(TAG, "Billing client ready")
                        _billingState.value = BillingState.Ready
                        checkAvailableProducts()
                    }
                    else -> {
                        Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                        _billingState.value = BillingState.Error("Billing setup failed")
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
                _billingState.value = BillingState.Error("Billing service disconnected")
            }
        })
    }

    private fun checkAvailableProducts() {
        val productList = products.map { product ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(product.productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Available products: ${productDetailsList.size}")
                productDetailsList.forEach { details ->
                    Log.d(TAG, "  - ${details.productId}: ${details.oneTimePurchaseOfferDetails?.formattedPrice}")
                }
            } else {
                Log.w(TAG, "Error querying products: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productId: String): Boolean {
        if (!billingClient.isReady) {
            Log.e(TAG, "BillingClient not ready")
            _billingState.value = BillingState.Error("Billing not ready. Try again.")
            return false
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                        )
                    )
                    .build()

                val result = billingClient.launchBillingFlow(activity, flowParams)
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.e(TAG, "Launch billing flow failed: ${result.debugMessage}")
                    _billingState.value = BillingState.Error("Could not start purchase")
                }
            } else if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.e(TAG, "Query error: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                _billingState.value = BillingState.Error("Error querying product: ${billingResult.debugMessage}")
            } else {
                Log.e(TAG, "Product not found: $productId. Make sure it exists in Google Play Console.")
                _billingState.value = BillingState.Error("Product not available. Verify product ID in Play Console.")
            }
        }

        return true
    }

    private fun processPremiumPurchase(purchase: Purchase) {
        val productId = purchase.products.firstOrNull() ?: return
        val orderId = purchase.orderId ?: "unknown"

        // Verify the purchase signature against the Play Console public key.
        // Rejects spoofed/tampered purchases (patched billing, fake stores).
        if (base64PublicKey.isNotBlank()) {
            val valid = Security.verifyPurchase(base64PublicKey, purchase.originalJson, purchase.signature)
            if (!valid) {
                Log.e(TAG, "Rejected purchase with invalid signature: $productId")
                _billingState.value = BillingState.Error("Invalid purchase signature")
                return
            }
        } else {
            Log.w(TAG, "base64PublicKey not set — skipping signature verification (configure it in MintConfig).")
        }

        Log.d(TAG, "Processing purchase: $productId -> $orderId")

        val product = products.find { it.productId == productId }
        if (product != null) {
            MintPremiumPreferences.processPremiumPurchase(
                context, productId, orderId, product.requestCount
            )
            _billingState.value = BillingState.PurchaseSuccess(productId, orderId)
        }

        // Consume the purchase so it can be bought again
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.consumeAsync(consumeParams) { result, _ ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase consumed successfully")
            } else {
                Log.w(TAG, "Error consuming purchase: ${result.debugMessage}")
            }
        }
    }

    fun destroy() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}
