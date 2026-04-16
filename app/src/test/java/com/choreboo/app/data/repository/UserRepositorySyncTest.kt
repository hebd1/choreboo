package com.choreboo.app.data.repository

import com.choreboo.app.data.datastore.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [UserRepository.syncPointsFromCloud] — the max-wins merge strategy.
 *
 * These tests verify the merge logic (local DataStore vs cloud values), the push-back
 * behaviour when merged values differ from cloud, and the early-return when
 * unauthenticated.
 *
 * Because the Data Connect connector is `lazy` and unavailable in JVM tests,
 * [syncPointsFromCloud] will throw when it tries to call `connector.getCurrentUser.execute()`.
 * To test the merge logic, we allow the exception to propagate (the method re-throws)
 * and instead focus on testing the branches we CAN isolate:
 * - Unauthenticated early-return (no connector access at all)
 * - syncPointsToCloud validation (already in UserRepositoryTest, but we add merge scenarios)
 *
 * For full merge integration testing, an instrumented test with a real Data Connect
 * instance would be required. Here we verify the guard behaviour.
 */
class UserRepositorySyncTest {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var userPreferences: UserPreferences
    private lateinit var firebaseStorage: FirebaseStorage
    private lateinit var repo: UserRepository

    private val testUid = "test-uid-sync"

    @Before
    fun setUp() {
        firebaseAuth = mockk()
        userPreferences = mockk(relaxed = true)
        firebaseStorage = mockk()

        val user = mockk<FirebaseUser>()
        every { firebaseAuth.currentUser } returns user
        every { user.uid } returns testUid
        every { user.displayName } returns "Test User"
        every { user.email } returns "test@example.com"
        every { user.photoUrl } returns null
        every { user.providerData } returns emptyList()

        repo = UserRepository(firebaseAuth, userPreferences, firebaseStorage)
    }

    // ── syncPointsFromCloud: unauthenticated ─────────────────────────────────

    @Test
    fun `syncPointsFromCloud returns immediately when not authenticated`() = runTest {
        every { firebaseAuth.currentUser } returns null

        // Should return without touching connector or DataStore
        repo.syncPointsFromCloud()

        // No DataStore writes should happen
        coVerify(exactly = 0) { userPreferences.setPointsAndLifetimeXp(any(), any()) }
    }

    // ── syncPointsFromCloud: connector access throws in JVM ──────────────────

    @Test
    fun `syncPointsFromCloud throws when connector is unavailable`() = runTest {
        // The lazy connector throws in JVM tests. This verifies the method propagates
        // the exception (it has `throw e` in the catch block).
        try {
            repo.syncPointsFromCloud()
            assert(false) { "Expected exception from connector access" }
        } catch (_: Exception) {
            // Expected — connector.getCurrentUser throws in JVM
        }
    }

    // ── syncPointsToCloud: unauthenticated ───────────────────────────────────

    @Test
    fun `syncPointsToCloud returns immediately when not authenticated`() = runTest {
        every { firebaseAuth.currentUser } returns null

        // Should return without accessing connector (lazy init would throw)
        repo.syncPointsToCloud(totalPoints = 100, totalLifetimeXp = 500)

        // If we get here without exception, the early-return worked
    }

    // ── syncCurrentUserToCloud: unauthenticated ──────────────────────────────

    @Test
    fun `syncCurrentUserToCloud returns immediately when not authenticated`() = runTest {
        every { firebaseAuth.currentUser } returns null

        // Should return without accessing connector
        repo.syncCurrentUserToCloud()

        // If no exception, early-return worked
    }

    // ── fetchCurrentUserFromCloud: unauthenticated ───────────────────────────

    @Test
    fun `fetchCurrentUserFromCloud returns null when not authenticated`() = runTest {
        every { firebaseAuth.currentUser } returns null

        val result = repo.fetchCurrentUserFromCloud()

        assert(result == null) { "Expected null for unauthenticated user" }
    }

    // ── fetchCurrentUserFromCloud: connector throws falls back to local ──────

    @Test
    fun `fetchCurrentUserFromCloud returns local AppUser when connector throws`() = runTest {
        // The lazy connector will throw. The method catches and falls back to getCurrentAppUser().
        val result = repo.fetchCurrentUserFromCloud()

        assert(result != null) { "Expected fallback AppUser from FirebaseAuth" }
        assert(result!!.uid == testUid)
        assert(result.displayName == "Test User")
    }
}
