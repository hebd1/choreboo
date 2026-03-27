package com.example.choreboo_habittrackerfriend.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.choreboo_habittrackerfriend.MainActivity
import com.example.choreboo_habittrackerfriend.ChorebooApplication

class HabitReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action != "com.example.choreboo_habittrackerfriend.HABIT_REMINDER") return

        val habitId = intent.getLongExtra("habitId", -1L)
        val habitTitle = intent.getStringExtra("habitTitle") ?: "Habit"

        if (habitId <= 0) return

        // Build and post the notification
        val mainIntent = Intent(context, MainActivity::class.java)
        mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            habitId.toInt(),
            mainIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, ChorebooApplication.REMINDER_CHANNEL_ID)
            .setContentTitle("Complete Your Habit!")
            .setContentText(habitTitle)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = (2000 + habitId).toInt()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
