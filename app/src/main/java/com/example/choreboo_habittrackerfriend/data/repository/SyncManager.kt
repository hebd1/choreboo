package com.example.choreboo_habittrackerfriend.data.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Minimum time between background (app-resume) syncs. Auth-triggered syncs bypass this. */
private const val SYNC_COOLDOWN_MS = 5 * 60 * 1000L // 5 minutes

/** Delays between retry attempts for transient sync failures (1 s, then 2 s). */
private val RETRY_DELAYS_MS = listOf(1_000L, 2_000L)

@Singleton
class SyncManager @Inject constructor(
    private val habitRepository: HabitRepository,
    private val chorebooRepository: ChorebooRepository,
    private val userRepository: UserRepository,
    private val backgroundRepository: BackgroundRepository,
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
     * Runs the given [block] while holding the sync mutex.
     * Use this to prevent `syncAll()` from running concurrently with a destructive operation
     * (e.g., account reset).
     */
    suspend fun <T> runExclusive(block: suspend () -> T): T = syncMutex.withLock { block() }

    /**
     * Executes [block] with simple exponential-backoff retry.
     * Retries up to [RETRY_DELAYS_MS].size times on exception, waiting the corresponding delay
     * between attempts. Returns the result of the first successful invocation, or rethrows the
     * last exception if all attempts fail.
     */
    private suspend fun <T> retryWithBackoff(block: suspend () -> T): T {
        var lastException: Exception? = null
        for ((attempt, delayMs) in RETRY_DELAYS_MS.withIndex()) {
            try {
                if (attempt > 0) {
                    Timber.d("Retry attempt %d after %dms delay", attempt, delayMs)
                    delay(delayMs)
                }
                return block()
            } catch (e: Exception) {
                Timber.w("Attempt %d failed: %s", attempt, e.message)
                lastException = e
            }
        }
        // Final attempt (no delay after this)
        return try {
            block()
        } catch (e: Exception) {
            throw lastException ?: e
        }
    }

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
        // We always pass false here — the goal is only to prime the SDK's token cache, not to
        // force-refresh. After sign-in the token is already fresh; forcing a refresh would be
        // a wasteful network round-trip.
        try {
            firebaseAuth.currentUser?.getIdToken(false)?.await()
        } catch (e: Exception) {
            Timber.w(e, "Failed to pre-fetch auth token — sync may fail with UNAUTHENTICATED")
        }

        // After sign-in (force=true), getIdToken(false) may resolve synchronously from
        // Firebase's in-memory cache without suspending. This means the coroutine never
        // yields, and the Data Connect SDK's internal IdTokenListener (which propagates
        // the token to the gRPC interceptor) may still be pending in the main thread's
        // message queue when the parallel sync coroutines launch. Yielding here gives
        // the main thread a chance to dispatch that callback before we send any gRPC calls.
        if (force) {
            delay(500)
        }

        if (!force && !shouldSync()) {
            Timber.d("Skipping sync — within cooldown window")
            return true // Not a failure, just throttled
        }

        return syncMutex.withLock {
            // Re-check after acquiring lock in case another coroutine already ran
            if (!force && !shouldSync()) {
                Timber.d("Skipping sync — within cooldown window (post-lock)")
                return@withLock true
            }

            var anySuccess = false

            // Steps 1, 2, and 4 are independent — run them in parallel.
            // Step 3 (habit logs) depends on step 1 (habits) for remoteId mapping.
            coroutineScope {
                // 1. Habits (logs depend on habit remoteIds being present)
                val habitsDeferred = async {
                    try {
                        retryWithBackoff { habitRepository.syncHabitsFromCloud() }
                        true
                    } catch (e: Exception) {
                        Timber.e(e, "Habit sync failed after retries")
                        false
                    }
                }

                // 2. Choreboo (independent of habits)
                val chorebooDeferred = async {
                    try {
                        retryWithBackoff { chorebooRepository.syncFromCloud() }
                        true
                    } catch (e: Exception) {
                        Timber.e(e, "Choreboo sync failed after retries")
                        false
                    }
                }

                // 4. User points (independent of habits)
                val pointsDeferred = async {
                    try {
                        retryWithBackoff { userRepository.syncPointsFromCloud() }
                        true
                    } catch (e: Exception) {
                        Timber.e(e, "Points sync failed after retries")
                        false
                    }
                }

                // 5. Purchased backgrounds (independent of habits)
                val backgroundsDeferred = async {
                    try {
                        retryWithBackoff { backgroundRepository.syncFromCloud() }
                        true
                    } catch (e: Exception) {
                        Timber.e(e, "Background sync failed after retries")
                        false
                    }
                }

                val habitsOk = habitsDeferred.await()
                val chorebooOk = chorebooDeferred.await()
                val pointsOk = pointsDeferred.await()
                backgroundsDeferred.await() // best-effort — don't gate anySuccess on it
                anySuccess = habitsOk || chorebooOk || pointsOk

                // 3. Habit logs — sequential after step 1 (needs habit remoteIds)
                if (habitsOk) {
                    try {
                        retryWithBackoff { habitRepository.syncHabitLogsFromCloud() }
                        anySuccess = true
                    } catch (e: Exception) {
                        Timber.e(e, "Habit log sync failed after retries")
                    }
                }

                // 5. Household habit logs for today (best-effort)
                try {
                    habitRepository.syncHouseholdHabitLogsForToday()
                } catch (e: Exception) {
                    Timber.e(e, "Household habit log sync failed (non-fatal)")
                }
            }

            // Only reset cooldown if at least one step succeeded — a total failure
            // should not block the next foreground-triggered retry.
            if (anySuccess) {
                lastSyncTimestampMs = System.currentTimeMillis()
            }
            Timber.d("Sync complete (anySuccess=$anySuccess)")
            anySuccess
        }
    }
}
