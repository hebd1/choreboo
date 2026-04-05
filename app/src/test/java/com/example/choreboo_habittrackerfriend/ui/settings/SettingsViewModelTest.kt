package com.example.choreboo_habittrackerfriend.ui.settings

import android.app.Application
import android.net.Uri
import com.example.choreboo_habittrackerfriend.TestDispatcherRule
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.repository.AuthRepository
import com.example.choreboo_habittrackerfriend.data.repository.ChorebooRepository
import com.example.choreboo_habittrackerfriend.data.repository.HabitRepository
import com.example.choreboo_habittrackerfriend.data.repository.HouseholdRepository
import com.example.choreboo_habittrackerfriend.data.repository.ResetRepository
import com.example.choreboo_habittrackerfriend.data.repository.UserRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for [SettingsViewModel]: profile photo upload/delete functionality.
 *
 * Simplified tests that verify the methods don't crash when repository methods
 * succeed or throw exceptions.
 */
class SettingsViewModelTest {

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
    private lateinit var viewModel: SettingsViewModel

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

        // Mock default values for StateFlow dependencies
        every { userPreferences.themeMode } returns MutableStateFlow("system")
        every { userPreferences.soundEnabled } returns MutableStateFlow(true)
        every { userPreferences.totalPoints } returns MutableStateFlow(0)
        every { userPreferences.profilePhotoUri } returns MutableStateFlow(null)
        every { userPreferences.householdNotificationsEnabled } returns MutableStateFlow(true)
        every { authRepository.currentUser } returns MutableStateFlow(null)
        every { householdRepository.currentHousehold } returns MutableStateFlow(null)
        every { householdRepository.householdMembers } returns MutableStateFlow(emptyList())

        viewModel = SettingsViewModel(
            application,
            userPreferences,
            authRepository,
            householdRepository,
            habitRepository,
            chorebooRepository,
            resetRepository,
            userRepository,
        )
    }

    // ── onProfilePhotoPicked ────────────────────────────────────────────

    @Test
    fun `onProfilePhotoPicked succeeds when upload succeeds`() = runTest {
        val mockUri = mockk<Uri>()
        every { application.filesDir } returns mockk()
        every { application.contentResolver.openInputStream(any()) } returns mockk()

        coEvery { userRepository.uploadProfilePhoto(any()) } returns Unit

        // Should not throw
        viewModel.onProfilePhotoPicked(mockUri)
        advanceUntilIdle()
    }

    @Test
    fun `onProfilePhotoPicked handles upload error gracefully`() = runTest {
        val mockUri = mockk<Uri>()
        every { application.filesDir } returns mockk()
        every { application.contentResolver.openInputStream(any()) } returns mockk()

        coEvery { userRepository.uploadProfilePhoto(any()) } throws Exception("Upload failed")

        // Should not throw — errors are handled
        viewModel.onProfilePhotoPicked(mockUri)
        advanceUntilIdle()
    }

    // ── clearProfilePhoto ───────────────────────────────────────────────

    @Test
    fun `clearProfilePhoto succeeds when deletion succeeds`() = runTest {
        coEvery { userRepository.deleteProfilePhoto(any()) } returns Unit

        // Should not throw
        viewModel.clearProfilePhoto()
        advanceUntilIdle()
    }

    @Test
    fun `clearProfilePhoto handles deletion error gracefully`() = runTest {
        coEvery { userRepository.deleteProfilePhoto(any()) } throws Exception("Delete failed")

        // Should not throw — errors are handled
        viewModel.clearProfilePhoto()
        advanceUntilIdle()
    }
}


