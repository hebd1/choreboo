package com.example.choreboo_habittrackerfriend

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.example.choreboo_habittrackerfriend.di.AppLifecycleObserver
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class ChorebooApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var appLifecycleObserver: AppLifecycleObserver

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        createNotificationChannels()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
        // Initialize AdMob — must be called before loading any ads.
        // Skip in debug builds to avoid polluting real ad metrics.
        if (!BuildConfig.DEBUG) {
            MobileAds.initialize(this)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Habit reminder channel (DEFAULT importance — sound + vibrate)
            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notif_channel_description)
            }
            manager.createNotificationChannel(reminderChannel)

            // Pet mood alert channel (LOW importance — no sound/vibrate by default)
            val petAlertChannel = NotificationChannel(
                PET_ALERT_CHANNEL_ID,
                getString(R.string.notif_pet_alert_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_pet_alert_channel_description)
            }
            manager.createNotificationChannel(petAlertChannel)
        }
    }

    companion object {
        const val REMINDER_CHANNEL_ID = "choreboo_reminders"
        const val PET_ALERT_CHANNEL_ID = "choreboo_pet_alerts"
    }
}
