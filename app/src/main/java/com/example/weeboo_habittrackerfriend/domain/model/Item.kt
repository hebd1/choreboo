package com.example.weeboo_habittrackerfriend.domain.model
data class Item(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val type: ItemType = ItemType.FOOD,
    val rarity: String = "COMMON",
    val price: Int = 10,
    val effectValue: Int? = null,
    val effectStat: String? = null,
    val animationAsset: String? = null,
)
