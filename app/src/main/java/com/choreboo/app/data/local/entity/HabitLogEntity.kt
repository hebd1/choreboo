package com.choreboo.app.data.local.entity
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("habitId"),
        Index("date"),
        Index("remoteId"),
        Index("completedByUid"),
        Index(value = ["habitId", "date"], unique = true),
    ]
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val completedAt: Long = System.currentTimeMillis(),
    val date: String,
    val xpEarned: Int = 0,
    val streakAtCompletion: Int = 0,
    val completedByUid: String? = null,
    val remoteId: String? = null,
    /**
     * D2: True while a write-through is pending (in-flight or awaiting retry).
     * Cloud-to-local sync skips overwriting rows with pendingSync=true.
     * Cleared to false once the write-through succeeds or exhausts retries.
     */
    val pendingSync: Boolean = false,
)
