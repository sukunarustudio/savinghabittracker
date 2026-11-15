package com.moneysaver.habittracker.domain.usecase.habit

import com.moneysaver.habittracker.domain.model.habit.Habit
import com.moneysaver.habittracker.domain.repository.HabitRepository
import java.time.LocalDate

class ArchiveHabitUseCase(
    private val habitRepository: HabitRepository
) {
    suspend operator fun invoke(
        habitId: String,
        archivedAt: LocalDate = LocalDate.now()
    ): Habit {
        val habit = habitRepository.getHabitById(habitId)
            ?: throw NoSuchElementException("Habit with id $habitId not found")

        val updatedHabit = habit.copy(
            archivedAt = archivedAt,
            isArchived = true
        )
        habitRepository.updateHabit(updatedHabit)
        return updatedHabit
    }
}
