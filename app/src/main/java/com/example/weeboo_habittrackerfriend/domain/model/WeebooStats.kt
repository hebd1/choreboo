package com.example.weeboo_habittrackerfriend.domain.model
data class WeebooStats(
    val id: Long = 0,
    val name: String = "Weeboo",
    val stage: WeebooStage = WeebooStage.EGG,
    val level: Int = 1,
    val xp: Int = 0,
    val hunger: Int = 80,
    val happiness: Int = 80,
    val energy: Int = 80,
    val lastInteractionAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
) {
    val overallMood: Int
        get() = (hunger + happiness + energy) / 3
    val mood: WeebooMood
        get() = when {
            hunger < 20 -> WeebooMood.HUNGRY
            energy < 20 -> WeebooMood.TIRED
            overallMood < 30 -> WeebooMood.SAD
            overallMood < 50 -> WeebooMood.IDLE
            overallMood < 70 -> WeebooMood.CONTENT
            else -> WeebooMood.HAPPY
        }
    val isHungry: Boolean get() = hunger < 30
    val needsAttention: Boolean get() = hunger < 20 || happiness < 20 || energy < 20
    val xpToNextLevel: Int get() = level * 50
    val xpProgressFraction: Float
        get() = if (xpToNextLevel > 0) xp.toFloat() / xpToNextLevel else 0f
}
