package com.choreboo.app.worker

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Shared utility for notification operations with Android 13+ permission handling.
 */
object NotificationUtils {

    // ── Shared notification ID / request code constants ───────────────────────
    /** Notification ID for pet mood alerts (predictive alarm + periodic worker). */
    const val PET_MOOD_NOTIF_ID = 9997

    /** PendingIntent request code for pet mood notifications. */
    const val PET_MOOD_NOTIF_REQUEST_ID = 9998

    /**
     * Offset added to a habit's local Room ID to produce the notification ID used by
     * [HabitReminderReceiver]. Using an offset avoids collisions with other notification IDs.
     */
    const val HABIT_REMINDER_NOTIF_OFFSET = 2000L

    /**
     * Offset added to a habit's local Room ID to produce the PendingIntent request code used by
     * [HabitCompleteReceiver]. Must differ from [HABIT_REMINDER_NOTIF_OFFSET] to avoid collisions.
     */
    const val HABIT_COMPLETE_NOTIF_OFFSET = 3000L

    /**
     * Posts a notification if the POST_NOTIFICATIONS permission is granted.
     * On Android 12 and below, always succeeds (no permission required).
     * On Android 13+, checks permission before posting.
     */
    fun notifyIfPermitted(
        context: Context,
        notificationId: Int,
        notification: NotificationCompat.Builder,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as? NotificationManager
                    ?: return
                notificationManager.notify(notificationId, notification.build())
            }
        } else {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as? NotificationManager
                ?: return
            notificationManager.notify(notificationId, notification.build())
        }
    }
}
