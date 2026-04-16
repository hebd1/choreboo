package com.choreboo.app.domain.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Immutable
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
    val isHouseholdHabit: Boolean = false,
    val ownerUid: String? = null,
    val householdId: String? = null,
    val assignedToUid: String? = null,
    val assignedToName: String? = null,
    val remoteId: String? = null,
) {
    fun isScheduledForToday(today: LocalDate = LocalDate.now()): Boolean {
        // Check if any day is a weekly day-of-week selector (MON, TUE, etc.)
        val weeklyDays = customDays.filter { it.length == 3 && it.all { c -> c.isLetter() } }
        if (weeklyDays.isNotEmpty()) {
            val todayShort = today.dayOfWeek.name.take(3).uppercase()
            if (weeklyDays.any { it.uppercase() == todayShort }) {
                return true
            }
        }

        // Check if any day is a monthly day-of-month selector (D1, D15, D31, etc.)
        val monthlyDays = customDays.filter { it.startsWith("D") }
        if (monthlyDays.isNotEmpty()) {
            val todayDayOfMonth = today.dayOfMonth
            val lastDayOfMonth = today.lengthOfMonth()

            return monthlyDays.any { dayStr ->
                val day = dayStr.substring(1).toIntOrNull() ?: return@any false
                // If the scheduled day exceeds the month's length, treat the last day of
                // the month as the trigger (e.g. D31 fires on Feb 28, Apr 30, etc.).
                if (day >= lastDayOfMonth && todayDayOfMonth == lastDayOfMonth) {
                    true
                } else {
                    day == todayDayOfMonth
                }
            }
        }

        return false
    }

    /**
     * Returns the number of minutes until the next scheduled reminder, or null if no reminder
     * is configured. Looks up to 7 days ahead for weekly habits, 31 days for monthly.
     */
    fun timeUntilNextReminderMinutes(now: LocalDateTime = LocalDateTime.now()): Long? {
        if (!reminderEnabled || reminderTime == null) return null
        val isMonthly = customDays.any { it.startsWith("D") }
        val maxDays = if (isMonthly) 31 else 7
        for (daysAhead in 0..maxDays) {
            val candidate = now.toLocalDate().plusDays(daysAhead.toLong())
            if (isScheduledForToday(candidate)) {
                val reminderAt = candidate.atTime(reminderTime)
                if (reminderAt.isAfter(now)) {
                    return java.time.Duration.between(now, reminderAt).toMinutes()
                }
            }
        }
        return null
    }
}
