package com.factory.habitharmony.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumStateTest {

    @Test
    fun `default state has all flags false and null subscription`() {
        val state = PremiumState()

        assertFalse(state.isPremium)
        assertFalse(state.hasActiveSubscription)
        assertNull(state.activeSubscriptionId)
        assertFalse(state.hasLifetime)
        assertFalse(state.hasSmallIap)
    }

    @Test
    fun `copy preserves unchanged fields`() {
        val state = PremiumState(
            isPremium = true,
            hasActiveSubscription = true,
            activeSubscriptionId = "sub_monthly",
            hasLifetime = false,
            hasSmallIap = true
        )

        val copied = state.copy(hasLifetime = true)

        assertTrue(copied.isPremium)
        assertTrue(copied.hasActiveSubscription)
        assertEquals("sub_monthly", copied.activeSubscriptionId)
        assertTrue(copied.hasLifetime)
        assertTrue(copied.hasSmallIap)
    }

    @Test
    fun `equality works correctly for same values`() {
        val state1 = PremiumState(isPremium = true, hasLifetime = true)
        val state2 = PremiumState(isPremium = true, hasLifetime = true)

        assertEquals(state1, state2)
    }

    @Test
    fun `states with different values are not equal`() {
        val state1 = PremiumState(isPremium = true)
        val state2 = PremiumState(isPremium = false)

        assertFalse(state1 == state2)
    }

    @Test
    fun `premium with active subscription`() {
        val state = PremiumState(
            isPremium = true,
            hasActiveSubscription = true,
            activeSubscriptionId = BillingManager.SUB_YEARLY
        )

        assertTrue(state.isPremium)
        assertTrue(state.hasActiveSubscription)
        assertEquals(BillingManager.SUB_YEARLY, state.activeSubscriptionId)
    }

    @Test
    fun `lifetime premium state`() {
        val state = PremiumState(
            isPremium = true,
            hasActiveSubscription = true,
            activeSubscriptionId = BillingManager.SUB_LIFETIME,
            hasLifetime = true
        )

        assertTrue(state.isPremium)
        assertTrue(state.hasLifetime)
    }

    @Test
    fun `iap only state is not premium`() {
        val state = PremiumState(hasSmallIap = true)

        assertFalse(state.isPremium)
        assertTrue(state.hasSmallIap)
    }
}
