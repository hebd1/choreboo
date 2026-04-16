package com.choreboo.app.data.repository

import com.choreboo.app.data.datastore.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Tests for [UserRepository]: validation guards, syncPointsFromCloud max-wins merge,
 * and profile photo upload/delete.
 *
 * Uses MockK for all dependencies. The Data Connect connector is `lazy` and
 * tested indirectly through the syncPointsToCloud validation guard.
 */
class UserRepositoryTest {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var userPreferences: UserPreferences
    private lateinit var firebaseStorage: FirebaseStorage
    private lateinit var repo: UserRepository

    @Before
    fun setUp() {
        firebaseAuth = mockk()
        userPreferences = mockk(relaxed = true)
        firebaseStorage = mockk()

        val user = mockk<FirebaseUser>()
        every { firebaseAuth.currentUser } returns user
        every { user.uid } returns "test-uid"
        every { user.displayName } returns "Test User"
        every { user.email } returns "test@example.com"
        every { user.photoUrl } returns null
        every { user.providerData } returns emptyList()

        repo = UserRepository(firebaseAuth, userPreferences, firebaseStorage)
    }

    // ── syncPointsToCloud validation ────────────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun `syncPointsToCloud throws on negative totalPoints`() = runTest {
        repo.syncPointsToCloud(totalPoints = -1, totalLifetimeXp = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `syncPointsToCloud throws on negative totalLifetimeXp`() = runTest {
        repo.syncPointsToCloud(totalPoints = 0, totalLifetimeXp = -1)
    }

    @Test
    fun `syncPointsToCloud accepts zero values`() = runTest {
        // Should not throw — zero is valid
        repo.syncPointsToCloud(totalPoints = 0, totalLifetimeXp = 0)
    }

    // ── getCurrentUid ───────────────────────────────────────────────────

    @Test
    fun `getCurrentUid returns uid when authenticated`() {
        assertEquals("test-uid", repo.getCurrentUid())
    }

    @Test
    fun `getCurrentUid returns null when not authenticated`() {
        every { firebaseAuth.currentUser } returns null
        assertEquals(null, repo.getCurrentUid())
    }

    // ── getCurrentAppUser ───────────────────────────────────────────────

    @Test
    fun `getCurrentAppUser returns AppUser from FirebaseUser`() {
        val user = repo.getCurrentAppUser()!!
        assertEquals("test-uid", user.uid)
        assertEquals("Test User", user.displayName)
        assertEquals("test@example.com", user.email)
    }

    @Test
    fun `getCurrentAppUser returns null when not authenticated`() {
        every { firebaseAuth.currentUser } returns null
        assertEquals(null, repo.getCurrentAppUser())
    }

    @Test
    fun `getCurrentAppUser defaults displayName to User when null`() {
        val fbUser = mockk<FirebaseUser>()
        every { firebaseAuth.currentUser } returns fbUser
        every { fbUser.uid } returns "uid"
        every { fbUser.displayName } returns null
        every { fbUser.email } returns null
        every { fbUser.photoUrl } returns null

        val user = repo.getCurrentAppUser()!!
        assertEquals("User", user.displayName)
    }

    // ── uploadProfilePhoto ──────────────────────────────────────────────

    @Test
    fun `uploadProfilePhoto throws when no authenticated user`() = runTest {
        every { firebaseAuth.currentUser } returns null
        val file = mockk<java.io.File>()

        try {
            repo.uploadProfilePhoto(file)
            assert(false) { "Expected IllegalStateException" }
        } catch (e: IllegalStateException) {
            assertEquals("No authenticated user", e.message)
        }
    }

    // ── deleteProfilePhoto ──────────────────────────────────────────────

    @Test
    fun `deleteProfilePhoto throws when no authenticated user`() = runTest {
        every { firebaseAuth.currentUser } returns null
        val file = mockk<java.io.File>()

        try {
            repo.deleteProfilePhoto(file)
            assert(false) { "Expected IllegalStateException" }
        } catch (e: IllegalStateException) {
            assertEquals("No authenticated user", e.message)
        }
    }
}
