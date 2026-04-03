package com.example.choreboo_habittrackerfriend.domain.model

data class AppUser(
    val uid: String,
    val displayName: String,
    val email: String? = null,
    val photoUrl: String? = null,
    val householdId: String? = null,
    val householdName: String? = null,
)
