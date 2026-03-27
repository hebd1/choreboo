package com.example.choreboo_habittrackerfriend.data.local.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val iconName: String = "CheckCircle",
    val customDays: String = "MON,TUE,WED,THU,FRI,SAT,SUN",
    val targetCount: Int = 1,
    val baseXp: Int = 10,
    val reminderEnabled: Boolean = false,
    val reminderTime: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
)
