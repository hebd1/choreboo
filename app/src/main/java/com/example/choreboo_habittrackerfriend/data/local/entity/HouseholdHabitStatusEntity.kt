package com.example.choreboo_habittrackerfriend.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "household_habit_statuses",
    indices = [Index(value = ["cachedDate"])],
)
data class HouseholdHabitStatusEntity(
    @PrimaryKey
    val habitId: String, // Data Connect UUID of the habit
    val title: String,
    val iconName: String,
    val ownerName: String, // Display name of the habit's creator
    val ownerUid: String, // Firebase Auth UID of the habit's creator
    val baseXp: Int, // XP reward for completing the habit
    val assignedToUid: String? = null, // UID of the assigned member (null = unassigned)
    val assignedToName: String? = null, // Display name of the assigned member
    val completedByName: String? = null, // Display name of whoever completed it today (null = not yet completed)
    val completedByUid: String? = null, // UID of whoever completed it today
    val cachedDate: String? = null, // ISO date when completion data was fetched — invalidate when stale
)
