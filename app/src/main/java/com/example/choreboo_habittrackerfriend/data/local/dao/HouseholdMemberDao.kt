package com.example.choreboo_habittrackerfriend.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.example.choreboo_habittrackerfriend.data.local.entity.HouseholdMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseholdMemberDao {

    /** Observe all cached household members with pets, ordered by display name. */
    @Query("SELECT * FROM household_members ORDER BY displayName ASC")
    fun getAllMembers(): Flow<List<HouseholdMemberEntity>>

    // ── Low-level primitives used by partial-update helpers ───────────────────────────────────

    @Query(
        """INSERT OR IGNORE INTO household_members
           (uid, displayName, photoUrl, email,
            chorebooId, chorebooName, chorebooStage, chorebooLevel,
            chorebooXp, chorebooHunger, chorebooHappiness, chorebooEnergy,
            chorebooPetType, lastSyncedAt)
           VALUES (:uid, :displayName, :photoUrl, :email,
                   '', '', 'EGG', 1, 0, 100, 100, 100, 'FOX', :lastSyncedAt)""",
    )
    suspend fun insertIgnoreIdentity(
        uid: String,
        displayName: String,
        photoUrl: String?,
        email: String?,
        lastSyncedAt: Long,
    )

    @Query(
        """UPDATE household_members
           SET displayName   = :displayName,
               photoUrl      = :photoUrl,
               email         = :email,
               lastSyncedAt  = :lastSyncedAt
           WHERE uid = :uid""",
    )
    suspend fun updateIdentityColumns(
        uid: String,
        displayName: String,
        photoUrl: String?,
        email: String?,
        lastSyncedAt: Long,
    )

    @Query(
        """INSERT OR IGNORE INTO household_members
           (uid, displayName, photoUrl, email,
            chorebooId, chorebooName, chorebooStage, chorebooLevel,
            chorebooXp, chorebooHunger, chorebooHappiness, chorebooEnergy,
            chorebooPetType, lastSyncedAt)
           VALUES (:uid, :displayName, :photoUrl, NULL,
                   :chorebooId, :chorebooName, :chorebooStage, :chorebooLevel,
                   :chorebooXp, :chorebooHunger, :chorebooHappiness, :chorebooEnergy,
                   :chorebooPetType, :lastSyncedAt)""",
    )
    suspend fun insertIgnorePet(
        uid: String,
        displayName: String,
        photoUrl: String?,
        chorebooId: String,
        chorebooName: String,
        chorebooStage: String,
        chorebooLevel: Int,
        chorebooXp: Int,
        chorebooHunger: Int,
        chorebooHappiness: Int,
        chorebooEnergy: Int,
        chorebooPetType: String,
        lastSyncedAt: Long,
    )

    @Query(
        """UPDATE household_members
           SET displayName      = :displayName,
               photoUrl         = :photoUrl,
               chorebooId       = :chorebooId,
               chorebooName     = :chorebooName,
               chorebooStage    = :chorebooStage,
               chorebooLevel    = :chorebooLevel,
               chorebooXp       = :chorebooXp,
               chorebooHunger   = :chorebooHunger,
               chorebooHappiness = :chorebooHappiness,
               chorebooEnergy   = :chorebooEnergy,
               chorebooPetType  = :chorebooPetType,
               lastSyncedAt     = :lastSyncedAt
           WHERE uid = :uid""",
    )
    suspend fun updatePetColumns(
        uid: String,
        displayName: String,
        photoUrl: String?,
        chorebooId: String,
        chorebooName: String,
        chorebooStage: String,
        chorebooLevel: Int,
        chorebooXp: Int,
        chorebooHunger: Int,
        chorebooHappiness: Int,
        chorebooEnergy: Int,
        chorebooPetType: String,
        lastSyncedAt: Long,
    )

    // ── High-level partial-upsert helpers ─────────────────────────────────────────────────────

    /**
     * Upsert only identity columns (displayName, photoUrl, email).
     * Pet columns are written by [upsertPetColumns] and are never touched here.
     * Uses INSERT OR IGNORE + UPDATE so existing pet data is never overwritten.
     */
    @Transaction
    suspend fun upsertIdentityColumns(
        uid: String,
        displayName: String,
        photoUrl: String?,
        email: String?,
        lastSyncedAt: Long,
    ) {
        insertIgnoreIdentity(uid, displayName, photoUrl, email, lastSyncedAt)
        updateIdentityColumns(uid, displayName, photoUrl, email, lastSyncedAt)
    }

    /**
     * Upsert only pet columns (choreboo* fields, displayName, photoUrl).
     * The email column is never written here, so a prior [upsertIdentityColumns] call
     * always wins for that field.
     * Uses INSERT OR IGNORE + UPDATE so the email value is never overwritten.
     */
    @Transaction
    suspend fun upsertPetColumns(
        uid: String,
        displayName: String,
        photoUrl: String?,
        chorebooId: String,
        chorebooName: String,
        chorebooStage: String,
        chorebooLevel: Int,
        chorebooXp: Int,
        chorebooHunger: Int,
        chorebooHappiness: Int,
        chorebooEnergy: Int,
        chorebooPetType: String,
        lastSyncedAt: Long,
    ) {
        insertIgnorePet(
            uid, displayName, photoUrl,
            chorebooId, chorebooName, chorebooStage, chorebooLevel,
            chorebooXp, chorebooHunger, chorebooHappiness, chorebooEnergy,
            chorebooPetType, lastSyncedAt,
        )
        updatePetColumns(
            uid, displayName, photoUrl,
            chorebooId, chorebooName, chorebooStage, chorebooLevel,
            chorebooXp, chorebooHunger, chorebooHappiness, chorebooEnergy,
            chorebooPetType, lastSyncedAt,
        )
    }

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
