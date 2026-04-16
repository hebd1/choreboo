package com.choreboo.app.data.local.entity
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
@Entity(
    tableName = "habits",
    indices = [Index("remoteId")],
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val iconName: String = "emoji_salad",
    val customDays: String = "MON,TUE,WED,THU,FRI,SAT,SUN",
    val difficulty: Int = 1,
    val baseXp: Int = 10,
    val reminderEnabled: Boolean = false,
    val reminderTime: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val isHouseholdHabit: Boolean = false,
    val ownerUid: String = "",
    val householdId: String? = null,
    val assignedToUid: String? = null,
    val assignedToName: String? = null,
    val remoteId: String? = null,
    /**
     * D2: True while a write-through is pending (in-flight or awaiting retry).
     * Cloud-to-local sync skips overwriting rows with pendingSync=true so that
     * a stale cloud value cannot clobber a local change that hasn't reached the
     * cloud yet. Cleared to false once the write-through succeeds or exhausts retries.
     */
    val pendingSync: Boolean = false,
)
