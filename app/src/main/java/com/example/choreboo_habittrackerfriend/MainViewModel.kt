package com.example.choreboo_habittrackerfriend

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.repository.AuthRepository
import com.example.choreboo_habittrackerfriend.data.repository.ChorebooRepository
import com.example.choreboo_habittrackerfriend.data.repository.SyncManager
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooMood
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MainViewModel"

@HiltViewModel
class MainViewModel @Inject constructor(
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
     * (sub-second). Cloud sync and Lottie animation parsing run in the background.
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
     *    Cloud sync and Lottie animation parsing both run in the background and
     *    do NOT block the splash screen. PetScreen shows an emoji placeholder
     *    while Lottie animations load.
     */
    private suspend fun runStartupSequence() {
        // 1. Wait for DataStore to emit the first real value
        val isOnboarded = onboardingComplete.first { it != null }

        // 2. Fast path for users who need auth or onboarding — no data to wait for
        if (!authRepository.isAuthenticated || isOnboarded != true) {
            Log.d(TAG, "Startup fast-path (unauth or not onboarded)")
            _startupComplete.value = true
            return
        }

        // 3. Full startup for authenticated + onboarded users.
        //    Cloud sync is fire-and-forget — does NOT block the splash screen.
        //    Lottie animations are also NOT awaited — PetScreen has an emoji
        //    fallback and will crossfade to the animation once it's ready.
        Log.d(TAG, "Running full startup sequence")

        viewModelScope.launch {
            try {
                val ok = syncManager.syncAll(force = false)
                Log.d(TAG, "Background cloud sync complete (success=$ok)")
            } catch (e: Exception) {
                Log.e(TAG, "Background cloud sync failed", e)
            }
        }

        // Room warmup: ensure choreboo exists and stats are current.
        // This is a fast local DB operation (sub-second) — blocking here is fine.
        try {
            chorebooRepository.getOrCreateChoreboo()
            chorebooRepository.applyStatDecay()
            Log.d(TAG, "Room warmup complete")
        } catch (e: Exception) {
            Log.e(TAG, "Room warmup failed", e)
        }

        _startupComplete.value = true
        Log.d(TAG, "Startup sequence finished — app ready")
    }
}
