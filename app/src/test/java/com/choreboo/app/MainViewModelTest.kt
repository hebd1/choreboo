package com.choreboo.app

import android.content.Context
import app.cash.turbine.test
import com.choreboo.app.data.datastore.UserPreferences
import com.choreboo.app.data.repository.AuthRepository
import com.choreboo.app.data.repository.ChorebooRepository
import com.choreboo.app.data.repository.SyncManager
import com.choreboo.app.domain.model.ChorebooMood
import com.choreboo.app.domain.model.ChorebooStage
import com.choreboo.app.domain.model.ChorebooStats
import com.choreboo.app.domain.model.PetType
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
 * Tests for [MainViewModel]: startup sequencing, fast/full path logic, and state flow derivations.
 *
 * WebM animation loading no longer blocks the splash screen — it runs in the background.
 * The only blocking work in the full startup path is Room warmup, which is mocked here to
 * complete instantly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var context: Context
    private lateinit var userPreferences: UserPreferences
    private lateinit var chorebooRepository: ChorebooRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var syncManager: SyncManager

    private val defaultChoreboo = ChorebooStats(
        id = 1,
        name = "TestBoo",
        stage = ChorebooStage.BABY,
        level = 2,
        xp = 10,
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
        context = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)
        chorebooRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        syncManager = mockk(relaxed = true)

        // Default stubs — authenticated, onboarded, all tasks succeed
        every { userPreferences.themeMode } returns flowOf("system")
        every { userPreferences.onboardingComplete } returns flowOf(true)
        every { chorebooRepository.getChoreboo() } returns MutableStateFlow(null)
        every { authRepository.isAuthenticated } returns true
        coEvery { syncManager.syncAll(any()) } returns true
        coEvery { chorebooRepository.ensureActiveChoreboo() } returns defaultChoreboo
    }

    private fun createViewModel() = MainViewModel(
        context = context,
        userPreferences = userPreferences,
        chorebooRepository = chorebooRepository,
        authRepository = authRepository,
        syncManager = syncManager,
    )

    // ── Initial state ────────────────────────────────────────────────────

    @Test
    fun `isAppReady is false before startup completes`() = runTest {
        // Room warmup never completes — block it so we can inspect in-progress state
        coEvery { chorebooRepository.ensureActiveChoreboo() } coAnswers {
            kotlinx.coroutines.awaitCancellation()
        }

        val vm = createViewModel()

        assertFalse(vm.isAppReady.value)
    }

    // ── State flows ──────────────────────────────────────────────────────

    @Test
    fun `themeMode reflects DataStore value`() = runTest {
        every { userPreferences.themeMode } returns flowOf("dark")

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals("dark", vm.themeMode.value)
    }

    @Test
    fun `onboardingComplete reflects DataStore value`() = runTest {
        every { userPreferences.onboardingComplete } returns flowOf(false)

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(false, vm.onboardingComplete.value)
    }

    @Test
    fun `petMood defaults to IDLE when no choreboo in Room`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.petMood.test {
            assertEquals(ChorebooMood.IDLE, awaitItem())
        }
    }

    @Test
    fun `petMood reflects choreboo mood when choreboo exists`() = runTest {
        val happyChoreboo = defaultChoreboo.copy(hunger = 90, happiness = 90, energy = 90)
        every { chorebooRepository.getChoreboo() } returns MutableStateFlow(happyChoreboo)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.petMood.test {
            assertEquals(ChorebooMood.HAPPY, awaitItem())
        }
    }

    // ── Fast path ────────────────────────────────────────────────────────

    @Test
    fun `isAppReady becomes true immediately for unauthenticated user`() = runTest {
        every { authRepository.isAuthenticated } returns false
        every { userPreferences.onboardingComplete } returns flowOf(false)

        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.isAppReady.value)
        // Room and cloud must NOT be touched on fast path
        coVerify(exactly = 0) { chorebooRepository.ensureActiveChoreboo() }
        coVerify(exactly = 0) { syncManager.syncAll(any()) }
    }

    @Test
    fun `isAppReady becomes true immediately for authenticated but not-onboarded user`() = runTest {
        every { authRepository.isAuthenticated } returns true
        every { userPreferences.onboardingComplete } returns flowOf(false)

        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.isAppReady.value)
        coVerify(exactly = 0) { chorebooRepository.ensureActiveChoreboo() }
        coVerify(exactly = 0) { syncManager.syncAll(any()) }
    }

    // ── Full startup path ────────────────────────────────────────────────

    @Test
    fun `isAppReady becomes true after Room warmup completes`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.isAppReady.value)
        coVerify { chorebooRepository.ensureActiveChoreboo() }
        coVerify { chorebooRepository.applyStatDecay() }
        coVerify { syncManager.syncAll(force = false) }
    }

    @Test
    fun `isAppReady becomes true even when cloud sync fails`() = runTest {
        coEvery { syncManager.syncAll(any()) } throws Exception("network error")

        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.isAppReady.value)
    }

    @Test
    fun `isAppReady becomes true even when Room warmup fails`() = runTest {
        coEvery { chorebooRepository.ensureActiveChoreboo() } throws Exception("db error")

        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.isAppReady.value)
    }

    @Test
    fun `full startup calls syncAll with force false`() = runTest {
        createViewModel()
        advanceUntilIdle()

        coVerify { syncManager.syncAll(force = false) }
    }
}
