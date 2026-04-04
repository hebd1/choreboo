package com.example.choreboo_habittrackerfriend.di

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.choreboo_habittrackerfriend.data.repository.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AppLifecycleObserver"

/**
 * Observes the process lifecycle and triggers a background sync when the app moves to the
 * foreground (ON_START). A 5-minute cooldown inside [SyncManager] prevents excessive network
 * calls when the user rapidly switches apps.
 *
 * On cold start the sync is **skipped** here because [MainViewModel] runs it as part of
 * the coordinated startup sequence (behind the branded splash screen). Only subsequent
 * app-resume events trigger a background sync from this observer.
 *
 * Registered in [com.example.choreboo_habittrackerfriend.ChorebooApplication.onCreate] via
 * ProcessLifecycleOwner.
 */
@Singleton
class AppLifecycleObserver @Inject constructor(
    private val syncManager: SyncManager,
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Tracks whether this is the first onStart (cold start). */
    @Volatile
    private var isColdStart = true

    override fun onStart(owner: LifecycleOwner) {
        // Cold-start sync is handled by MainViewModel's startup sequence so users
        // see the branded splash screen while data loads. Skip it here.
        if (isColdStart) {
            isColdStart = false
            Log.d(TAG, "Cold start — skipping sync (handled by MainViewModel)")
            return
        }

        Log.d(TAG, "App moved to foreground — checking sync")
        scope.launch {
            try {
                syncManager.syncAll(force = false)
            } catch (e: Exception) {
                // Background sync failures are silent — they do not surface to the user
                Log.e(TAG, "Background sync error", e)
            }
        }
    }
}
