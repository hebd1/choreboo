package com.choreboo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.choreboo.app.data.local.entity.PurchasedBackgroundEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchasedBackgroundDao {

    /** Observe all purchased backgrounds for the given owner, ordered by purchase time. */
    @Query("SELECT * FROM purchased_backgrounds WHERE ownerUid = :ownerUid ORDER BY purchasedAt ASC")
    fun getPurchasedBackgrounds(ownerUid: String): Flow<List<PurchasedBackgroundEntity>>

    /** One-shot check — returns true if the user has purchased a specific background. */
    @Query("SELECT EXISTS(SELECT 1 FROM purchased_backgrounds WHERE ownerUid = :ownerUid AND backgroundId = :backgroundId)")
    suspend fun hasPurchased(ownerUid: String, backgroundId: String): Boolean

    /**
     * Insert a purchased background, ignoring duplicates.
     * Returns the new row-id, or -1 if already present (IGNORE conflict strategy).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPurchase(entity: PurchasedBackgroundEntity): Long

    /**
     * Upsert a list of purchased backgrounds from cloud sync.
     * Existing rows are left unchanged (IGNORE) so local purchase timestamps are preserved.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<PurchasedBackgroundEntity>)

    /** Delete all purchased backgrounds for the given owner — used on sign-out and reset. */
    @Query("DELETE FROM purchased_backgrounds WHERE ownerUid = :ownerUid")
    suspend fun deleteAll(ownerUid: String)
}
