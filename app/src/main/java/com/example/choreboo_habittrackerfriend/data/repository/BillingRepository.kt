package com.example.choreboo_habittrackerfriend.data.repository

import android.app.Activity
import android.content.Context
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Subscription product ID registered in the Google Play Console. */
const val PREMIUM_PRODUCT_ID = "choreboo_premium_monthly"

/** Maximum number of reconnection attempts after billing service disconnects. */
private const val MAX_BILLING_RETRY_ATTEMPTS = 3

/** Maximum backoff delay between reconnection attempts. */
private const val MAX_BILLING_RETRY_DELAY_MS = 30_000L

@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Exposed to UI — true when an active, acknowledged premium subscription is detected.
    // Defaults to false; updated after BillingClient.queryPurchasesAsync confirms status.
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
                Timber.d("User cancelled the purchase flow.")
            }
            else -> {
                Timber.w("Purchase update error: ${billingResult.debugMessage}")
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    init {
        connectWithRetry()
    }

    // -----------------------------------------------------------------------------------------
    // Connection
    // -----------------------------------------------------------------------------------------

    private fun connectWithRetry(retryDelayMs: Long = 1_000L, attempt: Int = 0) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Timber.d("Billing client connected.")
                    scope.launch {
                        queryProductDetails()
                        verifyPremiumStatus()
                    }
                } else {
                    Timber.w("Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                if (attempt >= MAX_BILLING_RETRY_ATTEMPTS) {
                    Timber.w("Billing service disconnected — max retries ($MAX_BILLING_RETRY_ATTEMPTS) reached, giving up.")
                    return
                }
                Timber.d("Billing service disconnected — retrying in ${retryDelayMs}ms (attempt ${attempt + 1}/$MAX_BILLING_RETRY_ATTEMPTS)")
                scope.launch {
                    delay(retryDelayMs)
                    connectWithRetry(
                        retryDelayMs = minOf(retryDelayMs * 2, MAX_BILLING_RETRY_DELAY_MS),
                        attempt = attempt + 1,
                    )
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
            Timber.d("Product details loaded: ${_productDetails.value?.name}")
        } else {
            Timber.w("queryProductDetails failed: ${result.billingResult.debugMessage}")
        }
    }

    // -----------------------------------------------------------------------------------------
    // Purchase Verification
    // -----------------------------------------------------------------------------------------

    /** Re-queries active subscriptions and updates isPremium. */
    suspend fun verifyPremiumStatus() {
        if (!billingClient.isReady) {
            Timber.d("verifyPremiumStatus: billing client not ready, skipping.")
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
        Timber.d("verifyPremiumStatus: isPremium=$active")
        _isPremium.value = active
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
                    Timber.d("Purchase acknowledged.")
                } else {
                    Timber.w("Acknowledge failed: ${result.debugMessage}")
                }
            }
            // Mark premium — verified on this device.
            _isPremium.value = true
            Timber.d("Premium activated for purchase: ${purchase.orderId}")
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Timber.d("Purchase pending — waiting for completion.")
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
            Timber.w("launchPurchaseFlow: product details not loaded yet.")
            return false
        }
        if (!billingClient.isReady) {
            Timber.w("launchPurchaseFlow: billing client not ready.")
            return false
        }

        // Build subscription offer params — use the first available offer (3-day trial offer
        // should be the first/only offer on the subscription).
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: run {
                Timber.w("No subscription offer details available.")
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

    /**
     * Releases the BillingClient connection. Should be called only at process shutdown
     * (e.g., from [android.app.Application.onTerminate]).
     *
     * **Not called from any ViewModel** — this repository is `@Singleton`-scoped and outlives
     * all ViewModels. Calling `release()` from a ViewModel's `onCleared()` would terminate
     * the connection whenever the user navigates away from that screen, breaking billing for
     * the rest of the session. The Play Billing Library recommends maintaining the connection
     * for the process lifetime.
     */
    fun release() {
        if (billingClient.isReady) {
            billingClient.endConnection()
            Timber.d("BillingRepository: billing client connection ended.")
        }
    }
}
