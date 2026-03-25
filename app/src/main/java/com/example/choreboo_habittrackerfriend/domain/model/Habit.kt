package com.example.choreboo_habittrackerfriend.domain.model

import java.time.LocalDate

data class Habit(
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val iconName: String = "CheckCircle",
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val customDays: List<String>? = null,
    val targetCount: Int = 1,
    val baseXp: Int = 10,
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
) {
    fun isScheduledForToday(): Boolean {
        return when (frequency) {
            HabitFrequency.DAILY -> true
            HabitFrequency.WEEKLY -> true // any day of the week
            HabitFrequency.CUSTOM -> {
                if (customDays.isNullOrEmpty()) return true
                val todayShort = LocalDate.now().dayOfWeek.name
                    .take(3).uppercase() // "MON", "TUE", etc.
                customDays.any { it.uppercase() == todayShort }
            }
        }
    }
}
