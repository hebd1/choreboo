package com.example.choreboo_habittrackerfriend.worker

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.edit
import com.example.choreboo_habittrackerfriend.ChorebooApplication
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.datastore.dataStore
import com.example.choreboo_habittrackerfriend.data.local.ChorebooDatabase
import com.example.choreboo_habittrackerfriend.data.local.entity.HabitLogEntity
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HabitCompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action != ACTION_HABIT_COMPLETE) return

        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        val habitTitle = intent.getStringExtra(EXTRA_HABIT_TITLE) ?: "Habit"

        if (habitId <= 0) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val database = ChorebooDatabase.getInstance(context)
                val habitDao = database.habitDao()
                val habitLogDao = database.habitLogDao()
                val chorebooDao = database.chorebooDao()
                val notificationId = (2000 + habitId).toInt()

                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

                // Guard: already completed today
                val todayCount = habitLogDao.getCompletionCountForDate(habitId, today)
                if (todayCount >= 1) {
                    replaceWithConfirmation(
                        context = context,
                        notificationId = notificationId,
                        habitTitle = habitTitle,
                        message = "Already completed today!",
                    )
                    return@launch
                }

                // Fetch habit for baseXp
                val habitEntity = habitDao.getHabitByIdSync(habitId) ?: return@launch

                // Calculate streak (same logic as HabitRepository.calculateStreak)
                val dates = habitLogDao.getCompletionDatesForHabit(habitId)
                var streak = 0
                if (dates.isNotEmpty()) {
                    var checkDate = LocalDate.parse(today, DateTimeFormatter.ISO_LOCAL_DATE).minusDays(1)
                    for (dateStr in dates) {
                        val logDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
                        if (logDate == checkDate) {
                            streak++
                            checkDate = checkDate.minusDays(1)
                        } else if (logDate.isBefore(checkDate)) {
                            break
                        }
                    }
                    if (dates.contains(today)) streak++
                }

                val baseXp = habitEntity.baseXp
                val xpEarned = (baseXp + (streak * 2)).coerceAtMost(baseXp * 3)
                val newStreak = streak + 1

                // Insert habit log
                habitLogDao.insertLog(
                    HabitLogEntity(
                        habitId = habitId,
                        date = today,
                        xpEarned = xpEarned,
                        streakAtCompletion = newStreak,
                    )
                )

                // Add points to DataStore
                context.dataStore.edit { prefs ->
                    val current = prefs[UserPreferences.TOTAL_POINTS] ?: 0
                    prefs[UserPreferences.TOTAL_POINTS] = current + xpEarned
                }

                // Add XP to pet (mirrors ChorebooRepository.addXp)
                val choreboo = chorebooDao.getChorebooSync()
                if (choreboo != null) {
                    val oldStage = try {
                        ChorebooStage.valueOf(choreboo.stage)
                    } catch (_: Exception) {
                        ChorebooStage.EGG
                    }
                    var newXp = choreboo.xp + xpEarned
                    var newLevel = choreboo.level
                    var xpNeeded = newLevel * 50
                    while (newXp >= xpNeeded) {
                        newXp -= xpNeeded
                        newLevel++
                        xpNeeded = newLevel * 50
                    }
                    val totalXpEarned = (1 until newLevel).sumOf { it * 50 } + newXp
                    val newStage = ChorebooStage.fromTotalXp(totalXpEarned)
                    chorebooDao.updateChoreboo(
                        choreboo.copy(
                            xp = newXp,
                            level = newLevel,
                            stage = newStage.name,
                            lastInteractionAt = System.currentTimeMillis(),
                        )
                    )

                    // Auto-feed: if hunger < 30 and user has enough points, silently feed
                    // (mirrors ChorebooRepository.autoFeedIfNeeded)
                    val updatedChoreboo = chorebooDao.getChorebooSync()
                    if (updatedChoreboo != null && updatedChoreboo.hunger < 30) {
                        val points = context.dataStore.data.first()[UserPreferences.TOTAL_POINTS] ?: 0
                        if (points >= 10) {
                            var deducted = false
                            context.dataStore.edit { prefs ->
                                val current = prefs[UserPreferences.TOTAL_POINTS] ?: 0
                                if (current >= 10) {
                                    prefs[UserPreferences.TOTAL_POINTS] = current - 10
                                    deducted = true
                                }
                            }
                            if (deducted) {
                                chorebooDao.updateChoreboo(
                                    updatedChoreboo.copy(
                                        hunger = (updatedChoreboo.hunger + 20).coerceAtMost(100),
                                    )
                                )
                            }
                        }
                    }

                    // Build confirmation message; include level-up hint if applicable
                    val streakText = if (newStreak > 1) " | $newStreak day streak!" else ""
                    val evolvedHint = if (newStage != oldStage) " ✦ ${newStage.displayName}!" else ""
                    val message = "+$xpEarned XP$streakText$evolvedHint"

                    replaceWithConfirmation(
                        context = context,
                        notificationId = notificationId,
                        habitTitle = habitTitle,
                        message = message,
                    )
                } else {
                    // No pet yet, still confirm completion
                    val streakText = if (newStreak > 1) " | $newStreak day streak!" else ""
                    replaceWithConfirmation(
                        context = context,
                        notificationId = notificationId,
                        habitTitle = habitTitle,
                        message = "+$xpEarned XP$streakText",
                    )
                }
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
        val notification = NotificationCompat.Builder(context, ChorebooApplication.REMINDER_CHANNEL_ID)
            .setContentTitle(habitTitle)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setTimeoutAfter(8_000L)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    companion object {
        const val ACTION_HABIT_COMPLETE = "com.example.choreboo_habittrackerfriend.HABIT_COMPLETE"
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
                // pending intents which use habitId.toInt() directly.
                (3000 + habitId).toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
