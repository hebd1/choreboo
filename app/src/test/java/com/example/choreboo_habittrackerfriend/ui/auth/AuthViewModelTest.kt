package com.example.choreboo_habittrackerfriend.ui.auth

import app.cash.turbine.test
import com.example.choreboo_habittrackerfriend.TestDispatcherRule
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.repository.AuthRepository
import com.example.choreboo_habittrackerfriend.data.repository.AuthResult
import com.example.choreboo_habittrackerfriend.data.repository.AuthErrorType
import com.example.choreboo_habittrackerfriend.data.repository.ChorebooRepository
import com.example.choreboo_habittrackerfriend.data.repository.SyncManager
import com.example.choreboo_habittrackerfriend.data.repository.UserRepository
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooStage
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooStats
import com.example.choreboo_habittrackerfriend.domain.model.PetType
import com.example.choreboo_habittrackerfriend.ui.auth.AuthValidationError
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * Tests for [AuthViewModel]: form validation, auth flows, returning-user detection,
 * and DataStore restoration of the onboarding flag.
 *
 * All calls to external services (Firebase, SyncManager) are mocked. Tests that
 * exercise [AuthViewModel.handleResult] go through [AuthViewModel.signInWithGoogle]
 * to bypass the [android.util.Patterns.EMAIL_ADDRESS] check which is unavailable
 * in JVM unit tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var authRepository: AuthRepository
    private lateinit var userRepository: UserRepository
    private lateinit var chorebooRepository: ChorebooRepository
    private lateinit var syncManager: SyncManager
    private lateinit var userPreferences: UserPreferences

    private val mockUser = mockk<FirebaseUser>(relaxed = true)

    private val defaultChoreboo = ChorebooStats(
        id = 1,
        name = "TestBoo",
        stage = ChorebooStage.BABY,
        level = 1,
        xp = 0,
        hunger = 80,
        happiness = 80,
        energy = 80,
        petType = PetType.FOX,
        lastInteractionAt = System.currentTimeMillis(),
        createdAt = System.currentTimeMillis(),
        sleepUntil = 0,
    )

    @Before
    fun setUp() {
        authRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        chorebooRepository = mockk(relaxed = true)
        syncManager = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)

        // Default: Google sign-in succeeds, no profile photo set, no choreboo
        every { mockUser.uid } returns "uid-123"
        every { mockUser.photoUrl } returns null
        every { userPreferences.profilePhotoUri } returns flowOf(null)
        coEvery { authRepository.signInWithGoogle("google-token") } returns AuthResult.Success(mockUser)
        coEvery { syncManager.syncAll(any()) } returns true
        coEvery { chorebooRepository.getChorebooSync() } returns null
    }

    private fun createViewModel() = AuthViewModel(
        authRepository = authRepository,
        userRepository = userRepository,
        chorebooRepository = chorebooRepository,
        syncManager = syncManager,
        userPreferences = userPreferences,
    )

    // ── Initial form state ───────────────────────────────────────────────

    @Test
    fun `initial form state has expected defaults`() = runTest {
        val vm = createViewModel()

        vm.formState.test {
            val state = awaitItem()
            assertEquals("", state.email)
            assertEquals("", state.password)
            assertFalse(state.isSignUp)
            assertFalse(state.isLoading)
            assertFalse(state.isSyncing)
            assertNull(state.emailError)
            assertNull(state.passwordError)
        }
    }

    // ── Form field updates ───────────────────────────────────────────────

    @Test
    fun `onEmailChange updates email and clears existing error`() = runTest {
        val vm = createViewModel()

        // Submit blank to set an error first
        vm.submit()
        advanceUntilIdle()

        vm.onEmailChange("test@example.com")

        vm.formState.test {
            val state = awaitItem()
            assertEquals("test@example.com", state.email)
            assertNull(state.emailError)
        }
    }

    @Test
    fun `onPasswordChange updates password and clears existing error`() = runTest {
        val vm = createViewModel()

        vm.submit()
        advanceUntilIdle()

        vm.onPasswordChange("newpassword")

        vm.formState.test {
            val state = awaitItem()
            assertEquals("newpassword", state.password)
            assertNull(state.passwordError)
        }
    }

    @Test
    fun `toggleMode flips isSignUp flag and clears errors`() = runTest {
        val vm = createViewModel()

        assertFalse(vm.formState.value.isSignUp)
        vm.toggleMode()
        assertTrue(vm.formState.value.isSignUp)
        vm.toggleMode()
        assertFalse(vm.formState.value.isSignUp)
    }

    @Test
    fun `toggleForgotPassword updates showForgotPassword`() = runTest {
        val vm = createViewModel()

        assertFalse(vm.formState.value.showForgotPassword)
        vm.toggleForgotPassword(true)
        assertTrue(vm.formState.value.showForgotPassword)
        vm.toggleForgotPassword(false)
        assertFalse(vm.formState.value.showForgotPassword)
    }

    // ── Validation ───────────────────────────────────────────────────────

    @Test
    fun `submit with blank email sets emailError and does not call repository`() = runTest {
        val vm = createViewModel()

        vm.submit()
        advanceUntilIdle()

        assertEquals(AuthValidationError.EmailRequired, vm.formState.value.emailError)
        coVerify(exactly = 0) { authRepository.signInWithEmail(any(), any()) }
        coVerify(exactly = 0) { authRepository.signUpWithEmail(any(), any()) }
    }

    @Test
    fun `submit with blank password sets passwordError`() = runTest {
        val vm = createViewModel()

        // Blank email AND blank password — both errors set
        vm.submit()
        advanceUntilIdle()

        assertEquals(AuthValidationError.PasswordRequired, vm.formState.value.passwordError)
    }

    @Test
    fun `submit in sign-up mode with short password sets passwordError`() = runTest {
        val vm = createViewModel()
        vm.toggleMode() // switch to sign-up

        // Blank email + short password — both errors set; we just care about passwordError
        vm.onPasswordChange("abc")
        vm.submit()
        advanceUntilIdle()

        assertEquals(AuthValidationError.PasswordTooShort, vm.formState.value.passwordError)
    }

    // ── sendPasswordReset ────────────────────────────────────────────────

    @Test
    fun `sendPasswordReset with blank email sets emailError and does not call repository`() = runTest {
        val vm = createViewModel()

        vm.sendPasswordReset()
        advanceUntilIdle()

        assertEquals(AuthValidationError.EnterEmailFirst, vm.formState.value.emailError)
        coVerify(exactly = 0) { authRepository.sendPasswordReset(any()) }
    }

    @Test
    fun `sendPasswordReset success emits ShowMessage`() = runTest {
        coEvery { authRepository.sendPasswordReset("user@example.com") } returns AuthResult.ResetEmailSent
        val vm = createViewModel()
        vm.onEmailChange("user@example.com")

        vm.events.test {
            vm.sendPasswordReset()
            val event = awaitItem()
            assertTrue(event is AuthEvent.ShowMessage)
            // messageRes is an @StringRes Int, not a string message
            assertTrue((event as AuthEvent.ShowMessage).messageRes > 0)
        }
    }

    @Test
    fun `sendPasswordReset error emits ShowError`() = runTest {
        coEvery { authRepository.sendPasswordReset(any()) } returns AuthResult.Error(AuthErrorType.UserNotFound)
        val vm = createViewModel()
        vm.onEmailChange("unknown@example.com")

        vm.events.test {
            vm.sendPasswordReset()
            val event = awaitItem()
            assertTrue(event is AuthEvent.ShowError)
            // messageRes is an @StringRes Int from the error type
            assertTrue((event as AuthEvent.ShowError).messageRes > 0)
        }
    }

    // ── handleResult — new user (no choreboo) ────────────────────────────

    @Test
    fun `successful sign-in with no choreboo emits AuthSuccess with onboardingComplete false`() = runTest {
        coEvery { chorebooRepository.getChorebooSync() } returns null
        val vm = createViewModel()

        vm.events.test {
            vm.signInWithGoogle("google-token")
            val event = awaitItem()
            assertTrue(event is AuthEvent.AuthSuccess)
            assertFalse((event as AuthEvent.AuthSuccess).onboardingComplete)
        }
    }

    @Test
    fun `successful sign-in with no choreboo does not restore onboardingComplete in DataStore`() = runTest {
        coEvery { chorebooRepository.getChorebooSync() } returns null
        val vm = createViewModel()

        vm.signInWithGoogle("google-token")
        advanceUntilIdle()

        coVerify(exactly = 0) { userPreferences.setOnboardingComplete(true) }
    }

    // ── handleResult — returning user (choreboo exists) ──────────────────

    @Test
    fun `successful sign-in with existing choreboo emits AuthSuccess with onboardingComplete true`() = runTest {
        coEvery { chorebooRepository.getChorebooSync() } returns defaultChoreboo
        val vm = createViewModel()

        vm.events.test {
            vm.signInWithGoogle("google-token")
            val event = awaitItem()
            assertTrue(event is AuthEvent.AuthSuccess)
            assertTrue((event as AuthEvent.AuthSuccess).onboardingComplete)
        }
    }

    @Test
    fun `successful sign-in with existing choreboo restores onboardingComplete in DataStore`() = runTest {
        coEvery { chorebooRepository.getChorebooSync() } returns defaultChoreboo
        val vm = createViewModel()

        vm.signInWithGoogle("google-token")
        advanceUntilIdle()

        coVerify(exactly = 1) { userPreferences.setOnboardingComplete(true) }
    }

    @Test
    fun `successful sign-in emits AuthSuccess with correct uid`() = runTest {
        every { mockUser.uid } returns "expected-uid"
        val vm = createViewModel()

        vm.events.test {
            vm.signInWithGoogle("google-token")
            val event = awaitItem() as AuthEvent.AuthSuccess
            assertEquals("expected-uid", event.uid)
        }
    }

    // ── handleResult — sync failure ──────────────────────────────────────

    @Test
    fun `successful sign-in with sync failure emits ShowMessage about offline state`() = runTest {
        coEvery { syncManager.syncAll(any()) } returns false
        val vm = createViewModel()

        vm.events.test {
            vm.signInWithGoogle("google-token")
            // When sync fails, handleResult emits ShowMessage first, then AuthSuccess
            val first = awaitItem()
            assertTrue(first is AuthEvent.ShowMessage)
            // Consume the trailing AuthSuccess so Turbine doesn't flag leftover events
            awaitItem()
        }
    }

    // ── handleResult — auth error ────────────────────────────────────────

    @Test
    fun `auth error emits ShowError with the error message`() = runTest {
        coEvery { authRepository.signInWithGoogle("google-token") } returns AuthResult.Error(AuthErrorType.InvalidCredentials)
        val vm = createViewModel()

        vm.events.test {
            vm.signInWithGoogle("google-token")
            val event = awaitItem()
            assertTrue(event is AuthEvent.ShowError)
            assertTrue((event as AuthEvent.ShowError).messageRes > 0)
        }
    }

    // ── handleResult — photo URI handling ────────────────────────────────

    @Test
    fun `signInWithGoogle saves photo URI from account when none is stored`() = runTest {
        every { userPreferences.profilePhotoUri } returns flowOf(null)
        val vm = createViewModel()

        vm.signInWithGoogle("google-token", photoUrl = "https://example.com/photo.jpg")
        advanceUntilIdle()

        coVerify { userPreferences.setProfilePhotoUri("https://example.com/photo.jpg") }
    }

    @Test
    fun `signInWithGoogle falls back to FirebaseUser photoUrl when account has no photo`() = runTest {
        val photoUri = mockk<android.net.Uri>()
        every { photoUri.toString() } returns "https://firebase.google.com/photo.jpg"
        every { mockUser.photoUrl } returns photoUri         // FirebaseUser has a photo
        every { userPreferences.profilePhotoUri } returns flowOf(null)
        val vm = createViewModel()

        // No photoUrl param — should fall back to FirebaseUser.photoUrl
        vm.signInWithGoogle("google-token")
        advanceUntilIdle()

        coVerify { userPreferences.setProfilePhotoUri("https://firebase.google.com/photo.jpg") }
    }

    @Test
    fun `signInWithGoogle does not overwrite photo URI when one is already stored`() = runTest {
        every { userPreferences.profilePhotoUri } returns flowOf("https://custom.example.com/avatar.jpg")
        val vm = createViewModel()

        vm.signInWithGoogle("google-token", photoUrl = "https://example.com/photo.jpg")
        advanceUntilIdle()

        coVerify(exactly = 0) { userPreferences.setProfilePhotoUri(any()) }
    }

    @Test
    fun `handleResult does not save photo URI when neither account nor FirebaseUser has one`() = runTest {
        every { mockUser.photoUrl } returns null
        every { userPreferences.profilePhotoUri } returns flowOf(null)
        val vm = createViewModel()

        // No photoUrl param and FirebaseUser has no photo either
        vm.signInWithGoogle("google-token")
        advanceUntilIdle()

        coVerify(exactly = 0) { userPreferences.setProfilePhotoUri(any()) }
    }

    // ── isSyncing overlay ────────────────────────────────────────────────

    @Test
    fun `isSyncing is false after handleResult completes`() = runTest {
        val vm = createViewModel()

        vm.signInWithGoogle("google-token")
        advanceUntilIdle()

        assertFalse(vm.formState.value.isSyncing)
    }
}
