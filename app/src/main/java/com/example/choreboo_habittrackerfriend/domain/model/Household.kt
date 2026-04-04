package com.example.choreboo_habittrackerfriend.domain.model

data class Household(
    val id: String,
    val name: String,
    val inviteCode: String,
    val createdByUid: String,
    val createdByName: String? = null,
)

data class HouseholdMember(
    val uid: String,
    val displayName: String,
    val photoUrl: String? = null,
    val email: String? = null,
)

/**
 * Represents a household habit with its today-completion status and assignment info.
 * [completedByName] is non-null when someone in the household has already completed
 * this habit today, showing who did it.
 * [assignedToUid] and [assignedToName] show who the habit is assigned to;
 * both null means the habit is unassigned.
 */
data class HouseholdHabitStatus(
    val habitId: String,
    val title: String,
    val iconName: String,
    val ownerName: String,
    val ownerUid: String,
    val baseXp: Int,
    val assignedToUid: String? = null,
    val assignedToName: String? = null,
    val completedByName: String? = null,
    val completedByUid: String? = null,
)

data class HouseholdPet(
    val chorebooId: String,
    val name: String,
    val stage: ChorebooStage,
    val level: Int,
    val xp: Int,
    val hunger: Int,
    val happiness: Int,
    val energy: Int,
    val petType: PetType,
    val ownerName: String,
    val ownerUid: String,
    val ownerPhotoUrl: String? = null,
) {
    val mood: ChorebooMood
        get() {
            val overallMood = (hunger + happiness + energy) / 3
            return when {
                hunger < 20 -> ChorebooMood.HUNGRY
                energy < 20 -> ChorebooMood.TIRED
                overallMood < 30 -> ChorebooMood.SAD
                overallMood < 50 -> ChorebooMood.IDLE
                overallMood < 70 -> ChorebooMood.CONTENT
                else -> ChorebooMood.HAPPY
            }
        }
}
