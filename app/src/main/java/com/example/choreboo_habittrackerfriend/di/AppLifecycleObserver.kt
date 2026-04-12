package com.example.choreboo_habittrackerfriend.di

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.choreboo_habittrackerfriend.data.repository.BillingRepository
import com.example.choreboo_habittrackerfriend.data.repository.ChorebooRepository
import com.example.choreboo_habittrackerfriend.data.repository.SyncManager
import com.example.choreboo_habittrackerfriend.worker.PetMoodScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Observes the process lifecycle and triggers a background sync when the app moves to the
 * foreground (ON_START). A 5-minute cooldown inside [SyncManager] prevents excessive network
 * calls when the user rapidly switches apps.
 *
 * On cold start the sync is **skipped** here because [MainViewModel] runs it as part of
 * the coordinated startup sequence (behind the branded splash screen). Only subsequent
 * app-resume events trigger a background sync from this observer.
 *
 * Also schedules a pet mood predictive alarm when the app goes to background (ON_STOP)
 * and cancels it when the app returns to foreground (ON_START) so that mood decay happens
 * in the background rather than being frozen while the app is open.
 *
 * Registered in [com.example.choreboo_habittrackerfriend.ChorebooApplication.onCreate] via
 * ProcessLifecycleOwner.
 */
@Singleton
class AppLifecycleObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncManager: SyncManager,
    private val billingRepository: BillingRepository,
    private val chorebooRepository: ChorebooRepository,
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(
        SupervisorJob() +
            Dispatchers.IO +
            CoroutineExceptionHandler { _, throwable ->
                Timber.e(throwable, "AppLifecycleObserver: unhandled coroutine exception")
            },
    )

    /** Tracks whether this is the first onStart (cold start). */
    @Volatile
    private var isColdStart = true

    override fun onStart(owner: LifecycleOwner) {
        // Cold-start sync is handled by MainViewModel's startup sequence so users
        // see the branded splash screen while data loads. Skip it here.
        if (isColdStart) {
            isColdStart = false
            Timber.d("Cold start — skipping sync (handled by MainViewModel)")
            return
        }

        Timber.d("App moved to foreground — checking sync and canceling pet mood alarm")
        scope.launch {
            try {
                syncManager.syncAll(force = false)
            } catch (e: Exception) {
                // Background sync failures are silent — they do not surface to the user
                Timber.e(e, "Background sync error")
            }
        }
        // Re-verify billing subscription status on each app foreground (no cooldown needed —
        // the BillingClient query is lightweight and fast).
        scope.launch {
            try {
                billingRepository.verifyPremiumStatus()
            } catch (e: Exception) {
                Timber.e(e, "Billing verification error")
            }
        }

        // Cancel the pet mood predictive alarm when app comes to foreground
        // so decay happens live and the app sees up-to-date stats
        PetMoodScheduler.cancelPredictiveAlarm(context)
    }

    override fun onStop(owner: LifecycleOwner) {
        Timber.d("App moved to background — scheduling pet mood predictive alarm")
        scope.launch {
            try {
                // Get current choreboo stats to calculate next critical time
                val choreboo = chorebooRepository.getChorebooSync() ?: run {
                    Timber.d("No choreboo found, skipping predictive alarm")
                    return@launch
                }

                // Schedule the predictive alarm based on current stats
                PetMoodScheduler.schedulePredictiveAlarm(
                    context,
                    choreboo.hunger,
                    choreboo.happiness,
                    choreboo.energy,
                    choreboo.sleepUntil,
                    choreboo.name,
                )
            } catch (e: Exception) {
                Timber.e(e, "Error scheduling pet mood alarm on background")
            }
        }
    }
}
