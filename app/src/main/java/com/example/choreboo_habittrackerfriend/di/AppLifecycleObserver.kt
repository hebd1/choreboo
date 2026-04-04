package com.example.choreboo_habittrackerfriend.di

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.choreboo_habittrackerfriend.data.repository.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AppLifecycleObserver"

/**
 * Observes the process lifecycle and triggers a background sync when the app moves to the
 * foreground (ON_START). A 5-minute cooldown inside [SyncManager] prevents excessive network
 * calls when the user rapidly switches apps.
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
        Log.d(TAG, "App moved to foreground — checking sync")
        scope.launch {
            try {
                // On cold start, delay sync to let ViewModel init work finish first
                if (isColdStart) {
                    isColdStart = false
                    delay(3_000L)
                }
                syncManager.syncAll(force = false)
            } catch (e: Exception) {
                // Background sync failures are silent — they do not surface to the user
                Log.e(TAG, "Background sync error", e)
            }
        }
    }
}
