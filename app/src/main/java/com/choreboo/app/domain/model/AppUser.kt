package com.choreboo.app.domain.model

data class AppUser(
    val uid: String,
    val displayName: String,
    val email: String? = null,
    val photoUrl: String? = null,
    val activeChorebooId: String? = null,
    val activePetType: String? = null,
    val householdId: String? = null,
    val householdName: String? = null,
    val totalPoints: Int = 0,
    val totalLifetimeXp: Int = 0,
)
