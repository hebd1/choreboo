package com.example.choreboo_habittrackerfriend.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.choreboo_habittrackerfriend.data.local.entity.HouseholdHabitStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseholdHabitStatusDao {
    /**
     * Get all household habit statuses (reactive, unfiltered).
     */
    @Query("SELECT * FROM household_habit_statuses")
    fun getAllHabitStatuses(): Flow<List<HouseholdHabitStatusEntity>>

    /**
     * Get household habit statuses for a specific date (reactive).
     * Returns only rows whose cachedDate matches the given date string.
     */
    @Query("SELECT * FROM household_habit_statuses WHERE cachedDate = :date")
    fun getHabitStatusesForDate(date: String): Flow<List<HouseholdHabitStatusEntity>>

    /**
     * Get all household habit statuses (synchronous).
     */
    @Query("SELECT * FROM household_habit_statuses")
    suspend fun getAllHabitStatusesSync(): List<HouseholdHabitStatusEntity>

    /**
     * Insert or replace a list of habit statuses (replaceAll upsert).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceAll(statuses: List<HouseholdHabitStatusEntity>)

    /**
     * Delete all habit statuses (used when leaving a household or on sign-out).
     */
    @Query("DELETE FROM household_habit_statuses")
    suspend fun deleteAll()
}
