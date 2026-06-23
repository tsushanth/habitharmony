package com.factory.habitharmony.billing

import android.content.Context
import android.content.SharedPreferences
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PremiumManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var mockBillingManager: BillingManager
    private lateinit var billingEventsFlow: MutableSharedFlow<BillingEvent>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockEditor = mockk(relaxed = true) {
            every { putBoolean(any(), any()) } returns this@mockk
            every { putString(any(), any()) } returns this@mockk
            every { apply() } returns Unit
        }

        mockPrefs = mockk(relaxed = true) {
            every { getBoolean(any(), any()) } returns false
            every { getString(any(), any()) } returns null
            every { edit() } returns mockEditor
        }

        mockContext = mockk(relaxed = true) {
            every { getSharedPreferences("premium_prefs", Context.MODE_PRIVATE) } returns mockPrefs
        }

        billingEventsFlow = MutableSharedFlow(extraBufferCapacity = 10)
        mockBillingManager = mockk(relaxed = true) {
            every { billingEvents } returns billingEventsFlow
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Initial State Tests ---

    @Test
    fun `initial state loads from SharedPreferences with defaults`() {
        val premiumManager = PremiumManager(mockContext, mockBillingManager)

        val state = premiumManager.premiumState.value
        assertFalse(state.isPremium)
        assertFalse(state.hasActiveSubscription)
        assertNull(state.activeSubscriptionId)
        assertFalse(state.hasLifetime)
        assertFalse(state.hasSmallIap)
    }

    @Test
    fun `initial state loads persisted premium status`() {
        every { mockPrefs.getBoolean("is_premium", false) } returns true
        every { mockPrefs.getString("active_sub_id", null) } returns BillingManager.SUB_YEARLY
        every { mockPrefs.getBoolean("has_lifetime", false) } returns false
        every { mockPrefs.getBoolean("has_small_iap", false) } returns true

        val premiumManager = PremiumManager(mockContext, mockBillingManager)

        val state = premiumManager.premiumState.value
        assertTrue(state.isPremium)
        assertTrue(state.hasActiveSubscription)
        assertEquals(BillingManager.SUB_YEARLY, state.activeSubscriptionId)
        assertFalse(state.hasLifetime)
        assertTrue(state.hasSmallIap)
    }

    @Test
    fun `isPremium getter reflects current state`() {
        every { mockPrefs.getBoolean("is_premium", false) } returns true
        every { mockPrefs.getString("active_sub_id", null) } returns BillingManager.SUB_MONTHLY

        val premiumManager = PremiumManager(mockContext, mockBillingManager)
        assertTrue(premiumManager.isPremium)
    }

    @Test
    fun `isPremium is false by default`() {
        val premiumManager = PremiumManager(mockContext, mockBillingManager)
        assertFalse(premiumManager.isPremium)
    }

    // --- Purchase Complete Handling Tests ---

    @Test
    fun `weekly subscription purchase sets premium and active subscription`() = runTest {
        val premiumManager = PremiumManager(mockContext, mockBillingManager)

        billingEventsFlow.emit(BillingEvent.PurchaseComplete(BillingManager.SUB_WEEKLY))

        val state = premiumManager.premiumState.value
        assertTrue(state.isPremium)
        assertTrue(state.hasActiveSubscription)
        assertEquals(BillingManager.SUB_WEEKLY, state.activeSubscriptionId)
    }

    @Test
    fun `monthly subscription purchase sets premium`() = runTest {
        val premiumManager = PremiumManager(mockContext, mockBillingManager)

        billingEventsFlow.emit(BillingEvent.PurchaseComplete(BillingManager.SUB_MONTHLY))

        val state = premiumManager.premiumState.value
        assertTrue(state.isPremium)
        assertEquals(BillingManager.SUB_MONTHLY, state.activeSubscriptionId)
    }

    @Test
    fun `yearly subscription purchase sets premium`() = runTest {
        val premiumManager = PremiumManager(mockContext, mockBillingManager)

        billingEventsFlow.emit(BillingEvent.PurchaseComplete(BillingManager.SUB_YEARLY))

        val state = premiumManager.premiumState.value
        assertTrue(state.isPremium)
        assertEquals(BillingManager.SUB_YEARLY, state.activeSubscriptionId)
    }

    @Test
    fun `lifetime subscription sets premium and hasLifetime`() = runTest {
        val premiumManager = PremiumManager(mockContext, mockBillingManager)

        billingEventsFlow.emit(BillingEvent.PurchaseComplete(BillingManager.SUB_LIFETIME))

        val state = premiumManager.premiumState.value
        assertTrue(state.isPremium)
        assertTrue(state.hasLifetime)
        assertTrue(state.hasActiveSubscription)
        assertEquals(BillingManager.SUB_LIFETIME, state.activeSubscriptionId)
    }

    @Test
    fun `small IAP sets hasSmallIap but not premium`() = runTest {
        val premiumManager = PremiumManager(mockContext, mockBillingManager)

        billingEventsFlow.emit(BillingEvent.PurchaseComplete(BillingManager.IAP_SMALL))

        val state = premiumManager.premiumState.value
        assertFalse(state.isPremium)
        assertTrue(state.hasSmallIap)
    }

    @Test
    fun `unknown product ID does not change state`() = runTest {
        val premiumManager = PremiumManager(mockContext, mockBillingManager)
        val initialState = premiumManager.premiumState.value

        billingEventsFlow.emit(BillingEvent.PurchaseComplete("com.unknown.product"))

        assertEquals(initialState, premiumManager.premiumState.value)
    }

    // --- State Persistence Tests ---

    @Test
    fun `purchase saves state to SharedPreferences`() = runTest {
        val premiumManager = PremiumManager(mockContext, mockBillingManager)

        billingEventsFlow.emit(BillingEvent.PurchaseComplete(BillingManager.SUB_MONTHLY))

        verify { mockEditor.putBoolean("is_premium", true) }
        verify { mockEditor.putString("active_sub_id", BillingManager.SUB_MONTHLY) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `lifetime purchase saves hasLifetime to SharedPreferences`() = runTest {
        val premiumManager = PremiumManager(mockContext, mockBillingManager)

        billingEventsFlow.emit(BillingEvent.PurchaseComplete(BillingManager.SUB_LIFETIME))

        verify { mockEditor.putBoolean("has_lifetime", true) }
        verify { mockEditor.putBoolean("is_premium", true) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `small IAP purchase saves hasSmallIap to SharedPreferences`() = runTest {
        val premiumManager = PremiumManager(mockContext, mockBillingManager)

        billingEventsFlow.emit(BillingEvent.PurchaseComplete(BillingManager.IAP_SMALL))

        verify { mockEditor.putBoolean("has_small_iap", true) }
        verify { mockEditor.apply() }
    }

    // --- Subscription Expiry Tests ---

    @Test
    fun `subscription expired revokes premium status`() = runTest {
        every { mockPrefs.getBoolean("is_premium", false) } returns true
        every { mockPrefs.getString("active_sub_id", null) } returns BillingManager.SUB_MONTHLY

        val premiumManager = PremiumManager(mockContext, mockBillingManager)
        assertTrue(premiumManager.premiumState.value.isPremium)

        premiumManager.handleSubscriptionExpired()

        val state = premiumManager.premiumState.value
        assertFalse(state.isPremium)
        assertFalse(state.hasActiveSubscription)
        assertNull(state.activeSubscriptionId)
    }

    @Test
    fun `subscription expired does not affect lifetime users`() {
        every { mockPrefs.getBoolean("is_premium", false) } returns true
        every { mockPrefs.getString("active_sub_id", null) } returns BillingManager.SUB_LIFETIME
        every { mockPrefs.getBoolean("has_lifetime", false) } returns true

        val premiumManager = PremiumManager(mockContext, mockBillingManager)

        premiumManager.handleSubscriptionExpired()

        assertTrue(premiumManager.premiumState.value.isPremium)
        assertTrue(premiumManager.premiumState.value.hasLifetime)
    }

    @Test
    fun `subscription expired saves updated state`() = runTest {
        every { mockPrefs.getBoolean("is_premium", false) } returns true
        every { mockPrefs.getString("active_sub_id", null) } returns BillingManager.SUB_MONTHLY

        val premiumManager = PremiumManager(mockContext, mockBillingManager)

        premiumManager.handleSubscriptionExpired()

        verify { mockEditor.putBoolean("is_premium", false) }
        verify { mockEditor.putString("active_sub_id", null) }
        verify { mockEditor.apply() }
    }

    // --- Refresh Purchases Tests ---

    @Test
    fun `refreshPurchases delegates to billingManager restorePurchases`() {
        val premiumManager = PremiumManager(mockContext, mockBillingManager)

        premiumManager.refreshPurchases()

        verify { mockBillingManager.restorePurchases() }
    }

    // --- Billing Event Observation Tests ---

    @Test
    fun `PurchaseError event does not change premium state`() = runTest {
        val premiumManager = PremiumManager(mockContext, mockBillingManager)
        val initialState = premiumManager.premiumState.value

        billingEventsFlow.emit(BillingEvent.PurchaseError("Some error"))

        assertEquals(initialState, premiumManager.premiumState.value)
    }

    @Test
    fun `PurchaseCancelled event does not change premium state`() = runTest {
        val premiumManager = PremiumManager(mockContext, mockBillingManager)
        val initialState = premiumManager.premiumState.value

        billingEventsFlow.emit(BillingEvent.PurchaseCancelled)

        assertEquals(initialState, premiumManager.premiumState.value)
    }

    @Test
    fun `PurchasePending event does not change premium state`() = runTest {
        val premiumManager = PremiumManager(mockContext, mockBillingManager)
        val initialState = premiumManager.premiumState.value

        billingEventsFlow.emit(BillingEvent.PurchasePending)

        assertEquals(initialState, premiumManager.premiumState.value)
    }

    // --- Multiple Purchases Tests ---

    @Test
    fun `upgrading subscription updates active subscription ID`() = runTest {
        val premiumManager = PremiumManager(mockContext, mockBillingManager)

        billingEventsFlow.emit(BillingEvent.PurchaseComplete(BillingManager.SUB_MONTHLY))
        assertEquals(BillingManager.SUB_MONTHLY, premiumManager.premiumState.value.activeSubscriptionId)

        billingEventsFlow.emit(BillingEvent.PurchaseComplete(BillingManager.SUB_YEARLY))
        assertEquals(BillingManager.SUB_YEARLY, premiumManager.premiumState.value.activeSubscriptionId)
        assertTrue(premiumManager.premiumState.value.isPremium)
    }

    @Test
    fun `subscription plus IAP both tracked independently`() = runTest {
        val premiumManager = PremiumManager(mockContext, mockBillingManager)

        billingEventsFlow.emit(BillingEvent.PurchaseComplete(BillingManager.SUB_MONTHLY))
        billingEventsFlow.emit(BillingEvent.PurchaseComplete(BillingManager.IAP_SMALL))

        val state = premiumManager.premiumState.value
        assertTrue(state.isPremium)
        assertTrue(state.hasSmallIap)
        assertEquals(BillingManager.SUB_MONTHLY, state.activeSubscriptionId)
    }

    // --- StateFlow Emission Tests with Turbine ---

    @Test
    fun `premiumState emits updates when purchase completes`() = runTest {
        val premiumManager = PremiumManager(mockContext, mockBillingManager)

        premiumManager.premiumState.test {
            val initial = awaitItem()
            assertFalse(initial.isPremium)

            billingEventsFlow.emit(BillingEvent.PurchaseComplete(BillingManager.SUB_MONTHLY))

            val updated = awaitItem()
            assertTrue(updated.isPremium)
            assertEquals(BillingManager.SUB_MONTHLY, updated.activeSubscriptionId)
        }
    }

    @Test
    fun `premiumState emits update when subscription expires`() = runTest {
        every { mockPrefs.getBoolean("is_premium", false) } returns true
        every { mockPrefs.getString("active_sub_id", null) } returns BillingManager.SUB_MONTHLY

        val premiumManager = PremiumManager(mockContext, mockBillingManager)

        premiumManager.premiumState.test {
            val initial = awaitItem()
            assertTrue(initial.isPremium)

            premiumManager.handleSubscriptionExpired()

            val expired = awaitItem()
            assertFalse(expired.isPremium)
        }
    }
}
