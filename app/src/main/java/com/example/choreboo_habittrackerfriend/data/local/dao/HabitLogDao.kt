package com.example.choreboo_habittrackerfriend.data.local.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.choreboo_habittrackerfriend.data.local.entity.HabitLogEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface HabitLogDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLog(log: HabitLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLogs(logs: List<HabitLogEntity>)

    @Query("SELECT * FROM habit_logs WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getLogByRemoteId(remoteId: String): HabitLogEntity?
    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY completedAt DESC")
    fun getLogsForHabit(habitId: Long): Flow<List<HabitLogEntity>>
    @Query("SELECT * FROM habit_logs WHERE date = :date ORDER BY completedAt DESC")
    fun getLogsForDate(date: String): Flow<List<HabitLogEntity>>
    @Query("SELECT COUNT(*) FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun getCompletionCountForDate(habitId: Long, date: String): Int
    @Query("SELECT DISTINCT date FROM habit_logs WHERE habitId = :habitId ORDER BY date DESC")
    suspend fun getCompletionDatesForHabit(habitId: Long): List<String>
    @Query("SELECT * FROM habit_logs WHERE date LIKE :yearMonthPrefix ORDER BY completedAt DESC")
    fun getLogsForMonth(yearMonthPrefix: String): Flow<List<HabitLogEntity>>
    @Query("""
        SELECT habit_logs.* FROM habit_logs 
        INNER JOIN habits ON habit_logs.habitId = habits.id 
        WHERE habit_logs.date = :date 
        ORDER BY habit_logs.completedAt DESC
    """)
    fun getLogsWithHabitForDate(date: String): Flow<List<HabitLogEntity>>

    @Query("""
        SELECT habit_logs.id, habit_logs.habitId, habit_logs.completedAt, 
               habit_logs.date, habit_logs.xpEarned, habit_logs.streakAtCompletion,
               habits.title AS habitTitle, habits.iconName AS habitIcon
        FROM habit_logs 
        INNER JOIN habits ON habit_logs.habitId = habits.id 
        WHERE habit_logs.date = :date 
        ORDER BY habit_logs.completedAt DESC
    """)
    fun getLogsWithHabitNameForDate(date: String): Flow<List<HabitLogWithName>>

    @Query("""
        SELECT habitId, MAX(streakAtCompletion) AS streak
        FROM habit_logs
        WHERE date = :date
        GROUP BY habitId
    """)
    fun getStreaksForDate(date: String): Flow<List<HabitStreak>>

    /** Total number of habit completions ever — used for badge computation. */
    @Query("SELECT COUNT(*) FROM habit_logs")
    fun getTotalCompletionCount(): Flow<Int>

    /** Highest streak ever recorded across all habit logs — used for badge computation. */
    @Query("SELECT COALESCE(MAX(streakAtCompletion), 0) FROM habit_logs")
    fun getMaxStreakEver(): Flow<Int>

    /** Distinct dates with completions in a date range — used for weekly streak circles. */
    @Query("SELECT DISTINCT date FROM habit_logs WHERE date >= :startDate AND date <= :endDate")
    fun getCompletionDatesInRange(startDate: String, endDate: String): Flow<List<String>>

    /** Update the remoteId on a habit log after cloud write-through. */
    @Query("UPDATE habit_logs SET remoteId = :remoteId WHERE id = :logId")
    suspend fun updateLogRemoteId(logId: Long, remoteId: String)

    /** Delete all habit logs — used for sign-out data cleanup. */
    @Query("DELETE FROM habit_logs")
    suspend fun deleteAllLogs()
}

data class HabitStreak(
    val habitId: Long,
    val streak: Int,
)

data class HabitLogWithName(
    val id: Long,
    val habitId: Long,
    val completedAt: Long,
    val date: String,
    val xpEarned: Int,
    val streakAtCompletion: Int,
    val habitTitle: String,
    val habitIcon: String,
)

