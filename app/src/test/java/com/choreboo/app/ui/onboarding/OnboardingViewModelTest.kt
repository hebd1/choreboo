package com.choreboo.app.ui.onboarding

import app.cash.turbine.test
import com.choreboo.app.R
import com.choreboo.app.TestDispatcherRule
import com.choreboo.app.data.datastore.UserPreferences
import com.choreboo.app.data.repository.BillingRepository
import com.choreboo.app.data.repository.ChorebooRepository
import com.choreboo.app.domain.model.PetType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [OnboardingViewModel].
 *
 * Covers: step navigation, survey selection, pet type selection, hatch flow,
 * skipPremium (free-user and premium-fallback paths), and error events.
 */
class OnboardingViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var userPreferences: UserPreferences
    private lateinit var chorebooRepository: ChorebooRepository
    private lateinit var billingRepository: BillingRepository

    private fun createViewModel() = OnboardingViewModel(
        userPreferences = userPreferences,
        chorebooRepository = chorebooRepository,
        billingRepository = billingRepository,
    )

    @Before
    fun setUp() {
        userPreferences = mockk(relaxed = true)
        chorebooRepository = mockk(relaxed = true)
        billingRepository = mockk(relaxed = true) {
            every { productDetails } returns MutableStateFlow(null)
            every { isPremium } returns MutableStateFlow(false)
        }
    }

    // ── initial state ────────────────────────────────────────────────────────

    @Test
    fun `initial step is STEP_SURVEY_USAGE`() {
        val vm = createViewModel()
        assertEquals(STEP_SURVEY_USAGE, vm.currentStep.value)
    }

    @Test
    fun `initial selectedPetType is FOX`() {
        val vm = createViewModel()
        assertEquals(PetType.FOX, vm.selectedPetType.value)
    }

    @Test
    fun `initial isHatching is false`() {
        val vm = createViewModel()
        assertFalse(vm.isHatching.value)
    }

    // ── step navigation ──────────────────────────────────────────────────────

    @Test
    fun `goToStep updates currentStep`() {
        val vm = createViewModel()
        vm.goToStep(STEP_NAME)
        assertEquals(STEP_NAME, vm.currentStep.value)
    }

    @Test
    fun `goBack decrements step by one`() {
        val vm = createViewModel()
        vm.goToStep(STEP_PET_SELECT)
        vm.goBack()
        assertEquals(STEP_HOW_IT_HELPS, vm.currentStep.value)
    }

    @Test
    fun `goBack does not go below STEP_SURVEY_USAGE`() {
        val vm = createViewModel()
        vm.goBack()
        assertEquals(STEP_SURVEY_USAGE, vm.currentStep.value)
    }

    // ── survey selection ─────────────────────────────────────────────────────

    @Test
    fun `selectUsageIntent updates usageIntent`() {
        val vm = createViewModel()
        vm.selectUsageIntent(UsageIntent.HABITS_ROUTINES)
        assertEquals(UsageIntent.HABITS_ROUTINES, vm.usageIntent.value)
    }

    @Test
    fun `selectBiggestStruggle updates biggestStruggle`() {
        val vm = createViewModel()
        vm.selectBiggestStruggle(BiggestStruggle.MOTIVATION)
        assertEquals(BiggestStruggle.MOTIVATION, vm.biggestStruggle.value)
    }

    // ── pet type selection ───────────────────────────────────────────────────

    @Test
    fun `selectPetType updates selectedPetType`() {
        val vm = createViewModel()
        vm.selectPetType(PetType.PANDA)
        assertEquals(PetType.PANDA, vm.selectedPetType.value)
    }

    // ── hatchChoreboo ────────────────────────────────────────────────────────

    @Test
    fun `hatchChoreboo advances to STEP_PAYWALL on success`() = runTest {
        coEvery { chorebooRepository.createOrActivatePetType(any(), any()) } returns mockk(relaxed = true)
        val vm = createViewModel()
        vm.hatchChoreboo("Foxy")
        assertEquals(STEP_PAYWALL, vm.currentStep.value)
    }

    @Test
    fun `hatchChoreboo emits Error event on failure`() = runTest {
        coEvery { chorebooRepository.createOrActivatePetType(any(), any()) } throws RuntimeException("DB error")
        val vm = createViewModel()

        vm.events.test {
            vm.hatchChoreboo("Foxy")
            val event = awaitItem()
            assertTrue(event is OnboardingEvent.Error)
            assertEquals(R.string.onboarding_error_generic, (event as OnboardingEvent.Error).messageRes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `hatchChoreboo uses pet type name as default name when chorebooName is blank`() = runTest {
        coEvery { chorebooRepository.createOrActivatePetType(any(), any()) } returns mockk(relaxed = true)
        val vm = createViewModel()
        vm.selectPetType(PetType.PANDA)
        vm.hatchChoreboo("")
        coVerify { chorebooRepository.createOrActivatePetType("Panda", PetType.PANDA) }
    }

    @Test
    fun `hatchChoreboo does not create premium pet when user is not premium`() = runTest {
        val vm = createViewModel()
        vm.selectPetType(PetType.CAPYBARA)

        vm.hatchChoreboo("Capy")

        coVerify(exactly = 0) { chorebooRepository.createOrActivatePetType(any(), any()) }
        assertEquals(STEP_PAYWALL, vm.currentStep.value)
    }

    // ── skipPremium ──────────────────────────────────────────────────────────

    @Test
    fun `skipPremium completes onboarding for free pet type`() = runTest {
        coEvery { chorebooRepository.createOrActivatePetType(any(), any()) } returns mockk(relaxed = true)
        val vm = createViewModel()
        vm.selectPetType(PetType.FOX)
        vm.hatchChoreboo("Foxy")

        vm.events.test {
            vm.skipPremium()
            val event = awaitItem()
            assertTrue(event is OnboardingEvent.NavigateToHome)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { userPreferences.setOnboardingComplete(true) }
    }

    @Test
    fun `skipPremium emits PremiumPetFallback event for premium pet type`() = runTest {
        coEvery { chorebooRepository.createOrActivatePetType(any(), any()) } returns mockk(relaxed = true)
        coEvery { chorebooRepository.getChorebooNameSync() } returns "Axey"
        val vm = createViewModel()
        vm.selectPetType(PetType.AXOLOTL)
        vm.hatchChoreboo("Axey")

        vm.events.test {
            vm.skipPremium()
            val fallback = awaitItem()
            assertTrue(fallback is OnboardingEvent.PremiumPetFallback)
            val navigate = awaitItem()
            assertTrue(navigate is OnboardingEvent.NavigateToHome)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(PetType.FOX, vm.selectedPetType.value)
    }

    // ── completePremiumOnboarding ────────────────────────────────────────────

    @Test
    fun `completePremiumOnboarding marks onboarding complete and navigates`() = runTest {
        val vm = createViewModel()

        vm.events.test {
            vm.completePremiumOnboarding()
            assertTrue(awaitItem() is OnboardingEvent.NavigateToHome)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { userPreferences.setOnboardingComplete(true) }
    }
}
