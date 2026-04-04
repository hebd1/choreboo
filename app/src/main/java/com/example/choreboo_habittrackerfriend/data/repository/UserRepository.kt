package com.example.choreboo_habittrackerfriend.data.repository

import android.util.Log
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.dataconnect.ChorebooConnector
import com.example.choreboo_habittrackerfriend.dataconnect.execute
import com.example.choreboo_habittrackerfriend.dataconnect.instance
import com.example.choreboo_habittrackerfriend.domain.model.AppUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UserRepository"

@Singleton
class UserRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userPreferences: UserPreferences,
) {
    private val connector by lazy { ChorebooConnector.instance }

    /**
     * Returns the current user's UID, or null if not authenticated.
     */
    fun getCurrentUid(): String? = firebaseAuth.currentUser?.uid

    /**
     * Builds an AppUser from the current FirebaseAuth user.
     * Returns null if not authenticated.
     */
    fun getCurrentAppUser(): AppUser? {
        val fbUser = firebaseAuth.currentUser ?: return null
        return AppUser(
            uid = fbUser.uid,
            displayName = fbUser.displayName ?: "User",
            email = fbUser.email,
            photoUrl = fbUser.photoUrl?.toString(),
        )
    }

    /**
     * Upsert the current user record in the cloud DB via Data Connect.
     */
    suspend fun syncCurrentUserToCloud() {
        val user = getCurrentAppUser() ?: return
        try {
            connector.upsertUser.execute(
                displayName = user.displayName,
            ) {
                email = user.email
                photoUrl = user.photoUrl
            }
            Log.d(TAG, "Synced user ${user.uid} to cloud")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync user to cloud", e)
        }
    }

    /**
     * Updates the user's display name in both Firebase Auth and the cloud User record.
     * Throws on failure so callers can surface an error to the user.
     */
    suspend fun updateDisplayName(name: String) {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "Username must not be blank" }
        require(trimmed.length <= 30) { "Username must be 30 characters or fewer" }

        val user = firebaseAuth.currentUser
            ?: throw IllegalStateException("No authenticated user")

        val request = UserProfileChangeRequest.Builder()
            .setDisplayName(trimmed)
            .build()
        user.updateProfile(request).await()
        Log.d(TAG, "Updated Firebase Auth displayName to: $trimmed")

        // Write-through: sync the updated profile (reads fresh from FirebaseAuth) to cloud
        syncCurrentUserToCloud()
    }

    /**
     * Fetch current user's profile including household info from Data Connect.
     */
    suspend fun fetchCurrentUserFromCloud(): AppUser? {
        val uid = getCurrentUid() ?: return null
        return try {
            val result = connector.getCurrentUser.execute()
            val cloudUser = result.data.user
            if (cloudUser != null) {
                AppUser(
                    uid = cloudUser.id,
                    displayName = cloudUser.displayName,
                    email = cloudUser.email,
                    photoUrl = cloudUser.photoUrl,
                    householdId = cloudUser.household?.id?.toString(),
                    householdName = cloudUser.household?.name,
                    totalPoints = cloudUser.totalPoints,
                    totalLifetimeXp = cloudUser.totalLifetimeXp,
                )
            } else {
                // User not in cloud yet, return local auth data
                getCurrentAppUser()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch user from cloud", e)
            getCurrentAppUser()
        }
    }

    /**
     * Write-through: push current local totalPoints and totalLifetimeXp to the cloud.
     * Called after habit completion so both devices stay in sync.
     * Failures are silent (matching write-through convention).
     */
    suspend fun syncPointsToCloud(totalPoints: Int, totalLifetimeXp: Int) {
        require(totalPoints >= 0) { "totalPoints must be non-negative, was $totalPoints" }
        require(totalLifetimeXp >= 0) { "totalLifetimeXp must be non-negative, was $totalLifetimeXp" }

        if (getCurrentUid() == null) return
        try {
            connector.updateUserPoints.execute(
                totalPoints = totalPoints,
                totalLifetimeXp = totalLifetimeXp,
            )
            Log.d(TAG, "Synced points to cloud: totalPoints=$totalPoints, totalLifetimeXp=$totalLifetimeXp")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync points to cloud", e)
        }
    }

    /**
     * Cloud-to-local sync for point totals. Uses max-wins strategy so neither device loses
     * progress. Called during the post-auth/resume sync flow.
     * - totalLifetimeXp never decreases, so max is always correct.
     * - totalPoints can decrease from feeding, but max prevents losing progress across devices.
     */
    suspend fun syncPointsFromCloud() {
        if (getCurrentUid() == null) return
        try {
            val result = connector.getCurrentUser.execute()
            val cloudUser = result.data.user ?: return

            val cloudPoints = cloudUser.totalPoints
            val cloudLifetimeXp = cloudUser.totalLifetimeXp

            val localPoints = userPreferences.totalPoints.first()
            val localLifetimeXp = userPreferences.totalLifetimeXp.first()

            val mergedPoints = maxOf(localPoints, cloudPoints)
            val mergedLifetimeXp = maxOf(localLifetimeXp, cloudLifetimeXp)

            userPreferences.setPoints(mergedPoints)
            userPreferences.setLifetimeXp(mergedLifetimeXp)

            // Push merged values back to cloud if they differ from what the cloud had
            if (mergedPoints != cloudPoints || mergedLifetimeXp != cloudLifetimeXp) {
                syncPointsToCloud(mergedPoints, mergedLifetimeXp)
            }

            Log.d(
                TAG,
                "Synced points from cloud: local=($localPoints,$localLifetimeXp) " +
                    "cloud=($cloudPoints,$cloudLifetimeXp) merged=($mergedPoints,$mergedLifetimeXp)",
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync points from cloud", e)
            throw e
        }
    }
}
