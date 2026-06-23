package com.factory.habitharmony.billing

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PremiumState(
    val isPremium: Boolean = false,
    val hasActiveSubscription: Boolean = false,
    val activeSubscriptionId: String? = null,
    val hasLifetime: Boolean = false,
    val hasSmallIap: Boolean = false
)

class PremiumManager(
    context: Context,
    private val billingManager: BillingManager
) {
    companion object {
        private const val TAG = "PremiumManager"
        private const val PREFS_NAME = "premium_prefs"
        private const val KEY_IS_PREMIUM = "is_premium"
        private const val KEY_ACTIVE_SUB_ID = "active_sub_id"
        private const val KEY_HAS_LIFETIME = "has_lifetime"
        private const val KEY_HAS_SMALL_IAP = "has_small_iap"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _premiumState = MutableStateFlow(loadState())
    val premiumState: StateFlow<PremiumState> = _premiumState.asStateFlow()

    val isPremium: Boolean get() = _premiumState.value.isPremium

    init {
        observeBillingEvents()
    }

    private fun loadState(): PremiumState {
        val isPremium = prefs.getBoolean(KEY_IS_PREMIUM, false)
        val activeSubId = prefs.getString(KEY_ACTIVE_SUB_ID, null)
        val hasLifetime = prefs.getBoolean(KEY_HAS_LIFETIME, false)
        val hasSmallIap = prefs.getBoolean(KEY_HAS_SMALL_IAP, false)

        return PremiumState(
            isPremium = isPremium,
            hasActiveSubscription = activeSubId != null,
            activeSubscriptionId = activeSubId,
            hasLifetime = hasLifetime,
            hasSmallIap = hasSmallIap
        )
    }

    private fun saveState(state: PremiumState) {
        prefs.edit()
            .putBoolean(KEY_IS_PREMIUM, state.isPremium)
            .putString(KEY_ACTIVE_SUB_ID, state.activeSubscriptionId)
            .putBoolean(KEY_HAS_LIFETIME, state.hasLifetime)
            .putBoolean(KEY_HAS_SMALL_IAP, state.hasSmallIap)
            .apply()
    }

    private fun observeBillingEvents() {
        scope.launch {
            billingManager.billingEvents.collect { event ->
                when (event) {
                    is BillingEvent.PurchaseComplete -> handlePurchaseComplete(event.productId)
                    is BillingEvent.PurchaseError -> Log.e(TAG, "Purchase error: ${event.message}")
                    is BillingEvent.PurchaseCancelled -> Log.d(TAG, "Purchase cancelled")
                    is BillingEvent.PurchasePending -> Log.d(TAG, "Purchase pending")
                    is BillingEvent.NoActiveSubscriptions -> {
                        // Subscription query returned nothing — revoke subscription-based premium
                        // if the user had one, but only if they don't have a lifetime purchase.
                        val currentState = _premiumState.value
                        if (currentState.hasActiveSubscription && !currentState.hasLifetime) {
                            Log.d(TAG, "No active subscriptions detected — revoking subscription premium")
                            handleSubscriptionExpired()
                        }
                    }
                }
            }
        }
    }

    private fun handlePurchaseComplete(productId: String) {
        Log.d(TAG, "Purchase complete: $productId")

        val currentState = _premiumState.value

        val newState = when (productId) {
            BillingManager.SUB_WEEKLY,
            BillingManager.SUB_MONTHLY,
            BillingManager.SUB_YEARLY -> currentState.copy(
                isPremium = true,
                hasActiveSubscription = true,
                activeSubscriptionId = productId
            )
            BillingManager.SUB_LIFETIME -> currentState.copy(
                isPremium = true,
                hasActiveSubscription = true,
                activeSubscriptionId = productId,
                hasLifetime = true
            )
            BillingManager.IAP_SMALL -> currentState.copy(
                hasSmallIap = true
            )
            else -> {
                Log.w(TAG, "Unknown product: $productId")
                currentState
            }
        }

        _premiumState.value = newState
        saveState(newState)
    }

    fun handleSubscriptionExpired() {
        val currentState = _premiumState.value
        // Lifetime purchases never expire
        if (currentState.hasLifetime) return

        val newState = currentState.copy(
            isPremium = false,
            hasActiveSubscription = false,
            activeSubscriptionId = null
        )
        _premiumState.value = newState
        saveState(newState)
        Log.d(TAG, "Subscription expired, premium deactivated")
    }

    fun refreshPurchases() {
        billingManager.restorePurchases()
    }
}
