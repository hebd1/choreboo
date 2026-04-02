package com.example.choreboo_habittrackerfriend.data.repository

import com.example.choreboo_habittrackerfriend.data.local.dao.HabitDao
import com.example.choreboo_habittrackerfriend.data.local.dao.HabitLogDao
import com.example.choreboo_habittrackerfriend.data.local.entity.HabitEntity
import com.example.choreboo_habittrackerfriend.data.local.dao.HabitLogWithName
import com.example.choreboo_habittrackerfriend.data.local.entity.HabitLogEntity
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.domain.model.Habit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

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
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun getAllHabits(): Flow<List<Habit>> = habitDao.getAllHabits().map { entities ->
        entities.map { it.toDomain() }
    }

    fun getHabitById(id: Long): Flow<Habit?> = habitDao.getHabitById(id).map { it?.toDomain() }

    suspend fun upsertHabit(habit: Habit): Long {
        return habitDao.upsertHabit(habit.toEntity())
    }

    suspend fun deleteHabit(id: Long) {
        habitDao.deleteHabitById(id)
    }

    suspend fun archiveHabit(id: Long) {
        habitDao.archiveHabit(id)
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

        // Ensure habit is only completed once per day
        val todayCount = habitLogDao.getCompletionCountForDate(habitId, today)
        if (todayCount >= 1) {
            return CompletionResult(xpEarned = 0, newStreak = 0, alreadyComplete = true)
        }

        val streak = calculateStreak(habitId, today)
        val baseXp = habitEntity.baseXp
        val xpEarned = (baseXp + (streak * 2)).coerceAtMost(baseXp * 3)

        val log = HabitLogEntity(
            habitId = habitId,
            date = today,
            xpEarned = xpEarned,
            streakAtCompletion = streak + 1,
        )
        habitLogDao.insertLog(log)
        userPreferences.addPoints(xpEarned)

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

    fun getStreaksForToday(): Flow<Map<Long, Int>> {
        val today = LocalDate.now().format(dateFormatter)
        return habitLogDao.getStreaksForDate(today).map { streaks ->
            streaks.associate { it.habitId to it.streak }
        }
    }
}

private fun HabitEntity.toDomain() = Habit(
    id = id,
    title = title,
    description = description,
    iconName = iconName,
    customDays = customDays.split(",").map { it.trim() },
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
)

