package com.choreboo.app.data.repository

import android.content.Context
import com.choreboo.app.data.datastore.UserPreferences
import com.choreboo.app.data.local.dao.HabitDao
import com.choreboo.app.data.local.dao.HabitLogDao
import com.choreboo.app.data.local.entity.HabitEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Tests for [HabitRepository] sync and cleanup methods:
 * - [HabitRepository.convertHouseholdHabitsToPersonal]
 * - [HabitRepository.deleteHabit]
 * - [HabitRepository.clearLocalData]
 *
 * Cloud sync methods (syncHabitsFromCloud, syncHabitLogsFromCloud) access the Data Connect
 * connector directly and cannot be unit-tested without a running backend. These tests focus
 * on the local-only logic paths where remoteId = null avoids connector access.
 */
class HabitRepositorySyncTest {

    private lateinit var habitDao: HabitDao
    private lateinit var habitLogDao: HabitLogDao
    private lateinit var userPreferences: UserPreferences
    private lateinit var userRepository: UserRepository
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var reminderScheduler: HabitReminderHandler
    private lateinit var repo: HabitRepository

    private val testUid = "test-uid-123"
    private val otherUid = "other-uid-456"

    private fun habitEntity(
        id: Long = 1L,
        title: String = "Exercise",
        ownerUid: String = testUid,
        isHouseholdHabit: Boolean = false,
        householdId: String? = null,
        assignedToUid: String? = null,
        assignedToName: String? = null,
        remoteId: String? = null,
    ) = HabitEntity(
        id = id,
        title = title,
        ownerUid = ownerUid,
        isHouseholdHabit = isHouseholdHabit,
        householdId = householdId,
        assignedToUid = assignedToUid,
        assignedToName = assignedToName,
        remoteId = remoteId,
    )

    @Before
    fun setUp() {
        habitDao = mockk(relaxed = true)
        habitLogDao = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        firebaseAuth = mockk()
        reminderScheduler = mockk(relaxed = true)

        val user = mockk<FirebaseUser>()
        every { firebaseAuth.currentUser } returns user
        every { user.uid } returns testUid

        val context = mockk<Context>(relaxed = true)
        repo = HabitRepository(
            habitDao,
            habitLogDao,
            userPreferences,
            userRepository,
            firebaseAuth,
            context,
            reminderScheduler,
        )
    }

    // ── convertHouseholdHabitsToPersonal ─────────────────────────────────

    @Test
    fun `convertHouseholdHabitsToPersonal deletes non-owned household habits`() = runTest {
        coEvery { habitDao.getAllHabitsSync() } returns emptyList()

        repo.convertHouseholdHabitsToPersonal(testUid)

        coVerify(exactly = 1) { habitDao.deleteNonOwnedHouseholdHabits(testUid) }
    }

    @Test
    fun `convertHouseholdHabitsToPersonal converts own household habits to personal`() = runTest {
        val ownHouseholdHabit = habitEntity(
            id = 1L,
            ownerUid = testUid,
            isHouseholdHabit = true,
            householdId = "hh-001",
            assignedToUid = otherUid,
            assignedToName = "Other User",
            remoteId = null, // null → no write-through
        )
        val personalHabit = habitEntity(
            id = 2L,
            ownerUid = testUid,
            isHouseholdHabit = false,
        )
        coEvery { habitDao.getAllHabitsSync() } returns listOf(ownHouseholdHabit, personalHabit)

        val saved = slot<HabitEntity>()
        coEvery { habitDao.upsertHabit(capture(saved)) } returns 1L

        repo.convertHouseholdHabitsToPersonal(testUid)

        // Only the household habit should be updated, not the personal one
        coVerify(exactly = 1) { habitDao.upsertHabit(any()) }
        assertEquals(false, saved.captured.isHouseholdHabit)
        assertEquals(null, saved.captured.householdId)
        assertEquals(null, saved.captured.assignedToUid)
        assertEquals(null, saved.captured.assignedToName)
    }

    @Test
    fun `convertHouseholdHabitsToPersonal skips other users household habits`() = runTest {
        // This habit is owned by another user — should NOT be converted
        val otherUsersHabit = habitEntity(
            id = 3L,
            ownerUid = otherUid,
            isHouseholdHabit = true,
            householdId = "hh-001",
        )
        coEvery { habitDao.getAllHabitsSync() } returns listOf(otherUsersHabit)

        repo.convertHouseholdHabitsToPersonal(testUid)

        // deleteNonOwnedHouseholdHabits handles removal; upsertHabit should NOT be called
        coVerify(exactly = 0) { habitDao.upsertHabit(any()) }
    }

    @Test
    fun `convertHouseholdHabitsToPersonal handles multiple own household habits`() = runTest {
        val habit1 = habitEntity(
            id = 1L,
            ownerUid = testUid,
            isHouseholdHabit = true,
            householdId = "hh-001",
            remoteId = null,
        )
        val habit2 = habitEntity(
            id = 2L,
            ownerUid = testUid,
            isHouseholdHabit = true,
            householdId = "hh-001",
            remoteId = null,
        )
        coEvery { habitDao.getAllHabitsSync() } returns listOf(habit1, habit2)

        repo.convertHouseholdHabitsToPersonal(testUid)

        // Both should be converted
        coVerify(exactly = 2) { habitDao.upsertHabit(any()) }
    }

    // ── deleteHabit ─────────────────────────────────────────────────────

    @Test
    fun `deleteHabit deletes from local DAO`() = runTest {
        coEvery { habitDao.getHabitByIdSync(1L) } returns habitEntity(remoteId = null)

        repo.deleteHabit(1L)

        coVerify(exactly = 1) { habitDao.deleteHabitById(1L) }
    }

    @Test
    fun `deleteHabit does not trigger cloud delete when remoteId is null`() = runTest {
        coEvery { habitDao.getHabitByIdSync(1L) } returns habitEntity(remoteId = null)

        repo.deleteHabit(1L)

        // With remoteId = null, writeScope.launch block is never entered
        coVerify(exactly = 1) { habitDao.deleteHabitById(1L) }
    }

    @Test
    fun `deleteHabit handles missing entity gracefully`() = runTest {
        coEvery { habitDao.getHabitByIdSync(99L) } returns null

        repo.deleteHabit(99L)

        // Should still attempt to delete by ID (even if entity lookup returned null)
        coVerify(exactly = 1) { habitDao.deleteHabitById(99L) }
    }

    // ── clearLocalData ──────────────────────────────────────────────────

    @Test
    fun `clearLocalData deletes all logs then all habits`() = runTest {
        repo.clearLocalData()

        coVerify(exactly = 1) { habitLogDao.deleteAllLogs() }
        coVerify(exactly = 1) { habitDao.deleteAllHabits() }
    }

    // ── cancelAllReminders ──────────────────────────────────────────────

    @Test
    fun `cancelAllReminders does not crash when no habits exist`() = runTest {
        coEvery { habitDao.getAllHabitsSync() } returns emptyList()

        repo.cancelAllReminders()

        coVerify(exactly = 1) { habitDao.getAllHabitsSync() }
    }

    @Test
    fun `archiveHabit cancels reminder`() = runTest {
        coEvery { habitDao.getHabitByIdSync(1L) } returns habitEntity(id = 1L)

        repo.archiveHabit(1L)

        coVerify { reminderScheduler.cancel(any(), 1L) }
    }

    @Test
    fun `unarchiveHabit reschedules enabled reminder`() = runTest {
        coEvery { habitDao.getHabitByIdSync(1L) } returns habitEntity(
            id = 1L,
            title = "Read",
        ).copy(
            reminderEnabled = true,
            reminderTime = "08:30",
            customDays = "MON,WED,FRI",
        )

        repo.unarchiveHabit(1L)

        coVerify {
            reminderScheduler.schedule(
                any(),
                1L,
                "Read",
                LocalTime.of(8, 30),
                listOf("MON", "WED", "FRI"),
            )
        }
    }

    @Test
    fun `syncReminderFromCloud schedules owned enabled reminder`() {
        repo.syncReminderFromCloud(
            localHabitId = 7L,
            title = "Exercise",
            reminderEnabled = true,
            reminderTime = "09:15",
            isArchived = false,
            customDays = "MON,TUE",
            shouldOwnReminder = true,
            logTag = "test",
        )

        coVerify {
            reminderScheduler.schedule(
                any(),
                7L,
                "Exercise",
                LocalTime.of(9, 15),
                listOf("MON", "TUE"),
            )
        }
    }

    @Test
    fun `syncReminderFromCloud cancels when reminder disabled`() {
        repo.syncReminderFromCloud(
            localHabitId = 8L,
            title = "Exercise",
            reminderEnabled = false,
            reminderTime = "09:15",
            isArchived = false,
            customDays = "MON,TUE",
            shouldOwnReminder = true,
            logTag = "test",
        )

        coVerify { reminderScheduler.cancel(any(), 8L) }
    }

    @Test
    fun `syncReminderFromCloud cancels when archived`() {
        repo.syncReminderFromCloud(
            localHabitId = 9L,
            title = "Exercise",
            reminderEnabled = true,
            reminderTime = "09:15",
            isArchived = true,
            customDays = "MON,TUE",
            shouldOwnReminder = true,
            logTag = "test",
        )

        coVerify { reminderScheduler.cancel(any(), 9L) }
    }

    @Test
    fun `syncReminderFromCloud cancels when habit is not owned by current reminder device`() {
        repo.syncReminderFromCloud(
            localHabitId = 10L,
            title = "Exercise",
            reminderEnabled = true,
            reminderTime = "09:15",
            isArchived = false,
            customDays = "MON,TUE",
            shouldOwnReminder = false,
            logTag = "test",
        )

        coVerify { reminderScheduler.cancel(any(), 10L) }
    }
}
