package com.example.choreboo_habittrackerfriend.data.local.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.choreboo_habittrackerfriend.data.local.entity.HabitLogEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface HabitLogDao {
    @Insert
    suspend fun insertLog(log: HabitLogEntity): Long
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

