package com.choreboo.app.di

import android.content.Context
import com.choreboo.app.data.repository.BillingRepository
import com.choreboo.app.data.repository.ChorebooRepository
import com.choreboo.app.data.repository.SyncManager
import com.choreboo.app.domain.model.ChorebooStage
import com.choreboo.app.domain.model.ChorebooStats
import com.choreboo.app.domain.model.PetType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Tests for [AppLifecycleObserver]: cold-start skip, warm-resume sync trigger,
 * and silent error handling.
 *
 * A [TestScope] with [UnconfinedTestDispatcher] is injected via the secondary constructor
 * so coroutines execute eagerly and tests can use [advanceUntilIdle] instead of
 * fragile [Thread.sleep] calls.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppLifecycleObserverTest {

    private lateinit var context: Context
    private lateinit var syncManager: SyncManager
    private lateinit var billingRepository: BillingRepository
    private lateinit var chorebooRepository: ChorebooRepository
    private lateinit var petMoodAlarmHandler: PetMoodAlarmHandler
    private lateinit var observer: AppLifecycleObserver
    private val owner = mockk<androidx.lifecycle.LifecycleOwner>(relaxed = true)

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        syncManager = mockk(relaxed = true)
        billingRepository = mockk(relaxed = true)
        chorebooRepository = mockk(relaxed = true)
        petMoodAlarmHandler = mockk(relaxed = true)
    }

    private fun createObserver(testScope: TestScope): AppLifecycleObserver =
        AppLifecycleObserver(
            context = context,
            syncManager = syncManager,
            billingRepository = billingRepository,
            chorebooRepository = chorebooRepository,
            scope = testScope,
            petMoodAlarmHandler = petMoodAlarmHandler,
        )

    // ── Cold start ───────────────────────────────────────────────────────

    @Test
    fun `cold start onStart does not trigger syncAll`() = runTest {
        observer = createObserver(this)

        // First onStart is always cold start
        observer.onStart(owner)
        advanceUntilIdle()

        coVerify(exactly = 0) { syncManager.syncAll(any()) }
    }

    // ── Warm resume ──────────────────────────────────────────────────────

    @Test
    fun `second onStart triggers syncAll with force false`() = runTest {
        observer = createObserver(this)

        // First call is cold start (skipped), second is a warm resume
        observer.onStart(owner)
        observer.onStart(owner)
        advanceUntilIdle()

        coVerify(exactly = 1) { syncManager.syncAll(force = false) }
    }

    @Test
    fun `second onStart triggers billing verification`() = runTest {
        observer = createObserver(this)

        observer.onStart(owner)
        observer.onStart(owner)
        advanceUntilIdle()

        coVerify(exactly = 1) { billingRepository.verifyPremiumStatus() }
    }

    @Test
    fun `sync exception on warm resume is silently caught and does not propagate`() = runTest {
        observer = createObserver(this)
        coEvery { syncManager.syncAll(any()) } throws RuntimeException("network error")

        observer.onStart(owner)     // cold start — skipped
        observer.onStart(owner)     // warm resume — triggers sync which throws
        advanceUntilIdle()

        // If we reach here without the test thread crashing, the exception was caught
        coVerify(exactly = 1) { syncManager.syncAll(any()) }
    }

    @Test
    fun `onStop skips predictive alarm when no choreboo exists`() = runTest {
        observer = createObserver(this)
        coEvery { chorebooRepository.getChorebooSync() } returns null

        observer.onStop(owner)
        advanceUntilIdle()

        coVerify(exactly = 0) { petMoodAlarmHandler.schedule(any(), any()) }
    }

    @Test
    fun `onStop schedules predictive alarm when choreboo exists`() = runTest {
        observer = createObserver(this)
        coEvery { chorebooRepository.getChorebooSync() } returns ChorebooStats(
            id = 1,
            name = "Boo",
            stage = ChorebooStage.BABY,
            level = 2,
            xp = 10,
            hunger = 70,
            happiness = 80,
            energy = 60,
            petType = PetType.FOX,
            lastInteractionAt = 0L,
            createdAt = 0L,
            sleepUntil = 1234L,
        )

        observer.onStop(owner)
        advanceUntilIdle()

        coVerify(exactly = 1) { petMoodAlarmHandler.schedule(any(), any()) }
    }
}
