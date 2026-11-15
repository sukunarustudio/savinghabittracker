package com.moneysaver.habittracker.domain.usecase.habit

import com.moneysaver.habittracker.domain.engine.habit.HabitStreakCalculator
import com.moneysaver.habittracker.domain.model.habit.HabitStreakInfo
import com.moneysaver.habittracker.domain.repository.HabitRepository
import java.time.LocalDate

class CalculateHabitStreakUseCase(
    private val habitRepository: HabitRepository,
    private val streakCalculator: HabitStreakCalculator = HabitStreakCalculator()
) {
    suspend operator fun invoke(
        habitId: String,
        referenceDate: LocalDate = LocalDate.now()
    ): HabitStreakInfo {
        val habit = habitRepository.getHabitById(habitId)
            ?: throw NoSuchElementException("Habit with id $habitId not found")
        val logs = habitRepository.getCompletionLogsOnce(habitId)
        return streakCalculator.calculateStreak(habit, logs, referenceDate)
    }
}
