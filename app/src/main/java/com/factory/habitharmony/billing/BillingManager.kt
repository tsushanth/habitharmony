package com.factory.habitharmony.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class BillingEvent {
    data class PurchaseComplete(val productId: String) : BillingEvent()
    data class PurchaseError(val message: String) : BillingEvent()
    data object PurchaseCancelled : BillingEvent()
    data object PurchasePending : BillingEvent()
    /** Emitted after the subscription purchase query completes. Empty list means no active subs. */
    data object NoActiveSubscriptions : BillingEvent()
}

data class SubscriptionOffer(
    val productId: String,
    val name: String,
    val price: String,
    val priceMicros: Long,
    val billingPeriod: String,
    val productDetails: ProductDetails,
    val offerToken: String
)

data class InAppProductOffer(
    val productId: String,
    val name: String,
    val price: String,
    val priceMicros: Long,
    val productDetails: ProductDetails
)

class BillingManager(
    private val context: Context,
    billingClientOverride: BillingClient? = null
) {

    companion object {
        private const val TAG = "BillingManager"

        // Subscription product IDs
        const val SUB_WEEKLY = "com.factory.habitharmony.subscription.weekly"
        const val SUB_MONTHLY = "com.factory.habitharmony.subscription.monthly"
        const val SUB_YEARLY = "com.factory.habitharmony.subscription.yearly"
        const val SUB_LIFETIME = "com.factory.habitharmony.subscription.lifetime"

        // In-app purchase product IDs
        const val IAP_SMALL = "com.factory.habitharmony.small_iap"

        // Weekly/monthly/yearly are recurring SUBS; lifetime is a one-time INAPP purchase
        val SUBSCRIPTION_IDS = listOf(SUB_WEEKLY, SUB_MONTHLY, SUB_YEARLY)
        val LIFETIME_IDS = listOf(SUB_LIFETIME)
        val IAP_IDS = listOf(IAP_SMALL)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _billingEvents = MutableSharedFlow<BillingEvent>(extraBufferCapacity = 10)
    val billingEvents: SharedFlow<BillingEvent> = _billingEvents.asSharedFlow()

    private val _subscriptionOffers = MutableStateFlow<List<SubscriptionOffer>>(emptyList())
    val subscriptionOffers: StateFlow<List<SubscriptionOffer>> = _subscriptionOffers.asStateFlow()

    private val _iapOffers = MutableStateFlow<List<InAppProductOffer>>(emptyList())
    val iapOffers: StateFlow<List<InAppProductOffer>> = _iapOffers.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    internal val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                scope.launch { _billingEvents.emit(BillingEvent.PurchaseCancelled) }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // Re-query to acknowledge any unacknowledged purchases
                queryExistingPurchases()
            }
            else -> {
                scope.launch {
                    _billingEvents.emit(
                        BillingEvent.PurchaseError(
                            "Purchase failed: ${billingResult.debugMessage} (code: ${billingResult.responseCode})"
                        )
                    )
                }
            }
        }
    }

    private val billingClient: BillingClient = billingClientOverride
        ?: BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()

    fun startConnection() {
        _isLoading.value = true
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client connected")
                    _isConnected.value = true
                    queryProductDetails()
                    queryExistingPurchases()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                    _isConnected.value = false
                    _isLoading.value = false
                    scope.launch {
                        _billingEvents.emit(
                            BillingEvent.PurchaseError("Billing setup failed: ${billingResult.debugMessage}")
                        )
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
                _isConnected.value = false
            }
        })
    }

    fun endConnection() {
        billingClient.endConnection()
        _isConnected.value = false
    }

    private fun queryProductDetails() {
        querySubscriptionDetails()
        queryInAppDetails()
    }

    private fun querySubscriptionDetails() {
        val productList = SUBSCRIPTION_IDS.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val offers = productDetailsList.mapNotNull { productDetails ->
                    val offerDetails = productDetails.subscriptionOfferDetails?.firstOrNull()
                    val pricingPhase = offerDetails?.pricingPhases?.pricingPhaseList?.firstOrNull()

                    if (offerDetails != null && pricingPhase != null) {
                        SubscriptionOffer(
                            productId = productDetails.productId,
                            name = productDetails.name,
                            price = pricingPhase.formattedPrice,
                            priceMicros = pricingPhase.priceAmountMicros,
                            billingPeriod = pricingPhase.billingPeriod,
                            productDetails = productDetails,
                            offerToken = offerDetails.offerToken
                        )
                    } else null
                }.sortedBy { it.priceMicros }

                _subscriptionOffers.value = offers
                Log.d(TAG, "Loaded ${offers.size} subscription offers")
            } else {
                Log.e(TAG, "Failed to query subscriptions: ${billingResult.debugMessage}")
            }
            _isLoading.value = false
        }
    }

    private fun queryInAppDetails() {
        // Query both consumable IAPs and the lifetime one-time purchase (also INAPP type)
        val productList = (IAP_IDS + LIFETIME_IDS).map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val offers = productDetailsList.mapNotNull { productDetails ->
                    val oneTimePurchaseOfferDetails = productDetails.oneTimePurchaseOfferDetails
                    if (oneTimePurchaseOfferDetails != null) {
                        InAppProductOffer(
                            productId = productDetails.productId,
                            name = productDetails.name,
                            price = oneTimePurchaseOfferDetails.formattedPrice,
                            priceMicros = oneTimePurchaseOfferDetails.priceAmountMicros,
                            productDetails = productDetails
                        )
                    } else null
                }.sortedByDescending { it.priceMicros } // lifetime first, then tip jar
                _iapOffers.value = offers
                Log.d(TAG, "Loaded ${offers.size} IAP/lifetime offers")
            } else {
                Log.e(TAG, "Failed to query IAPs: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchSubscriptionFlow(activity: Activity, offer: SubscriptionOffer) {
        if (!_isConnected.value) {
            scope.launch {
                _billingEvents.emit(BillingEvent.PurchaseError("Billing not connected. Please try again."))
            }
            return
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(offer.productDetails)
            .setOfferToken(offer.offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            scope.launch {
                _billingEvents.emit(BillingEvent.PurchaseError("Failed to launch purchase: ${result.debugMessage}"))
            }
        }
    }

    fun launchInAppPurchaseFlow(activity: Activity, offer: InAppProductOffer) {
        if (!_isConnected.value) {
            scope.launch {
                _billingEvents.emit(BillingEvent.PurchaseError("Billing not connected. Please try again."))
            }
            return
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(offer.productDetails)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            scope.launch {
                _billingEvents.emit(BillingEvent.PurchaseError("Failed to launch purchase: ${result.debugMessage}"))
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                } else {
                    purchase.products.forEach { productId ->
                        scope.launch {
                            _billingEvents.emit(BillingEvent.PurchaseComplete(productId))
                        }
                    }
                }
            }
            Purchase.PurchaseState.PENDING -> {
                scope.launch { _billingEvents.emit(BillingEvent.PurchasePending) }
            }
            else -> {
                Log.w(TAG, "Unhandled purchase state: ${purchase.purchaseState}")
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged: ${purchase.products}")
                purchase.products.forEach { productId ->
                    scope.launch {
                        _billingEvents.emit(BillingEvent.PurchaseComplete(productId))
                    }
                }
            } else {
                Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
                scope.launch {
                    _billingEvents.emit(
                        BillingEvent.PurchaseError("Failed to verify purchase. Please contact support.")
                    )
                }
            }
        }
    }

    fun queryExistingPurchases() {
        if (!billingClient.isReady) return

        // Query subscriptions
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { purchase -> handlePurchase(purchase) }
                if (purchases.isEmpty()) {
                    Log.d(TAG, "No active subscriptions found")
                    scope.launch { _billingEvents.emit(BillingEvent.NoActiveSubscriptions) }
                }
            }
        }

        // Query in-app purchases
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { purchase -> handlePurchase(purchase) }
            }
        }
    }

    fun restorePurchases() {
        if (!_isConnected.value) {
            scope.launch {
                _billingEvents.emit(BillingEvent.PurchaseError("Not connected to billing service. Please check your internet connection."))
            }
            return
        }
        queryExistingPurchases()
    }
}
