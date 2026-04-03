package com.example.choreboo_habittrackerfriend.data.repository

import android.util.Log
import com.example.choreboo_habittrackerfriend.data.local.dao.HabitDao
import com.example.choreboo_habittrackerfriend.data.local.dao.HabitLogDao
import com.example.choreboo_habittrackerfriend.data.local.entity.HabitEntity
import com.example.choreboo_habittrackerfriend.data.local.dao.HabitLogWithName
import com.example.choreboo_habittrackerfriend.data.local.entity.HabitLogEntity
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.dataconnect.ChorebooConnector
import com.example.choreboo_habittrackerfriend.dataconnect.execute
import com.example.choreboo_habittrackerfriend.dataconnect.instance
import com.example.choreboo_habittrackerfriend.domain.model.Habit
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "HabitRepository"

data class CompletionResult(
    val xpEarned: Int,
    val newStreak: Int,
    val alreadyComplete: Boolean = false,
)

@Singleton
class HabitRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val userPreferences: UserPreferences,
    private val firebaseAuth: FirebaseAuth,
) {
    private val connector by lazy { ChorebooConnector.instance }
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun getAllHabits(): Flow<List<Habit>> = habitDao.getAllHabits().map { entities ->
        entities.map { it.toDomain() }
    }

    fun getHabitById(id: Long): Flow<Habit?> = habitDao.getHabitById(id).map { it?.toDomain() }

    suspend fun upsertHabit(habit: Habit): Long {
        val localId = habitDao.upsertHabit(habit.toEntity())

        // Write-through to Data Connect
        try {
            if (habit.remoteId != null) {
                // Update existing remote habit
                val remoteUuid = UUID.fromString(habit.remoteId)
                connector.updateHabit.execute(
                    habitId = remoteUuid,
                    title = habit.title,
                    iconName = habit.iconName,
                    customDays = habit.customDays.joinToString(","),
                    difficulty = habit.difficulty,
                    baseXp = habit.baseXp,
                    reminderEnabled = habit.reminderEnabled,
                    isHouseholdHabit = habit.isHouseholdHabit,
                ) {
                    description = habit.description
                    reminderTime = habit.reminderTime?.toString()
                    householdId = habit.householdId?.let { UUID.fromString(it) }
                }
                Log.d(TAG, "Updated habit in cloud: $remoteUuid")
            } else {
                // Create new remote habit
                val result = connector.createHabit.execute(
                    title = habit.title,
                    iconName = habit.iconName,
                    customDays = habit.customDays.joinToString(","),
                    difficulty = habit.difficulty,
                    baseXp = habit.baseXp,
                    reminderEnabled = habit.reminderEnabled,
                    isHouseholdHabit = habit.isHouseholdHabit,
                ) {
                    description = habit.description
                    reminderTime = habit.reminderTime?.toString()
                    householdId = habit.householdId?.let { UUID.fromString(it) }
                }
                // Store the remote ID back in Room
                val remoteId = result.data.habit_insert.id.toString()
                habitDao.upsertHabit(habit.toEntity().copy(id = localId, remoteId = remoteId))
                Log.d(TAG, "Created habit in cloud: $remoteId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync habit to cloud", e)
        }

        return localId
    }

    suspend fun deleteHabit(id: Long) {
        // Get the entity first to find the remote ID
        val entity = habitDao.getHabitByIdSync(id)
        habitDao.deleteHabitById(id)

        // Write-through: delete from Data Connect
        entity?.remoteId?.let { remoteId ->
            try {
                connector.deleteHabit.execute(
                    habitId = UUID.fromString(remoteId),
                )
                Log.d(TAG, "Deleted habit from cloud: $remoteId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete habit from cloud", e)
            }
        }
    }

    suspend fun archiveHabit(id: Long) {
        val entity = habitDao.getHabitByIdSync(id)
        habitDao.archiveHabit(id)

        // Write-through: archive in Data Connect
        entity?.remoteId?.let { remoteId ->
            try {
                connector.archiveHabit.execute(
                    habitId = UUID.fromString(remoteId),
                )
                Log.d(TAG, "Archived habit in cloud: $remoteId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to archive habit in cloud", e)
            }
        }
    }

    fun getLogsForDate(date: String): Flow<List<HabitLogEntity>> {
        return habitLogDao.getLogsForDate(date)
    }

    fun getLogsWithNamesForDate(date: String): Flow<List<HabitLogWithName>> {
        return habitLogDao.getLogsWithHabitNameForDate(date)
    }

    suspend fun getCompletionCountToday(habitId: Long): Int {
        val today = LocalDate.now().format(dateFormatter)
        return habitLogDao.getCompletionCountForDate(habitId, today)
    }

    suspend fun completeHabit(habitId: Long): CompletionResult {
        val today = LocalDate.now().format(dateFormatter)
        val habitEntity = habitDao.getHabitByIdSync(habitId)
            ?: return CompletionResult(xpEarned = 0, newStreak = 0, alreadyComplete = true)

        // Early return if already completed (avoids unnecessary work; the real
        // enforcement is the UNIQUE(habitId, date) index on insertLog below).
        val todayCount = habitLogDao.getCompletionCountForDate(habitId, today)
        if (todayCount >= 1) {
            return CompletionResult(xpEarned = 0, newStreak = 0, alreadyComplete = true)
        }

        val streak = calculateStreak(habitId, today)
        val baseXp = habitEntity.baseXp
        val xpEarned = (baseXp + (streak * 2)).coerceAtMost(baseXp * 3)

        val currentUid = firebaseAuth.currentUser?.uid
        val log = HabitLogEntity(
            habitId = habitId,
            date = today,
            xpEarned = xpEarned,
            streakAtCompletion = streak + 1,
            completedByUid = currentUid,
        )
        // Atomic duplicate prevention: UNIQUE(habitId, date) + IGNORE returns -1 on conflict
        val localLogId = habitLogDao.insertLog(log)
        if (localLogId == -1L) {
            return CompletionResult(xpEarned = 0, newStreak = 0, alreadyComplete = true)
        }
        userPreferences.addPoints(xpEarned)
        userPreferences.addLifetimeXp(xpEarned)

        // Write-through: create log in Data Connect and save remoteId back
        habitEntity.remoteId?.let { remoteHabitId ->
            try {
                val result = connector.createHabitLog.execute(
                    habitId = UUID.fromString(remoteHabitId),
                    date = today,
                    xpEarned = xpEarned,
                    streakAtCompletion = streak + 1,
                )
                val remoteLogId = result.data.habitLog_insert.id.toString()
                habitLogDao.updateLogRemoteId(localLogId, remoteLogId)
                Log.d(TAG, "Created habit log in cloud for habit: $remoteHabitId (remoteLogId: $remoteLogId)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync habit log to cloud", e)
            }
        }

        return CompletionResult(
            xpEarned = xpEarned,
            newStreak = streak + 1,
        )
    }

    private suspend fun calculateStreak(habitId: Long, today: String): Int {
        val dates = habitLogDao.getCompletionDatesForHabit(habitId)
        if (dates.isEmpty()) return 0

        var streak = 0
        var checkDate = LocalDate.parse(today, dateFormatter).minusDays(1)
        for (dateStr in dates) {
            val logDate = LocalDate.parse(dateStr, dateFormatter)
            if (logDate == checkDate) {
                streak++
                checkDate = checkDate.minusDays(1)
            } else if (logDate.isBefore(checkDate)) {
                break
            }
        }
        // If today already has logs, count today too
        if (dates.contains(today)) {
            streak++
        }
        return streak
    }

    fun getLogsForMonth(yearMonth: String): Flow<List<HabitLogEntity>> {
        return habitLogDao.getLogsForMonth("$yearMonth%")
    }

    /**
     * Returns the set of [DayOfWeek] values within the current week (Mon-Sun)
     * that have at least one habit completion.
     */
    fun getCompletionDaysForCurrentWeek(): Flow<Set<DayOfWeek>> {
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = monday.plusDays(6)
        val startDate = monday.format(dateFormatter)
        val endDate = sunday.format(dateFormatter)
        return habitLogDao.getCompletionDatesInRange(startDate, endDate).map { dateStrings ->
            dateStrings.mapNotNull { dateStr ->
                try {
                    LocalDate.parse(dateStr, dateFormatter).dayOfWeek
                } catch (_: Exception) {
                    null
                }
            }.toSet()
        }
    }

    fun getStreaksForToday(): Flow<Map<Long, Int>> {
        val today = LocalDate.now().format(dateFormatter)
        return habitLogDao.getStreaksForDate(today).map { streaks ->
            streaks.associate { it.habitId to it.streak }
        }
    }

    /**
     * Clear all local habit and habit log data — used for sign-out cleanup.
     */
    suspend fun clearLocalData() {
        habitLogDao.deleteAllLogs()
        habitDao.deleteAllHabits()
    }

    /**
     * Pull habits from Data Connect and merge into Room (cloud wins).
     * Called once after successful authentication.
     */
    suspend fun syncHabitsFromCloud() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        try {
            val result = connector.getMyHabits.execute()
            val cloudHabits = result.data.habits
            Log.d(TAG, "Fetched ${cloudHabits.size} habits from cloud")

            for (cloudHabit in cloudHabits) {
                val remoteId = cloudHabit.id.toString()
                val existing = habitDao.getHabitByRemoteId(remoteId)

                val entity = HabitEntity(
                    id = existing?.id ?: 0,
                    title = cloudHabit.title,
                    description = cloudHabit.description,
                    iconName = cloudHabit.iconName,
                    customDays = cloudHabit.customDays,
                    difficulty = cloudHabit.difficulty,
                    baseXp = cloudHabit.baseXp,
                    reminderEnabled = cloudHabit.reminderEnabled,
                    reminderTime = cloudHabit.reminderTime,
                    createdAt = cloudHabit.createdAt.toDate().time,
                    isArchived = cloudHabit.isArchived,
                    isHouseholdHabit = cloudHabit.isHouseholdHabit,
                    ownerUid = uid,
                    householdId = cloudHabit.household?.id?.toString(),
                    remoteId = remoteId,
                )
                habitDao.upsertHabit(entity)
            }
            Log.d(TAG, "Synced ${cloudHabits.size} habits from cloud to Room")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync habits from cloud", e)
            throw e
        }
    }

    /**
     * Pull habit logs from Data Connect (last 30 days) and merge into Room (cloud wins).
     * Requires habits to be synced first so remoteId → local ID mapping is available.
     */
    suspend fun syncHabitLogsFromCloud() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        val endDate = LocalDate.now().format(dateFormatter)
        val startDate = LocalDate.now().minusDays(30).format(dateFormatter)

        try {
            val result = connector.getMyLogsForDateRange.execute(
                startDate = startDate,
                endDate = endDate,
            )
            val cloudLogs = result.data.habitLogs
            Log.d(TAG, "Fetched ${cloudLogs.size} habit logs from cloud")

            // Build a map of remote habit UUID → local habit ID
            val allLocalHabits = habitDao.getAllHabitsSync()
            val remoteToLocalId = allLocalHabits
                .filter { it.remoteId != null }
                .associate { it.remoteId!! to it.id }

            val logsToInsert = cloudLogs.mapNotNull { cloudLog ->
                val remoteLogId = cloudLog.id.toString()
                val remoteHabitId = cloudLog.habit.id.toString()
                val localHabitId = remoteToLocalId[remoteHabitId] ?: return@mapNotNull null

                // Skip if we already have this log locally
                val existingLog = habitLogDao.getLogByRemoteId(remoteLogId)
                if (existingLog != null) return@mapNotNull null

                HabitLogEntity(
                    habitId = localHabitId,
                    completedAt = cloudLog.completedAt.toDate().time,
                    date = cloudLog.date,
                    xpEarned = cloudLog.xpEarned,
                    streakAtCompletion = cloudLog.streakAtCompletion,
                    completedByUid = uid,
                    remoteId = remoteLogId,
                )
            }

            if (logsToInsert.isNotEmpty()) {
                habitLogDao.insertLogs(logsToInsert)
            }
            Log.d(TAG, "Synced ${logsToInsert.size} new habit logs from cloud to Room")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync habit logs from cloud", e)
            throw e
        }
    }
}

private fun HabitEntity.toDomain() = Habit(
    id = id,
    title = title,
    description = description,
    iconName = iconName,
    customDays = customDays.split(",").map { it.trim() }.filter { it.isNotEmpty() },
    difficulty = difficulty,
    baseXp = baseXp,
    reminderEnabled = reminderEnabled,
    reminderTime = reminderTime?.let { timeStr ->
        try {
            LocalTime.parse(timeStr)
        } catch (_: Exception) {
            null
        }
    },
    createdAt = createdAt,
    isArchived = isArchived,
    isHouseholdHabit = isHouseholdHabit,
    ownerUid = ownerUid,
    householdId = householdId,
    remoteId = remoteId,
)

private fun Habit.toEntity() = HabitEntity(
    id = id,
    title = title,
    description = description,
    iconName = iconName,
    customDays = customDays.joinToString(","),
    difficulty = difficulty,
    baseXp = baseXp,
    reminderEnabled = reminderEnabled,
    reminderTime = reminderTime?.toString(),
    createdAt = createdAt,
    isArchived = isArchived,
    isHouseholdHabit = isHouseholdHabit,
    ownerUid = ownerUid,
    householdId = householdId,
    remoteId = remoteId,
)
