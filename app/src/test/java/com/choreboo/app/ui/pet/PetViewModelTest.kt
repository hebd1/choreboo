package com.choreboo.app.ui.pet

import app.cash.turbine.test
import com.choreboo.app.TestDispatcherRule
import com.choreboo.app.data.datastore.UserPreferences
import com.choreboo.app.data.repository.AuthRepository
import com.choreboo.app.domain.model.HabitLog
import com.choreboo.app.data.repository.BackgroundRepository
import com.choreboo.app.data.repository.BillingRepository
import com.choreboo.app.data.repository.ChorebooRepository
import com.choreboo.app.data.repository.CompletionResult
import com.choreboo.app.data.repository.HabitRepository
import com.choreboo.app.data.repository.HouseholdRepository
import com.choreboo.app.data.repository.SyncManager
import com.choreboo.app.data.repository.UserRepository
import com.choreboo.app.data.repository.XpResult
import com.choreboo.app.domain.model.ChorebooMood
import com.choreboo.app.domain.model.ChorebooStage
import com.choreboo.app.domain.model.ChorebooStats
import com.choreboo.app.domain.model.Habit
import com.choreboo.app.domain.model.HouseholdMember
import com.choreboo.app.domain.model.PetType
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for [PetViewModel]: feeding, sleeping, habit completion,
 * event emission, and state derivation.
 *
 * Uses MockK for all dependencies. [TestDispatcherRule] replaces Dispatchers.Main
 * so viewModelScope.launch runs synchronously.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PetViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var chorebooRepository: ChorebooRepository
    private lateinit var habitRepository: HabitRepository
    private lateinit var userPreferences: UserPreferences
    private lateinit var authRepository: AuthRepository
    private lateinit var householdRepository: HouseholdRepository
    private lateinit var syncManager: SyncManager
    private lateinit var userRepository: UserRepository
    private lateinit var backgroundRepository: BackgroundRepository
    private lateinit var billingRepository: BillingRepository

    private val chorebooFlow = MutableStateFlow<ChorebooStats?>(null)
    private val habitsFlow = MutableStateFlow<List<Habit>>(emptyList())
    private val logsForDateFlow = MutableStateFlow<List<HabitLog>>(emptyList())

    private val defaultChoreboo = ChorebooStats(
        id = 1,
        name = "TestBoo",
        stage = ChorebooStage.BABY,
        level = 3,
        xp = 20,
        hunger = 80,
        happiness = 70,
        energy = 60,
        petType = PetType.FOX,
        lastInteractionAt = System.currentTimeMillis(),
        createdAt = System.currentTimeMillis(),
        sleepUntil = 0,
    )

    @Before
    fun setUp() {
        chorebooRepository = mockk(relaxed = true)
        habitRepository = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        householdRepository = mockk(relaxed = true)
        syncManager = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        backgroundRepository = mockk(relaxed = true)
        billingRepository = mockk(relaxed = true)

        // Default stubs
        every { chorebooRepository.getChoreboo() } returns chorebooFlow
        every { habitRepository.getHabitsForUser(any()) } returns habitsFlow
        every { habitRepository.getLogsForDate(any()) } returns logsForDateFlow
        every { habitRepository.getStreaksForToday() } returns flowOf(emptyMap())
        every { userPreferences.totalPoints } returns flowOf(100)
        every { userPreferences.totalLifetimeXp } returns flowOf(500)
        every { userPreferences.profilePhotoUri } returns flowOf(null)
        every { authRepository.currentFirebaseUser } returns null
        every { householdRepository.householdMembers } returns flowOf(emptyList())
        every { backgroundRepository.getPurchasedBackgrounds(any()) } returns flowOf(emptyList())
        every { billingRepository.isPremium } returns MutableStateFlow(false)

        // Mock current user for habits filtering
        val currentUser = mockk<FirebaseUser>()
        every { currentUser.uid } returns "test-uid"
        every { authRepository.currentUser } returns flowOf(currentUser)

        // init block calls
        coEvery { chorebooRepository.ensureActiveChoreboo() } returns defaultChoreboo
        coEvery { chorebooRepository.applyStatDecay() } returns Unit
    }

    private fun createViewModel() = PetViewModel(
        chorebooRepository = chorebooRepository,
        habitRepository = habitRepository,
        userPreferences = userPreferences,
        authRepository = authRepository,
        householdRepository = householdRepository,
        syncManager = syncManager,
        userRepository = userRepository,
        backgroundRepository = backgroundRepository,
        billingRepository = billingRepository,
    )

    // -----------------------------------------------------------------------
    // Init
    // -----------------------------------------------------------------------

    @Test
    fun `init calls ensureActiveChoreboo and applyStatDecay`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coVerify(exactly = 1) { chorebooRepository.ensureActiveChoreboo() }
        coVerify(exactly = 1) { chorebooRepository.applyStatDecay() }
    }

    // -----------------------------------------------------------------------
    // Choreboo state derivation
    // -----------------------------------------------------------------------

    @Test
    fun `chorebooState reflects upstream flow`() = runTest {
        chorebooFlow.value = defaultChoreboo
        val vm = createViewModel()

        vm.chorebooState.test {
            assertEquals(defaultChoreboo, awaitItem())
        }
    }

    @Test
    fun `currentMood defaults to IDLE when no choreboo`() = runTest {
        val vm = createViewModel()

        vm.currentMood.test {
            assertEquals(ChorebooMood.IDLE, awaitItem())
        }
    }

    @Test
    fun `currentMood reflects choreboo mood`() = runTest {
        chorebooFlow.value = defaultChoreboo.copy(hunger = 80, happiness = 80, energy = 80)
        val vm = createViewModel()

        vm.currentMood.test {
            assertEquals(ChorebooMood.HAPPY, awaitItem())
        }
    }

    @Test
    fun `isSleeping defaults to false`() = runTest {
        val vm = createViewModel()

        vm.isSleeping.test {
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `petType defaults to FOX when no choreboo`() = runTest {
        val vm = createViewModel()

        vm.petType.test {
            assertEquals(PetType.FOX, awaitItem())
        }
    }

    // -----------------------------------------------------------------------
    // Feed
    // -----------------------------------------------------------------------

    @Test
    fun `feedChoreboo emits Fed when enough points`() = runTest {
        coEvery { userPreferences.deductPoints(10) } returns true

        val vm = createViewModel()
        advanceUntilIdle()

        vm.events.test {
            vm.feedChoreboo()
            val event = awaitItem()
            assertTrue(event is PetEvent.Fed)
        }

        coVerify { chorebooRepository.feedChoreboo() }
    }

    @Test
    fun `feedChoreboo sets isEating to true`() = runTest {
        coEvery { userPreferences.deductPoints(10) } returns true

        val vm = createViewModel()
        advanceUntilIdle()

        vm.feedChoreboo()
        advanceUntilIdle()

        assertTrue(vm.isEating.value)
    }

    @Test
    fun `feedChoreboo emits InsufficientPoints when not enough points`() = runTest {
        coEvery { userPreferences.deductPoints(10) } returns false

        val vm = createViewModel()
        advanceUntilIdle()

        vm.events.test {
            vm.feedChoreboo()
            val event = awaitItem()
            assertTrue(event is PetEvent.InsufficientPoints)
        }

        coVerify(exactly = 0) { chorebooRepository.feedChoreboo() }
    }

    @Test
    fun `onEatingAnimationComplete resets isEating to false`() = runTest {
        coEvery { userPreferences.deductPoints(10) } returns true

        val vm = createViewModel()
        advanceUntilIdle()

        vm.feedChoreboo()
        advanceUntilIdle()
        assertTrue(vm.isEating.value)

        vm.onEatingAnimationComplete()
        assertFalse(vm.isEating.value)
    }

    // -----------------------------------------------------------------------
    // Sleep
    // -----------------------------------------------------------------------

    @Test
    fun `sleepChoreboo emits Sleeping when pet is awake`() = runTest {
        coEvery { chorebooRepository.getChorebooSync() } returns defaultChoreboo.copy(sleepUntil = 0)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.events.test {
            vm.sleepChoreboo()
            val event = awaitItem()
            assertTrue(event is PetEvent.Sleeping)
        }

        coVerify { chorebooRepository.putToSleep() }
    }

    @Test
    fun `sleepChoreboo emits AlreadySleeping when pet is sleeping`() = runTest {
        val futureSleepUntil = System.currentTimeMillis() + 1_000_000
        coEvery { chorebooRepository.getChorebooSync() } returns defaultChoreboo.copy(
            sleepUntil = futureSleepUntil,
        )

        val vm = createViewModel()
        advanceUntilIdle()

        vm.events.test {
            vm.sleepChoreboo()
            val event = awaitItem()
            assertTrue(event is PetEvent.AlreadySleeping)
        }

        coVerify(exactly = 0) { chorebooRepository.putToSleep() }
    }

    // -----------------------------------------------------------------------
    // Complete habit
    // -----------------------------------------------------------------------

    @Test
    fun `completeHabit emits AlreadyComplete when habit was already done`() = runTest {
        coEvery { habitRepository.completeHabit(1L) } returns CompletionResult(
            xpEarned = 0,
            newStreak = 0,
            alreadyComplete = true,
        )

        val vm = createViewModel()
        advanceUntilIdle()

        vm.events.test {
            vm.completeHabit(1L)
            val event = awaitItem()
            assertTrue(event is PetEvent.AlreadyComplete)
            assertEquals(1L, (event as PetEvent.AlreadyComplete).habitId)
        }

        // addXp should NOT be called
        coVerify(exactly = 0) { chorebooRepository.addXp(any()) }
    }

    @Test
    fun `completeHabit emits HabitCompleted with XP details`() = runTest {
        coEvery { habitRepository.completeHabit(1L) } returns CompletionResult(
            xpEarned = 25,
            newStreak = 3,
            alreadyComplete = false,
        )
        coEvery { chorebooRepository.addXp(25) } returns XpResult(
            levelsGained = 0,
            newLevel = 5,
            evolved = false,
            newStage = null,
        )

        val vm = createViewModel()
        advanceUntilIdle()

        vm.events.test {
            vm.completeHabit(1L)
            val event = awaitItem()
            assertTrue(event is PetEvent.HabitCompleted)
            val completed = event as PetEvent.HabitCompleted
            assertEquals(1L, completed.habitId)
            assertEquals(25, completed.xpEarned)
            assertEquals(3, completed.streak)
            assertFalse(completed.leveledUp)
            assertFalse(completed.evolved)
        }
    }

    @Test
    fun `completeHabit emits HabitCompleted with leveledUp when levels gained`() = runTest {
        coEvery { habitRepository.completeHabit(1L) } returns CompletionResult(
            xpEarned = 40,
            newStreak = 5,
            alreadyComplete = false,
        )
        coEvery { chorebooRepository.addXp(40) } returns XpResult(
            levelsGained = 1,
            newLevel = 6,
            evolved = false,
            newStage = null,
        )

        val vm = createViewModel()
        advanceUntilIdle()

        vm.events.test {
            vm.completeHabit(1L)
            val event = awaitItem() as PetEvent.HabitCompleted
            assertEquals(1L, event.habitId)
            assertTrue(event.leveledUp)
            assertEquals(6, event.newLevel)
            assertFalse(event.evolved)
        }
    }

    @Test
    fun `completeHabit emits HabitCompleted with evolved when stage changes`() = runTest {
        coEvery { habitRepository.completeHabit(1L) } returns CompletionResult(
            xpEarned = 40,
            newStreak = 10,
            alreadyComplete = false,
        )
        coEvery { chorebooRepository.addXp(40) } returns XpResult(
            levelsGained = 2,
            newLevel = 8,
            evolved = true,
            newStage = ChorebooStage.CHILD,
        )

        val vm = createViewModel()
        advanceUntilIdle()

        vm.events.test {
            vm.completeHabit(1L)
            val event = awaitItem() as PetEvent.HabitCompleted
            assertEquals(1L, event.habitId)
            assertTrue(event.leveledUp)
            assertTrue(event.evolved)
            assertEquals(ChorebooStage.CHILD, event.newStage)
        }
    }

    @Test
    fun `completeHabit emits CompletionError with habitId when repository throws`() = runTest {
        coEvery { habitRepository.completeHabit(7L) } throws IllegalStateException("boom")

        val vm = createViewModel()
        advanceUntilIdle()

        vm.events.test {
            vm.completeHabit(7L)
            val event = awaitItem()
            assertTrue(event is PetEvent.CompletionError)
            assertEquals(7L, (event as PetEvent.CompletionError).habitId)
        }
    }

    @Test
    fun `completeHabit calls autoFeedIfNeeded after adding XP`() = runTest {
        coEvery { habitRepository.completeHabit(1L) } returns CompletionResult(
            xpEarned = 10,
            newStreak = 1,
            alreadyComplete = false,
        )
        coEvery { chorebooRepository.addXp(10) } returns XpResult(newLevel = 3)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.completeHabit(1L)
        advanceUntilIdle()

        coVerify { chorebooRepository.autoFeedIfNeeded(userPreferences) }
    }

    // -----------------------------------------------------------------------
    // Delete habit
    // -----------------------------------------------------------------------

    @Test
    fun `deleteHabit delegates to repository`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.deleteHabit(42L)
        advanceUntilIdle()

        coVerify { habitRepository.deleteHabit(42L) }
    }

    // -----------------------------------------------------------------------
    // Habits and completions state
    // -----------------------------------------------------------------------

    @Test
    fun `habits reflects repository flow`() = runTest {
        val testHabits = listOf(
            Habit(id = 1, title = "Exercise"),
            Habit(id = 2, title = "Read"),
        )
        habitsFlow.value = testHabits

        val vm = createViewModel()

        vm.habits.test {
            val habits = awaitItem()
            assertEquals(2, habits.size)
            assertEquals("Exercise", habits[0].title)
        }
    }

    @Test
    fun `habits sorts completed habits to the bottom`() = runTest {
        // Three habits: ids 1, 2, 3 in DAO order
        habitsFlow.value = listOf(
            Habit(id = 1, title = "First"),
            Habit(id = 2, title = "Second"),
            Habit(id = 3, title = "Third"),
        )
        // Habit 2 has a completion today — it should sink to the bottom
        logsForDateFlow.value = listOf(
            HabitLog(id = 1, habitId = 2, date = "2026-04-14", xpEarned = 10, streakAtCompletion = 0, completedByUid = null),
        )

        val vm = createViewModel()

        vm.habits.test {
            val habits = awaitItem()
            assertEquals(3, habits.size)
            // Incomplete habits come first (original order preserved), completed last
            assertEquals(1L, habits[0].id)
            assertEquals(3L, habits[1].id)
            assertEquals(2L, habits[2].id)
        }
    }

    @Test
    fun `todayCompletions groups logs by habitId`() = runTest {
        logsForDateFlow.value = listOf(
            HabitLog(id = 1, habitId = 10, date = "2026-04-04", xpEarned = 10, streakAtCompletion = 0, completedByUid = null),
            HabitLog(id = 2, habitId = 10, date = "2026-04-04", xpEarned = 10, streakAtCompletion = 0, completedByUid = null),
            HabitLog(id = 3, habitId = 20, date = "2026-04-04", xpEarned = 15, streakAtCompletion = 0, completedByUid = null),
        )

        val vm = createViewModel()

        vm.todayCompletions.test {
            val completions = awaitItem()
            assertEquals(2, completions[10L])
            assertEquals(1, completions[20L])
        }
    }

    // -----------------------------------------------------------------------
    // Household completer names
    // -----------------------------------------------------------------------

    @Test
    fun `householdCompleterNames maps other users completions to names`() = runTest {
        val currentUser = mockk<FirebaseUser>()
        every { currentUser.uid } returns "my-uid"
        every { currentUser.photoUrl } returns null
        every { authRepository.currentFirebaseUser } returns currentUser

        logsForDateFlow.value = listOf(
            HabitLog(
                id = 1,
                habitId = 10,
                date = "2026-04-04",
                xpEarned = 10,
                streakAtCompletion = 0,
                completedByUid = "other-uid",
            ),
        )
        every { householdRepository.householdMembers } returns flowOf(
            listOf(
                HouseholdMember(uid = "other-uid", displayName = "Alice"),
            ),
        )

        val vm = createViewModel()

        vm.householdCompleterNames.test {
            val names = awaitItem()
            assertEquals("Alice", names[10L])
        }
    }

    @Test
    fun `householdCompleterNames excludes current user completions`() = runTest {
        val currentUser = mockk<FirebaseUser>()
        every { currentUser.uid } returns "my-uid"
        every { currentUser.photoUrl } returns null
        every { authRepository.currentFirebaseUser } returns currentUser

        logsForDateFlow.value = listOf(
            HabitLog(
                id = 1,
                habitId = 10,
                date = "2026-04-04",
                xpEarned = 10,
                streakAtCompletion = 0,
                completedByUid = "my-uid",
            ),
        )

        val vm = createViewModel()

        vm.householdCompleterNames.test {
            val names = awaitItem()
            assertTrue(names.isEmpty())
        }
    }
}
