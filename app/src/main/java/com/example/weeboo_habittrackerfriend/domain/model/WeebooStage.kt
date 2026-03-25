package com.example.weeboo_habittrackerfriend.domain.model
enum class WeebooStage(val xpThreshold: Int, val displayName: String) {
    EGG(0, "Egg"),
    BABY(100, "Baby"),
    CHILD(500, "Child"),
    TEEN(1500, "Teen"),
    ADULT(5000, "Adult"),
    LEGENDARY(15000, "Legendary");
    companion object {
        fun fromTotalXp(totalXp: Int): WeebooStage {
            return entries.reversed().first { totalXp >= it.xpThreshold }
        }
    }
}
