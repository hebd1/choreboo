package com.example.weeboo_habittrackerfriend.data.local.entity
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
@Entity(
    tableName = "equipped_items",
    foreignKeys = [
        ForeignKey(
            entity = WeebooEntity::class,
            parentColumns = ["id"],
            childColumns = ["weebooId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("weebooId"), Index("itemId")]
)
data class EquippedItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weebooId: Long,
    val itemId: Long,
    val slot: String,
)
