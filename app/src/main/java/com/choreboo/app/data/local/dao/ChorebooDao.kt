package com.choreboo.app.data.local.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.choreboo.app.data.local.entity.ChorebooEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface ChorebooDao {
    @Query("SELECT * FROM choreboos WHERE isActive = 1 LIMIT 1")
    fun getActiveChoreboo(): Flow<ChorebooEntity?>

    @Query("SELECT * FROM choreboos WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveChorebooSync(): ChorebooEntity?

    @Query("SELECT * FROM choreboos ORDER BY createdAt ASC")
    fun getAllChoreboos(): Flow<List<ChorebooEntity>>

    @Query("SELECT * FROM choreboos ORDER BY createdAt ASC")
    suspend fun getAllChoreboosSync(): List<ChorebooEntity>

    @Query("SELECT * FROM choreboos WHERE petType = :petType LIMIT 1")
    suspend fun getChorebooByPetType(petType: String): ChorebooEntity?

    @Query("SELECT * FROM choreboos WHERE id = :id LIMIT 1")
    suspend fun getChorebooById(id: Long): ChorebooEntity?

    @Query("SELECT * FROM choreboos WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getChorebooByRemoteId(remoteId: String): ChorebooEntity?

    @Query("SELECT * FROM choreboos WHERE ownerUid = :ownerUid AND petType = :petType LIMIT 1")
    suspend fun getChorebooByOwnerAndPetType(ownerUid: String, petType: String): ChorebooEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChoreboo(choreboo: ChorebooEntity): Long

    @Update
    suspend fun updateChoreboo(choreboo: ChorebooEntity)

    @Query("UPDATE choreboos SET isActive = 0")
    suspend fun clearActiveChoreboo()

    @Query("UPDATE choreboos SET isActive = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun setActiveChoreboo(id: Long)

    @Query("DELETE FROM choreboos WHERE remoteId IS NOT NULL AND remoteId NOT IN (:remoteIds)")
    suspend fun deleteRemoteChoreboosNotIn(remoteIds: List<String>)

    /** Delete all choreboos — used for sign-out data cleanup. */
    @Query("DELETE FROM choreboos")
    suspend fun deleteAllChoreboos()

    /** D2: Set pendingSync=true to protect this choreboo from cloud-wins overwrite during write-through. */
    @Query("UPDATE choreboos SET pendingSync = 1 WHERE id = :id")
    suspend fun markPendingSync(id: Long)

    /** D2: Clear pendingSync=false once write-through succeeds or exhausts retries. */
    @Query("UPDATE choreboos SET pendingSync = 0 WHERE id = :id")
    suspend fun clearPendingSync(id: Long)
}
