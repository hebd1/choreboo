package com.example.choreboo_habittrackerfriend.data.local.entity
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
@Entity(
    tableName = "equipped_items",
    foreignKeys = [
        ForeignKey(
            entity = ChorebooEntity::class,
            parentColumns = ["id"],
            childColumns = ["chorebooId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chorebooId"), Index("itemId")]
)
data class EquippedItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chorebooId: Long,
    val itemId: Long,
    val slot: String,
)
