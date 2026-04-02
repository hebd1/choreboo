package com.example.choreboo_habittrackerfriend.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class Habit(
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val iconName: String = "emoji_salad",
    val customDays: List<String> = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"),
    val difficulty: Int = 1,
    val baseXp: Int = 10,
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
) {
    fun isScheduledForToday(): Boolean {
        // Check if any day is a weekly day-of-week selector (MON, TUE, etc.)
        val weeklyDays = customDays.filter { it.length == 3 && it.all { c -> c.isLetter() } }
        if (weeklyDays.isNotEmpty()) {
            val todayShort = LocalDate.now().dayOfWeek.name.take(3).uppercase()
            if (weeklyDays.any { it.uppercase() == todayShort }) {
                return true
            }
        }

        // Check if any day is a monthly day-of-month selector (D1, D15, D31, etc.)
        val monthlyDays = customDays.filter { it.startsWith("D") }
        if (monthlyDays.isNotEmpty()) {
            val todayDayOfMonth = LocalDate.now().dayOfMonth
            val lastDayOfMonth = LocalDate.now().lengthOfMonth()
            
            return monthlyDays.any { dayStr ->
                val day = dayStr.substring(1).toIntOrNull() ?: return@any false
                // Handle special case: D31 matches the last day of the month for months with fewer days
                if (day == 31 && todayDayOfMonth == lastDayOfMonth) {
                    true
                } else {
                    day == todayDayOfMonth
                }
            }
        }

        return false
    }
}
