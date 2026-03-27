package com.example.choreboo_habittrackerfriend.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.choreboo_habittrackerfriend.data.repository.HabitRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReminderRescheduleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val habitRepository: HabitRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            habitRepository.getAllHabits().collect { habits ->
                habits.forEach { habit ->
                    if (habit.reminderEnabled && habit.reminderTime != null) {
                        HabitReminderScheduler.scheduleReminder(
                            applicationContext,
                            habit.id,
                            habit.title,
                            habit.reminderTime,
                            habit.customDays,
                        )
                    }
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
