package com.example.choreboo_habittrackerfriend.di

import android.content.Context
import com.example.choreboo_habittrackerfriend.data.repository.BillingRepository
import com.example.choreboo_habittrackerfriend.data.repository.ChorebooRepository
import com.example.choreboo_habittrackerfriend.data.repository.SyncManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Tests for [AppLifecycleObserver]: cold-start skip, warm-resume sync trigger,
 * and silent error handling.
 *
 * The observer's internal [kotlinx.coroutines.CoroutineScope] uses [kotlinx.coroutines.Dispatchers.IO],
 * so warm-resume tests use a short [Thread.sleep] to allow the background coroutine to
 * execute before asserting. Cold-start tests need no delay since no coroutine is launched.
 */
class AppLifecycleObserverTest {

    private lateinit var context: Context
    private lateinit var syncManager: SyncManager
    private lateinit var billingRepository: BillingRepository
    private lateinit var chorebooRepository: ChorebooRepository
    private lateinit var observer: AppLifecycleObserver
    private val owner = mockk<androidx.lifecycle.LifecycleOwner>(relaxed = true)

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        syncManager = mockk(relaxed = true)
        billingRepository = mockk(relaxed = true)
        chorebooRepository = mockk(relaxed = true)
        observer = AppLifecycleObserver(
            context = context,
            syncManager = syncManager,
            billingRepository = billingRepository,
            chorebooRepository = chorebooRepository,
        )
    }

    // ── Cold start ───────────────────────────────────────────────────────

    @Test
    fun `cold start onStart does not trigger syncAll`() = runTest {
        // First onStart is always cold start
        observer.onStart(owner)

        // No delay needed: no coroutine is ever launched on cold start
        coVerify(exactly = 0) { syncManager.syncAll(any()) }
    }

    // ── Warm resume ──────────────────────────────────────────────────────

    @Test
    fun `second onStart triggers syncAll with force false`() {
        // First call is cold start (skipped), second is a warm resume
        observer.onStart(owner)
        observer.onStart(owner)

        // Give the Dispatchers.IO coroutine time to execute the mocked syncAll
        Thread.sleep(200)

        coVerify(exactly = 1) { syncManager.syncAll(force = false) }
    }

    @Test
    fun `second onStart triggers billing verification`() {
        observer.onStart(owner)
        observer.onStart(owner)

        Thread.sleep(200)

        coVerify(exactly = 1) { billingRepository.verifyPremiumStatus() }
    }

    @Test
    fun `sync exception on warm resume is silently caught and does not propagate`() {
        coEvery { syncManager.syncAll(any()) } throws RuntimeException("network error")

        observer.onStart(owner)     // cold start — skipped
        observer.onStart(owner)     // warm resume — triggers sync which throws

        // Give the IO coroutine time to run and swallow the exception
        Thread.sleep(200)

        // If we reach here without the test thread crashing, the exception was caught
        coVerify(exactly = 1) { syncManager.syncAll(any()) }
    }
}
