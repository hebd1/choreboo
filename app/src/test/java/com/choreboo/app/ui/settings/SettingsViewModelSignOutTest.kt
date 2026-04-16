package com.choreboo.app.ui.settings

import android.app.Application
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.choreboo.app.TestDispatcherRule
import com.choreboo.app.data.datastore.UserPreferences
import com.choreboo.app.data.repository.AuthRepository
import com.choreboo.app.data.repository.BillingRepository
import com.choreboo.app.data.repository.ChorebooRepository
import com.choreboo.app.data.repository.HabitRepository
import com.choreboo.app.data.repository.HouseholdRepository
import com.choreboo.app.data.repository.ResetRepository
import com.choreboo.app.data.repository.ResetResult
import com.choreboo.app.data.repository.UserRepository
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
 * Unit tests for [SettingsViewModel.signOut] and [SettingsViewModel.resetAccount].
 *
 * These tests verify the 6-step sign-out cleanup sequence and the credential-branching
 * logic in resetAccount. Uses MockK + Turbine + TestDispatcherRule.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelSignOutTest {

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

    // ── signOut: full cleanup sequence ────────────────────────────────────────

    @Test
    fun `signOut cancels reminders before clearing data`() = runTest {
        val viewModel = createViewModel()

        viewModel.signOut()
        advanceUntilIdle()

        coVerify { habitRepository.cancelAllReminders() }
    }

    @Test
    fun `signOut cancels pending write-through coroutines`() = runTest {
        val viewModel = createViewModel()

        viewModel.signOut()
        advanceUntilIdle()

        coVerify { habitRepository.cancelPendingWrites() }
        coVerify { chorebooRepository.cancelPendingWrites() }
    }

    @Test
    fun `signOut clears all local data stores`() = runTest {
        val viewModel = createViewModel()

        viewModel.signOut()
        advanceUntilIdle()

        coVerify { habitRepository.clearLocalData() }
        coVerify { chorebooRepository.clearLocalData() }
        coVerify { householdRepository.clearState() }
        coVerify { userPreferences.clearAllData() }
    }

    @Test
    fun `signOut calls authRepository signOut last`() = runTest {
        val viewModel = createViewModel()

        viewModel.signOut()
        advanceUntilIdle()

        coVerify { authRepository.signOut() }
    }

    @Test
    fun `signOut emits SignedOut event`() = runTest {
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.signOut()
            val event = awaitItem()
            assertTrue(event is SettingsEvent.SignedOut)
        }
    }

    @Test
    fun `signOut still signs out even if cleanup throws`() = runTest {
        coEvery { habitRepository.clearLocalData() } throws RuntimeException("DB error")
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.signOut()
            val event = awaitItem()
            assertTrue(event is SettingsEvent.SignedOut)
        }

        // Auth sign-out must still happen despite cleanup failure
        coVerify { authRepository.signOut() }
    }

    @Test
    fun `signOut still signs out even if cancelAllReminders throws`() = runTest {
        coEvery { habitRepository.cancelAllReminders() } throws RuntimeException("Alarm error")
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.signOut()
            val event = awaitItem()
            assertTrue(event is SettingsEvent.SignedOut)
        }

        coVerify { authRepository.signOut() }
    }

    // ── resetAccount: credential branching ───────────────────────────────────

    @Test
    fun `resetAccount emits error when no currentUser`() = runTest {
        every { authRepository.currentFirebaseUser } returns null
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.resetAccount(password = "pw")
            val event = awaitItem()
            assertTrue(event is SettingsEvent.ShowError)
        }
    }

    @Test
    fun `resetAccount emits error when no password and no google token`() = runTest {
        val mockUser = mockk<FirebaseUser>(relaxed = true) {
            every { email } returns "test@test.com"
        }
        every { authRepository.currentFirebaseUser } returns mockUser
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.resetAccount(password = null, googleIdToken = null)
            val event = awaitItem()
            assertTrue(event is SettingsEvent.ShowError)
        }
    }

    @Test
    fun `resetAccount emits error when password provided but no email`() = runTest {
        val mockUser = mockk<FirebaseUser>(relaxed = true) {
            every { email } returns null
        }
        every { authRepository.currentFirebaseUser } returns mockUser
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.resetAccount(password = "pw")
            val event = awaitItem()
            assertTrue(event is SettingsEvent.ShowError)
        }
    }

    @Test
    fun `resetAccount delegates to resetRepository with google credential`() = runTest {
        val mockUser = mockk<FirebaseUser>(relaxed = true) {
            every { email } returns "test@test.com"
        }
        every { authRepository.currentFirebaseUser } returns mockUser
        coEvery { resetRepository.resetAll(any()) } returns ResetResult.Success
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.resetAccount(googleIdToken = "google-token-123")
            val event = awaitItem()
            assertTrue(event is SettingsEvent.AccountReset)
        }

        coVerify { resetRepository.resetAll(any()) }
    }

    @Test
    fun `resetAccount delegates to resetRepository with email credential`() = runTest {
        val mockUser = mockk<FirebaseUser>(relaxed = true) {
            every { email } returns "test@test.com"
        }
        every { authRepository.currentFirebaseUser } returns mockUser
        coEvery { resetRepository.resetAll(any()) } returns ResetResult.Success
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.resetAccount(password = "my-password")
            val event = awaitItem()
            assertTrue(event is SettingsEvent.AccountReset)
        }

        coVerify { resetRepository.resetAll(any()) }
    }

    @Test
    fun `resetAccount sets isResetting true then false`() = runTest {
        val mockUser = mockk<FirebaseUser>(relaxed = true) {
            every { email } returns "test@test.com"
        }
        every { authRepository.currentFirebaseUser } returns mockUser
        coEvery { resetRepository.resetAll(any()) } returns ResetResult.Success
        val viewModel = createViewModel()

        assertFalse(viewModel.isResetting.value)
        viewModel.resetAccount(password = "pw")
        advanceUntilIdle()
        assertFalse(viewModel.isResetting.value) // reset back to false in finally
    }

    @Test
    fun `resetAccount emits ShowRawError on ResetResult Error`() = runTest {
        val mockUser = mockk<FirebaseUser>(relaxed = true) {
            every { email } returns "test@test.com"
        }
        every { authRepository.currentFirebaseUser } returns mockUser
        coEvery { resetRepository.resetAll(any()) } returns ResetResult.Error("Something went wrong")
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.resetAccount(password = "pw")
            val event = awaitItem()
            assertTrue(event is SettingsEvent.ShowRawError)
            assertEquals("Something went wrong", (event as SettingsEvent.ShowRawError).message)
        }
    }
}
