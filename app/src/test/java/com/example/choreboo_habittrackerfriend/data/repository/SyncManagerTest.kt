package com.example.choreboo_habittrackerfriend.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [SyncManager]: cooldown logic, force flag, auth guard, and mutex.
 *
 * All repository methods are mocked to succeed (or fail) without touching the
 * network or database. The goal is to verify SyncManager's orchestration logic.
 */
class SyncManagerTest {

    private lateinit var habitRepository: HabitRepository
    private lateinit var chorebooRepository: ChorebooRepository
    private lateinit var userRepository: UserRepository
    private lateinit var backgroundRepository: BackgroundRepository
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var syncManager: SyncManager

    @Before
    fun setUp() {
        habitRepository = mockk(relaxed = true)
        chorebooRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        backgroundRepository = mockk(relaxed = true)
        firebaseAuth = mockk()

        // Default: authenticated user with a valid token
        val user = mockk<FirebaseUser>()
        every { firebaseAuth.currentUser } returns user
        val tokenTask = Tasks.forResult(mockk<GetTokenResult>())
        every { user.getIdToken(false) } returns tokenTask

        syncManager = SyncManager(habitRepository, chorebooRepository, userRepository, backgroundRepository, firebaseAuth)
    }

    // ── Auth guard ──────────────────────────────────────────────────────

    @Test
    fun `syncAll returns false when no user is authenticated`() = runTest {
        every { firebaseAuth.currentUser } returns null

        val result = syncManager.syncAll(force = true)

        assertFalse(result)
        coVerify(exactly = 0) { habitRepository.syncHabitsFromCloud() }
    }

    // ── Happy path ──────────────────────────────────────────────────────

    @Test
    fun `syncAll returns true when all steps succeed`() = runTest {
        val result = syncManager.syncAll(force = true)

        assertTrue(result)
        coVerify { habitRepository.syncHabitsFromCloud() }
        coVerify { chorebooRepository.syncFromCloud() }
        coVerify { userRepository.syncPointsFromCloud() }
        coVerify { habitRepository.syncHabitLogsFromCloud() }
        coVerify { habitRepository.syncHouseholdHabitLogsForToday() }
    }

    // ── Cooldown ────────────────────────────────────────────────────────

    @Test
    fun `shouldSync returns true initially`() {
        assertTrue(syncManager.shouldSync())
    }

    @Test
    fun `syncAll with force=true bypasses cooldown`() = runTest {
        // First sync sets the cooldown
        syncManager.syncAll(force = true)
        // Second sync with force=true should still run
        val result = syncManager.syncAll(force = true)
        assertTrue(result)
        // Habits should have been synced twice
        coVerify(exactly = 2) { habitRepository.syncHabitsFromCloud() }
    }

    @Test
    fun `syncAll without force skips within cooldown`() = runTest {
        // First sync sets the cooldown
        syncManager.syncAll(force = true)
        // Second sync without force — within cooldown, returns true (throttled, not failure)
        val result = syncManager.syncAll(force = false)
        assertTrue(result)
        // Habits should have been synced only once (second call skipped)
        coVerify(exactly = 1) { habitRepository.syncHabitsFromCloud() }
    }

    // ── Partial failure ─────────────────────────────────────────────────

    @Test
    fun `syncAll returns true when at least one step succeeds`() = runTest {
        coEvery { habitRepository.syncHabitsFromCloud() } throws Exception("fail")
        coEvery { userRepository.syncPointsFromCloud() } throws Exception("fail")
        // chorebooRepository.syncFromCloud() still succeeds (relaxed mock)

        val result = syncManager.syncAll(force = true)

        assertTrue(result)
    }

    @Test
    fun `syncAll returns false when all steps fail`() = runTest {
        coEvery { habitRepository.syncHabitsFromCloud() } throws Exception("fail")
        coEvery { chorebooRepository.syncFromCloud() } throws Exception("fail")
        coEvery { userRepository.syncPointsFromCloud() } throws Exception("fail")

        val result = syncManager.syncAll(force = true)

        assertFalse(result)
    }

    @Test
    fun `cooldown is NOT updated when all steps fail`() = runTest {
        coEvery { habitRepository.syncHabitsFromCloud() } throws Exception("fail")
        coEvery { chorebooRepository.syncFromCloud() } throws Exception("fail")
        coEvery { userRepository.syncPointsFromCloud() } throws Exception("fail")

        syncManager.syncAll(force = true)

        // shouldSync should still return true because cooldown was not updated
        assertTrue(syncManager.shouldSync())
    }

    // ── Habit log sync depends on habit sync ────────────────────────────

    @Test
    fun `habit logs are skipped when habit sync fails`() = runTest {
        coEvery { habitRepository.syncHabitsFromCloud() } throws Exception("fail")

        syncManager.syncAll(force = true)

        coVerify(exactly = 0) { habitRepository.syncHabitLogsFromCloud() }
    }

    @Test
    fun `habit logs run after successful habit sync`() = runTest {
        syncManager.syncAll(force = true)

        coVerify(exactly = 1) { habitRepository.syncHabitLogsFromCloud() }
    }

    // ── Household logs are best-effort ──────────────────────────────────

    @Test
    fun `household log sync failure does not affect result`() = runTest {
        coEvery { habitRepository.syncHouseholdHabitLogsForToday() } throws Exception("fail")

        val result = syncManager.syncAll(force = true)

        assertTrue(result) // Main sync steps succeeded
    }
}
