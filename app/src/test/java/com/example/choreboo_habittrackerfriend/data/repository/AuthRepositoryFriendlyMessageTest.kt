package com.example.choreboo_habittrackerfriend.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [toFriendlyMessage] — the extension function that converts
 * Firebase Auth exceptions into user-friendly error strings.
 *
 * This function is `internal` visibility so it can be tested directly.
 * Each Firebase error code has both a legacy (ALL_CAPS) and modern
 * (kebab-case) variant; both must be recognized.
 */
class AuthRepositoryFriendlyMessageTest {

    private fun exceptionWithMessage(msg: String) = Exception(msg)

    // ── Invalid credentials ─────────────────────────────────────────────

    @Test
    fun `INVALID_LOGIN_CREDENTIALS maps to friendly message`() {
        val result = exceptionWithMessage("INVALID_LOGIN_CREDENTIALS").toFriendlyMessage()
        assertEquals("Incorrect email or password.", result)
    }

    @Test
    fun `invalid-credential maps to friendly message`() {
        val result = exceptionWithMessage("invalid-credential").toFriendlyMessage()
        assertEquals("Incorrect email or password.", result)
    }

    // ── Email already exists ────────────────────────────────────────────

    @Test
    fun `EMAIL_EXISTS maps to friendly message`() {
        val result = exceptionWithMessage("EMAIL_EXISTS").toFriendlyMessage()
        assertEquals("An account with this email already exists.", result)
    }

    @Test
    fun `email-already-in-use maps to friendly message`() {
        val result = exceptionWithMessage("email-already-in-use").toFriendlyMessage()
        assertEquals("An account with this email already exists.", result)
    }

    // ── Weak password ───────────────────────────────────────────────────

    @Test
    fun `WEAK_PASSWORD maps to friendly message`() {
        val result = exceptionWithMessage("WEAK_PASSWORD").toFriendlyMessage()
        assertEquals("Password must be at least 6 characters.", result)
    }

    @Test
    fun `weak-password maps to friendly message`() {
        val result = exceptionWithMessage("weak-password").toFriendlyMessage()
        assertEquals("Password must be at least 6 characters.", result)
    }

    // ── Invalid email ───────────────────────────────────────────────────

    @Test
    fun `INVALID_EMAIL maps to friendly message`() {
        val result = exceptionWithMessage("INVALID_EMAIL").toFriendlyMessage()
        assertEquals("Please enter a valid email address.", result)
    }

    @Test
    fun `invalid-email maps to friendly message`() {
        val result = exceptionWithMessage("invalid-email").toFriendlyMessage()
        assertEquals("Please enter a valid email address.", result)
    }

    // ── User not found ──────────────────────────────────────────────────

    @Test
    fun `USER_NOT_FOUND maps to friendly message`() {
        val result = exceptionWithMessage("USER_NOT_FOUND").toFriendlyMessage()
        assertEquals("No account found with this email.", result)
    }

    @Test
    fun `user-not-found maps to friendly message`() {
        val result = exceptionWithMessage("user-not-found").toFriendlyMessage()
        assertEquals("No account found with this email.", result)
    }

    // ── Too many requests ───────────────────────────────────────────────

    @Test
    fun `TOO_MANY_REQUESTS maps to friendly message`() {
        val result = exceptionWithMessage("TOO_MANY_REQUESTS").toFriendlyMessage()
        assertEquals("Too many attempts. Please try again later.", result)
    }

    @Test
    fun `too-many-requests maps to friendly message`() {
        val result = exceptionWithMessage("too-many-requests").toFriendlyMessage()
        assertEquals("Too many attempts. Please try again later.", result)
    }

    // ── Network error ───────────────────────────────────────────────────

    @Test
    fun `NETWORK_ERROR maps to friendly message`() {
        val result = exceptionWithMessage("NETWORK_ERROR").toFriendlyMessage()
        assertEquals("Network error. Check your connection and try again.", result)
    }

    @Test
    fun `network-request-failed maps to friendly message`() {
        val result = exceptionWithMessage("network-request-failed").toFriendlyMessage()
        assertEquals("Network error. Check your connection and try again.", result)
    }

    // ── Fallback ────────────────────────────────────────────────────────

    @Test
    fun `unknown error with bracket prefix is stripped`() {
        val result = exceptionWithMessage("[auth/unknown] Something went wrong").toFriendlyMessage()
        assertEquals("Something went wrong", result)
    }

    @Test
    fun `unknown error without bracket prefix passes through`() {
        val result = exceptionWithMessage("Some unexpected error").toFriendlyMessage()
        assertEquals("Some unexpected error", result)
    }

    @Test
    fun `null message returns generic message`() {
        val exception = object : Exception() {
            override val message: String? get() = null
        }
        assertEquals("An unknown error occurred.", exception.toFriendlyMessage())
    }

    // ── Priority: first matching rule wins ──────────────────────────────

    @Test
    fun `message containing multiple keywords matches first rule`() {
        // Edge case: message contains both INVALID_EMAIL and WEAK_PASSWORD.
        // The when-chain checks in order, so INVALID_EMAIL should not match
        // before a credential check. Actually INVALID_LOGIN_CREDENTIALS is
        // first; let's test that path.
        val result = exceptionWithMessage("INVALID_LOGIN_CREDENTIALS and WEAK_PASSWORD").toFriendlyMessage()
        assertEquals("Incorrect email or password.", result)
    }

    // ── Error codes embedded in longer messages ─────────────────────────

    @Test
    fun `error code embedded in Firebase-style message is detected`() {
        val result = exceptionWithMessage(
            "com.google.firebase.auth.FirebaseAuthInvalidCredentialsException: [auth/invalid-email] The email address is badly formatted."
        ).toFriendlyMessage()
        assertEquals("Please enter a valid email address.", result)
    }
}
