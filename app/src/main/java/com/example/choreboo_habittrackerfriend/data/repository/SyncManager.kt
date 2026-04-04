package com.example.choreboo_habittrackerfriend.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SyncManager"

/** Minimum time between background (app-resume) syncs. Auth-triggered syncs bypass this. */
private const val SYNC_COOLDOWN_MS = 5 * 60 * 1000L // 5 minutes

@Singleton
class SyncManager @Inject constructor(
    private val habitRepository: HabitRepository,
    private val chorebooRepository: ChorebooRepository,
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth,
) {
    /** Guards against concurrent sync calls (e.g. rapid foreground/background cycling). */
    private val syncMutex = Mutex()

    @Volatile
    private var lastSyncTimestampMs: Long = 0L

    /**
     * Returns true if enough time has passed since the last sync to warrant a new one.
     * Auth-triggered syncs should bypass this check by calling [syncAll] with [force] = true.
     */
    fun shouldSync(): Boolean =
        System.currentTimeMillis() - lastSyncTimestampMs >= SYNC_COOLDOWN_MS

    /**
     * Run a full cloud-to-local sync: habits → choreboo → habit logs → user points.
     *
     * @param force If true, skips the cooldown check (used after explicit auth events).
     * @return true if at least one step succeeded, false if all steps failed.
     */
    suspend fun syncAll(force: Boolean = false): Boolean {
        // Only unauthenticated check needed here — individual methods also guard themselves
        if (firebaseAuth.currentUser == null) return false

        // Ensure the auth token is in the cache before any Data Connect gRPC calls.
        // After sign-in, currentUser is non-null immediately but the Data Connect SDK's
        // internal IdTokenListener receives the token asynchronously. Calling getIdToken()
        // here forces the token to be resolved so the gRPC auth interceptor can include it.
        try {
            firebaseAuth.currentUser?.getIdToken(false)?.await()
        } catch (_: Exception) {
            Log.w(TAG, "Failed to pre-fetch auth token — sync may fail with UNAUTHENTICATED")
        }

        if (!force && !shouldSync()) {
            Log.d(TAG, "Skipping sync — within cooldown window")
            return true // Not a failure, just throttled
        }

        return syncMutex.withLock {
            // Re-check after acquiring lock in case another coroutine already ran
            if (!force && !shouldSync()) {
                Log.d(TAG, "Skipping sync — within cooldown window (post-lock)")
                return@withLock true
            }

            var anySuccess = false

            // Steps 1, 2, and 4 are independent — run them in parallel.
            // Step 3 (habit logs) depends on step 1 (habits) for remoteId mapping.
            coroutineScope {
                // 1. Habits (logs depend on habit remoteIds being present)
                val habitsDeferred = async {
                    try {
                        habitRepository.syncHabitsFromCloud()
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "Habit sync failed", e)
                        false
                    }
                }

                // 2. Choreboo (independent of habits)
                val chorebooDeferred = async {
                    try {
                        chorebooRepository.syncFromCloud()
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "Choreboo sync failed", e)
                        false
                    }
                }

                // 4. User points (independent of habits)
                val pointsDeferred = async {
                    try {
                        userRepository.syncPointsFromCloud()
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "Points sync failed", e)
                        false
                    }
                }

                val habitsOk = habitsDeferred.await()
                val chorebooOk = chorebooDeferred.await()
                val pointsOk = pointsDeferred.await()
                anySuccess = habitsOk || chorebooOk || pointsOk

                // 3. Habit logs — sequential after step 1 (needs habit remoteIds)
                if (habitsOk) {
                    try {
                        habitRepository.syncHabitLogsFromCloud()
                        anySuccess = true
                    } catch (e: Exception) {
                        Log.e(TAG, "Habit log sync failed", e)
                    }
                }

                // 5. Household habit logs for today (best-effort)
                try {
                    habitRepository.syncHouseholdHabitLogsForToday()
                } catch (e: Exception) {
                    Log.e(TAG, "Household habit log sync failed (non-fatal)", e)
                }
            }

            lastSyncTimestampMs = System.currentTimeMillis()
            Log.d(TAG, "Sync complete (anySuccess=$anySuccess)")
            anySuccess
        }
    }
}
