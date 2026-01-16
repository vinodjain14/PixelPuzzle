package com.example.pixelpuzzle

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages Google Play Billing for in-app purchases
 * Handles consumable purchases (hints, coins) and non-consumables
 */
class BillingManager(private val context: Context) {

    private var billingClient: BillingClient? = null
    private var isInitialized = false

    companion object {
        private const val TAG = "BillingManager"

        // Product IDs - Define these in Google Play Console
        const val PRODUCT_50_COINS = "coins_pack_50"
        const val PRODUCT_150_COINS = "coins_pack_150"
        const val PRODUCT_500_COINS = "coins_pack_500"
        const val PRODUCT_1000_COINS = "coins_pack_1000"
        const val PRODUCT_5_PREMIUM_HINTS = "premium_hints_5"
        const val PRODUCT_10_PREMIUM_HINTS = "premium_hints_10"
        const val PRODUCT_3_MASTER_REVEALS = "master_reveals_3"
        const val PRODUCT_REMOVE_ADS = "remove_ads_forever"

        @Volatile
        private var instance: BillingManager? = null

        fun getInstance(context: Context): BillingManager {
            return instance ?: synchronized(this) {
                instance ?: BillingManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                DebugConfig.d(TAG, "Purchase canceled by user")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                DebugConfig.d(TAG, "Item already owned")
            }
            else -> {
                DebugConfig.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Initialize billing client
     */
    fun initialize(onReady: () -> Unit = {}) {
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isInitialized = true
                    DebugConfig.d(TAG, "Billing client initialized successfully")

                    // Query and process existing purchases
                    queryPurchases()
                    onReady()
                } else {
                    DebugConfig.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                isInitialized = false
                DebugConfig.w(TAG, "Billing service disconnected")
                // Try to reconnect
            }
        })
    }

    /**
     * Query available products
     */
    fun queryProducts(onProductsLoaded: (List<ProductDetails>) -> Unit) {
        if (!isInitialized) {
            DebugConfig.w(TAG, "Billing not initialized")
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_50_COINS)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_150_COINS)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_500_COINS)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_1000_COINS)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_5_PREMIUM_HINTS)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_10_PREMIUM_HINTS)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_3_MASTER_REVEALS)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_REMOVE_ADS)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                DebugConfig.d(TAG, "Products loaded: ${productDetailsList.size}")
                onProductsLoaded(productDetailsList)
            } else {
                DebugConfig.e(TAG, "Failed to load products: ${billingResult.debugMessage}")
                onProductsLoaded(emptyList())
            }
        }
    }

    /**
     * Launch purchase flow
     */
    fun launchPurchaseFlow(
        activity: Activity,
        productDetails: ProductDetails
    ) {
        if (!isInitialized) {
            DebugConfig.w(TAG, "Billing not initialized")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient?.launchBillingFlow(activity, billingFlowParams)
    }

    /**
     * Handle completed purchase
     */
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                // Grant items to user
                grantPurchaseItems(purchase)

                // Acknowledge or consume purchase
                if (isConsumable(purchase)) {
                    consumePurchase(purchase)
                } else {
                    acknowledgePurchase(purchase)
                }
            }
        }
    }

    /**
     * Grant purchased items to user
     */
    private fun grantPurchaseItems(purchase: Purchase) {
        purchase.products.forEach { productId ->
            when (productId) {
                PRODUCT_50_COINS -> GamePreferences.addPoints(context, 50)
                PRODUCT_150_COINS -> GamePreferences.addPoints(context, 150)
                PRODUCT_500_COINS -> GamePreferences.addPoints(context, 500)
                PRODUCT_1000_COINS -> GamePreferences.addPoints(context, 1000)
                PRODUCT_5_PREMIUM_HINTS -> HintSystemManager.addPremiumHints(context, 5)
                PRODUCT_10_PREMIUM_HINTS -> HintSystemManager.addPremiumHints(context, 10)
                PRODUCT_3_MASTER_REVEALS -> HintSystemManager.addMasterReveals(context, 3)
                PRODUCT_REMOVE_ADS -> setAdsRemoved(context, true)
            }
            DebugConfig.d(TAG, "Granted items for: $productId")
        }
    }

    /**
     * Check if product is consumable
     */
    private fun isConsumable(purchase: Purchase): Boolean {
        return purchase.products.any { productId ->
            productId in listOf(
                PRODUCT_50_COINS,
                PRODUCT_150_COINS,
                PRODUCT_500_COINS,
                PRODUCT_1000_COINS,
                PRODUCT_5_PREMIUM_HINTS,
                PRODUCT_10_PREMIUM_HINTS,
                PRODUCT_3_MASTER_REVEALS
            )
        }
    }

    /**
     * Consume purchase (for consumable items)
     */
    private fun consumePurchase(purchase: Purchase) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            val result = billingClient?.consumePurchase(consumeParams)
            withContext(Dispatchers.Main) {
                if (result?.billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
                    DebugConfig.d(TAG, "Purchase consumed successfully")
                } else {
                    DebugConfig.e(TAG, "Failed to consume purchase: ${result?.billingResult?.debugMessage}")
                }
            }
        }
    }

    /**
     * Acknowledge purchase (for non-consumable items)
     */
    private fun acknowledgePurchase(purchase: Purchase) {
        val ackParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.acknowledgePurchase(ackParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                DebugConfig.d(TAG, "Purchase acknowledged successfully")
            } else {
                DebugConfig.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Query existing purchases
     */
    private fun queryPurchases() {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
        }
    }

    /**
     * Check if ads are removed
     */
    fun hasRemovedAds(context: Context): Boolean {
        val prefs = GamePreferences.getPrefs(context)
        return prefs.getBoolean("ads_removed", false)
    }

    private fun setAdsRemoved(context: Context, removed: Boolean) {
        val prefs = GamePreferences.getPrefs(context)
        prefs.edit().putBoolean("ads_removed", removed).apply()
        DebugConfig.d(TAG, "Ads removed status: $removed")
    }

    /**
     * Release billing client
     */
    fun release() {
        billingClient?.endConnection()
        billingClient = null
        isInitialized = false
        DebugConfig.d(TAG, "Billing client released")
    }
}

/**
 * IAP Product information
 */
data class IAPProduct(
    val productId: String,
    val title: String,
    val description: String,
    val price: String,
    val icon: String,
    val productDetails: ProductDetails? = null
)