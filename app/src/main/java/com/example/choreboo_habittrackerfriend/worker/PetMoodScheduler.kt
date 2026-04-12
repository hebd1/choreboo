package com.example.choreboo_habittrackerfriend.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import timber.log.Timber
import java.time.ZonedDateTime

object PetMoodScheduler {

    /**
     * Schedules a one-shot predictive alarm for when the pet's mood will reach critical.
     * Critical = any stat (hunger, energy, happiness) drops below 20.
     *
     * Calculation:
     * - Decay rates: hunger -1/hr, happiness -0.5/hr, energy -0.5/hr
     * - Current stats are already decayed by [applyStatDecay] logic (lazy decay)
     * - If currently sleeping: no decay, schedule for sleepUntil + time until critical
     * - Otherwise: calculate when each stat reaches 20, schedule for the soonest one
     *
     * @param context
     * @param currentHunger current hunger stat (0-100)
     * @param currentHappiness current happiness stat (0-100)
     * @param currentEnergy current energy stat (0-100)
     * @param sleepUntil timestamp when sleep ends (0 if not sleeping), milliseconds
     * @param petName name of the pet (for notification)
     */
    fun schedulePredictiveAlarm(
        context: Context,
        currentHunger: Int,
        currentHappiness: Int,
        currentEnergy: Int,
        sleepUntil: Long,
        petName: String,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return

        val now = System.currentTimeMillis()

        // If currently sleeping, schedule for sleepUntil + time until critical after wake
        val scheduleFromTime = if (sleepUntil > now) {
            Timber.d("Pet is sleeping until $sleepUntil; scheduling mood alarm from sleep end time")
            sleepUntil
        } else {
            now
        }

        val nextCriticalTime = calculateNextCriticalTime(
            currentHunger,
            currentHappiness,
            currentEnergy,
            scheduleFromTime,
        )

        if (nextCriticalTime == null) {
            Timber.d("Pet mood is already critical or will not reach critical; canceling predictive alarm")
            cancelPredictiveAlarm(context)
            return
        }

        val intent = Intent(context, PetMoodReceiver::class.java).apply {
            action = "com.example.choreboo_habittrackerfriend.PET_MOOD_CHECK"
            putExtra("petName", petName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            PET_MOOD_ALARM_REQUEST_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextCriticalTime,
                pendingIntent,
            )
            Timber.d("Scheduled pet mood predictive alarm for $nextCriticalTime")
        } catch (_: SecurityException) {
            // Fall back to inexact alarm if SCHEDULE_EXACT_ALARM not granted
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextCriticalTime,
                pendingIntent,
            )
            Timber.d("Scheduled pet mood inexact alarm for $nextCriticalTime")
        }
    }

    fun cancelPredictiveAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return

        val intent = Intent(context, PetMoodReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            PET_MOOD_ALARM_REQUEST_ID,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return

        alarmManager.cancel(pendingIntent)
        Timber.d("Canceled pet mood predictive alarm")
    }

    /**
     * Calculates when the next stat will drop below 20 (critical).
     * Returns null if already critical or no critical point in a reasonable future (7 days).
     *
     * Decay rates (per hour from [scheduleFromTime]):
     * - Hunger: -1/hour
     * - Happiness: -0.5/hour
     * - Energy: -0.5/hour
     *
     * Time until critical = (currentStat - 20) / decayRate
     *
     * **Note:** This function is internal for testing purposes.
     */
    internal fun calculateNextCriticalTime(
        hunger: Int,
        happiness: Int,
        energy: Int,
        scheduleFromTime: Long,
    ): Long? {
        // If already critical, return null
        if (hunger < 20 || happiness < 20 || energy < 20) {
            Timber.d("Pet is already critical (hunger=$hunger, happiness=$happiness, energy=$energy)")
            return null
        }

        // Calculate hours until each stat reaches 20
        val hungerHours = (hunger - 20) / 1.0 // -1/hr
        val happinessHours = (happiness - 20) / 0.5 // -0.5/hr
        val energyHours = (energy - 20) / 0.5 // -0.5/hr

        // Find the minimum (soonest critical time)
        val minHours = minOf(hungerHours, happinessHours, energyHours)

        // Clamp to reasonable bounds: at least 30 minutes from now, at most 7 days
        val minHoursChecked = maxOf(minHours, 0.5) // at least 30 minutes
        val maxHoursChecked = minOf(minHoursChecked, 7 * 24.0) // cap at 7 days

        val millisUntilCritical = (maxHoursChecked * 60 * 60 * 1000).toLong()
        val triggerTime = scheduleFromTime + millisUntilCritical

        Timber.d(
            "Next critical time in %.1f hours (hunger=%.1f, happiness=%.1f, energy=%.1f)",
            maxHoursChecked,
            hungerHours,
            happinessHours,
            energyHours,
        )

        return triggerTime
    }

    private const val PET_MOOD_ALARM_REQUEST_ID = 9999
}
