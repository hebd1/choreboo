package com.example.choreboo_habittrackerfriend.data.repository

import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.dataconnect.ChorebooConnector
import com.example.choreboo_habittrackerfriend.dataconnect.execute
import com.example.choreboo_habittrackerfriend.dataconnect.instance
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val CLOUD_TIMEOUT_MS = 5000L

sealed class ResetResult {
    data object Success : ResetResult()
    data class Error(val message: String) : ResetResult()
}

@Singleton
class ResetRepository @Inject constructor(
    private val habitRepository: HabitRepository,
    private val chorebooRepository: ChorebooRepository,
    private val householdRepository: HouseholdRepository,
    private val backgroundRepository: BackgroundRepository,
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

        // Cancel pending write-through coroutines before cloud cleanup so in-flight writes
        // cannot re-create data that is about to be deleted.
        habitRepository.cancelPendingWrites()
        chorebooRepository.cancelPendingWrites()

        // ── Cloud cleanup (best-effort, FK-safe order) ──────────────────────────────

        // Step 1: Delete all habit logs (references Habit + User)
        try {
            withTimeoutOrNull(CLOUD_TIMEOUT_MS) { connector.deleteAllMyHabitLogs.execute() }
                ?: Timber.w("deleteAllMyHabitLogs timed out")
            Timber.d("Deleted all habit logs")
        } catch (e: Exception) {
            Timber.w(e, "Failed to delete habit logs (continuing)")
        }

        // Step 2: Delete all habits (references User + Household)
        try {
            withTimeoutOrNull(CLOUD_TIMEOUT_MS) { connector.deleteAllMyHabits.execute() }
                ?: Timber.w("deleteAllMyHabits timed out")
            Timber.d("Deleted all habits")
        } catch (e: Exception) {
            Timber.w(e, "Failed to delete habits (continuing)")
        }

        // Step 2b: Delete all purchased backgrounds (references User)
        try {
            withTimeoutOrNull(CLOUD_TIMEOUT_MS) { connector.deleteAllMyPurchasedBackgrounds.execute() }
                ?: Timber.w("deleteAllMyPurchasedBackgrounds timed out")
            Timber.d("Deleted all purchased backgrounds")
        } catch (e: Exception) {
            Timber.w(e, "Failed to delete purchased backgrounds (continuing)")
        }

        // Step 3: Delete the Choreboo (references User)
        try {
            withTimeoutOrNull(CLOUD_TIMEOUT_MS) { connector.deleteMyChoreboo.execute() }
                ?: Timber.w("deleteMyChoreboo timed out")
            Timber.d("Deleted Choreboo")
        } catch (e: Exception) {
            Timber.w(e, "Failed to delete Choreboo (continuing)")
        }

        // Step 4: Household cleanup
        // Null out User.household FK first (breaks the User ↔ Household circular dep),
        // then delete the household if the current user is its creator.
        // Fetch from cloud rather than in-memory state, which may be null even when the
        // user actually has a household (the in-memory state is only populated when the
        // HouseholdScreen is visited).
        try {
            val householdResult = withTimeoutOrNull(CLOUD_TIMEOUT_MS) {
                connector.getMyHousehold.execute()
            }
            if (householdResult == null) {
                Timber.w("getMyHousehold timed out during reset")
            }
            val cloudHousehold = householdResult?.data?.user?.household
            // D7 fix: Null out ALL members' User.household FK before deleting the household row.
            // Without this, other members' FKs remain pointing at the deleted Household row,
            // which violates PostgreSQL's FK RESTRICT constraint on subsequent User writes.
            // This must run before UpdateUserHousehold (which only clears the creator's FK)
            // and before DeleteHousehold.
            if (cloudHousehold != null) {
                try {
                    withTimeoutOrNull(CLOUD_TIMEOUT_MS) {
                        connector.nullifyHouseholdForMembers.execute(householdId = cloudHousehold.id)
                    } ?: Timber.w("nullifyHouseholdForMembers timed out")
                    Timber.d("Nullified household FK for all members of household %s", cloudHousehold.id)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to nullify household FK for members (continuing)")
                }
            }
            withTimeoutOrNull(CLOUD_TIMEOUT_MS) {
                connector.updateUserHousehold.execute { householdId = null }
            } ?: Timber.w("updateUserHousehold timed out")
            Timber.d("Nulled out household FK")
            if (cloudHousehold != null && cloudHousehold.createdBy.id == uid) {
                withTimeoutOrNull(CLOUD_TIMEOUT_MS) {
                    connector.deleteHousehold.execute(
                        householdId = cloudHousehold.id,
                    )
                } ?: Timber.w("deleteHousehold timed out")
                Timber.d("Deleted household %s", cloudHousehold.id)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed household cleanup (continuing)")
        }

        // Step 5: Delete the User record (must be last — all dependent rows gone now)
        try {
            withTimeoutOrNull(CLOUD_TIMEOUT_MS) { connector.deleteMyUser.execute() }
                ?: Timber.w("deleteMyUser timed out")
            Timber.d("Deleted User record from Data Connect")
        } catch (e: Exception) {
            Timber.w(e, "Failed to delete User record (continuing)")
        }

        // ── Firebase Auth re-authentication + deletion ───────────────────────────────
        return@runExclusive try {
            // Firebase requires recent auth for account deletion. Re-authenticate first
            // so this works regardless of how long ago the user signed in.
            currentUser.reauthenticate(credential).await()
            Timber.d("Re-authentication successful for user %s", uid)
            currentUser.delete().await()
            Timber.d("Deleted Firebase Auth user %s", uid)

            // ── Local cleanup ────────────────────────────────────────────────────────
            // Pass uid explicitly — firebaseAuth.currentUser is null after deletion (D8 fix).
            clearLocalData(uid)

            ResetResult.Success
        } catch (e: Exception) {
            Timber.e(e, "Failed to re-authenticate or delete Firebase Auth user")
            // Still clear local data so the app can start fresh on next launch
            clearLocalData(uid)
            ResetResult.Error(
                "Account reset failed: ${e.message}. " +
                    "Cloud data has been cleared. Delete your user manually from the " +
                    "Firebase Console (Authentication tab) to re-register with the same email.",
            )
        }
    }

    private suspend fun clearLocalData(uid: String) {
        try {
            habitRepository.clearLocalData()
            chorebooRepository.clearLocalData()
            householdRepository.clearState()
            backgroundRepository.clearLocalData(uid) // explicit uid — auth is already deleted (D8 fix)
            userPreferences.clearAllData()
            Timber.d("Cleared all local data")
        } catch (e: Exception) {
            Timber.w(e, "Failed to clear some local data")
        }
    }
}
