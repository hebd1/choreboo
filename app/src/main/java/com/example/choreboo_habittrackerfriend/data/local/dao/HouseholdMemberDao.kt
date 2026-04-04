package com.example.choreboo_habittrackerfriend.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.choreboo_habittrackerfriend.data.local.entity.HouseholdMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseholdMemberDao {

    /** Observe all cached household members with pets, ordered by display name. */
    @Query("SELECT * FROM household_members ORDER BY displayName ASC")
    fun getAllMembers(): Flow<List<HouseholdMemberEntity>>

    /**
     * Insert or replace all members — used when syncing from cloud.
     * Uses REPLACE strategy so a re-fetch updates existing rows in-place.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(members: List<HouseholdMemberEntity>)

    /**
     * Remove members whose UIDs are not in the given list — used for reconciliation
     * after a cloud sync so departed members are removed from the local cache.
     * Callers must ensure [uids] is non-empty; if the member list is empty, call
     * [deleteAll] instead.
     */
    @Query("DELETE FROM household_members WHERE uid NOT IN (:uids)")
    suspend fun deleteMembersNotIn(uids: List<String>)

    /** Delete all cached household members — used on sign-out and leave-household. */
    @Query("DELETE FROM household_members")
    suspend fun deleteAll()
}
