package com.example.choreboo_habittrackerfriend.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.choreboo_habittrackerfriend.MainActivity
import com.example.choreboo_habittrackerfriend.ChorebooApplication
import com.example.choreboo_habittrackerfriend.R
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.local.ChorebooDatabase
import com.example.choreboo_habittrackerfriend.data.repository.ChorebooRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Periodic (6-hour) fallback worker that checks if the pet's mood is critical.
 * Serves as a safety net in case the predictive alarm was missed or cancelled.
 */
@HiltWorker
class PetMoodCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val chorebooRepository: ChorebooRepository,
    private val userPreferences: UserPreferences,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("PetMoodCheckWorker: running periodic mood check")

            val database = ChorebooDatabase.getInstance(applicationContext)

            val choreboo = database.chorebooDao().getChorebooSync() ?: run {
                Timber.d("PetMoodCheckWorker: no choreboo found, skipping")
                return Result.success()
            }

            val petName = choreboo.name

            // Apply stat decay to get current state
            chorebooRepository.applyStatDecay()

            // Re-fetch the now-decayed choreboo
            val decayedChoreboo = database.chorebooDao().getChorebooSync() ?: return Result.success()

            // Check if any stat is critical (< 20)
            val isCritical =
                decayedChoreboo.hunger < 20 ||
                decayedChoreboo.happiness < 20 ||
                decayedChoreboo.energy < 20

            if (!isCritical) {
                Timber.d(
                    "PetMoodCheckWorker: pet not critical (h=%d, ha=%d, e=%d)",
                    decayedChoreboo.hunger,
                    decayedChoreboo.happiness,
                    decayedChoreboo.energy,
                )
                // Reschedule predictive alarm
                PetMoodScheduler.schedulePredictiveAlarm(
                    applicationContext,
                    decayedChoreboo.hunger,
                    decayedChoreboo.happiness,
                    decayedChoreboo.energy,
                    decayedChoreboo.sleepUntil,
                    petName,
                )
                return Result.success()
            }

            // Mood is critical — check 24-hour cooldown
            val lastNotificationTime = userPreferences.lastMoodNotificationTime.first()
            val now = System.currentTimeMillis()
            val cooldownMs = 24 * 60 * 60 * 1000L

            if (now - lastNotificationTime < cooldownMs) {
                Timber.d("PetMoodCheckWorker: cooldown active, skipping notification")
                // Reschedule predictive alarm
                PetMoodScheduler.schedulePredictiveAlarm(
                    applicationContext,
                    decayedChoreboo.hunger,
                    decayedChoreboo.happiness,
                    decayedChoreboo.energy,
                    decayedChoreboo.sleepUntil,
                    petName,
                )
                return Result.success()
            }

            // Determine mood reason
            val moodReason = when {
                decayedChoreboo.hunger < 20 -> "hungry"
                decayedChoreboo.energy < 20 -> "tired"
                else -> "sad"
            }

            Timber.d("PetMoodCheckWorker: posting critical mood notification (reason=$moodReason)")

            // Post notification
            postNotification(petName, moodReason)

            // Update cooldown timestamp
            userPreferences.setLastMoodNotificationTime(now)

            // Reschedule predictive alarm
            PetMoodScheduler.schedulePredictiveAlarm(
                applicationContext,
                decayedChoreboo.hunger,
                decayedChoreboo.happiness,
                decayedChoreboo.energy,
                decayedChoreboo.sleepUntil,
                petName,
            )

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "PetMoodCheckWorker: error during mood check")
            Result.retry()
        }
    }

    private fun postNotification(petName: String, reason: String) {
        val mainIntent = android.content.Intent(applicationContext, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = android.app.PendingIntent.getActivity(
            applicationContext,
            PET_MOOD_NOTIF_REQUEST_ID,
            mainIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        // Randomly select title from variants
        val titles = listOf(
            applicationContext.getString(R.string.pet_mood_notif_title_1, petName),
            applicationContext.getString(R.string.pet_mood_notif_title_2, petName),
            applicationContext.getString(R.string.pet_mood_notif_title_3, petName),
            applicationContext.getString(R.string.pet_mood_notif_title_4, petName),
        )
        val title = titles.random()

        // Select message based on mood reason
        val message = when (reason) {
            "hungry" -> {
                val messages = listOf(
                    applicationContext.getString(R.string.pet_mood_notif_hungry_1, petName),
                    applicationContext.getString(R.string.pet_mood_notif_hungry_2, petName),
                    applicationContext.getString(R.string.pet_mood_notif_hungry_3, petName),
                    applicationContext.getString(R.string.pet_mood_notif_hungry_4, petName),
                    applicationContext.getString(R.string.pet_mood_notif_hungry_5, petName),
                )
                messages.random()
            }
            "tired" -> {
                val messages = listOf(
                    applicationContext.getString(R.string.pet_mood_notif_tired_1, petName),
                    applicationContext.getString(R.string.pet_mood_notif_tired_2, petName),
                    applicationContext.getString(R.string.pet_mood_notif_tired_3, petName),
                    applicationContext.getString(R.string.pet_mood_notif_tired_4, petName),
                    applicationContext.getString(R.string.pet_mood_notif_tired_5, petName),
                )
                messages.random()
            }
            else -> {
                val messages = listOf(
                    applicationContext.getString(R.string.pet_mood_notif_sad_1, petName),
                    applicationContext.getString(R.string.pet_mood_notif_sad_2, petName),
                    applicationContext.getString(R.string.pet_mood_notif_sad_3, petName),
                    applicationContext.getString(R.string.pet_mood_notif_sad_4, petName),
                    applicationContext.getString(R.string.pet_mood_notif_sad_5, petName),
                )
                messages.random()
            }
        }

        val notificationBuilder = NotificationCompat.Builder(applicationContext, ChorebooApplication.PET_ALERT_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)

        NotificationUtils.notifyIfPermitted(applicationContext, PET_MOOD_NOTIF_ID, notificationBuilder)
    }

    private companion object {
        const val PET_MOOD_NOTIF_REQUEST_ID = 9998
        const val PET_MOOD_NOTIF_ID = 9997
    }
}
