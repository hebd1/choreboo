package com.choreboo.app.ui.calendar

import app.cash.turbine.test
import com.choreboo.app.TestDispatcherRule
import com.choreboo.app.data.datastore.UserPreferences
import com.choreboo.app.data.local.dao.HabitLogWithName
import com.choreboo.app.data.repository.AuthRepository
import com.choreboo.app.data.repository.HabitRepository
import com.choreboo.app.data.repository.SyncManager
import com.choreboo.app.domain.model.HabitLog
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * Tests for [CalendarViewModel]: month navigation, date selection, and completion
 * map derivation (including malformed date safety).
 *
 * Uses MockK for all dependencies. [TestDispatcherRule] replaces Dispatchers.Main
 * so viewModelScope.launch runs synchronously.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var habitRepository: HabitRepository
    private lateinit var userPreferences: UserPreferences
    private lateinit var authRepository: AuthRepository
    private lateinit var syncManager: SyncManager

    private val monthLogFlow = MutableStateFlow<List<HabitLog>>(emptyList())

    @Before
    fun setUp() {
        habitRepository = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        syncManager = mockk(relaxed = true)

        // Default stubs
        every { userPreferences.totalPoints } returns flowOf(0)
        every { userPreferences.profilePhotoUri } returns flowOf(null)
        every { authRepository.currentFirebaseUser } returns null

        // getLogsForMonth — default to the mutable flow so tests can push values
        every { habitRepository.getLogsForMonth(any()) } returns monthLogFlow
        // getLogsWithNamesForDate — default to empty
        every { habitRepository.getLogsWithNamesForDate(any()) } returns flowOf(emptyList())
    }

    private fun createViewModel() = CalendarViewModel(
        habitRepository = habitRepository,
        userPreferences = userPreferences,
        authRepository = authRepository,
        syncManager = syncManager,
    )

    // -----------------------------------------------------------------------
    // Initial state
    // -----------------------------------------------------------------------

    @Test
    fun `initial selectedMonth is current month`() {
        val vm = createViewModel()
        assertEquals(YearMonth.now(), vm.selectedMonth.value)
    }

    @Test
    fun `initial selectedDate is today`() {
        val vm = createViewModel()
        assertEquals(LocalDate.now(), vm.selectedDate.value)
    }

    @Test
    fun `initial completionsForMonth is empty`() {
        val vm = createViewModel()
        assertTrue(vm.completionsForMonth.value.isEmpty())
    }

    // -----------------------------------------------------------------------
    // Month navigation
    // -----------------------------------------------------------------------

    @Test
    fun `previousMonth decrements month by 1`() {
        val vm = createViewModel()
        val original = vm.selectedMonth.value

        vm.previousMonth()

        assertEquals(original.minusMonths(1), vm.selectedMonth.value)
    }

    @Test
    fun `previousMonth clears selected date`() {
        val vm = createViewModel()
        vm.selectDate(LocalDate.of(2026, 3, 15))

        vm.previousMonth()

        assertNull(vm.selectedDate.value)
    }

    @Test
    fun `nextMonth increments month by 1`() {
        val vm = createViewModel()
        val original = vm.selectedMonth.value

        vm.nextMonth()

        assertEquals(original.plusMonths(1), vm.selectedMonth.value)
    }

    @Test
    fun `nextMonth clears selected date`() {
        val vm = createViewModel()
        vm.selectDate(LocalDate.of(2026, 3, 15))

        vm.nextMonth()

        assertNull(vm.selectedDate.value)
    }

    @Test
    fun `multiple month navigations accumulate correctly`() {
        val vm = createViewModel()
        val original = vm.selectedMonth.value

        vm.nextMonth()
        vm.nextMonth()
        vm.previousMonth()

        assertEquals(original.plusMonths(1), vm.selectedMonth.value)
    }

    // -----------------------------------------------------------------------
    // Date selection
    // -----------------------------------------------------------------------

    @Test
    fun `selectDate sets new date`() {
        val vm = createViewModel()
        // Use a fixed past date that will never match LocalDate.now()
        val target = LocalDate.of(2020, 1, 1)

        vm.selectDate(target)

        assertEquals(target, vm.selectedDate.value)
    }

    @Test
    fun `selectDate toggles off when same date selected again`() {
        val vm = createViewModel()
        // Use a fixed past date that will never match LocalDate.now()
        val target = LocalDate.of(2020, 1, 1)

        vm.selectDate(target)
        vm.selectDate(target)

        assertNull(vm.selectedDate.value)
    }

    @Test
    fun `selectDate changes to new date without toggle`() {
        val vm = createViewModel()
        val first = LocalDate.of(2026, 4, 15)
        val second = LocalDate.of(2026, 4, 20)

        vm.selectDate(first)
        vm.selectDate(second)

        assertEquals(second, vm.selectedDate.value)
    }

    // -----------------------------------------------------------------------
    // completionsForMonth derivation
    // -----------------------------------------------------------------------

    @Test
    fun `completionsForMonth groups logs by date`() = runTest {
        val vm = createViewModel()

        vm.completionsForMonth.test {
            // Initial empty
            assertEquals(emptyMap<LocalDate, Int>(), awaitItem())

            // Push logs
            monthLogFlow.value = listOf(
                HabitLog(id = 1, habitId = 1, date = "2026-04-01", xpEarned = 10, streakAtCompletion = 0, completedByUid = null),
                HabitLog(id = 2, habitId = 2, date = "2026-04-01", xpEarned = 20, streakAtCompletion = 0, completedByUid = null),
                HabitLog(id = 3, habitId = 1, date = "2026-04-03", xpEarned = 10, streakAtCompletion = 0, completedByUid = null),
            )

            val completions = awaitItem()
            assertEquals(2, completions[LocalDate.of(2026, 4, 1)])
            assertEquals(1, completions[LocalDate.of(2026, 4, 3)])
            assertEquals(2, completions.size)
        }
    }

    @Test
    fun `completionsForMonth skips malformed dates`() = runTest {
        val vm = createViewModel()

        vm.completionsForMonth.test {
            awaitItem() // initial empty

            monthLogFlow.value = listOf(
                HabitLog(id = 1, habitId = 1, date = "2026-04-01", xpEarned = 10, streakAtCompletion = 0, completedByUid = null),
                HabitLog(id = 2, habitId = 1, date = "not-a-date", xpEarned = 10, streakAtCompletion = 0, completedByUid = null),
                HabitLog(id = 3, habitId = 1, date = "", xpEarned = 10, streakAtCompletion = 0, completedByUid = null),
                HabitLog(id = 4, habitId = 1, date = "2026-04-02", xpEarned = 10, streakAtCompletion = 0, completedByUid = null),
            )

            val completions = awaitItem()
            // Only the two valid dates should appear
            assertEquals(2, completions.size)
            assertEquals(1, completions[LocalDate.of(2026, 4, 1)])
            assertEquals(1, completions[LocalDate.of(2026, 4, 2)])
        }
    }

    @Test
    fun `completionsForMonth handles empty logs`() = runTest {
        val vm = createViewModel()

        vm.completionsForMonth.test {
            val initial = awaitItem()
            assertTrue(initial.isEmpty())
        }
    }

    // -----------------------------------------------------------------------
    // selectedDateLogs derivation
    // -----------------------------------------------------------------------

    @Test
    fun `selectedDateLogs returns empty when no date selected`() = runTest {
        val vm = createViewModel()

        // Deselect any date
        vm.selectDate(LocalDate.now())
        vm.selectDate(LocalDate.now()) // toggle off

        vm.selectedDateLogs.test {
            val logs = awaitItem()
            assertTrue(logs.isEmpty())
        }
    }

    @Test
    fun `selectedDateLogs returns logs for selected date`() = runTest {
        val testDate = LocalDate.of(2026, 1, 15)
        val logsForDate = listOf(
            HabitLogWithName(
                id = 1,
                habitId = 1,
                completedAt = 1000L,
                date = "2026-01-15",
                xpEarned = 10,
                streakAtCompletion = 3,
                habitTitle = "Exercise",
                habitIcon = "emoji_run",
            ),
        )

        every { habitRepository.getLogsWithNamesForDate("2026-01-15") } returns flowOf(logsForDate)

        val vm = createViewModel()
        vm.selectDate(testDate)

        vm.selectedDateLogs.test {
            val logs = awaitItem()
            assertEquals(1, logs.size)
            assertEquals("Exercise", logs[0].habitTitle)
        }
    }

    // -----------------------------------------------------------------------
    // totalPoints and profilePhotoUri passthrough
    // -----------------------------------------------------------------------

    @Test
    fun `totalPoints reflects userPreferences value`() = runTest {
        every { userPreferences.totalPoints } returns flowOf(42)

        val vm = createViewModel()

        vm.totalPoints.test {
            assertEquals(42, awaitItem())
        }
    }

    @Test
    fun `profilePhotoUri reflects userPreferences value`() = runTest {
        every { userPreferences.profilePhotoUri } returns flowOf("content://photo/1")

        val vm = createViewModel()

        vm.profilePhotoUri.test {
            assertEquals("content://photo/1", awaitItem())
        }
    }
}
