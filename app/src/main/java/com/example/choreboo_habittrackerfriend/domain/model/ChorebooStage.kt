package com.example.choreboo_habittrackerfriend.domain.model
enum class ChorebooStage(val xpThreshold: Int) {
    EGG(0),
    BABY(100),
    CHILD(500),
    TEEN(1500),
    ADULT(5000),
    LEGENDARY(15000);
    companion object {
        fun fromTotalXp(totalXp: Int): ChorebooStage {
            val clamped = totalXp.coerceAtLeast(0)
            return entries.reversed().first { clamped >= it.xpThreshold }
        }
    }
}
