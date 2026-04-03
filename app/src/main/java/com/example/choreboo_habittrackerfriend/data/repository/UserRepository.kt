package com.example.choreboo_habittrackerfriend.data.repository

import android.util.Log
import com.example.choreboo_habittrackerfriend.dataconnect.ChorebooConnector
import com.example.choreboo_habittrackerfriend.dataconnect.execute
import com.example.choreboo_habittrackerfriend.dataconnect.instance
import com.example.choreboo_habittrackerfriend.domain.model.AppUser
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UserRepository"

@Singleton
class UserRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
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
}
