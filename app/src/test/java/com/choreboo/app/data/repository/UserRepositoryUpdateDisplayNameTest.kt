package com.choreboo.app.data.repository

import com.choreboo.app.data.datastore.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [UserRepository.updateDisplayName] validation guards.
 *
 * Only covers the precondition paths (blank name, name > 30 chars, no authenticated user)
 * that throw before any Firebase API is touched, avoiding connector access in JVM tests.
 */
class UserRepositoryUpdateDisplayNameTest {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var userPreferences: UserPreferences
    private lateinit var firebaseStorage: FirebaseStorage
    private lateinit var repo: UserRepository

    @Before
    fun setUp() {
        firebaseAuth = mockk()
        userPreferences = mockk(relaxed = true)
        firebaseStorage = mockk()
        repo = UserRepository(firebaseAuth, userPreferences, firebaseStorage)
    }

    // ── blank name ───────────────────────────────────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun `updateDisplayName throws on blank name`() = runTest {
        every { firebaseAuth.currentUser } returns mockk(relaxed = true)
        repo.updateDisplayName("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updateDisplayName throws on whitespace-only name`() = runTest {
        every { firebaseAuth.currentUser } returns mockk(relaxed = true)
        repo.updateDisplayName("   ")
    }

    // ── name too long ────────────────────────────────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun `updateDisplayName throws on name longer than 30 chars`() = runTest {
        every { firebaseAuth.currentUser } returns mockk(relaxed = true)
        repo.updateDisplayName("A".repeat(31))
    }

    @Test
    fun `updateDisplayName accepts name of exactly 30 chars`() = runTest {
        // 30-char name passes validation but then hits FirebaseUser.updateProfile — no user set
        // so it throws IllegalStateException, NOT IllegalArgumentException
        every { firebaseAuth.currentUser } returns null
        try {
            repo.updateDisplayName("A".repeat(30))
        } catch (e: IllegalStateException) {
            assertEquals("No authenticated user", e.message)
        }
    }

    // ── no authenticated user ────────────────────────────────────────────────

    @Test
    fun `updateDisplayName throws IllegalStateException when no current user`() = runTest {
        every { firebaseAuth.currentUser } returns null
        try {
            repo.updateDisplayName("ValidName")
            assert(false) { "Expected IllegalStateException" }
        } catch (e: IllegalStateException) {
            assertEquals("No authenticated user", e.message)
        }
    }

    @Test
    fun `updateDisplayName trims name before validating length`() = runTest {
        // "  " + 30 "A"s + "  " is 34 chars raw but only 30 after trim — should pass validation
        // then fail with IllegalStateException (no user), not IllegalArgumentException
        every { firebaseAuth.currentUser } returns null
        try {
            repo.updateDisplayName("  " + "A".repeat(30) + "  ")
        } catch (e: IllegalStateException) {
            assertEquals("No authenticated user", e.message)
        }
    }

    @Test
    fun `updateDisplayName trims name before blank check`() = runTest {
        // Whitespace-only collapses to blank after trim
        every { firebaseAuth.currentUser } returns mockk(relaxed = true)
        try {
            repo.updateDisplayName("   \t\n   ")
            assert(false) { "Expected IllegalArgumentException" }
        } catch (e: IllegalArgumentException) {
            // pass — blank after trim is correctly rejected
        }
    }
}
