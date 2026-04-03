package com.example.choreboo_habittrackerfriend.domain.model
data class ChorebooStats(
    val id: Long = 0,
    val name: String = "Choreboo",
    val stage: ChorebooStage = ChorebooStage.EGG,
    val level: Int = 1,
    val xp: Int = 0,
    val hunger: Int = 10,
    val happiness: Int = 80,
    val energy: Int = 80,
    val petType: PetType = PetType.FOX,
    val lastInteractionAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val sleepUntil: Long = 0,
) {
    val overallMood: Int
        get() = (hunger + happiness + energy) / 3
    val mood: ChorebooMood
        get() = when {
            hunger < 20 -> ChorebooMood.HUNGRY
            energy < 20 -> ChorebooMood.TIRED
            overallMood < 30 -> ChorebooMood.SAD
            overallMood < 50 -> ChorebooMood.IDLE
            overallMood < 70 -> ChorebooMood.CONTENT
            else -> ChorebooMood.HAPPY
        }
    val isHungry: Boolean get() = hunger < 30
    val needsAttention: Boolean get() = hunger < 20 || happiness < 20 || energy < 20
    val xpToNextLevel: Int get() = level * 50
    val xpProgressFraction: Float
        get() = if (xpToNextLevel > 0) xp.toFloat() / xpToNextLevel else 0f
    val isSleeping: Boolean
        get() = sleepUntil > System.currentTimeMillis()
}
