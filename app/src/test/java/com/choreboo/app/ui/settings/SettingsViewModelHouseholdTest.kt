package com.choreboo.app.ui.settings

import android.app.Application
import app.cash.turbine.test
import com.choreboo.app.R
import com.choreboo.app.TestDispatcherRule
import com.choreboo.app.data.datastore.UserPreferences
import com.choreboo.app.data.repository.AuthRepository
import com.choreboo.app.data.repository.BillingRepository
import com.choreboo.app.data.repository.ChorebooRepository
import com.choreboo.app.data.repository.HabitRepository
import com.choreboo.app.data.repository.HouseholdRepository
import com.choreboo.app.data.repository.HouseholdResult
import com.choreboo.app.data.repository.ResetRepository
import com.choreboo.app.data.repository.UserRepository
import com.choreboo.app.domain.model.Household
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [SettingsViewModel.createHousehold], [SettingsViewModel.joinHousehold],
 * and [SettingsViewModel.leaveHousehold].
 *
 * Verifies the loading-flag lifecycle (_isCreatingHousehold / _isJoiningHousehold /
 * _isLeavingHousehold) and the [SettingsEvent.ShowSuccess] / [SettingsEvent.ShowRawError]
 * events emitted for both success and error paths.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelHouseholdTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var application: Application
    private lateinit var userPreferences: UserPreferences
    private lateinit var authRepository: AuthRepository
    private lateinit var householdRepository: HouseholdRepository
    private lateinit var habitRepository: HabitRepository
    private lateinit var chorebooRepository: ChorebooRepository
    private lateinit var resetRepository: ResetRepository
    private lateinit var userRepository: UserRepository
    private lateinit var billingRepository: BillingRepository

    private fun createViewModel() = SettingsViewModel(
        application,
        userPreferences,
        authRepository,
        householdRepository,
        habitRepository,
        chorebooRepository,
        resetRepository,
        userRepository,
        billingRepository,
    )

    private fun fakeHousehold(name: String = "Test House") = Household(
        id = "household-id",
        name = name,
        inviteCode = "ABCD1234",
        createdByUid = "uid-1",
    )

    @Before
    fun setUp() {
        application = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        householdRepository = mockk(relaxed = true)
        habitRepository = mockk(relaxed = true)
        chorebooRepository = mockk(relaxed = true)
        resetRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        billingRepository = mockk(relaxed = true)

        // Mock default values for StateFlow dependencies
        every { userPreferences.themeMode } returns MutableStateFlow("system")
        every { userPreferences.soundEnabled } returns MutableStateFlow(true)
        every { userPreferences.totalPoints } returns MutableStateFlow(0)
        every { userPreferences.profilePhotoUri } returns MutableStateFlow(null)
        every { authRepository.currentUser } returns MutableStateFlow(null)
        every { householdRepository.currentHousehold } returns MutableStateFlow(null)
        every { householdRepository.householdMembers } returns MutableStateFlow(emptyList())
        every { billingRepository.isPremium } returns MutableStateFlow(false)
    }

    // ── createHousehold: success ─────────────────────────────────────────────

    @Test
    fun `createHousehold emits ShowSuccess with household name on success`() = runTest {
        val household = fakeHousehold("My House")
        coEvery { householdRepository.createHousehold("My House") } returns HouseholdResult.Success(household)
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.createHousehold("My House")
            val event = awaitItem()
            assertTrue(event is SettingsEvent.ShowSuccess)
            assertEquals(R.string.settings_msg_household_created, (event as SettingsEvent.ShowSuccess).messageRes)
            assertEquals("My House", event.formatArg)
        }
    }

    @Test
    fun `createHousehold emits ShowRawError on Error result`() = runTest {
        coEvery { householdRepository.createHousehold(any()) } returns HouseholdResult.Error("Already in a household")
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.createHousehold("Any Name")
            val event = awaitItem()
            assertTrue(event is SettingsEvent.ShowRawError)
            assertEquals("Already in a household", (event as SettingsEvent.ShowRawError).message)
        }
    }

    @Test
    fun `createHousehold resets isCreatingHousehold to false after success`() = runTest {
        val household = fakeHousehold()
        coEvery { householdRepository.createHousehold(any()) } returns HouseholdResult.Success(household)
        val viewModel = createViewModel()

        assertFalse(viewModel.isCreatingHousehold.value)
        viewModel.createHousehold("Test House")
        advanceUntilIdle()
        assertFalse(viewModel.isCreatingHousehold.value)
    }

    @Test
    fun `createHousehold resets isCreatingHousehold to false after error`() = runTest {
        coEvery { householdRepository.createHousehold(any()) } returns HouseholdResult.Error("Error")
        val viewModel = createViewModel()

        assertFalse(viewModel.isCreatingHousehold.value)
        viewModel.createHousehold("Test House")
        advanceUntilIdle()
        assertFalse(viewModel.isCreatingHousehold.value)
    }

    // ── joinHousehold: success ───────────────────────────────────────────────

    @Test
    fun `joinHousehold emits ShowSuccess with household name on success`() = runTest {
        val household = fakeHousehold("Joined House")
        coEvery { householdRepository.joinHousehold("INVITE1") } returns HouseholdResult.Success(household)
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.joinHousehold("INVITE1")
            val event = awaitItem()
            assertTrue(event is SettingsEvent.ShowSuccess)
            assertEquals(R.string.settings_msg_household_joined, (event as SettingsEvent.ShowSuccess).messageRes)
            assertEquals("Joined House", event.formatArg)
        }
    }

    @Test
    fun `joinHousehold emits ShowRawError on Error result`() = runTest {
        coEvery { householdRepository.joinHousehold(any()) } returns HouseholdResult.Error("Invalid invite code")
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.joinHousehold("BADINVITE")
            val event = awaitItem()
            assertTrue(event is SettingsEvent.ShowRawError)
            assertEquals("Invalid invite code", (event as SettingsEvent.ShowRawError).message)
        }
    }

    @Test
    fun `joinHousehold resets isJoiningHousehold to false after success`() = runTest {
        val household = fakeHousehold()
        coEvery { householdRepository.joinHousehold(any()) } returns HouseholdResult.Success(household)
        val viewModel = createViewModel()

        assertFalse(viewModel.isJoiningHousehold.value)
        viewModel.joinHousehold("INVITE1")
        advanceUntilIdle()
        assertFalse(viewModel.isJoiningHousehold.value)
    }

    @Test
    fun `joinHousehold resets isJoiningHousehold to false after error`() = runTest {
        coEvery { householdRepository.joinHousehold(any()) } returns HouseholdResult.Error("Error")
        val viewModel = createViewModel()

        assertFalse(viewModel.isJoiningHousehold.value)
        viewModel.joinHousehold("INVITE1")
        advanceUntilIdle()
        assertFalse(viewModel.isJoiningHousehold.value)
    }

    // ── leaveHousehold: success ──────────────────────────────────────────────

    @Test
    fun `leaveHousehold emits ShowSuccess with no formatArg on success`() = runTest {
        val household = fakeHousehold()
        coEvery { householdRepository.leaveHousehold() } returns HouseholdResult.Success(household)
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.leaveHousehold()
            val event = awaitItem()
            assertTrue(event is SettingsEvent.ShowSuccess)
            assertEquals(R.string.settings_msg_household_left, (event as SettingsEvent.ShowSuccess).messageRes)
            assertEquals(null, event.formatArg)
        }
    }

    @Test
    fun `leaveHousehold emits ShowRawError on Error result`() = runTest {
        coEvery { householdRepository.leaveHousehold() } returns HouseholdResult.Error("Not in a household")
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.leaveHousehold()
            val event = awaitItem()
            assertTrue(event is SettingsEvent.ShowRawError)
            assertEquals("Not in a household", (event as SettingsEvent.ShowRawError).message)
        }
    }

    @Test
    fun `leaveHousehold resets isLeavingHousehold to false after success`() = runTest {
        val household = fakeHousehold()
        coEvery { householdRepository.leaveHousehold() } returns HouseholdResult.Success(household)
        val viewModel = createViewModel()

        assertFalse(viewModel.isLeavingHousehold.value)
        viewModel.leaveHousehold()
        advanceUntilIdle()
        assertFalse(viewModel.isLeavingHousehold.value)
    }

    @Test
    fun `leaveHousehold resets isLeavingHousehold to false after error`() = runTest {
        coEvery { householdRepository.leaveHousehold() } returns HouseholdResult.Error("Error")
        val viewModel = createViewModel()

        assertFalse(viewModel.isLeavingHousehold.value)
        viewModel.leaveHousehold()
        advanceUntilIdle()
        assertFalse(viewModel.isLeavingHousehold.value)
    }
}
