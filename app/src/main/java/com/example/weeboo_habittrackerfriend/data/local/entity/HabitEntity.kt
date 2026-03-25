package com.example.weeboo_habittrackerfriend.data.local.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val iconName: String = "CheckCircle",
    val frequency: String = "DAILY",
    val customDays: String? = null,
    val targetCount: Int = 1,
    val baseXp: Int = 10,
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
)
