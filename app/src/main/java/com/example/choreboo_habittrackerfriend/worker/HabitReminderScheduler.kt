package com.example.choreboo_habittrackerfriend.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

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
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val nextTriggerTime = calculateNextTriggerTime(reminderTime, scheduledDays)
        if (nextTriggerTime != null) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerTime.toInstant().toEpochMilli(),
                    pendingIntent,
                )
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

        val intent = Intent(context, HabitReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.toInt(),
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
        val dayNames = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
        val todayIndex = today.dayOfWeek.value % 7  // 0=Mon, 6=Sun

        // First, try today if it's a scheduled day
        val todayShort = today.dayOfWeek.name.take(3).uppercase()
        if (scheduledDays.any { it.uppercase() == todayShort }) {
            val todayTrigger = today.atTime(reminderTime).atZone(java.time.ZoneId.systemDefault())
            if (todayTrigger.isAfter(ZonedDateTime.now())) {
                return todayTrigger
            }
        }

        // Find the next scheduled day
        for (i in 1..7) {
            val checkDate = today.plusDays(i.toLong())
            val dayShort = checkDate.dayOfWeek.name.take(3).uppercase()
            if (scheduledDays.any { it.uppercase() == dayShort }) {
                return checkDate.atTime(reminderTime).atZone(java.time.ZoneId.systemDefault())
            }
        }

        return null
    }
}
