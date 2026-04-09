package com.example.choreboo_habittrackerfriend.data.repository

import android.util.Log
import com.example.choreboo_habittrackerfriend.data.local.dao.PurchasedBackgroundDao
import com.example.choreboo_habittrackerfriend.data.local.entity.PurchasedBackgroundEntity
import com.example.choreboo_habittrackerfriend.dataconnect.ChorebooConnector
import com.example.choreboo_habittrackerfriend.dataconnect.execute
import com.example.choreboo_habittrackerfriend.dataconnect.instance
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BackgroundRepository"
private const val CLOUD_TIMEOUT_MS = 5000L

@Singleton
class BackgroundRepository @Inject constructor(
    private val purchasedBackgroundDao: PurchasedBackgroundDao,
    private val firebaseAuth: FirebaseAuth,
) {
    private val connector by lazy { ChorebooConnector.instance }

    /** Observe all purchased backgrounds for the current user. */
    fun getPurchasedBackgrounds(): Flow<List<PurchasedBackgroundEntity>> {
        val uid = firebaseAuth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return purchasedBackgroundDao.getPurchasedBackgrounds(uid)
    }

    /**
     * Purchase a background: deduct points locally (caller's responsibility), record in Room,
     * and write-through to Data Connect. Returns true on success, false on failure.
     */
    suspend fun purchaseBackground(backgroundId: String): Boolean {
        val uid = firebaseAuth.currentUser?.uid ?: return false
        val now = System.currentTimeMillis()
        val entity = PurchasedBackgroundEntity(
            ownerUid = uid,
            backgroundId = backgroundId,
            purchasedAt = now,
        )
        purchasedBackgroundDao.insertPurchase(entity)

        // Write-through to cloud (best-effort — failure is silent)
        try {
            val result = withTimeoutOrNull(CLOUD_TIMEOUT_MS) {
                connector.purchaseBackground.execute(backgroundId = backgroundId)
            }
            if (result == null) {
                Log.w(TAG, "purchaseBackground: cloud write timed out for $backgroundId")
            } else {
                Log.d(TAG, "Purchased background in cloud: $backgroundId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write purchase to cloud (purchase recorded locally)", e)
        }
        return true
    }

    /**
     * Sync purchased backgrounds from Data Connect to Room (cloud wins).
     * Called from SyncManager as part of the standard sync flow.
     */
    suspend fun syncFromCloud() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        try {
            val result = withTimeoutOrNull(CLOUD_TIMEOUT_MS) {
                connector.getMyPurchasedBackgrounds.execute()
            }
            if (result == null) {
                Log.w(TAG, "syncFromCloud: timed out")
                return
            }
            val cloudItems = result.data.purchasedBackgrounds
            if (cloudItems.isEmpty()) {
                Log.d(TAG, "No purchased backgrounds found in cloud")
                return
            }
            val entities = cloudItems.map { item ->
                PurchasedBackgroundEntity(
                    ownerUid = uid,
                    backgroundId = item.backgroundId,
                    purchasedAt = item.purchasedAt.toDate().time,
                )
            }
            purchasedBackgroundDao.insertAll(entities)
            Log.d(TAG, "Synced ${entities.size} purchased backgrounds from cloud")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync purchased backgrounds from cloud", e)
            throw e
        }
    }

    /**
     * Delete all local purchased background data for the current user.
     * Called on sign-out and account reset.
     */
    suspend fun clearLocalData() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        purchasedBackgroundDao.deleteAll(uid)
    }
}
