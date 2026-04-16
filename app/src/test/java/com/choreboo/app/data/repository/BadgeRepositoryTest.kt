package com.choreboo.app.data.repository

import com.choreboo.app.data.local.dao.HabitDao
import com.choreboo.app.data.local.dao.HabitLogDao
import com.choreboo.app.data.datastore.UserPreferences
import com.choreboo.app.domain.model.Badge
import com.choreboo.app.domain.model.BadgeCategory
import com.choreboo.app.domain.model.BadgeDefinition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [BadgeRepository].
 *
 * Covers: unauthenticated path (empty list), badge unlock thresholds across all four
 * [BadgeCategory] types, and [getEarnedBadgeCount] counting.
 */
class BadgeRepositoryTest {

    private lateinit var habitDao: HabitDao
    private lateinit var habitLogDao: HabitLogDao
    private lateinit var userPreferences: UserPreferences
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var repo: BadgeRepository

    private val totalCompletionsFlow = MutableStateFlow(0)
    private val maxStreakFlow = MutableStateFlow(0)
    private val totalHabitCountFlow = MutableStateFlow(0)
    private val totalLifetimeXpFlow = MutableStateFlow(0)

    @Before
    fun setUp() {
        habitDao = mockk(relaxed = true)
        habitLogDao = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)
        firebaseAuth = mockk()

        val user = mockk<FirebaseUser>()
        every { firebaseAuth.currentUser } returns user
        every { user.uid } returns "uid-badge"

        every { habitLogDao.getTotalCompletionCount("uid-badge") } returns totalCompletionsFlow
        every { habitLogDao.getMaxStreakEver("uid-badge") } returns maxStreakFlow
        every { habitDao.getTotalHabitCount("uid-badge") } returns totalHabitCountFlow
        every { userPreferences.totalLifetimeXp } returns totalLifetimeXpFlow

        repo = BadgeRepository(habitDao, habitLogDao, userPreferences, firebaseAuth)
    }

    // ── unauthenticated ───────────────────────────────────────────────────────

    @Test
    fun `getAllBadges returns empty list when user is not authenticated`() = runTest {
        every { firebaseAuth.currentUser } returns null
        val unauthRepo = BadgeRepository(habitDao, habitLogDao, userPreferences, firebaseAuth)

        val badges = unauthRepo.getAllBadges().first()
        assertTrue(badges.isEmpty())
    }

    // ── total completions badges ─────────────────────────────────────────────

    @Test
    fun `FIRST_STEP badge is locked when completions is 0`() = runTest {
        totalCompletionsFlow.value = 0
        repo.getAllBadges().test {
            val badges = awaitItem()
            val badge = badges.first { it.definition == BadgeDefinition.FIRST_STEP }
            assertFalse(badge.isUnlocked)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `FIRST_STEP badge is unlocked when completions is at least 1`() = runTest {
        totalCompletionsFlow.value = 1
        repo.getAllBadges().test {
            val badges = awaitItem()
            val badge = badges.first { it.definition == BadgeDefinition.FIRST_STEP }
            assertTrue(badge.isUnlocked)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── streak badges ─────────────────────────────────────────────────────────

    @Test
    fun `ON_FIRE badge unlocks at streak 3`() = runTest {
        maxStreakFlow.value = 3
        repo.getAllBadges().test {
            val badge = awaitItem().first { it.definition == BadgeDefinition.ON_FIRE }
            assertTrue(badge.isUnlocked)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WEEK_WARRIOR badge is locked at streak 6`() = runTest {
        maxStreakFlow.value = 6
        repo.getAllBadges().test {
            val badge = awaitItem().first { it.definition == BadgeDefinition.WEEK_WARRIOR }
            assertFalse(badge.isUnlocked)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `WEEK_WARRIOR badge unlocks at streak 7`() = runTest {
        maxStreakFlow.value = 7
        repo.getAllBadges().test {
            val badge = awaitItem().first { it.definition == BadgeDefinition.WEEK_WARRIOR }
            assertTrue(badge.isUnlocked)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── habits created badges ─────────────────────────────────────────────────

    @Test
    fun `HABIT_STARTER badge unlocks at 3 habits created`() = runTest {
        totalHabitCountFlow.value = 3
        repo.getAllBadges().test {
            val badge = awaitItem().first { it.definition == BadgeDefinition.HABIT_STARTER }
            assertTrue(badge.isUnlocked)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── lifetime XP badges ────────────────────────────────────────────────────

    @Test
    fun `CENTURION badge unlocks at 100 lifetime XP`() = runTest {
        totalLifetimeXpFlow.value = 100
        repo.getAllBadges().test {
            val badge = awaitItem().first { it.definition == BadgeDefinition.CENTURION }
            assertTrue(badge.isUnlocked)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `LEGENDARY_GRINDER badge is locked at 999 XP`() = runTest {
        totalLifetimeXpFlow.value = 999
        repo.getAllBadges().test {
            val badge = awaitItem().first { it.definition == BadgeDefinition.LEGENDARY_GRINDER }
            assertFalse(badge.isUnlocked)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getAllBadges total count ───────────────────────────────────────────────

    @Test
    fun `getAllBadges returns one Badge per BadgeDefinition entry`() = runTest {
        repo.getAllBadges().test {
            val badges = awaitItem()
            assertEquals(BadgeDefinition.entries.size, badges.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getEarnedBadgeCount ───────────────────────────────────────────────────

    @Test
    fun `getEarnedBadgeCount returns 0 when no badges are unlocked`() = runTest {
        repo.getEarnedBadgeCount().test {
            assertEquals(0, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getEarnedBadgeCount returns correct count after unlocking some badges`() = runTest {
        totalCompletionsFlow.value = 1  // FIRST_STEP
        maxStreakFlow.value = 3         // ON_FIRE
        totalLifetimeXpFlow.value = 0
        totalHabitCountFlow.value = 0

        repo.getEarnedBadgeCount().test {
            assertEquals(2, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
