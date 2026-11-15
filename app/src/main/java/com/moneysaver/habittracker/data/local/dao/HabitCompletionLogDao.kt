package com.moneysaver.habittracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.moneysaver.habittracker.data.local.entity.HabitCompletionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitCompletionLogDao {

    /**
     * Idempotent insert: ignored if (habitId, completedDate) already exists.
     * Returns rowId if inserted, -1L if ignored.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(log: HabitCompletionLogEntity): Long

    @Query("DELETE FROM habit_completion_logs WHERE habitId = :habitId AND completedDate = :completedDate")
    suspend fun deleteByDate(habitId: Long, completedDate: String): Int

    @Query("DELETE FROM habit_completion_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM habit_completion_logs WHERE habitId = :habitId")
    suspend fun deleteLogsForHabit(habitId: Long)

    @Query("SELECT * FROM habit_completion_logs WHERE habitId = :habitId ORDER BY completedDate ASC")
    fun getLogsForHabit(habitId: Long): Flow<List<HabitCompletionLogEntity>>

    @Query("SELECT * FROM habit_completion_logs WHERE habitId = :habitId ORDER BY completedDate ASC")
    suspend fun getLogsForHabitList(habitId: Long): List<HabitCompletionLogEntity>

    @Query("SELECT * FROM habit_completion_logs ORDER BY completedDate DESC, completedAt DESC")
    fun getAllLogs(): Flow<List<HabitCompletionLogEntity>>

    @Query("SELECT * FROM habit_completion_logs ORDER BY completedDate DESC, completedAt DESC")
    suspend fun getAllLogsList(): List<HabitCompletionLogEntity>

    @Query("SELECT * FROM habit_completion_logs WHERE completedDate = :completedDate")
    fun getLogsForDate(completedDate: String): Flow<List<HabitCompletionLogEntity>>

    @Query("SELECT * FROM habit_completion_logs WHERE completedDate = :completedDate")
    suspend fun getLogsForDateList(completedDate: String): List<HabitCompletionLogEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM habit_completion_logs WHERE habitId = :habitId AND completedDate = :completedDate)")
    suspend fun isHabitCompletedOnDate(habitId: Long, completedDate: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM habit_completion_logs WHERE habitId = :habitId AND completedDate = :completedDate)")
    fun isHabitCompletedOnDateFlow(habitId: Long, completedDate: String): Flow<Boolean>
}
