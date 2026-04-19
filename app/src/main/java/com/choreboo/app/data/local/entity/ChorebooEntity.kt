package com.choreboo.app.data.local.entity
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
@Entity(
    tableName = "choreboos",
    indices = [Index("remoteId"), Index(value = ["ownerUid", "petType"], unique = true)],
)
data class ChorebooEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "Choreboo",
    val stage: String = "EGG",
    val level: Int = 1,
    val xp: Int = 0,
    val hunger: Int = 10,
    val happiness: Int = 80,
    val energy: Int = 80,
    val petType: String = "FOX",
    val lastInteractionAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val sleepUntil: Long = 0,
    val ownerUid: String? = null,
    val remoteId: String? = null,
    val isActive: Boolean = false,
    /** Selected background id (matches BackgroundItem.id). Null = Default mood gradient. */
    val backgroundId: String? = null,
    /**
     * D2: True while a write-through is pending (in-flight or awaiting retry).
     * Cloud-to-local sync skips overwriting this row while pendingSync=true.
     * Cleared to false once the write-through succeeds or exhausts retries.
     */
    val pendingSync: Boolean = false,
)
