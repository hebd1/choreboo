package com.example.choreboo_habittrackerfriend.data.repository

import com.example.choreboo_habittrackerfriend.data.local.dao.PurchasedBackgroundDao
import com.example.choreboo_habittrackerfriend.data.local.entity.PurchasedBackgroundEntity
import com.example.choreboo_habittrackerfriend.dataconnect.ChorebooConnector
import com.example.choreboo_habittrackerfriend.dataconnect.execute
import com.example.choreboo_habittrackerfriend.dataconnect.instance
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

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
     * and write-through to Data Connect — passing [cost] and [newTotalPoints] so the server
     * can atomically verify the balance and update it (P5-01 fix). Returns true on success,
     * false on failure.
     */
    suspend fun purchaseBackground(backgroundId: String, cost: Int, newTotalPoints: Int): Boolean {
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
                connector.purchaseBackground.execute(
                    backgroundId = backgroundId,
                    cost = cost,
                    newTotalPoints = newTotalPoints,
                )
            }
            if (result == null) {
                Timber.w("purchaseBackground: cloud write timed out for %s", backgroundId)
            } else {
                Timber.d("Purchased background in cloud: %s", backgroundId)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to write purchase to cloud (purchase recorded locally)")
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
                Timber.w("syncFromCloud: timed out")
                return
            }
            val cloudItems = result.data.purchasedBackgrounds
            if (cloudItems.isEmpty()) {
                Timber.d("No purchased backgrounds found in cloud")
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
            Timber.d("Synced %d purchased backgrounds from cloud", entities.size)
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync purchased backgrounds from cloud")
            throw e
        }
    }

    /**
     * Delete all local purchased background data for the given user.
     * Accepts an explicit [uid] so this works even after the Firebase Auth user has been deleted
     * (e.g. during account reset, where auth deletion happens before local cleanup — D8 fix).
     */
    suspend fun clearLocalData(uid: String) {
        purchasedBackgroundDao.deleteAll(uid)
    }
}
