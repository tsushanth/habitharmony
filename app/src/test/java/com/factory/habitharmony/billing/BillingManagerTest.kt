package com.factory.habitharmony.billing

import android.app.Activity
import android.content.Context
import app.cash.turbine.test
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BillingManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockContext: Context
    private lateinit var mockBillingClient: BillingClient
    private lateinit var billingManager: BillingManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockContext = mockk(relaxed = true)
        mockBillingClient = mockk(relaxed = true)
        billingManager = BillingManager(mockContext, billingClientOverride = mockBillingClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Initial State Tests ---

    @Test
    fun `initial state has no connection`() {
        assertFalse(billingManager.isConnected.value)
    }

    @Test
    fun `initial state is not loading`() {
        assertFalse(billingManager.isLoading.value)
    }

    @Test
    fun `initial subscription offers are empty`() {
        assertTrue(billingManager.subscriptionOffers.value.isEmpty())
    }

    @Test
    fun `initial iap offers are empty`() {
        assertTrue(billingManager.iapOffers.value.isEmpty())
    }

    // --- Connection Tests ---

    @Test
    fun `startConnection sets loading to true`() {
        billingManager.startConnection()
        assertTrue(billingManager.isLoading.value)
    }

    @Test
    fun `startConnection calls billingClient startConnection`() {
        val listenerSlot = slot<BillingClientStateListener>()
        every { mockBillingClient.startConnection(capture(listenerSlot)) } answers {}

        billingManager.startConnection()

        verify { mockBillingClient.startConnection(any()) }
    }

    @Test
    fun `successful connection sets isConnected to true`() {
        val listenerSlot = slot<BillingClientStateListener>()
        every { mockBillingClient.startConnection(capture(listenerSlot)) } answers {}
        every { mockBillingClient.isReady } returns true

        billingManager.startConnection()
        val okResult = createBillingResult(BillingClient.BillingResponseCode.OK)
        listenerSlot.captured.onBillingSetupFinished(okResult)

        assertTrue(billingManager.isConnected.value)
    }

    @Test
    fun `failed connection sets isConnected to false`() = runTest {
        val listenerSlot = slot<BillingClientStateListener>()
        every { mockBillingClient.startConnection(capture(listenerSlot)) } answers {}

        billingManager.startConnection()
        val errorResult = createBillingResult(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE, "Unavailable")
        listenerSlot.captured.onBillingSetupFinished(errorResult)

        assertFalse(billingManager.isConnected.value)
        assertFalse(billingManager.isLoading.value)
    }

    @Test
    fun `failed connection emits PurchaseError event`() = runTest {
        val listenerSlot = slot<BillingClientStateListener>()
        every { mockBillingClient.startConnection(capture(listenerSlot)) } answers {}

        billingManager.startConnection()

        billingManager.billingEvents.test {
            val errorResult = createBillingResult(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE, "Unavailable")
            listenerSlot.captured.onBillingSetupFinished(errorResult)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is BillingEvent.PurchaseError)
            assertTrue((event as BillingEvent.PurchaseError).message.contains("Billing setup failed"))
        }
    }

    @Test
    fun `service disconnected sets isConnected to false`() {
        val listenerSlot = slot<BillingClientStateListener>()
        every { mockBillingClient.startConnection(capture(listenerSlot)) } answers {}

        billingManager.startConnection()
        val okResult = createBillingResult(BillingClient.BillingResponseCode.OK)
        listenerSlot.captured.onBillingSetupFinished(okResult)
        assertTrue(billingManager.isConnected.value)

        listenerSlot.captured.onBillingServiceDisconnected()
        assertFalse(billingManager.isConnected.value)
    }

    @Test
    fun `successful connection queries product details`() {
        val listenerSlot = slot<BillingClientStateListener>()
        every { mockBillingClient.startConnection(capture(listenerSlot)) } answers {}
        every { mockBillingClient.isReady } returns true

        billingManager.startConnection()
        val okResult = createBillingResult(BillingClient.BillingResponseCode.OK)
        listenerSlot.captured.onBillingSetupFinished(okResult)

        // Should query both subs and IAPs
        verify(exactly = 2) { mockBillingClient.queryProductDetailsAsync(any(), any()) }
    }

    @Test
    fun `successful connection queries existing purchases`() {
        val listenerSlot = slot<BillingClientStateListener>()
        every { mockBillingClient.startConnection(capture(listenerSlot)) } answers {}
        every { mockBillingClient.isReady } returns true

        billingManager.startConnection()
        val okResult = createBillingResult(BillingClient.BillingResponseCode.OK)
        listenerSlot.captured.onBillingSetupFinished(okResult)

        // Should query both SUBS and INAPP purchases
        verify(exactly = 2) { mockBillingClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any()) }
    }

    // --- endConnection Tests ---

    @Test
    fun `endConnection disconnects and sets isConnected false`() {
        billingManager.endConnection()

        verify { mockBillingClient.endConnection() }
        assertFalse(billingManager.isConnected.value)
    }

    // --- Purchase Flow Tests ---

    @Test
    fun `launchSubscriptionFlow emits error when not connected`() = runTest {
        val mockActivity = mockk<Activity>(relaxed = true)
        val mockOffer = createMockSubscriptionOffer()

        billingManager.billingEvents.test {
            billingManager.launchSubscriptionFlow(mockActivity, mockOffer)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is BillingEvent.PurchaseError)
            assertTrue((event as BillingEvent.PurchaseError).message.contains("not connected"))
        }
    }

    @Test
    fun `launchInAppPurchaseFlow emits error when not connected`() = runTest {
        val mockActivity = mockk<Activity>(relaxed = true)
        val mockOffer = createMockInAppOffer()

        billingManager.billingEvents.test {
            billingManager.launchInAppPurchaseFlow(mockActivity, mockOffer)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is BillingEvent.PurchaseError)
            assertTrue((event as BillingEvent.PurchaseError).message.contains("not connected"))
        }
    }

    // --- PurchasesUpdatedListener Tests ---

    @Test
    fun `purchase cancelled emits PurchaseCancelled event`() = runTest {
        billingManager.billingEvents.test {
            val cancelResult = createBillingResult(BillingClient.BillingResponseCode.USER_CANCELED)
            billingManager.purchasesUpdatedListener.onPurchasesUpdated(cancelResult, null)
            advanceUntilIdle()

            val event = awaitItem()
            assertEquals(BillingEvent.PurchaseCancelled, event)
        }
    }

    @Test
    fun `purchase error emits PurchaseError event`() = runTest {
        billingManager.billingEvents.test {
            val errorResult = createBillingResult(BillingClient.BillingResponseCode.ERROR, "Network error")
            billingManager.purchasesUpdatedListener.onPurchasesUpdated(errorResult, null)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is BillingEvent.PurchaseError)
            assertTrue((event as BillingEvent.PurchaseError).message.contains("Network error"))
        }
    }

    @Test
    fun `item already owned triggers re-query of purchases`() {
        every { mockBillingClient.isReady } returns true

        val alreadyOwnedResult = createBillingResult(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED)
        billingManager.purchasesUpdatedListener.onPurchasesUpdated(alreadyOwnedResult, null)

        verify { mockBillingClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any()) }
    }

    @Test
    fun `purchased item with PENDING state emits PurchasePending`() = runTest {
        val mockPurchase = mockk<Purchase>(relaxed = true)
        every { mockPurchase.purchaseState } returns Purchase.PurchaseState.PENDING

        val okResult = createBillingResult(BillingClient.BillingResponseCode.OK)

        billingManager.billingEvents.test {
            billingManager.purchasesUpdatedListener.onPurchasesUpdated(okResult, listOf(mockPurchase))
            advanceUntilIdle()

            assertEquals(BillingEvent.PurchasePending, awaitItem())
        }
    }

    @Test
    fun `acknowledged purchase emits PurchaseComplete for each product`() = runTest {
        val mockPurchase = mockk<Purchase>(relaxed = true)
        every { mockPurchase.purchaseState } returns Purchase.PurchaseState.PURCHASED
        every { mockPurchase.isAcknowledged } returns true
        every { mockPurchase.products } returns listOf(BillingManager.SUB_MONTHLY)

        val okResult = createBillingResult(BillingClient.BillingResponseCode.OK)

        billingManager.billingEvents.test {
            billingManager.purchasesUpdatedListener.onPurchasesUpdated(okResult, listOf(mockPurchase))
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is BillingEvent.PurchaseComplete)
            assertEquals(BillingManager.SUB_MONTHLY, (event as BillingEvent.PurchaseComplete).productId)
        }
    }

    // --- Restore Purchases Tests ---

    @Test
    fun `restorePurchases emits error when not connected`() = runTest {
        billingManager.billingEvents.test {
            billingManager.restorePurchases()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is BillingEvent.PurchaseError)
            assertTrue((event as BillingEvent.PurchaseError).message.contains("Not connected"))
        }
    }

    @Test
    fun `queryExistingPurchases does nothing when client not ready`() {
        every { mockBillingClient.isReady } returns false

        billingManager.queryExistingPurchases()

        verify(exactly = 0) { mockBillingClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any()) }
    }

    @Test
    fun `queryExistingPurchases queries subs and inapp when ready`() {
        every { mockBillingClient.isReady } returns true

        billingManager.queryExistingPurchases()

        verify(exactly = 2) { mockBillingClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any()) }
    }

    // --- Companion Object Tests ---

    @Test
    fun `subscription IDs contains recurring subscription products`() {
        assertEquals(3, BillingManager.SUBSCRIPTION_IDS.size)
        assertTrue(BillingManager.SUBSCRIPTION_IDS.contains(BillingManager.SUB_WEEKLY))
        assertTrue(BillingManager.SUBSCRIPTION_IDS.contains(BillingManager.SUB_MONTHLY))
        assertTrue(BillingManager.SUBSCRIPTION_IDS.contains(BillingManager.SUB_YEARLY))
    }

    @Test
    fun `lifetime IDs contains the lifetime product`() {
        assertEquals(1, BillingManager.LIFETIME_IDS.size)
        assertTrue(BillingManager.LIFETIME_IDS.contains(BillingManager.SUB_LIFETIME))
    }

    @Test
    fun `IAP IDs contains small IAP`() {
        assertEquals(1, BillingManager.IAP_IDS.size)
        assertTrue(BillingManager.IAP_IDS.contains(BillingManager.IAP_SMALL))
    }

    // --- Helper Functions ---

    private fun createBillingResult(responseCode: Int, debugMessage: String = ""): BillingResult {
        val result = mockk<BillingResult>()
        every { result.responseCode } returns responseCode
        every { result.debugMessage } returns debugMessage
        return result
    }

    private fun createMockSubscriptionOffer(): SubscriptionOffer {
        val mockProductDetails = mockk<ProductDetails>(relaxed = true)
        return SubscriptionOffer(
            productId = BillingManager.SUB_MONTHLY,
            name = "Monthly",
            price = "$11.99",
            priceMicros = 11990000L,
            billingPeriod = "P1M",
            productDetails = mockProductDetails,
            offerToken = "test-offer-token"
        )
    }

    private fun createMockInAppOffer(): InAppProductOffer {
        val mockProductDetails = mockk<ProductDetails>(relaxed = true)
        return InAppProductOffer(
            productId = BillingManager.IAP_SMALL,
            name = "Tip Jar",
            price = "$2.24",
            priceMicros = 2240000L,
            productDetails = mockProductDetails
        )
    }
}
