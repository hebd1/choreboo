package com.choreboo.app.worker

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.choreboo.app.ChorebooApplication
import com.choreboo.app.R
import com.choreboo.app.data.datastore.UserPreferences
import com.choreboo.app.data.local.dao.HabitDao
import com.choreboo.app.data.repository.ChorebooRepository
import com.choreboo.app.data.repository.HabitRepository
import com.choreboo.app.ui.util.displayNameRes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class HabitCompleteReceiver : BroadcastReceiver() {

    @Inject lateinit var habitRepository: HabitRepository
    @Inject lateinit var chorebooRepository: ChorebooRepository
    @Inject lateinit var userPreferences: UserPreferences
    @Inject lateinit var habitDao: HabitDao

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action != ACTION_HABIT_COMPLETE) return

        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        val habitTitle = intent.getStringExtra(EXTRA_HABIT_TITLE) ?: context.getString(R.string.notif_habit_name_fallback)

        if (habitId <= 0) return

        val pendingResult = goAsync()
        val notificationId = ((NotificationUtils.HABIT_REMINDER_NOTIF_OFFSET + habitId) and 0x7FFFFFFF).toInt()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val timedOut = withTimeoutOrNull(25_000L) {
                    // B1: Guard — look up the habit and verify today is a scheduled day.
                    // A notification action could be stale (e.g. user left it overnight); completing
                    // a habit on an unscheduled day would log an incorrect entry.
                    val habitEntity = habitDao.getHabitByIdSync(habitId)
                    if (habitEntity != null) {
                        val customDays = habitEntity.customDays
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        if (customDays.isNotEmpty() && !customDays.isScheduledForToday()) {
                            replaceWithConfirmation(
                                context = context,
                                notificationId = notificationId,
                                habitTitle = habitTitle,
                                message = context.getString(R.string.notif_already_completed),
                            )
                            return@withTimeoutOrNull
                        }
                    }

                    // Delegate to repositories — same flow as HabitListViewModel.completeHabit()
                    val result = habitRepository.completeHabit(habitId)

                    if (result.alreadyComplete) {
                        replaceWithConfirmation(
                            context = context,
                            notificationId = notificationId,
                            habitTitle = habitTitle,
                            message = context.getString(R.string.notif_already_completed),
                        )
                        return@withTimeoutOrNull
                    }

                    // Add XP to pet (handles level-up, stage evolution, cloud sync)
                    val xpResult = chorebooRepository.addXp(result.xpEarned)

                    // Auto-feed if hungry (handles point deduction, cloud sync)
                    chorebooRepository.autoFeedIfNeeded(userPreferences)

                    // A5: Reschedule the next alarm for this habit to ensure the reminder chain
                    // continues. The HabitReminderReceiver normally handles rescheduling when the
                    // alarm fires, but if the action is tapped quickly before the receiver's
                    // coroutine finishes or if the alarm was otherwise not rescheduled, this
                    // guarantees the next occurrence is always armed.
                    habitDao.getHabitByIdSync(habitId)?.let { entity ->
                        if (entity.reminderEnabled && entity.reminderTime != null) {
                            try {
                                val parsedTime = try {
                                    LocalTime.parse(entity.reminderTime)
                                } catch (_: Exception) { LocalTime.of(9, 0) }
                                val customDays = entity.customDays
                                    .split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                HabitReminderScheduler.scheduleReminder(
                                    context, entity.id, entity.title, parsedTime, customDays,
                                )
                            } catch (e: Exception) {
                                Timber.w(e, "A5: failed to reschedule reminder after completion for habitId=$habitId")
                            }
                        }
                    }

                    // Build confirmation message
                    val streakText = if (result.newStreak > 1) context.getString(R.string.notif_streak_suffix, result.newStreak) else ""
                    val evolvedHint = if (xpResult.evolved && xpResult.newStage != null) {
                        " ✦ ${context.getString(xpResult.newStage.displayNameRes())}!"
                    } else {
                        ""
                    }
                    val levelUpHint = if (xpResult.levelsGained > 0 && !xpResult.evolved) {
                        context.getString(R.string.notif_level_up_suffix, xpResult.newLevel)
                    } else {
                        ""
                    }
                    val message = context.getString(R.string.notif_xp_earned, result.xpEarned) + streakText + levelUpHint + evolvedHint

                    replaceWithConfirmation(
                        context = context,
                        notificationId = notificationId,
                        habitTitle = habitTitle,
                        message = message,
                    )
                }
                if (timedOut == null) {
                    Timber.w("HabitCompleteReceiver timed out for habitId=$habitId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to complete habit from notification")
                replaceWithConfirmation(
                    context = context,
                    notificationId = notificationId,
                    habitTitle = habitTitle,
                    message = context.getString(R.string.notif_complete_failed),
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun replaceWithConfirmation(
        context: Context,
        notificationId: Int,
        habitTitle: String,
        message: String,
    ) {
        val notificationBuilder = NotificationCompat.Builder(context, ChorebooApplication.REMINDER_CHANNEL_ID)
            .setContentTitle("\"$habitTitle\"")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setTimeoutAfter(8_000L)

        NotificationUtils.notifyIfPermitted(context, notificationId, notificationBuilder)
    }

    companion object {
        const val ACTION_HABIT_COMPLETE = "com.choreboo.app.HABIT_COMPLETE"
        const val EXTRA_HABIT_ID = "habitId"
        const val EXTRA_HABIT_TITLE = "habitTitle"

        fun buildPendingIntent(context: Context, habitId: Long, habitTitle: String): PendingIntent {
            val intent = Intent(context, HabitCompleteReceiver::class.java).apply {
                action = ACTION_HABIT_COMPLETE
                putExtra(EXTRA_HABIT_ID, habitId)
                putExtra(EXTRA_HABIT_TITLE, habitTitle)
            }
            return PendingIntent.getBroadcast(
                context,
                // Use a unique request code offset to avoid colliding with HabitReminderReceiver's
                // pending intents which use habitId directly.
                ((NotificationUtils.HABIT_COMPLETE_NOTIF_OFFSET + habitId) and 0x7FFFFFFF).toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}

/**
 * Returns true if today falls on a scheduled day according to [this] list of day codes.
 * Mirrors [com.choreboo.app.domain.model.Habit.isScheduledForToday].
 * An empty list is treated as "always scheduled" (daily).
 */
private fun List<String>.isScheduledForToday(): Boolean {
    if (isEmpty()) return true
    val today = java.time.LocalDate.now()
    val weeklyDays = filter { it.length == 3 && it.all { c -> c.isLetter() } }
    if (weeklyDays.isNotEmpty()) {
        val todayShort = today.dayOfWeek.name.take(3).uppercase()
        return weeklyDays.any { it.uppercase() == todayShort }
    }
    val monthlyDays = filter { it.startsWith("D", ignoreCase = true) }
    if (monthlyDays.isNotEmpty()) {
        val todayDom = today.dayOfMonth
        val lastDom = today.lengthOfMonth()
        return monthlyDays.any { dayStr ->
            val day = dayStr.substring(1).toIntOrNull() ?: return@any false
            (day >= lastDom && todayDom == lastDom) || day == todayDom
        }
    }
    return true
}
