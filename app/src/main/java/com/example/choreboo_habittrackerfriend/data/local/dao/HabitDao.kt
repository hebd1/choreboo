package com.example.choreboo_habittrackerfriend.data.local.dao
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.choreboo_habittrackerfriend.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getAllHabits(): Flow<List<HabitEntity>>
    @Query("SELECT * FROM habits WHERE id = :id")
    fun getHabitById(id: Long): Flow<HabitEntity?>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitByIdSync(id: Long): HabitEntity?

    @Query("SELECT * FROM habits WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getHabitByRemoteId(remoteId: String): HabitEntity?

    @Query("SELECT * FROM habits")
    suspend fun getAllHabitsSync(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: HabitEntity): Long
    @Update
    suspend fun updateHabit(habit: HabitEntity)
    @Delete
    suspend fun deleteHabit(habit: HabitEntity)
    @Query("UPDATE habits SET isArchived = 1 WHERE id = :id")
    suspend fun archiveHabit(id: Long)
    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: Long)

    /** Total count of all habits (including archived) — used for badge computation. */
    @Query("SELECT COUNT(*) FROM habits")
    fun getTotalHabitCount(): Flow<Int>

    /** All habits that have been synced to cloud (non-null remoteId) — used for G13 reconciliation. */
    @Query("SELECT * FROM habits WHERE remoteId IS NOT NULL")
    suspend fun getHabitsWithRemoteId(): List<HabitEntity>

    /** Bulk-delete habits by local id — used for G13 reconciliation. */
    @Query("DELETE FROM habits WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    /** Delete all habits — used for sign-out data cleanup. */
    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()

    /**
     * Delete all household habits not owned by the given UID.
     * Used when a user leaves a household — other members' synced habits are removed from Room.
     */
    @Query("DELETE FROM habits WHERE ownerUid != :ownerUid AND isHouseholdHabit = 1")
    suspend fun deleteNonOwnedHouseholdHabits(ownerUid: String)

    /**
     * Return all household habits that were synced from other household members
     * (have a remoteId and are not owned by [ownerUid]).
     * Used for reconciliation after household habit sync.
     */
    @Query("SELECT * FROM habits WHERE remoteId IS NOT NULL AND ownerUid != :ownerUid AND isHouseholdHabit = 1")
    suspend fun getOtherMembersHouseholdHabits(ownerUid: String): List<HabitEntity>

    /** Returns true if the user has any household habits (owned or synced from others). */
    @Query("SELECT COUNT(*) > 0 FROM habits WHERE isHouseholdHabit = 1 AND householdId IS NOT NULL")
    suspend fun hasHouseholdHabits(): Boolean
}
