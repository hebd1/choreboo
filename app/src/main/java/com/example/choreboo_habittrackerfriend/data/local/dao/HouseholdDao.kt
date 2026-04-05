package com.example.choreboo_habittrackerfriend.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.choreboo_habittrackerfriend.data.local.entity.HouseholdEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseholdDao {
    /**
     * Get the current user's household (reactive).
     * There is at most one row in this table at any time (the current user's household).
     */
    @Query("SELECT * FROM households LIMIT 1")
    fun getHousehold(): Flow<HouseholdEntity?>

    /**
     * Synchronous getter for the current household.
     */
    @Query("SELECT * FROM households LIMIT 1")
    suspend fun getHouseholdSync(): HouseholdEntity?

    /**
     * Insert or replace a household (upsert).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHousehold(household: HouseholdEntity): Long

    /**
     * Update an existing household.
     */
    @Update
    suspend fun updateHousehold(household: HouseholdEntity): Int

    /**
     * Delete all households (used on sign-out or leave household).
     */
    @Query("DELETE FROM households")
    suspend fun deleteAll()
}
