package com.moneysaver.habittracker.domain.repository

import com.moneysaver.habittracker.domain.model.habit.Habit
import com.moneysaver.habittracker.domain.model.habit.HabitCompletionLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class FakeHabitRepository : HabitRepository {
    private val habitsMap = mutableMapOf<String, Habit>()
    private val habitsFlow = MutableStateFlow<List<Habit>>(emptyList())

    private val completionLogs = mutableListOf<HabitCompletionLog>()
    private val completionLogsFlow = MutableStateFlow<List<HabitCompletionLog>>(emptyList())

    override fun getHabits(): Flow<List<Habit>> = habitsFlow

    override suspend fun getHabitById(id: String): Habit? = habitsMap[id]

    override suspend fun insertHabit(habit: Habit) {
        habitsMap[habit.id] = habit
        emitHabits()
    }

    override suspend fun updateHabit(habit: Habit) {
        habitsMap[habit.id] = habit
        emitHabits()
    }

    override suspend fun deleteHabit(id: String) {
        habitsMap.remove(id)
        completionLogs.removeAll { it.habitId == id }
        emitHabits()
        emitLogs()
    }

    override fun getCompletionLogs(habitId: String): Flow<List<HabitCompletionLog>> {
        return completionLogsFlow.map { logs -> logs.filter { it.habitId == habitId } }
    }

    override suspend fun getCompletionLogsOnce(habitId: String): List<HabitCompletionLog> {
        return completionLogs.filter { it.habitId == habitId }
    }

    override suspend fun getCompletionLog(habitId: String, date: LocalDate): HabitCompletionLog? {
        return completionLogs.firstOrNull { it.habitId == habitId && it.completedDate == date }
    }

    override suspend fun insertCompletionLog(log: HabitCompletionLog) {
        completionLogs.removeAll { it.habitId == log.habitId && it.completedDate == log.completedDate }
        completionLogs.add(log)
        emitLogs()
    }

    override suspend fun deleteCompletionLog(habitId: String, date: LocalDate) {
        completionLogs.removeAll { it.habitId == habitId && it.completedDate == date }
        emitLogs()
    }

    override fun getAllCompletionLogs(): Flow<List<HabitCompletionLog>> = completionLogsFlow

    private fun emitHabits() {
        habitsFlow.value = habitsMap.values.toList()
    }

    private fun emitLogs() {
        completionLogsFlow.value = completionLogs.toList()
    }
}
