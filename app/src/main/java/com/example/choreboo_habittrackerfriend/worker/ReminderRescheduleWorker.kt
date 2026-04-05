package com.example.choreboo_habittrackerfriend.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.choreboo_habittrackerfriend.data.repository.HabitRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class ReminderRescheduleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val habitRepository: HabitRepository,
    private val firebaseAuth: FirebaseAuth,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val currentUid = firebaseAuth.currentUser?.uid
            if (currentUid == null) {
                // No user is authenticated — don't reschedule any alarms.
                // This prevents stale alarms from a previous user from rescheduling after device reboot.
                return Result.success()
            }

            val habits = habitRepository.getAllHabits().first()
            habits.forEach { habit ->
                // Reschedule reminders only for habits the current user owns or is assigned to (fixes B6).
                // Personal habits: owned by current user.
                // Household habits: owned by current user or assigned to current user.
                // This prevents cross-user alarm contamination when multiple accounts are used on the same device.
                val isOwnedByUser = habit.ownerUid == currentUid
                val isAssignedToUser = habit.assignedToUid == currentUid

                if ((isOwnedByUser || isAssignedToUser) && habit.reminderEnabled && habit.reminderTime != null) {
                    HabitReminderScheduler.scheduleReminder(
                        applicationContext,
                        habit.id,
                        habit.title,
                        habit.reminderTime,
                        habit.customDays,
                    )
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
