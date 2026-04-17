package com.choreboo.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.choreboo.app.data.repository.ChorebooRepository
import com.choreboo.app.data.repository.HabitRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

@HiltWorker
class ReminderRescheduleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val habitRepository: HabitRepository,
    private val chorebooRepository: ChorebooRepository,
    private val firebaseAuth: FirebaseAuth,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val currentUid = firebaseAuth.currentUser?.uid
            if (currentUid == null) {
                // No user is authenticated — don't reschedule any alarms.
                // This prevents stale alarms from a previous user from rescheduling after device reboot.
                Timber.d("ReminderRescheduleWorker: no authenticated user, skipping alarm reschedule")
                return Result.success()
            }

            Timber.d("ReminderRescheduleWorker: rescheduling habit reminders and pet mood alarms")

            // Reschedule habit reminders
            val habits = habitRepository.getAllHabits().first()
            habits.forEach { habit ->
                // Reschedule reminders only for habits the current user owns or is assigned to (fixes B6).
                // Personal habits: owned by current user.
                // Household habits: owned by current user or assigned to current user.
                // This prevents cross-user alarm contamination when multiple accounts are used on the same device.
                val isOwnedByUser = habit.ownerUid == currentUid
                val isAssignedToUser = habit.assignedToUid == currentUid

                // A3: Skip archived habits — they must not fire reminders.
                // A4: Owners only get reminders when the habit is NOT assigned to someone else.
                //     If the owner assigned it to another member, only that assignee gets the alarm.
                val shouldRemind = !habit.isArchived && habit.reminderEnabled && habit.reminderTime != null &&
                    ((isOwnedByUser && habit.assignedToUid == null) || isAssignedToUser)

                if (shouldRemind) {
                    HabitReminderScheduler.scheduleReminder(
                        applicationContext,
                        habit.id,
                        habit.title,
                        habit.reminderTime,
                        habit.customDays,
                    )
                }
            }

            // Reschedule pet mood alarm
            val choreboo = chorebooRepository.getChorebooSync()
            if (choreboo != null) {
                Timber.d("ReminderRescheduleWorker: rescheduling pet mood alarm")
                PetMoodScheduler.schedulePredictiveAlarm(
                    applicationContext,
                    choreboo.hunger,
                    choreboo.happiness,
                    choreboo.energy,
                    choreboo.sleepUntil,
                    choreboo.name,
                )
            }

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "ReminderRescheduleWorker: error during reschedule")
            Result.retry()
        }
    }
}

