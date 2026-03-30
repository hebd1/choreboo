package com.example.choreboo_habittrackerfriend.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class Habit(
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val iconName: String = "emoji_salad",
    val customDays: List<String> = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"),
    val targetCount: Int = 1,
    val baseXp: Int = 10,
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
) {
    fun isScheduledForToday(): Boolean {
        val todayShort = LocalDate.now().dayOfWeek.name
            .take(3).uppercase()
        return customDays.any { it.uppercase() == todayShort }
    }
}
