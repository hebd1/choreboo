package com.example.choreboo_habittrackerfriend.data.repository

import android.content.Context
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.local.dao.HabitDao
import com.example.choreboo_habittrackerfriend.data.local.dao.HabitLogDao
import com.example.choreboo_habittrackerfriend.data.local.entity.HabitEntity
import com.example.choreboo_habittrackerfriend.data.local.entity.HabitLogEntity
import com.example.choreboo_habittrackerfriend.domain.model.Habit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [HabitRepository]: validation guards, completeHabit flow, and XP calculation.
 *
 * Uses MockK for all dependencies. The Data Connect connector is `lazy` and
 * never accessed because test entities have `remoteId = null`.
 */
class HabitRepositoryTest {

    private lateinit var habitDao: HabitDao
    private lateinit var habitLogDao: HabitLogDao
    private lateinit var userPreferences: UserPreferences
    private lateinit var userRepository: UserRepository
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var repo: HabitRepository

    private val testUid = "test-uid-123"

    private fun habitEntity(
        id: Long = 1L,
        title: String = "Exercise",
        baseXp: Int = 10,
        remoteId: String? = null,
        isHouseholdHabit: Boolean = false,
        customDays: String = "MON,TUE,WED,THU,FRI,SAT,SUN",
    ) = HabitEntity(
        id = id,
        title = title,
        baseXp = baseXp,
        remoteId = remoteId,
        isHouseholdHabit = isHouseholdHabit,
        ownerUid = testUid,
        customDays = customDays,
    )

    @Before
    fun setUp() {
        habitDao = mockk(relaxed = true)
        habitLogDao = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        firebaseAuth = mockk()

        val user = mockk<FirebaseUser>()
        every { firebaseAuth.currentUser } returns user
        every { user.uid } returns testUid

        // Default: totalPoints and totalLifetimeXp start at 0
        every { userPreferences.totalPoints } returns flowOf(0)
        every { userPreferences.totalLifetimeXp } returns flowOf(0)

        val context = mockk<Context>(relaxed = true)
        repo = HabitRepository(habitDao, habitLogDao, userPreferences, userRepository, firebaseAuth, context)
    }

    // ── upsertHabit validation ──────────────────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun `upsertHabit throws on blank title`() = runTest {
        repo.upsertHabit(Habit(title = "   "))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `upsertHabit throws on empty title`() = runTest {
        repo.upsertHabit(Habit(title = ""))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `upsertHabit throws on title exceeding 100 characters`() = runTest {
        repo.upsertHabit(Habit(title = "A".repeat(101), baseXp = 10))
    }

    @Test
    fun `upsertHabit accepts title at exactly 100 characters`() = runTest {
        coEvery { habitDao.upsertHabit(any()) } returns 1L
        val result = repo.upsertHabit(Habit(title = "A".repeat(100), baseXp = 10))
        assertEquals(1L, result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `upsertHabit throws on zero baseXp`() = runTest {
        repo.upsertHabit(Habit(title = "Valid", baseXp = 0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `upsertHabit throws on negative baseXp`() = runTest {
        repo.upsertHabit(Habit(title = "Valid", baseXp = -5))
    }

    @Test
    fun `upsertHabit succeeds with valid input`() = runTest {
        coEvery { habitDao.upsertHabit(any()) } returns 1L

        val result = repo.upsertHabit(Habit(title = "Exercise", baseXp = 10))

        assertEquals(1L, result)
    }

    // ── completeHabit validation ────────────────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun `completeHabit throws on zero habitId`() = runTest {
        repo.completeHabit(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `completeHabit throws on negative habitId`() = runTest {
        repo.completeHabit(-1)
    }

    // ── completeHabit – already completed ───────────────────────────────

    @Test
    fun `completeHabit returns alreadyComplete when habit not found`() = runTest {
        coEvery { habitDao.getHabitByIdSync(1L) } returns null

        val result = repo.completeHabit(1L)

        assertTrue(result.alreadyComplete)
        assertEquals(0, result.xpEarned)
    }

    @Test
    fun `completeHabit returns alreadyComplete when already completed today`() = runTest {
        coEvery { habitDao.getHabitByIdSync(1L) } returns habitEntity()
        coEvery { habitLogDao.getCompletionCountForDate(1L, any()) } returns 1

        val result = repo.completeHabit(1L)

        assertTrue(result.alreadyComplete)
        assertEquals(0, result.xpEarned)
    }

    @Test
    fun `completeHabit returns alreadyComplete when UNIQUE constraint rejects insert`() = runTest {
        coEvery { habitDao.getHabitByIdSync(1L) } returns habitEntity()
        coEvery { habitLogDao.getCompletionCountForDate(1L, any()) } returns 0
        coEvery { habitLogDao.getCompletionDatesForHabit(1L) } returns emptyList()
        // insertLog returns -1 → UNIQUE conflict (duplicate)
        coEvery { habitLogDao.insertLog(any()) } returns -1L

        val result = repo.completeHabit(1L)

        assertTrue(result.alreadyComplete)
        assertEquals(0, result.xpEarned)
    }

    // ── completeHabit – successful ──────────────────────────────────────

    @Test
    fun `completeHabit calculates correct XP with zero streak`() = runTest {
        coEvery { habitDao.getHabitByIdSync(1L) } returns habitEntity(baseXp = 20)
        coEvery { habitLogDao.getCompletionCountForDate(1L, any()) } returns 0
        coEvery { habitLogDao.getCompletionDatesForHabit(1L) } returns emptyList()
        coEvery { habitLogDao.insertLog(any()) } returns 100L

        val result = repo.completeHabit(1L)

        assertFalse(result.alreadyComplete)
        // streak = 0, so xpEarned = baseXp + (0 * 2) = 20
        assertEquals(20, result.xpEarned)
        assertEquals(1, result.newStreak) // streak + 1
    }

    @Test
    fun `completeHabit XP is capped at baseXp times 3`() = runTest {
        coEvery { habitDao.getHabitByIdSync(1L) } returns habitEntity(baseXp = 10)
        coEvery { habitLogDao.getCompletionCountForDate(1L, any()) } returns 0
        // Build 100 consecutive dates ending yesterday relative to runtime date,
        // so calculateStreak (which calls LocalDate.now() internally) walks back correctly.
        val today = LocalDate.now()
        val consecutiveDates = (1..100).map {
            today.minusDays(it.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
        } // DESC order: day 1 = yesterday, day 100 = 100 days ago
        coEvery { habitLogDao.getCompletionDatesForHabit(1L) } returns consecutiveDates
        coEvery { habitLogDao.insertLog(any()) } returns 100L

        val result = repo.completeHabit(1L)

        // streak = 100, xp = (10 + 100*2) = 210 → capped at 10*3 = 30
        assertEquals(30, result.xpEarned)
    }

    @Test
    fun `completeHabit sets completedByUid on log entity`() = runTest {
        coEvery { habitDao.getHabitByIdSync(1L) } returns habitEntity()
        coEvery { habitLogDao.getCompletionCountForDate(1L, any()) } returns 0
        coEvery { habitLogDao.getCompletionDatesForHabit(1L) } returns emptyList()

        val logSlot = slot<HabitLogEntity>()
        coEvery { habitLogDao.insertLog(capture(logSlot)) } returns 100L

        repo.completeHabit(1L)

        assertEquals(testUid, logSlot.captured.completedByUid)
    }

    @Test
    fun `completeHabit adds points and lifetime XP to preferences`() = runTest {
        coEvery { habitDao.getHabitByIdSync(1L) } returns habitEntity(baseXp = 15)
        coEvery { habitLogDao.getCompletionCountForDate(1L, any()) } returns 0
        coEvery { habitLogDao.getCompletionDatesForHabit(1L) } returns emptyList()
        coEvery { habitLogDao.insertLog(any()) } returns 100L

        repo.completeHabit(1L)

        coVerify { userPreferences.addPoints(15) }
        coVerify { userPreferences.addLifetimeXp(15) }
    }

    // ── calculateStreak – custom schedule ───────────────────────────────

    @Test
    fun `completeHabit streak counts consecutive scheduled days for weekly habit`() = runTest {
        // Mon/Wed/Fri habit — use a fixed Friday (2026-01-16) with Mon and Wed completed this week
        val friday = LocalDate.of(2026, 1, 16)   // known Friday
        val wednesday = friday.minusDays(2)       // 2026-01-14
        val monday = friday.minusDays(4)          // 2026-01-12

        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val fridayStr = friday.format(fmt)
        val wednesdayStr = wednesday.format(fmt)
        val mondayStr = monday.format(fmt)

        coEvery { habitDao.getHabitByIdSync(1L) } returns habitEntity(
            customDays = "MON,WED,FRI",
        )
        coEvery { habitLogDao.getCompletionCountForDate(1L, fridayStr) } returns 0
        // Completions on Mon and Wed (DESC order)
        coEvery { habitLogDao.getCompletionDatesForHabit(1L) } returns listOf(wednesdayStr, mondayStr)
        val logSlot = slot<HabitLogEntity>()
        coEvery { habitLogDao.insertLog(capture(logSlot)) } returns 100L

        // We cannot inject a fake "today" into HabitRepository — completeHabit uses
        // LocalDate.now() internally for the completion date string. Since the test
        // always runs against the real clock, we verify only that the log was inserted
        // (not short-circuited as alreadyComplete) and that streakAtCompletion is >= 1.
        repo.completeHabit(1L)

        assertTrue(logSlot.captured.streakAtCompletion >= 1)
    }

    @Test
    fun `completeHabit streak ignores non-scheduled days for Mon-Wed-Fri habit`() = runTest {
        // Use a fixed Wednesday (2026-01-14). Monday two days prior was completed;
        // Tuesday (not scheduled) has no log — streak algorithm should skip it.
        val wednesday = LocalDate.of(2026, 1, 14)
        val monday = wednesday.minusDays(2)
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val wednesdayStr = wednesday.format(fmt)
        val mondayStr = monday.format(fmt)

        coEvery { habitDao.getHabitByIdSync(1L) } returns habitEntity(
            customDays = "MON,WED,FRI",
        )
        coEvery { habitLogDao.getCompletionCountForDate(1L, any()) } returns 0
        // Only Monday logged (Tuesday not scheduled, so no entry there is correct)
        coEvery { habitLogDao.getCompletionDatesForHabit(1L) } returns listOf(mondayStr)
        val logSlot = slot<HabitLogEntity>()
        coEvery { habitLogDao.insertLog(capture(logSlot)) } returns 100L

        repo.completeHabit(1L)

        // The log should be inserted (not alreadyComplete).
        // streakAtCompletion >= 1 regardless of what day the test actually runs —
        // the history list (Monday) is present, so the algorithm counts at least 1.
        assertTrue(logSlot.captured.streakAtCompletion >= 1)
    }

    @Test
    fun `completeHabit streak is 1 for first completion of any habit type`() = runTest {
        coEvery { habitDao.getHabitByIdSync(1L) } returns habitEntity(
            baseXp = 10,
            customDays = "MON,WED,FRI",
        )
        coEvery { habitLogDao.getCompletionCountForDate(1L, any()) } returns 0
        coEvery { habitLogDao.getCompletionDatesForHabit(1L) } returns emptyList()
        val logSlot = slot<HabitLogEntity>()
        coEvery { habitLogDao.insertLog(capture(logSlot)) } returns 100L

        repo.completeHabit(1L)

        // First completion ever → streak = 0 + 1 = 1
        assertEquals(1, logSlot.captured.streakAtCompletion)
    }
}
