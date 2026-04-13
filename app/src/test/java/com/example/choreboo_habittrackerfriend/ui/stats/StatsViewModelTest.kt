package com.example.choreboo_habittrackerfriend.ui.stats

import app.cash.turbine.test
import com.example.choreboo_habittrackerfriend.TestDispatcherRule
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.local.entity.HabitLogEntity
import com.example.choreboo_habittrackerfriend.data.repository.AuthRepository
import com.example.choreboo_habittrackerfriend.data.repository.BadgeRepository
import com.example.choreboo_habittrackerfriend.data.repository.ChorebooRepository
import com.example.choreboo_habittrackerfriend.data.repository.HabitRepository
import com.example.choreboo_habittrackerfriend.data.repository.SyncManager
import com.example.choreboo_habittrackerfriend.domain.model.Badge
import com.example.choreboo_habittrackerfriend.domain.model.BadgeCategory
import com.example.choreboo_habittrackerfriend.domain.model.BadgeDefinition
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek

/**
 * Unit tests for [StatsViewModel].
 *
 * Covers: initial loading state, badge count, streak aggregation, XP summation,
 * pull-to-refresh flow, and refreshTodayDate.
 */
class StatsViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var habitRepository: HabitRepository
    private lateinit var chorebooRepository: ChorebooRepository
    private lateinit var userPreferences: UserPreferences
    private lateinit var authRepository: AuthRepository
    private lateinit var badgeRepository: BadgeRepository
    private lateinit var syncManager: SyncManager

    // Backing flows for mocked repositories
    private val totalPointsFlow = MutableStateFlow(0)
    private val totalLifetimeXpFlow = MutableStateFlow(0)
    private val badgesFlow = MutableStateFlow<List<Badge>>(emptyList())
    private val streaksFlow = MutableStateFlow<Map<Long, Int>>(emptyMap())
    private val weeklyDaysFlow = MutableStateFlow<Set<DayOfWeek>>(emptySet())
    private val logsFlow = MutableStateFlow<List<HabitLogEntity>>(emptyList())
    private val habitsFlow = MutableStateFlow<List<com.example.choreboo_habittrackerfriend.domain.model.Habit>>(emptyList())

    private fun createViewModel() = StatsViewModel(
        habitRepository = habitRepository,
        chorebooRepository = chorebooRepository,
        userPreferences = userPreferences,
        authRepository = authRepository,
        badgeRepository = badgeRepository,
        syncManager = syncManager,
    )

    private fun badge(definition: BadgeDefinition, unlocked: Boolean) =
        Badge(definition = definition, isUnlocked = unlocked)

    @Before
    fun setUp() {
        habitRepository = mockk(relaxed = true)
        chorebooRepository = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        badgeRepository = mockk(relaxed = true)
        syncManager = mockk(relaxed = true)

        every { userPreferences.totalPoints } returns totalPointsFlow
        every { userPreferences.totalLifetimeXp } returns totalLifetimeXpFlow
        every { badgeRepository.getAllBadges() } returns badgesFlow
        every { badgeRepository.getEarnedBadgeCount() } returns flowOf(0)
        every { habitRepository.getStreaksForToday() } returns streaksFlow
        every { habitRepository.getCompletionDaysForCurrentWeek() } returns weeklyDaysFlow
        every { habitRepository.getLogsForDate(any()) } returns logsFlow
        every { habitRepository.getLogsForMonth(any()) } returns flowOf(emptyList())
        every { habitRepository.getAllHabits() } returns habitsFlow
        every { chorebooRepository.getChoreboo() } returns flowOf(null)
        every { authRepository.currentUser } returns flowOf(null)
        every { authRepository.currentFirebaseUser } returns null
        every { userPreferences.profilePhotoUri } returns flowOf(null)
    }

    // ── totalPoints ──────────────────────────────────────────────────────────

    @Test
    fun `totalPoints reflects userPreferences value`() = runTest {
        totalPointsFlow.value = 250
        val vm = createViewModel()

        vm.totalPoints.test {
            assertEquals(250, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── earnedBadgeCount ─────────────────────────────────────────────────────

    @Test
    fun `earnedBadgeCount reflects badgeRepository value`() = runTest {
        every { badgeRepository.getEarnedBadgeCount() } returns flowOf(4)
        val vm = createViewModel()

        vm.earnedBadgeCount.test {
            assertEquals(4, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── recentBadges ─────────────────────────────────────────────────────────

    @Test
    fun `recentBadges returns at most 3 unlocked badges sorted by threshold descending`() = runTest {
        badgesFlow.value = BadgeDefinition.entries.map { badge(it, unlocked = true) }
        val vm = createViewModel()

        vm.recentBadges.test {
            val recent = awaitItem()
            assertTrue(recent.size <= 3)
            // Verify descending threshold sort
            if (recent.size >= 2) {
                assertTrue(recent[0].definition.threshold >= recent[1].definition.threshold)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `recentBadges is empty when no badges are unlocked`() = runTest {
        badgesFlow.value = BadgeDefinition.entries.map { badge(it, unlocked = false) }
        val vm = createViewModel()

        vm.recentBadges.test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── maxStreak ────────────────────────────────────────────────────────────

    @Test
    fun `maxStreak returns max value from streaks map`() = runTest {
        streaksFlow.value = mapOf(1L to 5, 2L to 12, 3L to 3)
        val vm = createViewModel()

        vm.maxStreak.test {
            assertEquals(12, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `maxStreak is 0 when streaks map is empty`() = runTest {
        streaksFlow.value = emptyMap()
        val vm = createViewModel()

        vm.maxStreak.test {
            assertEquals(0, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── todayXp ──────────────────────────────────────────────────────────────

    @Test
    fun `todayXp sums xpEarned from today logs`() = runTest {
        logsFlow.value = listOf(
            HabitLogEntity(id = 1, habitId = 1L, completedAt = 0L, date = "2026-01-15", xpEarned = 20, streakAtCompletion = 1, completedByUid = "uid"),
            HabitLogEntity(id = 2, habitId = 2L, completedAt = 0L, date = "2026-01-15", xpEarned = 15, streakAtCompletion = 0, completedByUid = "uid"),
        )
        val vm = createViewModel()

        vm.todayXp.test {
            assertEquals(35, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── isRefreshing ─────────────────────────────────────────────────────────

    @Test
    fun `isRefreshing is false initially`() {
        val vm = createViewModel()
        assertFalse(vm.isRefreshing.value)
    }

    @Test
    fun `refreshData calls syncManager syncAll with force=true`() = runTest {
        val vm = createViewModel()
        vm.refreshData()
        coVerify { syncManager.syncAll(force = true) }
        assertFalse(vm.isRefreshing.value)
    }

    // ── isLoading ────────────────────────────────────────────────────────────

    @Test
    fun `isLoading becomes false after first badge emission`() = runTest {
        badgesFlow.value = emptyList()
        val vm = createViewModel()

        vm.isLoading.test {
            // With UnconfinedTestDispatcher, the init block runs immediately and
            // allBadges.first() resolves on the already-emitted empty list → isLoading=false
            val value = awaitItem()
            assertFalse(value)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
