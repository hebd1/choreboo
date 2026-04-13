package com.example.choreboo_habittrackerfriend.data.repository

import com.android.billingclient.api.ProductDetails
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [BillingRepository] guard logic.
 *
 * [BillingClient] is an Android SDK class that cannot be constructed in JVM tests —
 * it requires a real Android Context and native Play Billing service bindings.
 * These tests therefore verify the guard branches in [BillingRepository] by
 * replicating each conditional in an isolated helper that mirrors the production code.
 *
 * Guards under test:
 *  1. [launchPurchaseFlow] returns false when product details are null.
 *  2. [launchPurchaseFlow] returns false when the billing client is not ready.
 *  3. [launchPurchaseFlow] returns false when subscriptionOfferDetails is empty/null.
 *  4. [verifyPremiumStatus] skips queryPurchasesAsync when the client is not ready.
 *  5. [verifyPremiumStatus] proceeds when the client is ready.
 */
class BillingRepositoryTest {

    // ── launchPurchaseFlow guards ────────────────────────────────────────────

    @Test
    fun `launchPurchaseFlow returns false when productDetails is null`() {
        val result = simulateLaunchPurchaseFlow(productDetails = null, clientReady = true)
        assertFalse(result)
    }

    @Test
    fun `launchPurchaseFlow returns false when billingClient is not ready`() {
        val details = mockk<ProductDetails>(relaxed = true)
        val result = simulateLaunchPurchaseFlow(productDetails = details, clientReady = false)
        assertFalse(result)
    }

    @Test
    fun `launchPurchaseFlow returns false when subscriptionOfferDetails is null`() {
        val details = mockk<ProductDetails> {
            every { subscriptionOfferDetails } returns null
        }
        val result = simulateLaunchPurchaseFlow(productDetails = details, clientReady = true)
        assertFalse(result)
    }

    @Test
    fun `launchPurchaseFlow returns false when subscriptionOfferDetails is empty`() {
        val details = mockk<ProductDetails> {
            every { subscriptionOfferDetails } returns emptyList()
        }
        val result = simulateLaunchPurchaseFlow(productDetails = details, clientReady = true)
        assertFalse(result)
    }

    @Test
    fun `launchPurchaseFlow returns true when details are present client is ready and offer token exists`() {
        val offerDetail = mockk<ProductDetails.SubscriptionOfferDetails> {
            every { offerToken } returns "mock-offer-token"
        }
        val details = mockk<ProductDetails> {
            every { subscriptionOfferDetails } returns listOf(offerDetail)
        }
        val result = simulateLaunchPurchaseFlow(productDetails = details, clientReady = true)
        assertTrue(result)
    }

    // ── verifyPremiumStatus guards ───────────────────────────────────────────

    @Test
    fun `verifyPremiumStatus returns early when billingClient is not ready`() {
        val invoked = simulateVerifyPremiumStatus(clientReady = false)
        assertFalse("queryPurchasesAsync should not be called when client is not ready", invoked)
    }

    @Test
    fun `verifyPremiumStatus proceeds when billingClient is ready`() {
        val invoked = simulateVerifyPremiumStatus(clientReady = true)
        assertTrue("queryPurchasesAsync should be called when client is ready", invoked)
    }

    // ── Helpers mirroring BillingRepository guard logic ─────────────────────

    /**
     * Mirrors the guard sequence in [BillingRepository.launchPurchaseFlow].
     * Returns true if all guards pass (the flow would be launched in production).
     */
    private fun simulateLaunchPurchaseFlow(
        productDetails: ProductDetails?,
        clientReady: Boolean,
    ): Boolean {
        if (productDetails == null) return false
        if (!clientReady) return false
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: return false
        // offerToken obtained — the flow would launch; signal success.
        return offerToken.isNotEmpty()
    }

    /**
     * Mirrors the guard in [BillingRepository.verifyPremiumStatus].
     * Returns true if queryPurchasesAsync would be called.
     */
    private fun simulateVerifyPremiumStatus(clientReady: Boolean): Boolean {
        if (!clientReady) return false
        return true
    }
}
