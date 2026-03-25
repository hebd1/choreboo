package com.example.choreboo_habittrackerfriend.data.local.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val type: String = "FOOD",
    val rarity: String = "COMMON",
    val price: Int = 10,
    val effectValue: Int? = null,
    val effectStat: String? = null,
    val animationAsset: String? = null,
)
