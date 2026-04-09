package com.example.choreboo_habittrackerfriend.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Subscription product ID registered in the Google Play Console. */
const val PREMIUM_PRODUCT_ID = "choreboo_premium_monthly"

@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences,
) {
    private val TAG = "BillingRepository"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Exposed to UI — true when an active, acknowledged premium subscription is detected.
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    // Product details populated after a successful queryProductDetails call.
    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase -> scope.launch { handlePurchase(purchase) } }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User cancelled the purchase flow.")
            }
            else -> {
                Log.w(TAG, "Purchase update error: ${billingResult.debugMessage}")
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    init {
        // Sync cached isPremium from DataStore immediately so UI is instant on launch.
        scope.launch {
            userPreferences.isPremium.collect { cached ->
                _isPremium.value = cached
            }
        }
        connectWithRetry()
    }

    // -----------------------------------------------------------------------------------------
    // Connection
    // -----------------------------------------------------------------------------------------

    private fun connectWithRetry(retryDelayMs: Long = 1_000L) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client connected.")
                    scope.launch {
                        queryProductDetails()
                        verifyPremiumStatus()
                    }
                } else {
                    Log.w(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing service disconnected — retrying in ${retryDelayMs}ms")
                scope.launch {
                    delay(retryDelayMs)
                    connectWithRetry(minOf(retryDelayMs * 2, 30_000L))
                }
            }
        })
    }

    // -----------------------------------------------------------------------------------------
    // Product Details
    // -----------------------------------------------------------------------------------------

    private suspend fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _productDetails.value = result.productDetailsList?.firstOrNull()
            Log.d(TAG, "Product details loaded: ${_productDetails.value?.name}")
        } else {
            Log.w(TAG, "queryProductDetails failed: ${result.billingResult.debugMessage}")
        }
    }

    // -----------------------------------------------------------------------------------------
    // Purchase Verification
    // -----------------------------------------------------------------------------------------

    /** Re-queries active subscriptions and updates isPremium + UserPreferences cache. */
    suspend fun verifyPremiumStatus() {
        if (!billingClient.isReady) {
            Log.d(TAG, "verifyPremiumStatus: billing client not ready, skipping.")
            return
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val result = billingClient.queryPurchasesAsync(params)
        val active = result.purchasesList.any { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                purchase.products.contains(PREMIUM_PRODUCT_ID)
        }
        Log.d(TAG, "verifyPremiumStatus: isPremium=$active")
        _isPremium.value = active
        userPreferences.setIsPremium(active)
    }

    // -----------------------------------------------------------------------------------------
    // Purchase Handling
    // -----------------------------------------------------------------------------------------

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                val result = billingClient.acknowledgePurchase(params)
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Purchase acknowledged.")
                } else {
                    Log.w(TAG, "Acknowledge failed: ${result.debugMessage}")
                }
            }
            // Mark premium — verified on this device.
            _isPremium.value = true
            userPreferences.setIsPremium(true)
            Log.d(TAG, "Premium activated for purchase: ${purchase.orderId}")
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.d(TAG, "Purchase pending — waiting for completion.")
        }
    }

    // -----------------------------------------------------------------------------------------
    // Launch Purchase Flow
    // -----------------------------------------------------------------------------------------

    /**
     * Launches the Google Play subscription purchase flow.
     * Must be called from an Activity context.
     * Returns true if the flow was launched successfully.
     */
    fun launchPurchaseFlow(activity: Activity): Boolean {
        val details = _productDetails.value
        if (details == null) {
            Log.w(TAG, "launchPurchaseFlow: product details not loaded yet.")
            return false
        }
        if (!billingClient.isReady) {
            Log.w(TAG, "launchPurchaseFlow: billing client not ready.")
            return false
        }

        // Build subscription offer params — use the first available offer (3-day trial offer
        // should be the first/only offer on the subscription).
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: run {
                Log.w(TAG, "No subscription offer details available.")
                return false
            }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    // -----------------------------------------------------------------------------------------
    // Restore Purchases
    // -----------------------------------------------------------------------------------------

    /** Restores existing purchases — call from Settings "Restore Purchases" action. */
    suspend fun restorePurchases() {
        verifyPremiumStatus()
    }
}
