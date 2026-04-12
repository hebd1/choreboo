package com.example.choreboo_habittrackerfriend.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

object HabitReminderScheduler {

    fun scheduleReminder(
        context: Context,
        habitId: Long,
        habitTitle: String,
        reminderTime: LocalTime,
        scheduledDays: List<String>,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return

        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = "com.example.choreboo_habittrackerfriend.HABIT_REMINDER"
            putExtra("habitId", habitId)
            putExtra("habitTitle", habitTitle)
            putExtra("reminderTime", reminderTime.toString())
            putExtra("scheduledDays", scheduledDays.toTypedArray())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (habitId and 0x7FFFFFFF).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val nextTriggerTime = calculateNextTriggerTime(reminderTime, scheduledDays)
        if (nextTriggerTime != null) {
            try {
                val canScheduleExact = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    alarmManager.canScheduleExactAlarms()
                } else {
                    true
                }

                if (canScheduleExact) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerTime.toInstant().toEpochMilli(),
                        pendingIntent,
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerTime.toInstant().toEpochMilli(),
                        pendingIntent,
                    )
                }
            } catch (_: SecurityException) {
                // If SCHEDULE_EXACT_ALARM permission isn't granted, fall back to inexact
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerTime.toInstant().toEpochMilli(),
                    pendingIntent,
                )
            }
        }
    }

    fun cancelReminder(context: Context, habitId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return

        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = "com.example.choreboo_habittrackerfriend.HABIT_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (habitId and 0x7FFFFFFF).toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return

        alarmManager.cancel(pendingIntent)
    }

    private fun calculateNextTriggerTime(
        reminderTime: LocalTime,
        scheduledDays: List<String>,
    ): ZonedDateTime? {
        if (scheduledDays.isEmpty()) return null

        val today = LocalDate.now()
        
        // Check if any scheduled day is a weekly pattern (MON-SUN)
        val weeklyDays = scheduledDays.filter { it.length == 3 && it.all { c -> c.isLetter() } }
        // Check if any scheduled day is a monthly pattern (D1-D31)
        val monthlyDays = scheduledDays.filter { it.startsWith("D") }.mapNotNull { it.drop(1).toIntOrNull() }

        // Try to find the next occurrence
        val nextWeeklyTrigger = if (weeklyDays.isNotEmpty()) {
            calculateNextWeeklyTrigger(reminderTime, weeklyDays)
        } else null

        val nextMonthlyTrigger = if (monthlyDays.isNotEmpty()) {
            calculateNextMonthlyTrigger(reminderTime, monthlyDays)
        } else null

        // Return whichever is sooner
        return when {
            nextWeeklyTrigger != null && nextMonthlyTrigger != null -> {
                if (nextWeeklyTrigger.isBefore(nextMonthlyTrigger)) nextWeeklyTrigger else nextMonthlyTrigger
            }
            nextWeeklyTrigger != null -> nextWeeklyTrigger
            nextMonthlyTrigger != null -> nextMonthlyTrigger
            else -> null
        }
    }

    private fun calculateNextWeeklyTrigger(
        reminderTime: LocalTime,
        weeklyDays: List<String>,
    ): ZonedDateTime? {
        val today = LocalDate.now()
        
        // First, try today if it's a scheduled day
        val todayShort = today.dayOfWeek.name.take(3).uppercase()
        if (weeklyDays.any { it.uppercase() == todayShort }) {
            val todayTrigger = today.atTime(reminderTime).atZone(java.time.ZoneId.systemDefault())
            if (todayTrigger.isAfter(ZonedDateTime.now())) {
                return todayTrigger
            }
        }

        // Find the next scheduled day within 7 days
        for (i in 1..7) {
            val checkDate = today.plusDays(i.toLong())
            val dayShort = checkDate.dayOfWeek.name.take(3).uppercase()
            if (weeklyDays.any { it.uppercase() == dayShort }) {
                return checkDate.atTime(reminderTime).atZone(java.time.ZoneId.systemDefault())
            }
        }

        return null
    }

    private fun calculateNextMonthlyTrigger(
        reminderTime: LocalTime,
        monthlyDays: List<Int>,
    ): ZonedDateTime? {
        val today = LocalDate.now()
        val now = ZonedDateTime.now()
        
        val lastDayOfMonth = today.lengthOfMonth()
        val normalizedDays = monthlyDays.map { day ->
            if (day > lastDayOfMonth) lastDayOfMonth else day
        }.distinct().sorted()

        // Check if today is a scheduled day
        if (today.dayOfMonth in normalizedDays) {
            val todayTrigger = today.atTime(reminderTime).atZone(java.time.ZoneId.systemDefault())
            if (todayTrigger.isAfter(now)) {
                return todayTrigger
            }
        }

        // Find the next scheduled day in this month
        for (day in normalizedDays) {
            if (day > today.dayOfMonth) {
                return today.withDayOfMonth(day).atTime(reminderTime).atZone(java.time.ZoneId.systemDefault())
            }
        }

        // If no match this month, try next month
        val nextMonth = today.plusMonths(1)
        val nextMonthLastDay = nextMonth.lengthOfMonth()
        val nextMonthNormalizedDays = monthlyDays.map { day ->
            if (day > nextMonthLastDay) nextMonthLastDay else day
        }.distinct().sorted()

        return if (nextMonthNormalizedDays.isNotEmpty()) {
            nextMonth.withDayOfMonth(nextMonthNormalizedDays.first())
                .atTime(reminderTime)
                .atZone(java.time.ZoneId.systemDefault())
        } else null
    }
}
