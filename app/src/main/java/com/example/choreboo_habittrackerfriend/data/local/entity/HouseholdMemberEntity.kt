package com.example.choreboo_habittrackerfriend.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the household_members table.
 *
 * Caches all household members who have an active Choreboo. Members who have not yet
 * completed onboarding (no Choreboo created) are excluded. This table is a read-only
 * cloud cache: all writes are driven by [HouseholdRepository.refreshHouseholdPets] and
 * the table is cleared on sign-out or when the user leaves their household.
 *
 * [uid] is the Firebase Auth UID and serves as the primary key, making INSERT OR REPLACE
 * upserts idiomatic — a re-fetch always updates the row in-place rather than inserting
 * a duplicate.
 */
@Entity(tableName = "household_members")
data class HouseholdMemberEntity(
    @PrimaryKey val uid: String,
    val displayName: String,
    val photoUrl: String? = null,
    val chorebooId: String,
    val chorebooName: String,
    val chorebooStage: String = "EGG",
    val chorebooLevel: Int = 1,
    val chorebooXp: Int = 0,
    val chorebooHunger: Int = 100,
    val chorebooHappiness: Int = 100,
    val chorebooEnergy: Int = 100,
    val chorebooPetType: String = "FOX",
    val lastSyncedAt: Long = System.currentTimeMillis(),
)
