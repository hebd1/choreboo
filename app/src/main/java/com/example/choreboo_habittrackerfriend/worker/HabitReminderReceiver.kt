package com.example.choreboo_habittrackerfriend.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.choreboo_habittrackerfriend.MainActivity
import com.example.choreboo_habittrackerfriend.ChorebooApplication
import com.example.choreboo_habittrackerfriend.R
import com.example.choreboo_habittrackerfriend.data.local.ChorebooDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class HabitReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action != "com.example.choreboo_habittrackerfriend.HABIT_REMINDER") return

        val habitId = intent.getLongExtra("habitId", -1L)
        val habitTitle = intent.getStringExtra("habitTitle") ?: "Habit"
        val reminderTimeStr = intent.getStringExtra("reminderTime") ?: "09:00"
        val scheduledDaysArray = intent.getStringArrayExtra("scheduledDays") ?: arrayOf()
        val scheduledDays = scheduledDaysArray.toList()

        if (habitId <= 0) return

        // Use goAsync to allow async work before completing
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val database = ChorebooDatabase.getInstance(context)

                // Suppress the notification if the habit was already completed today
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val todayCount = database.habitLogDao().getCompletionCountForDate(habitId, today)
                if (todayCount >= 1) return@launch

                // Get pet name from database
                val choreboo = database.chorebooDao().getChorebooSync()
                val petName = choreboo?.name ?: "Your Choreboo"

                // Parse reminder time
                val reminderTime = try {
                    LocalTime.parse(reminderTimeStr)
                } catch (_: Exception) {
                    LocalTime.of(9, 0)
                }

                // Post notification with cute message and Mark Complete action
                postNotification(context, habitId, habitTitle, petName)

                // Reschedule the next alarm
                if (scheduledDays.isNotEmpty()) {
                    HabitReminderScheduler.scheduleReminder(
                        context,
                        habitId,
                        habitTitle,
                        reminderTime,
                        scheduledDays,
                    )
                }
            } finally {
                // Finish the pending broadcast
                pendingResult.finish()
            }
        }
    }

    private fun postNotification(context: Context, habitId: Long, habitTitle: String, petName: String) {
        val mainIntent = Intent(context, MainActivity::class.java)
        mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val contentPendingIntent = android.app.PendingIntent.getActivity(
            context,
            habitId.toInt(),
            mainIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        // "Mark Complete" action — fires HabitCompleteReceiver directly without opening the app
        val completePendingIntent = HabitCompleteReceiver.buildPendingIntent(context, habitId, habitTitle)

        // Generate a cute message with the pet name
        val cuteMessage = generateCuteMessage(context, habitTitle, petName)

        val notification = NotificationCompat.Builder(context, ChorebooApplication.REMINDER_CHANNEL_ID)
            .setContentTitle(habitTitle)
            .setContentText(cuteMessage)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.checkbox_on_background,
                context.getString(R.string.notif_mark_complete),
                completePendingIntent,
            )
            .build()

        val notificationId = (2000 + habitId).toInt()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun generateCuteMessage(context: Context, habitTitle: String, petName: String): String {
        // Use parameterized strings from strings.xml with context.getString()
        val messages = listOf(
            context.getString(R.string.notif_msg_time_to, habitTitle),
            context.getString(R.string.notif_msg_believes_in_you, habitTitle, petName),
            context.getString(R.string.notif_msg_dont_forget, petName),
            context.getString(R.string.notif_msg_feeling_neglected, habitTitle, petName),
            context.getString(R.string.notif_msg_cheering_you_on, petName),
            context.getString(R.string.notif_msg_buddy_waiting, petName),
            context.getString(R.string.notif_msg_crush_it, habitTitle, petName),
            context.getString(R.string.notif_msg_keep_pet_happy, habitTitle, petName),
            context.getString(R.string.notif_msg_misses_you, petName),
            context.getString(R.string.notif_msg_rooting_for_you, petName),
        )
        return messages.random()
    }
}
