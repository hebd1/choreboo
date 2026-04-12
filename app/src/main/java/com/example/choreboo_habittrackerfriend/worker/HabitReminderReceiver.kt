package com.example.choreboo_habittrackerfriend.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.choreboo_habittrackerfriend.MainActivity
import com.example.choreboo_habittrackerfriend.ChorebooApplication
import com.example.choreboo_habittrackerfriend.R
import com.example.choreboo_habittrackerfriend.data.local.dao.ChorebooDao
import com.example.choreboo_habittrackerfriend.data.local.dao.HabitLogDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class HabitReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var habitLogDao: HabitLogDao

    @Inject
    lateinit var chorebooDao: ChorebooDao

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action != "com.example.choreboo_habittrackerfriend.HABIT_REMINDER") return

        val habitId = intent.getLongExtra("habitId", -1L)
        val habitTitle = intent.getStringExtra("habitTitle") ?: context.getString(R.string.notif_habit_name_fallback)
        val reminderTimeStr = intent.getStringExtra("reminderTime") ?: "09:00"
        val scheduledDaysArray = intent.getStringArrayExtra("scheduledDays") ?: arrayOf()
        val scheduledDays = scheduledDaysArray.toList()

        if (habitId <= 0) return

        // Use goAsync to allow async work before completing
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Suppress the notification if the habit was already completed today
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val todayCount = habitLogDao.getCompletionCountForDate(habitId, today)
                if (todayCount >= 1) return@launch

                // Get pet name from database
                val choreboo = chorebooDao.getChorebooSync()
                val petName = choreboo?.name ?: context.getString(R.string.notif_pet_name_fallback)

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
            (habitId and 0x7FFFFFFF).toInt(),
            mainIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        // "Mark Complete" action — fires HabitCompleteReceiver directly without opening the app
        val completePendingIntent = HabitCompleteReceiver.buildPendingIntent(context, habitId, habitTitle)

        // Generate a cute message with the pet name
        val cuteMessage = generateCuteMessage(context, habitTitle, petName)

        val notificationBuilder = NotificationCompat.Builder(context, ChorebooApplication.REMINDER_CHANNEL_ID)
            .setContentTitle(habitTitle)
            .setContentText(cuteMessage)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.checkbox_on_background,
                context.getString(R.string.notif_mark_complete),
                completePendingIntent,
            )

        val notificationId = ((2000L + habitId) and 0x7FFFFFFF).toInt()
        NotificationUtils.notifyIfPermitted(context, notificationId, notificationBuilder)
    }

    private fun generateCuteMessage(context: Context, habitTitle: String, petName: String): String {
        // Use parameterized strings from strings.xml with context.getString()
        val messages = listOf(
            context.getString(R.string.notif_msg_1, habitTitle, petName),
            context.getString(R.string.notif_msg_2, habitTitle, petName),
            context.getString(R.string.notif_msg_3, habitTitle, petName),
            context.getString(R.string.notif_msg_4, habitTitle, petName),
            context.getString(R.string.notif_msg_5, habitTitle, petName),
            context.getString(R.string.notif_msg_6, habitTitle, petName),
            context.getString(R.string.notif_msg_7, habitTitle, petName),
            context.getString(R.string.notif_msg_8, habitTitle, petName),
            context.getString(R.string.notif_msg_9, habitTitle, petName),
            context.getString(R.string.notif_msg_10, habitTitle, petName),
            context.getString(R.string.notif_msg_11, habitTitle, petName),
            context.getString(R.string.notif_msg_12, habitTitle, petName),
            context.getString(R.string.notif_msg_13, habitTitle, petName),
            context.getString(R.string.notif_msg_14, habitTitle, petName),
            context.getString(R.string.notif_msg_15, habitTitle, petName),
            context.getString(R.string.notif_msg_16, habitTitle, petName),
            context.getString(R.string.notif_msg_17, habitTitle, petName),
            context.getString(R.string.notif_msg_18, habitTitle, petName),
            context.getString(R.string.notif_msg_19, habitTitle, petName),
            context.getString(R.string.notif_msg_20, habitTitle, petName),
            context.getString(R.string.notif_msg_21, habitTitle, petName),
            context.getString(R.string.notif_msg_22, habitTitle, petName),
            context.getString(R.string.notif_msg_23, habitTitle, petName),
            context.getString(R.string.notif_msg_24, habitTitle, petName),
            context.getString(R.string.notif_msg_25, habitTitle, petName),
            context.getString(R.string.notif_msg_26, habitTitle, petName),
            context.getString(R.string.notif_msg_27, habitTitle, petName),
            context.getString(R.string.notif_msg_28, habitTitle, petName),
            context.getString(R.string.notif_msg_29, habitTitle, petName),
            context.getString(R.string.notif_msg_30, habitTitle, petName),
        )
        return messages.random()
    }
}
