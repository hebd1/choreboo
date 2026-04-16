package com.choreboo.app.data.local.dao
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.choreboo.app.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    /**
     * Get habits for a specific user: personal habits they own, household habits assigned to them,
     * and unassigned household habits (visible to all household members).
     * Filters by isArchived = 0.
     *
     * Visibility rules (fixes B5 where creators lost visibility of assigned habits):
     * - Personal habits: (isHouseholdHabit = 0 AND ownerUid = uid) — user sees habits they created
     * - Assigned household habits: (isHouseholdHabit = 1 AND assignedToUid = uid) — user sees habits assigned to them
     * - Unassigned household habits: (isHouseholdHabit = 1 AND assignedToUid IS NULL) — all household members see shared habits
     *
     * This ensures:
     * 1. Household habit creators see habits they assigned to others (doesn't exclude via assignedTo)
     * 2. Assignees see habits assigned to them
     * 3. All household members see unassigned (shared) household habits
     */
    @Query(
        """
        SELECT * FROM habits
        WHERE isArchived = 0
        AND (
            (isHouseholdHabit = 0 AND ownerUid = :uid)
            OR (isHouseholdHabit = 1 AND assignedToUid = :uid)
            OR (isHouseholdHabit = 1 AND assignedToUid IS NULL)
        )
        ORDER BY createdAt DESC
        """,
    )
    fun getHabitsForUser(uid: String): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    fun getHabitById(id: Long): Flow<HabitEntity?>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitByIdSync(id: Long): HabitEntity?

    @Query("SELECT * FROM habits WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getHabitByRemoteId(remoteId: String): HabitEntity?

    @Query("SELECT * FROM habits")
    suspend fun getAllHabitsSync(): List<HabitEntity>

    @Upsert
    suspend fun upsertHabit(habit: HabitEntity): Long
    @Update
    suspend fun updateHabit(habit: HabitEntity)
    @Delete
    suspend fun deleteHabit(habit: HabitEntity)
    @Query("UPDATE habits SET isArchived = 1 WHERE id = :id")
    suspend fun archiveHabit(id: Long)
    @Query("UPDATE habits SET isArchived = 0 WHERE id = :id")
    suspend fun unarchiveHabit(id: Long)
    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: Long)

    /**
     * Total count of habits owned by the given user (includes archived) — used for badge
     * computation. Scoped to [uid] so other household members' synced habits don't inflate
     * the count and unlock "Habit Collector" badges prematurely.
     */
    @Query("SELECT COUNT(*) FROM habits WHERE ownerUid = :uid")
    fun getTotalHabitCount(uid: String): Flow<Int>

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

    /** D2: Set pendingSync=true to protect this row from cloud-wins overwrite during write-through. */
    @Query("UPDATE habits SET pendingSync = 1 WHERE id = :id")
    suspend fun markPendingSync(id: Long)

    /** D2: Clear pendingSync=false once write-through succeeds or exhausts retries. */
    @Query("UPDATE habits SET pendingSync = 0 WHERE id = :id")
    suspend fun clearPendingSync(id: Long)
}
