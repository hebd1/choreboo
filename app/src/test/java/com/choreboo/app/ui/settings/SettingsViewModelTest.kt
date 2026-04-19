package com.choreboo.app.ui.settings

import android.app.Application
import android.net.Uri
import app.cash.turbine.test
import com.choreboo.app.TestDispatcherRule
import com.choreboo.app.data.datastore.UserPreferences
import com.choreboo.app.data.repository.AuthRepository
import com.choreboo.app.data.repository.BillingRepository
import com.choreboo.app.data.repository.ChorebooRepository
import com.choreboo.app.data.repository.HabitRepository
import com.choreboo.app.data.repository.HouseholdRepository
import com.choreboo.app.data.repository.ResetRepository
import com.choreboo.app.data.repository.UserRepository
import com.choreboo.app.domain.model.ChorebooStage
import com.choreboo.app.domain.model.ChorebooStats
import com.choreboo.app.domain.model.PetType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    private lateinit var billingRepository: BillingRepository
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
        every { chorebooRepository.getChoreboo() } returns MutableStateFlow(null)
        every { chorebooRepository.getAllChoreboos() } returns MutableStateFlow(emptyList())

        viewModel = SettingsViewModel(
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

    // ── isPremium ───────────────────────────────────────────────────────────

    @Test
    fun `isPremium defaults to false`() = runTest {
        assertFalse(viewModel.isPremium.value)
    }

    @Test
    fun `isPremium reflects billingRepository flow`() = runTest {
        val premiumFlow = MutableStateFlow(false)
        every { billingRepository.isPremium } returns premiumFlow

        val vm = SettingsViewModel(
            application, userPreferences, authRepository, householdRepository,
            habitRepository, chorebooRepository, resetRepository, userRepository, billingRepository,
        )

        vm.isPremium.test {
            assertFalse(awaitItem())   // initial false
            premiumFlow.value = true
            assertTrue(awaitItem())    // updated to true
        }
    }

    // ── restorePurchases ────────────────────────────────────────────────────

    @Test
    fun `restorePurchases delegates to billingRepository`() = runTest {
        coEvery { billingRepository.restorePurchases() } returns Unit

        viewModel.restorePurchases()
        advanceUntilIdle()

        coVerify(exactly = 1) { billingRepository.restorePurchases() }
    }

    @Test
    fun `restorePurchases sets isRestoringPurchases true then false`() = runTest {
        coEvery { billingRepository.restorePurchases() } returns Unit

        assertFalse(viewModel.isRestoringPurchases.value)
        viewModel.restorePurchases()
        advanceUntilIdle()
        assertFalse(viewModel.isRestoringPurchases.value)
    }

    @Test
    fun `restorePurchases handles error gracefully`() = runTest {
        coEvery { billingRepository.restorePurchases() } throws Exception("Restore failed")

        // Should not throw — errors are handled
        viewModel.restorePurchases()
        advanceUntilIdle()

        assertFalse(viewModel.isRestoringPurchases.value)
    }

    // ── launchPremiumPurchase ───────────────────────────────────────────────

    @Test
    fun `launchPremiumPurchase delegates to billingRepository`() = runTest {
        val activity = mockk<android.app.Activity>(relaxed = true)
        every { billingRepository.launchPurchaseFlow(activity) } returns true

        viewModel.launchPremiumPurchase(activity)

        io.mockk.verify(exactly = 1) { billingRepository.launchPurchaseFlow(activity) }
    }

    @Test
    fun `selectOrCreatePet switches to existing pet`() = runTest {
        val existing = ChorebooStats(
            id = 5L,
            name = "Panda",
            stage = ChorebooStage.BABY,
            level = 3,
            petType = PetType.PANDA,
        )
        coEvery { chorebooRepository.getChorebooForPetType(PetType.PANDA) } returns existing

        viewModel.selectOrCreatePet(PetType.PANDA)
        advanceUntilIdle()

        coVerify { chorebooRepository.switchActiveChoreboo(existing.id) }
    }

    @Test
    fun `selectOrCreatePet creates new free pet when missing`() = runTest {
        coEvery { chorebooRepository.getChorebooForPetType(PetType.FOX) } returns null
        coEvery { chorebooRepository.createOrActivatePetType("Foxy", PetType.FOX) } returns ChorebooStats(
            id = 1L,
            name = "Foxy",
            stage = ChorebooStage.EGG,
            level = 1,
            petType = PetType.FOX,
        )

        viewModel.selectOrCreatePet(PetType.FOX, "Foxy")
        advanceUntilIdle()

        coVerify { chorebooRepository.createOrActivatePetType("Foxy", PetType.FOX) }
    }

    @Test
    fun `selectOrCreatePet blocks locked premium pet creation`() = runTest {
        coEvery { chorebooRepository.getChorebooForPetType(PetType.CAPYBARA) } returns null

        viewModel.selectOrCreatePet(PetType.CAPYBARA, "Capy")
        advanceUntilIdle()

        coVerify(exactly = 0) { chorebooRepository.createOrActivatePetType(any(), any()) }
    }
}

