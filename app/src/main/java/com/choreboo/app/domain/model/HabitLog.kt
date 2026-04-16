package com.choreboo.app.domain.model

/**
 * Domain model for a single habit completion log entry.
 * Mirrors [com.choreboo.app.data.local.entity.HabitLogEntity]
 * but belongs to the domain layer so ViewModels never hold entity references.
 */
data class HabitLog(
    val id: Long = 0,
    val habitId: Long,
    val completedAt: Long = System.currentTimeMillis(),
    /** ISO-8601 date string, e.g. "2026-04-15". */
    val date: String,
    val xpEarned: Int,
    val streakAtCompletion: Int,
    val completedByUid: String?,
    val remoteId: String? = null,
)
