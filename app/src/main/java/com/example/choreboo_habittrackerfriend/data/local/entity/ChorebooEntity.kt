package com.example.choreboo_habittrackerfriend.data.local.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "choreboos")
data class ChorebooEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "Choreboo",
    val stage: String = "EGG",
    val level: Int = 1,
    val xp: Int = 0,
    val hunger: Int = 80,
    val happiness: Int = 80,
    val energy: Int = 80,
    val petType: String = "FOX",
    val lastInteractionAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val sleepUntil: Long = 0,
)
