package com.choreboo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.choreboo.app.data.local.entity.HouseholdHabitStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class HouseholdHabitStatusDao {
    /**
     * Get all household habit statuses (reactive, unfiltered).
     */
    @Query("SELECT * FROM household_habit_statuses")
    abstract fun getAllHabitStatuses(): Flow<List<HouseholdHabitStatusEntity>>

    /**
     * Get household habit statuses for a specific date (reactive).
     * Returns only rows whose cachedDate matches the given date string.
     */
    @Query("SELECT * FROM household_habit_statuses WHERE cachedDate = :date")
    abstract fun getHabitStatusesForDate(date: String): Flow<List<HouseholdHabitStatusEntity>>

    /**
     * Get all household habit statuses (synchronous).
     */
    @Query("SELECT * FROM household_habit_statuses")
    abstract suspend fun getAllHabitStatusesSync(): List<HouseholdHabitStatusEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAll(statuses: List<HouseholdHabitStatusEntity>)

    /**
     * Atomically replace all household habit statuses.
     * Deletes every existing row first, then inserts the fresh set, so stale rows for habits
     * that were deleted in the cloud are never left behind.
     */
    @Transaction
    open suspend fun replaceAll(statuses: List<HouseholdHabitStatusEntity>) {
        deleteAll()
        insertAll(statuses)
    }

    /**
     * Delete all habit statuses (used when leaving a household or on sign-out).
     */
    @Query("DELETE FROM household_habit_statuses")
    abstract suspend fun deleteAll()
}
