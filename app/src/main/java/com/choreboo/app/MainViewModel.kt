package com.choreboo.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.choreboo.app.data.datastore.UserPreferences
import com.choreboo.app.data.repository.AuthRepository
import com.choreboo.app.data.repository.ChorebooRepository
import com.choreboo.app.data.repository.SyncManager
import com.choreboo.app.domain.model.ChorebooMood
import com.choreboo.app.worker.PetMoodCheckWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val userPreferences: UserPreferences,
    private val chorebooRepository: ChorebooRepository,
    val authRepository: AuthRepository,
    private val syncManager: SyncManager,
) : ViewModel() {

    val themeMode: StateFlow<String> = userPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")

    val onboardingComplete: StateFlow<Boolean?> = userPreferences.onboardingComplete
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val petMood: StateFlow<ChorebooMood> = chorebooRepository.getChoreboo()
        .map { it?.mood ?: ChorebooMood.IDLE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChorebooMood.IDLE)

    /**
     * Set to `true` once all startup tasks have completed.
     * For unauthenticated / non-onboarded users this fires immediately after DataStore
     * resolves. For fully authenticated+onboarded users it waits for Room warmup only
     * (sub-second). Cloud sync and WebM animation loading run in the background.
     */
    private val _startupComplete = MutableStateFlow(false)

    /**
     * `true` when the app is ready to display its first destination screen.
     * Gates the branded splash screen in [MainActivity].
     */
    val isAppReady: StateFlow<Boolean> = combine(
        onboardingComplete,
        _startupComplete,
    ) { onboarding, startup ->
        onboarding != null && startup
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            runStartupSequence()
        }
    }

    /**
     * Orchestrates all startup work:
     * 1. Wait for DataStore to resolve (onboardingComplete != null).
     * 2. If user is unauthenticated or not yet onboarded → done immediately.
     * 3. Otherwise, run Room warmup (fast, sub-second) then show the app.
     *    Cloud sync and WebM animation loading both run in the background and
     *    do NOT block the splash screen. PetScreen shows an emoji placeholder
     *    while WebM videos load.
     * 4. Enqueue the periodic pet mood check worker (6-hour interval).
     */
    private suspend fun runStartupSequence() {
        // 1. Wait for DataStore to emit the first real value
        val isOnboarded = onboardingComplete.first { it != null }

        // 2. Fast path for users who need auth or onboarding — no data to wait for
        if (!authRepository.isAuthenticated || isOnboarded != true) {
            Timber.d("Startup fast-path (unauth or not onboarded)")
            _startupComplete.value = true
            return
        }

        // 3. Full startup for authenticated + onboarded users.
        //    Cloud sync is fire-and-forget — does NOT block the splash screen.
        //    WebM animations are also NOT awaited — PetScreen has an emoji
        //    fallback and will show the video once it's ready.
        Timber.d("Running full startup sequence")

        viewModelScope.launch {
            try {
                val ok = syncManager.syncAll(force = false)
                Timber.d("Background cloud sync complete (success=$ok)")
            } catch (e: Exception) {
                Timber.e(e, "Background cloud sync failed")
            }
        }

        // Room warmup: ensure an active choreboo is selected and stats are current.
        // This is a fast local DB operation (sub-second) — blocking here is fine.
        try {
            if (chorebooRepository.ensureActiveChoreboo() != null) {
                chorebooRepository.applyStatDecay()
            }
            Timber.d("Room warmup complete")
        } catch (e: Exception) {
            Timber.e(e, "Room warmup failed")
        }

        // Enqueue the periodic pet mood check worker (6-hour interval, won't duplicate if already enqueued).
        enqueuePetMoodCheckWorker()

        _startupComplete.value = true
        Timber.d("Startup sequence finished — app ready")
    }

    private fun enqueuePetMoodCheckWorker() {
        try {
            val petMoodCheckWork = PeriodicWorkRequestBuilder<PetMoodCheckWorker>(
                6,
                TimeUnit.HOURS,
            ).build()

            // Enqueue with KEEP policy so it won't duplicate if already enqueued
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "pet_mood_check",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                petMoodCheckWork,
            )
            Timber.d("Enqueued periodic pet mood check worker (6-hour interval)")
        } catch (e: Exception) {
            Timber.e(e, "Failed to enqueue pet mood check worker")
        }
    }
}
