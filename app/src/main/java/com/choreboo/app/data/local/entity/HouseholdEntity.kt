package com.choreboo.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "households")
data class HouseholdEntity(
    @PrimaryKey
    val id: String, // Data Connect UUID
    val name: String, // Household display name (max 50 chars)
    val inviteCode: String, // 6-char alphanumeric code (unique in cloud)
    val createdByUid: String, // Firebase Auth UID of the creator
    val createdByName: String? = null, // Display name of the creator
)
