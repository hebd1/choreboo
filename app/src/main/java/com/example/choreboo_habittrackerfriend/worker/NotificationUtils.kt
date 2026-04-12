package com.example.choreboo_habittrackerfriend.worker

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Shared utility for notification operations with Android 13+ permission handling.
 */
object NotificationUtils {

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
