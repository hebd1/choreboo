package com.example.choreboo_habittrackerfriend.data.repository

import android.util.Log
import androidx.core.net.toUri
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.dataconnect.ChorebooConnector
import com.example.choreboo_habittrackerfriend.dataconnect.execute
import com.example.choreboo_habittrackerfriend.dataconnect.instance
import com.example.choreboo_habittrackerfriend.domain.model.AppUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UserRepository"
private const val CLOUD_TIMEOUT_MS = 5000L

@Singleton
class UserRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userPreferences: UserPreferences,
    private val firebaseStorage: FirebaseStorage,
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
            val result = withTimeoutOrNull(CLOUD_TIMEOUT_MS) { connector.getCurrentUser.execute() }
            if (result == null) {
                Log.w(TAG, "fetchCurrentUserFromCloud timed out")
                return getCurrentAppUser()
            }
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
            val result = withTimeoutOrNull(CLOUD_TIMEOUT_MS) { connector.getCurrentUser.execute() }
            if (result == null) {
                Log.w(TAG, "syncPointsFromCloud timed out")
                return
            }
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

    /**
     * Upload a profile photo to Firebase Storage and update FirebaseAuth + cloud User.
     * - Uploads file to `profile_photos/{uid}.jpg`
     * - Updates FirebaseAuth.photoUrl with the download URL
     * - Saves download URL to DataStore `profilePhotoUri` for instant display
     * - Calls syncCurrentUserToCloud() so the URL reaches the cloud and household members see it
     *
     * Throws on failure so callers can surface an error to the user.
     */
    suspend fun uploadProfilePhoto(photoFile: File) {
        val user = firebaseAuth.currentUser
            ?: throw IllegalStateException("No authenticated user")

        try {
            // Upload to Firebase Storage at profile_photos/{uid}.jpg
            val storageRef = firebaseStorage.reference.child("profile_photos/${user.uid}.jpg")
            val uploadTask = storageRef.putFile(photoFile.toUri())
            uploadTask.await()
            Log.d(TAG, "Uploaded profile photo to Storage for ${user.uid}")

            // Get the download URL
            val downloadUrl = storageRef.downloadUrl.await()
            Log.d(TAG, "Got download URL for profile photo: $downloadUrl")

            // Update FirebaseAuth.photoUrl with the Storage URL
            val request = UserProfileChangeRequest.Builder()
                .setPhotoUri(downloadUrl)
                .build()
            user.updateProfile(request).await()
            Log.d(TAG, "Updated Firebase Auth photoUrl to: $downloadUrl")

            // Save to DataStore for instant display (avoids waiting for cloud sync)
            userPreferences.setProfilePhotoUri(downloadUrl.toString())
            Log.d(TAG, "Saved profile photo URL to DataStore")

            // Write-through: sync to cloud so other devices and household members see it
            syncCurrentUserToCloud()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload profile photo", e)
            throw e
        }
    }

    /**
     * Delete the user's profile photo from Firebase Storage and restore to original state.
     * - Deletes from Firebase Storage `profile_photos/{uid}.jpg`
     * - Restores FirebaseAuth.photoUrl to the original Google photo (if Google sign-in)
     *   or null for email/password users
     * - Clears DataStore `profilePhotoUri`
     * - Deletes the local file
     * - Calls syncCurrentUserToCloud() so the change reaches the cloud
     *
     * Throws on failure so callers can surface an error to the user.
     */
    suspend fun deleteProfilePhoto(localPhotoFile: File) {
        val user = firebaseAuth.currentUser
            ?: throw IllegalStateException("No authenticated user")

        try {
            // Delete from Firebase Storage
            val storageRef = firebaseStorage.reference.child("profile_photos/${user.uid}.jpg")
            try {
                storageRef.delete().await()
                Log.d(TAG, "Deleted profile photo from Storage for ${user.uid}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete photo from Storage, continuing anyway", e)
            }

            // Restore FirebaseAuth.photoUrl to the original Google photo (if available)
            val googlePhotoUrl = user.providerData
                .firstOrNull { it.providerId == "google.com" }
                ?.photoUrl

            val request = UserProfileChangeRequest.Builder()
                .setPhotoUri(googlePhotoUrl)
                .build()
            user.updateProfile(request).await()
            Log.d(TAG, "Restored Firebase Auth photoUrl to: ${googlePhotoUrl ?: "null (email user)"}")

            // Clear DataStore
            userPreferences.setProfilePhotoUri("")
            Log.d(TAG, "Cleared profile photo URI from DataStore")

            // Delete local file
            if (localPhotoFile.exists()) {
                localPhotoFile.delete()
                Log.d(TAG, "Deleted local photo file")
            }

            // Write-through: sync to cloud so other devices and household members see the change
            syncCurrentUserToCloud()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete profile photo", e)
            throw e
        }
    }
}
