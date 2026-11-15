package com.moneysaver.habittracker.domain.repository

import com.moneysaver.habittracker.domain.model.habit.Habit
import com.moneysaver.habittracker.domain.model.habit.HabitCompletionLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HabitRepository {
    fun getHabits(): Flow<List<Habit>>
    suspend fun getHabitById(id: String): Habit?
    suspend fun insertHabit(habit: Habit)
    suspend fun updateHabit(habit: Habit)
    suspend fun deleteHabit(id: String)

    fun getCompletionLogs(habitId: String): Flow<List<HabitCompletionLog>>
    suspend fun getCompletionLogsOnce(habitId: String): List<HabitCompletionLog>
    suspend fun getCompletionLog(habitId: String, date: LocalDate): HabitCompletionLog?
    suspend fun insertCompletionLog(log: HabitCompletionLog)
    suspend fun deleteCompletionLog(habitId: String, date: LocalDate)
    fun getAllCompletionLogs(): Flow<List<HabitCompletionLog>>
}
