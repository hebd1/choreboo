package com.choreboo.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.choreboo.app.MainActivity
import com.choreboo.app.ChorebooApplication
import com.choreboo.app.R
import com.choreboo.app.data.datastore.UserPreferences
import com.choreboo.app.data.local.dao.ChorebooDao
import com.choreboo.app.data.repository.ChorebooRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PetMoodReceiver : BroadcastReceiver() {

    @Inject
    lateinit var chorebooRepository: ChorebooRepository

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var chorebooDao: ChorebooDao

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action != "com.choreboo.app.PET_MOOD_CHECK") return

        Timber.d("PetMoodReceiver: received mood check intent")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Get current choreboo and apply decay
                val choreboo = chorebooDao.getChorebooSync() ?: run {
                    Timber.w("PetMoodReceiver: no choreboo found")
                    return@launch
                }

                // Apply stat decay to get current state
                chorebooRepository.applyStatDecay()

                // Re-fetch the now-decayed choreboo
                val decayedChoreboo = chorebooDao.getChorebooSync() ?: return@launch
                val petName = decayedChoreboo.name

                // Check if any stat is critical (< 20)
                val isCritical =
                    decayedChoreboo.hunger < 20 ||
                    decayedChoreboo.happiness < 20 ||
                    decayedChoreboo.energy < 20

                if (!isCritical) {
                    Timber.d("PetMoodReceiver: pet not critical yet (h=%d, ha=%d, e=%d), rescheduling",
                        decayedChoreboo.hunger, decayedChoreboo.happiness, decayedChoreboo.energy)
                    // Reschedule for next critical time
                    PetMoodScheduler.schedulePredictiveAlarm(
                        context,
                        decayedChoreboo.hunger,
                        decayedChoreboo.happiness,
                        decayedChoreboo.energy,
                        decayedChoreboo.sleepUntil,
                        petName,
                    )
                    return@launch
                }

                // Mood is critical — check 24-hour cooldown
                val lastNotificationTime = userPreferences.lastMoodNotificationTime.first()
                val now = System.currentTimeMillis()
                val cooldownMs = 24 * 60 * 60 * 1000L

                if (now - lastNotificationTime < cooldownMs) {
                    Timber.d("PetMoodReceiver: cooldown active, skipping notification")
                    // Still reschedule next check
                    PetMoodScheduler.schedulePredictiveAlarm(
                        context,
                        decayedChoreboo.hunger,
                        decayedChoreboo.happiness,
                        decayedChoreboo.energy,
                        decayedChoreboo.sleepUntil,
                        petName,
                    )
                    return@launch
                }

                // Determine mood reason
                val moodReason = when {
                    decayedChoreboo.hunger < 20 -> "hungry"
                    decayedChoreboo.energy < 20 -> "tired"
                    else -> "sad"
                }

                Timber.d("PetMoodReceiver: posting critical mood notification (reason=$moodReason)")

                // Post notification
                postNotification(context, petName, moodReason)

                // Update cooldown timestamp
                userPreferences.setLastMoodNotificationTime(now)

                // Reschedule next alarm
                PetMoodScheduler.schedulePredictiveAlarm(
                    context,
                    decayedChoreboo.hunger,
                    decayedChoreboo.happiness,
                    decayedChoreboo.energy,
                    decayedChoreboo.sleepUntil,
                    petName,
                )
            } catch (e: Exception) {
                Timber.e(e, "PetMoodReceiver: error processing mood check")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun postNotification(context: Context, petName: String, reason: String) {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = android.app.PendingIntent.getActivity(
            context,
            NotificationUtils.PET_MOOD_NOTIF_REQUEST_ID,
            mainIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        // Randomly select title from variants
        val titles = listOf(
            context.getString(R.string.pet_mood_notif_title_1, petName),
            context.getString(R.string.pet_mood_notif_title_2, petName),
            context.getString(R.string.pet_mood_notif_title_3, petName),
            context.getString(R.string.pet_mood_notif_title_4, petName),
        )
        val title = titles.random()

        // Select message based on mood reason
        val message = when (reason) {
            "hungry" -> {
                val messages = listOf(
                    context.getString(R.string.pet_mood_notif_hungry_1, petName),
                    context.getString(R.string.pet_mood_notif_hungry_2, petName),
                    context.getString(R.string.pet_mood_notif_hungry_3, petName),
                    context.getString(R.string.pet_mood_notif_hungry_4, petName),
                    context.getString(R.string.pet_mood_notif_hungry_5, petName),
                )
                messages.random()
            }
            "tired" -> {
                val messages = listOf(
                    context.getString(R.string.pet_mood_notif_tired_1, petName),
                    context.getString(R.string.pet_mood_notif_tired_2, petName),
                    context.getString(R.string.pet_mood_notif_tired_3, petName),
                    context.getString(R.string.pet_mood_notif_tired_4, petName),
                    context.getString(R.string.pet_mood_notif_tired_5, petName),
                )
                messages.random()
            }
            else -> {
                val messages = listOf(
                    context.getString(R.string.pet_mood_notif_sad_1, petName),
                    context.getString(R.string.pet_mood_notif_sad_2, petName),
                    context.getString(R.string.pet_mood_notif_sad_3, petName),
                    context.getString(R.string.pet_mood_notif_sad_4, petName),
                    context.getString(R.string.pet_mood_notif_sad_5, petName),
                )
                messages.random()
            }
        }

        val notificationBuilder = NotificationCompat.Builder(context, ChorebooApplication.PET_ALERT_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)

        NotificationUtils.notifyIfPermitted(context, NotificationUtils.PET_MOOD_NOTIF_ID, notificationBuilder)
    }
}
