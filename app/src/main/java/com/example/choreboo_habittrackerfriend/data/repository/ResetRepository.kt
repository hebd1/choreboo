package com.example.choreboo_habittrackerfriend.data.repository

import android.util.Log
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.dataconnect.ChorebooConnector
import com.example.choreboo_habittrackerfriend.dataconnect.execute
import com.example.choreboo_habittrackerfriend.dataconnect.instance
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ResetRepository"

sealed class ResetResult {
    data object Success : ResetResult()
    data class Error(val message: String) : ResetResult()
}

@Singleton
class ResetRepository @Inject constructor(
    private val habitRepository: HabitRepository,
    private val chorebooRepository: ChorebooRepository,
    private val householdRepository: HouseholdRepository,
    private val userPreferences: UserPreferences,
    private val firebaseAuth: FirebaseAuth,
    private val syncManager: SyncManager,
) {
    private val connector by lazy { ChorebooConnector.instance }

    /**
     * Fully resets the current user's account:
     * 1. Deletes all cloud data for the user in FK-safe order (best-effort — failures are
     *    logged but do not block subsequent steps).
     * 2. Deletes the Firebase Auth user so they can re-register with the same email.
     * 3. Clears all local Room tables and DataStore.
     *
     * Deletion order respects foreign key constraints:
     *   HabitLogs → Habits → Choreboo → (null household FK) → Household → User
     */
    suspend fun resetAll(credential: AuthCredential): ResetResult = syncManager.runExclusive {
        val currentUser = firebaseAuth.currentUser
            ?: return@runExclusive ResetResult.Error("Not authenticated")
        val uid = currentUser.uid

        // ── Cloud cleanup (best-effort, FK-safe order) ──────────────────────────────

        // Step 1: Delete all habit logs (references Habit + User)
        try {
            connector.deleteAllMyHabitLogs.execute()
            Log.d(TAG, "Deleted all habit logs")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete habit logs (continuing)", e)
        }

        // Step 2: Delete all habits (references User + Household)
        try {
            connector.deleteAllMyHabits.execute()
            Log.d(TAG, "Deleted all habits")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete habits (continuing)", e)
        }

        // Step 3: Delete the Choreboo (references User)
        try {
            connector.deleteMyChoreboo.execute()
            Log.d(TAG, "Deleted Choreboo")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete Choreboo (continuing)", e)
        }

        // Step 4: Household cleanup
        // Null out User.household FK first (breaks the User ↔ Household circular dep),
        // then delete the household if the current user is its creator.
        // Fetch from cloud rather than in-memory state, which may be null even when the
        // user actually has a household (the in-memory state is only populated when the
        // HouseholdScreen is visited).
        try {
            val cloudHousehold = connector.getMyHousehold.execute().data.user?.household
            connector.updateUserHousehold.execute { householdId = null }
            Log.d(TAG, "Nulled out household FK")
            if (cloudHousehold != null && cloudHousehold.createdBy.id == uid) {
                connector.deleteHousehold.execute(
                    householdId = cloudHousehold.id,
                )
                Log.d(TAG, "Deleted household ${cloudHousehold.id}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed household cleanup (continuing)", e)
        }

        // Step 5: Delete the User record (must be last — all dependent rows gone now)
        try {
            connector.deleteMyUser.execute()
            Log.d(TAG, "Deleted User record from Data Connect")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete User record (continuing)", e)
        }

        // ── Firebase Auth re-authentication + deletion ───────────────────────────────
        return@runExclusive try {
            // Firebase requires recent auth for account deletion. Re-authenticate first
            // so this works regardless of how long ago the user signed in.
            currentUser.reauthenticate(credential).await()
            Log.d(TAG, "Re-authentication successful for user $uid")
            currentUser.delete().await()
            Log.d(TAG, "Deleted Firebase Auth user $uid")

            // ── Local cleanup ────────────────────────────────────────────────────────
            clearLocalData()

            ResetResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to re-authenticate or delete Firebase Auth user", e)
            // Still clear local data so the app can start fresh on next launch
            clearLocalData()
            ResetResult.Error(
                "Account reset failed: ${e.message}. " +
                    "Cloud data has been cleared. Delete your user manually from the " +
                    "Firebase Console (Authentication tab) to re-register with the same email.",
            )
        }
    }

    private suspend fun clearLocalData() {
        try {
            habitRepository.clearLocalData()
            chorebooRepository.clearLocalData()
            householdRepository.clearState()
            userPreferences.clearAllData()
            Log.d(TAG, "Cleared all local data")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear some local data", e)
        }
    }
}
