package com.factory.habitharmony.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingEventTest {

    @Test
    fun `PurchaseComplete holds correct product ID`() {
        val event = BillingEvent.PurchaseComplete("test.product.id")
        assertEquals("test.product.id", event.productId)
    }

    @Test
    fun `PurchaseError holds correct error message`() {
        val event = BillingEvent.PurchaseError("Something went wrong")
        assertEquals("Something went wrong", event.message)
    }

    @Test
    fun `PurchaseCancelled is a singleton`() {
        val event1 = BillingEvent.PurchaseCancelled
        val event2 = BillingEvent.PurchaseCancelled
        assertTrue(event1 === event2)
    }

    @Test
    fun `PurchasePending is a singleton`() {
        val event1 = BillingEvent.PurchasePending
        val event2 = BillingEvent.PurchasePending
        assertTrue(event1 === event2)
    }

    @Test
    fun `PurchaseComplete equality by product ID`() {
        val event1 = BillingEvent.PurchaseComplete("product.a")
        val event2 = BillingEvent.PurchaseComplete("product.a")
        val event3 = BillingEvent.PurchaseComplete("product.b")

        assertEquals(event1, event2)
        assertFalse(event1 == event3)
    }

    @Test
    fun `PurchaseError equality by message`() {
        val event1 = BillingEvent.PurchaseError("error")
        val event2 = BillingEvent.PurchaseError("error")
        val event3 = BillingEvent.PurchaseError("different error")

        assertEquals(event1, event2)
        assertFalse(event1 == event3)
    }

    @Test
    fun `all BillingEvent types are instances of BillingEvent`() {
        val events: List<BillingEvent> = listOf(
            BillingEvent.PurchaseComplete("id"),
            BillingEvent.PurchaseError("msg"),
            BillingEvent.PurchaseCancelled,
            BillingEvent.PurchasePending
        )

        events.forEach { event ->
            assertTrue(event is BillingEvent)
        }
    }

    @Test
    fun `different event types are not equal`() {
        val complete = BillingEvent.PurchaseComplete("id")
        val error = BillingEvent.PurchaseError("id")
        val cancelled = BillingEvent.PurchaseCancelled
        val pending = BillingEvent.PurchasePending

        assertFalse(complete == error as Any)
        assertFalse(cancelled == pending as Any)
    }
}
