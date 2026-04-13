package com.example.choreboo_habittrackerfriend.data.repository

import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.android.gms.tasks.Tasks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ResetRepository].
 *
 * Because the Data Connect connector is `lazy` and cannot be constructed in JVM tests,
 * these tests cover the paths where:
 *  - The user is not authenticated → returns Error immediately.
 *  - The Firebase Auth re-authentication and deletion succeed → local cleanup is called.
 *  - Re-authentication fails → local cleanup still runs and Error is returned.
 *
 * The cloud cleanup steps are individually try/caught in [ResetRepository.resetAll], so
 * connector failures (which occur in JVM tests because the lazy connector throws) are
 * treated as non-fatal. We verify the final outcome and that local cleanup always runs.
 */
class ResetRepositoryTest {

    private lateinit var habitRepository: HabitRepository
    private lateinit var chorebooRepository: ChorebooRepository
    private lateinit var householdRepository: HouseholdRepository
    private lateinit var backgroundRepository: BackgroundRepository
    private lateinit var userPreferences: UserPreferences
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var syncManager: SyncManager
    private lateinit var repo: ResetRepository

    @Before
    fun setUp() {
        habitRepository = mockk(relaxed = true)
        chorebooRepository = mockk(relaxed = true)
        householdRepository = mockk(relaxed = true)
        backgroundRepository = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)
        firebaseAuth = mockk(relaxed = true)
        syncManager = mockk(relaxed = true)

        // syncManager.runExclusive should execute the block
        coEvery { syncManager.runExclusive<ResetResult>(any()) } coAnswers {
            val block = firstArg<suspend () -> ResetResult>()
            block()
        }

        repo = ResetRepository(
            habitRepository = habitRepository,
            chorebooRepository = chorebooRepository,
            householdRepository = householdRepository,
            backgroundRepository = backgroundRepository,
            userPreferences = userPreferences,
            firebaseAuth = firebaseAuth,
            syncManager = syncManager,
        )
    }

    // ── not authenticated ─────────────────────────────────────────────────────

    @Test
    fun `resetAll returns Error when user is not authenticated`() = runTest {
        every { firebaseAuth.currentUser } returns null
        val credential = mockk<AuthCredential>(relaxed = true)

        val result = repo.resetAll(credential)

        assertTrue(result is ResetResult.Error)
    }

    // ── successful reset ──────────────────────────────────────────────────────

    @Test
    fun `resetAll returns Success and clears local data when auth succeeds`() = runTest {
        val mockUser = mockk<FirebaseUser>(relaxed = true) {
            every { uid } returns "uid-reset"
            every { reauthenticate(any()) } returns Tasks.forResult(null)
            every { delete() } returns Tasks.forResult(null)
        }
        every { firebaseAuth.currentUser } returns mockUser
        val credential = mockk<AuthCredential>(relaxed = true)

        val result = repo.resetAll(credential)

        // Cloud steps will throw (connector not available) but are caught individually,
        // so auth deletion and local cleanup still run → result is Success.
        assertTrue(result is ResetResult.Success)
        coVerify { habitRepository.clearLocalData() }
        coVerify { chorebooRepository.clearLocalData() }
        coVerify { userPreferences.clearAllData() }
    }

    // ── re-authentication failure ─────────────────────────────────────────────

    @Test
    fun `resetAll returns Error and still clears local data when re-auth fails`() = runTest {
        val mockUser = mockk<FirebaseUser>(relaxed = true) {
            every { uid } returns "uid-reset-fail"
            every { reauthenticate(any()) } returns Tasks.forException(Exception("Re-auth failed"))
        }
        every { firebaseAuth.currentUser } returns mockUser
        val credential = mockk<AuthCredential>(relaxed = true)

        val result = repo.resetAll(credential)

        assertTrue(result is ResetResult.Error)
        // Local cleanup must still run even when auth deletion fails
        coVerify { habitRepository.clearLocalData() }
        coVerify { userPreferences.clearAllData() }
    }

    // ── cancelPendingWrites is called ─────────────────────────────────────────

    @Test
    fun `resetAll cancels pending writes before cloud cleanup`() = runTest {
        val mockUser = mockk<FirebaseUser>(relaxed = true) {
            every { uid } returns "uid-cancel"
            every { reauthenticate(any()) } returns Tasks.forResult(null)
            every { delete() } returns Tasks.forResult(null)
        }
        every { firebaseAuth.currentUser } returns mockUser
        val credential = mockk<AuthCredential>(relaxed = true)

        repo.resetAll(credential)

        coVerify { habitRepository.cancelPendingWrites() }
        coVerify { chorebooRepository.cancelPendingWrites() }
    }
}
