package com.example.choreboo_habittrackerfriend.worker

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.choreboo_habittrackerfriend.ChorebooApplication
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.repository.ChorebooRepository
import com.example.choreboo_habittrackerfriend.data.repository.HabitRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "HabitCompleteReceiver"

@AndroidEntryPoint
class HabitCompleteReceiver : BroadcastReceiver() {

    @Inject lateinit var habitRepository: HabitRepository
    @Inject lateinit var chorebooRepository: ChorebooRepository
    @Inject lateinit var userPreferences: UserPreferences

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action != ACTION_HABIT_COMPLETE) return

        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        val habitTitle = intent.getStringExtra(EXTRA_HABIT_TITLE) ?: "Habit"

        if (habitId <= 0) return

        val pendingResult = goAsync()
        val notificationId = (2000 + habitId).toInt()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Delegate to repositories — same flow as HabitListViewModel.completeHabit()
                val result = habitRepository.completeHabit(habitId)

                if (result.alreadyComplete) {
                    replaceWithConfirmation(
                        context = context,
                        notificationId = notificationId,
                        habitTitle = habitTitle,
                        message = "Already completed today!",
                    )
                    return@launch
                }

                // Add XP to pet (handles level-up, stage evolution, cloud sync)
                val xpResult = chorebooRepository.addXp(result.xpEarned)

                // Auto-feed if hungry (handles point deduction, cloud sync)
                chorebooRepository.autoFeedIfNeeded(userPreferences)

                // Build confirmation message
                val streakText = if (result.newStreak > 1) " | ${result.newStreak} day streak!" else ""
                val evolvedHint = if (xpResult.evolved && xpResult.newStage != null) {
                    " ✦ ${xpResult.newStage.displayName}!"
                } else {
                    ""
                }
                val levelUpHint = if (xpResult.levelsGained > 0 && !xpResult.evolved) {
                    " ↑ Level ${xpResult.newLevel}!"
                } else {
                    ""
                }
                val message = "+${result.xpEarned} XP$streakText$levelUpHint$evolvedHint"

                replaceWithConfirmation(
                    context = context,
                    notificationId = notificationId,
                    habitTitle = habitTitle,
                    message = message,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to complete habit from notification", e)
                replaceWithConfirmation(
                    context = context,
                    notificationId = notificationId,
                    habitTitle = habitTitle,
                    message = "Failed to complete. Try in the app.",
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
