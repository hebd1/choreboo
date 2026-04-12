package com.example.choreboo_habittrackerfriend.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import timber.log.Timber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        val action = intent?.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != "android.intent.action.MY_PACKAGE_REPLACED") return

        Timber.d("BootReceiver: received ${action}")

        // Use a WorkManager one-time job to reschedule all habit reminders and pet mood alarms
        val rescheduleWork = OneTimeWorkRequestBuilder<ReminderRescheduleWorker>()
            .setInitialDelay(30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "reschedule_habit_reminders",
            androidx.work.ExistingWorkPolicy.KEEP,
            rescheduleWork,
        )
    }
}

