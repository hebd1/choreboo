package com.choreboo.app.ui.habits

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.choreboo.app.TestDispatcherRule
import com.choreboo.app.data.datastore.UserPreferences
import com.choreboo.app.data.repository.AuthRepository
import com.choreboo.app.data.repository.HabitRepository
import com.choreboo.app.data.repository.HouseholdRepository
import com.choreboo.app.domain.model.Habit
import com.choreboo.app.domain.model.Household
import com.choreboo.app.domain.model.HouseholdMember
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for [AddEditHabitViewModel]: form state updates, saveHabit validation,
 * calculateSuggestedXp keyword matching, frequency mode switching.
 *
 * Uses MockK for all dependencies. [TestDispatcherRule] replaces Dispatchers.Main
 * so viewModelScope.launch runs synchronously.
 *
 * Note: HabitReminderScheduler is a static utility — we don't verify its calls
 * here because it requires a real Android context. The test context is a relaxed mock.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddEditHabitViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var habitRepository: HabitRepository
    private lateinit var householdRepository: HouseholdRepository
    private lateinit var userPreferences: UserPreferences
    private lateinit var authRepository: AuthRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        savedStateHandle = SavedStateHandle(mapOf("habitId" to -1L))
        habitRepository = mockk(relaxed = true)
        householdRepository = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { userPreferences.profilePhotoUri } returns flowOf(null)
        every { authRepository.currentFirebaseUser } returns null
        every { householdRepository.currentHousehold } returns flowOf(null)
        every { householdRepository.householdMembers } returns flowOf(emptyList())
    }

    private fun createViewModel(habitId: Long = -1L): AddEditHabitViewModel {
        savedStateHandle = SavedStateHandle(mapOf("habitId" to habitId))
        return AddEditHabitViewModel(
            savedStateHandle = savedStateHandle,
            habitRepository = habitRepository,
            householdRepository = householdRepository,
            userPreferences = userPreferences,
            authRepository = authRepository,
            context = context,
        )
    }

    // -----------------------------------------------------------------------
    // Initial state (new habit)
    // -----------------------------------------------------------------------

    @Test
    fun `new habit has blank title and default form state`() {
        val vm = createViewModel()
        val state = vm.formState.value

        assertEquals("", state.title)
        assertEquals("", state.description)
        assertEquals("emoji_salad", state.iconName)
        assertEquals(1, state.difficulty)
        assertEquals(10, state.baseXp)
        assertFalse(state.isEditing)
        assertFalse(state.isHouseholdHabit)
        assertNull(state.assignedToUid)
    }

    @Test
    fun `new habit defaults to all weekly days`() {
        val vm = createViewModel()
        val days = vm.formState.value.customDays

        assertEquals(
            listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"),
            days,
        )
    }

    // -----------------------------------------------------------------------
    // Editing existing habit
    // -----------------------------------------------------------------------

    @Test
    fun `editing habit loads form state from repository`() = runTest {
        val existingHabit = Habit(
            id = 42,
            title = "Morning Run",
            description = "5K every day",
            iconName = "emoji_run",
            customDays = listOf("MON", "WED", "FRI"),
            difficulty = 2,
            baseXp = 25,
            isHouseholdHabit = true,
            assignedToUid = "user-123",
            assignedToName = "Alice",
        )
        every { habitRepository.getHabitById(42L) } returns flowOf(existingHabit)

        val vm = createViewModel(habitId = 42L)
        advanceUntilIdle()

        val state = vm.formState.value
        assertEquals("Morning Run", state.title)
        assertEquals("5K every day", state.description)
        assertEquals("emoji_run", state.iconName)
        assertEquals(listOf("MON", "WED", "FRI"), state.customDays)
        assertEquals(2, state.difficulty)
        assertEquals(25, state.baseXp)
        assertTrue(state.isEditing)
        assertTrue(state.isHouseholdHabit)
        assertEquals("user-123", state.assignedToUid)
    }

    // -----------------------------------------------------------------------
    // Form state updates
    // -----------------------------------------------------------------------

    @Test
    fun `updateTitle changes title`() {
        val vm = createViewModel()
        vm.updateTitle("New Habit")
        assertEquals("New Habit", vm.formState.value.title)
    }

    @Test
    fun `updateDescription changes description`() {
        val vm = createViewModel()
        vm.updateDescription("Do the thing")
        assertEquals("Do the thing", vm.formState.value.description)
    }

    @Test
    fun `updateIconName changes iconName`() {
        val vm = createViewModel()
        vm.updateIconName("emoji_fire")
        assertEquals("emoji_fire", vm.formState.value.iconName)
    }

    @Test
    fun `updateDifficulty coerces to 1-3 range`() {
        val vm = createViewModel()

        vm.updateDifficulty(0)
        assertEquals(1, vm.formState.value.difficulty)

        vm.updateDifficulty(5)
        assertEquals(3, vm.formState.value.difficulty)

        vm.updateDifficulty(2)
        assertEquals(2, vm.formState.value.difficulty)
    }

    @Test
    fun `updateDifficulty sets corresponding baseXp`() {
        val vm = createViewModel()

        vm.updateDifficulty(1)
        assertEquals(10, vm.formState.value.baseXp)

        vm.updateDifficulty(2)
        assertEquals(25, vm.formState.value.baseXp)

        vm.updateDifficulty(3)
        assertEquals(40, vm.formState.value.baseXp)
    }

    @Test
    fun `toggleCustomDay adds day when not present`() {
        val vm = createViewModel()
        // Start with all 7 days, remove MON, then verify
        vm.toggleCustomDay("MON")
        assertFalse(vm.formState.value.customDays.contains("MON"))

        // Toggle it back
        vm.toggleCustomDay("MON")
        assertTrue(vm.formState.value.customDays.contains("MON"))
    }

    @Test
    fun `toggleCustomDay removes day when present`() {
        val vm = createViewModel()
        // All days are present by default
        assertTrue(vm.formState.value.customDays.contains("MON"))

        vm.toggleCustomDay("MON")
        assertFalse(vm.formState.value.customDays.contains("MON"))
    }

    // -----------------------------------------------------------------------
    // Household habit toggle
    // -----------------------------------------------------------------------

    @Test
    fun `updateIsHouseholdHabit clears assignee when turned off`() {
        val vm = createViewModel()
        vm.updateAssignedTo("user-1", "Bob")
        vm.updateIsHouseholdHabit(true)

        // Assignee should persist when household is on
        assertEquals("user-1", vm.formState.value.assignedToUid)

        vm.updateIsHouseholdHabit(false)
        assertNull(vm.formState.value.assignedToUid)
        assertNull(vm.formState.value.assignedToName)
    }

    // -----------------------------------------------------------------------
    // Frequency mode switching
    // -----------------------------------------------------------------------

    @Test
    fun `updateFrequencyMode to MONTHLY defaults to D1 and D15`() {
        val vm = createViewModel()
        assertEquals(FrequencyMode.WEEKLY, vm.formState.value.frequencyMode)

        vm.updateFrequencyMode(FrequencyMode.MONTHLY)

        assertEquals(FrequencyMode.MONTHLY, vm.formState.value.frequencyMode)
        assertEquals(listOf("D1", "D15"), vm.formState.value.customDays)
    }

    @Test
    fun `updateFrequencyMode to WEEKLY defaults to all days`() {
        val vm = createViewModel()
        vm.updateFrequencyMode(FrequencyMode.MONTHLY)

        vm.updateFrequencyMode(FrequencyMode.WEEKLY)

        assertEquals(FrequencyMode.WEEKLY, vm.formState.value.frequencyMode)
        assertEquals(
            listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"),
            vm.formState.value.customDays,
        )
    }

    @Test
    fun `switching to same frequency mode preserves current days`() {
        val vm = createViewModel()
        vm.toggleCustomDay("MON") // remove MON
        val daysBeforeSwitch = vm.formState.value.customDays

        vm.updateFrequencyMode(FrequencyMode.WEEKLY) // same mode

        assertEquals(daysBeforeSwitch, vm.formState.value.customDays)
    }

    @Test
    fun `toggleMonthlyDay adds and removes monthly day`() {
        val vm = createViewModel()
        vm.updateFrequencyMode(FrequencyMode.MONTHLY)
        // Starts with D1, D15

        vm.toggleMonthlyDay(10)
        assertTrue(vm.formState.value.customDays.contains("D10"))

        vm.toggleMonthlyDay(10)
        assertFalse(vm.formState.value.customDays.contains("D10"))
    }

    @Test
    fun `toggleMonthlyDay keeps days sorted`() {
        val vm = createViewModel()
        vm.updateFrequencyMode(FrequencyMode.MONTHLY) // D1, D15

        vm.toggleMonthlyDay(5) // Add D5

        assertEquals(listOf("D1", "D15", "D5"), vm.formState.value.customDays.sorted().let {
            // The actual sort is string sort in the VM
            vm.formState.value.customDays
        })
        // The VM sorts, so D1, D15, D5 → sorted as D1, D15, D5 (string-sort)
        val days = vm.formState.value.customDays
        assertEquals(days, days.sorted())
    }

    // -----------------------------------------------------------------------
    // Reminder
    // -----------------------------------------------------------------------

    @Test
    fun `updateReminderEnabled toggles reminder state`() {
        val vm = createViewModel()
        assertFalse(vm.formState.value.reminderEnabled)

        vm.updateReminderEnabled(true)
        assertTrue(vm.formState.value.reminderEnabled)

        vm.updateReminderEnabled(false)
        assertFalse(vm.formState.value.reminderEnabled)
    }

    // -----------------------------------------------------------------------
    // Save validation
    // -----------------------------------------------------------------------

    @Test
    fun `saveHabit emits ValidationError when title is blank`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.events.test {
            vm.saveHabit()
            val event = awaitItem()
            assertTrue(event is AddEditHabitEvent.ValidationError)
            assertEquals(
                com.choreboo.app.R.string.validation_title_required,
                (event as AddEditHabitEvent.ValidationError).messageResId,
            )
        }

        coVerify(exactly = 0) { habitRepository.upsertHabit(any()) }
    }

    @Test
    fun `saveHabit emits ValidationError when title is whitespace only`() = runTest {
        val vm = createViewModel()
        vm.updateTitle("   ")
        advanceUntilIdle()

        vm.events.test {
            vm.saveHabit()
            val event = awaitItem()
            assertTrue(event is AddEditHabitEvent.ValidationError)
        }
    }

    @Test
    fun `saveHabit emits ValidationError when no days selected`() = runTest {
        val vm = createViewModel()
        vm.updateTitle("Valid Title")
        // Remove all days
        listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").forEach {
            vm.toggleCustomDay(it)
        }
        advanceUntilIdle()

        vm.events.test {
            vm.saveHabit()
            val event = awaitItem()
            assertTrue(event is AddEditHabitEvent.ValidationError)
            assertEquals(
                com.choreboo.app.R.string.validation_select_day,
                (event as AddEditHabitEvent.ValidationError).messageResId,
            )
        }
    }

    @Test
    fun `saveHabit emits ValidationError for household habit when no household`() = runTest {
        val vm = createViewModel()
        vm.updateTitle("Shared Chore")
        vm.updateIsHouseholdHabit(true)
        advanceUntilIdle()

        // No current household set (default mock returns null)
        vm.events.test {
            vm.saveHabit()
            val event = awaitItem()
            assertTrue(event is AddEditHabitEvent.ValidationError)
            assertEquals(
                com.choreboo.app.R.string.validation_join_household,
                (event as AddEditHabitEvent.ValidationError).messageResId,
            )
        }
    }

    @Test
    fun `saveHabit succeeds and emits Saved for valid new habit`() = runTest {
        coEvery { habitRepository.upsertHabit(any()) } returns 1L

        val vm = createViewModel()
        vm.updateTitle("Exercise")
        advanceUntilIdle()

        vm.events.test {
            vm.saveHabit()
            val event = awaitItem()
            assertTrue(event is AddEditHabitEvent.Saved)
            assertTrue((event as AddEditHabitEvent.Saved).isNew)
        }

        coVerify { habitRepository.upsertHabit(match { it.title == "Exercise" }) }
    }

    @Test
    fun `saveHabit sets ownerUid from currentFirebaseUser for new habits`() = runTest {
        val firebaseUser = mockk<com.google.firebase.auth.FirebaseUser>(relaxed = true)
        every { firebaseUser.uid } returns "user-abc"
        every { authRepository.currentFirebaseUser } returns firebaseUser
        val habitSlot = slot<Habit>()
        coEvery { habitRepository.upsertHabit(capture(habitSlot)) } returns 1L

        val vm = createViewModel()
        vm.updateTitle("New Chore")
        advanceUntilIdle()

        vm.saveHabit()
        advanceUntilIdle()

        assertEquals("user-abc", habitSlot.captured.ownerUid)
    }

    @Test
    fun `saveHabit emits Saved with isNew=false when editing`() = runTest {
        val existingHabit = Habit(
            id = 42,
            title = "Morning Run",
            description = null,
            iconName = "emoji_run",
            customDays = listOf("MON", "WED", "FRI"),
            difficulty = 2,
            baseXp = 25,
        )
        every { habitRepository.getHabitById(42L) } returns flowOf(existingHabit)
        coEvery { habitRepository.upsertHabit(any()) } returns 42L

        val vm = createViewModel(habitId = 42L)
        advanceUntilIdle()
        vm.updateTitle("Updated Run")
        advanceUntilIdle()

        vm.events.test {
            vm.saveHabit()
            val event = awaitItem()
            assertTrue(event is AddEditHabitEvent.Saved)
            assertFalse((event as AddEditHabitEvent.Saved).isNew)
        }
    }

    @Test
    fun `saveHabit trims title and description`() = runTest {
        val habitSlot = slot<Habit>()
        coEvery { habitRepository.upsertHabit(capture(habitSlot)) } returns 1L

        val vm = createViewModel()
        vm.updateTitle("  Exercise  ")
        vm.updateDescription("  Daily workout  ")
        advanceUntilIdle()

        vm.saveHabit()
        advanceUntilIdle()

        assertEquals("Exercise", habitSlot.captured.title)
        assertEquals("Daily workout", habitSlot.captured.description)
    }

    @Test
    fun `saveHabit sets description to null when blank`() = runTest {
        val habitSlot = slot<Habit>()
        coEvery { habitRepository.upsertHabit(capture(habitSlot)) } returns 1L

        val vm = createViewModel()
        vm.updateTitle("Exercise")
        vm.updateDescription("   ")
        advanceUntilIdle()

        vm.saveHabit()
        advanceUntilIdle()

        assertNull(habitSlot.captured.description)
    }

    @Test
    fun `saveHabit resolves householdId from currentHousehold`() = runTest {
        val household = Household(
            id = "hh-uuid",
            name = "Test House",
            inviteCode = "ABC123",
            createdByUid = "owner",
        )
        val householdFlow = MutableStateFlow<Household?>(household)
        every { householdRepository.currentHousehold } returns householdFlow

        val habitSlot = slot<Habit>()
        coEvery { habitRepository.upsertHabit(capture(habitSlot)) } returns 1L

        val vm = createViewModel()
        vm.updateTitle("Shared Chore")
        vm.updateIsHouseholdHabit(true)
        advanceUntilIdle()

        // Subscribe to currentHousehold so stateIn(WhileSubscribed) activates
        vm.currentHousehold.test {
            assertEquals(household, awaitItem())

            vm.saveHabit()
            advanceUntilIdle()

            assertEquals("hh-uuid", habitSlot.captured.householdId)
            assertTrue(habitSlot.captured.isHouseholdHabit)
        }
    }

    @Test
    fun `saveHabit clears householdId when not household habit`() = runTest {
        val habitSlot = slot<Habit>()
        coEvery { habitRepository.upsertHabit(capture(habitSlot)) } returns 1L

        val vm = createViewModel()
        vm.updateTitle("Personal Habit")
        vm.updateIsHouseholdHabit(false)
        advanceUntilIdle()

        vm.saveHabit()
        advanceUntilIdle()

        assertNull(habitSlot.captured.householdId)
        assertFalse(habitSlot.captured.isHouseholdHabit)
    }

    // -----------------------------------------------------------------------
    // calculateSuggestedXp (tested via applySuggestedXp trigger)
    // -----------------------------------------------------------------------

    @Test
    fun `suggestedXp is 35 for high-effort keywords`() = runTest {
        val vm = createViewModel()
        vm.updateTitle("Morning workout")
        advanceUntilIdle()

        // The debounce-based init watcher should have populated suggestedXp
        assertEquals(35, vm.formState.value.suggestedXp)
    }

    @Test
    fun `suggestedXp is 20 for medium-effort keywords`() = runTest {
        val vm = createViewModel()
        vm.updateTitle("Read a book")
        advanceUntilIdle()

        assertEquals(20, vm.formState.value.suggestedXp)
    }

    @Test
    fun `suggestedXp is 10 for low-effort keywords`() = runTest {
        val vm = createViewModel()
        vm.updateTitle("Drink water")
        advanceUntilIdle()

        assertEquals(10, vm.formState.value.suggestedXp)
    }

    @Test
    fun `suggestedXp is 15 for unrecognized title`() = runTest {
        val vm = createViewModel()
        vm.updateTitle("Do something random")
        advanceUntilIdle()

        assertEquals(15, vm.formState.value.suggestedXp)
    }

    @Test
    fun `suggestedXp matches description keywords too`() = runTest {
        val vm = createViewModel()
        vm.updateTitle("Daily task")
        vm.updateDescription("Go to the gym and train")
        advanceUntilIdle()

        // "gym" and "train" are high-effort keywords
        assertEquals(35, vm.formState.value.suggestedXp)
    }

    @Test
    fun `applySuggestedXp updates baseXp from suggestedXp`() = runTest {
        val vm = createViewModel()
        vm.updateTitle("Morning workout")
        advanceUntilIdle()
        assertEquals(35, vm.formState.value.suggestedXp)

        vm.applySuggestedXp()

        assertEquals(35, vm.formState.value.baseXp)
    }

    @Test
    fun `applySuggestedXp does nothing when suggestedXp is null`() = runTest {
        val vm = createViewModel()
        // Don't advance — suggestedXp hasn't been set yet by the debounce watcher
        // Manually verify it's null at this point
        assertNull(vm.formState.value.suggestedXp)

        val originalXp = vm.formState.value.baseXp
        vm.applySuggestedXp()

        // suggestedXp is null → applySuggestedXp is a no-op
        assertEquals(originalXp, vm.formState.value.baseXp)
    }
}
