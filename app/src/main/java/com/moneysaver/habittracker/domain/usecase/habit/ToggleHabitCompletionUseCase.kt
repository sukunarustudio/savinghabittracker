package com.moneysaver.habittracker.domain.usecase.habit

import com.moneysaver.habittracker.domain.model.habit.HabitCompletionLog
import com.moneysaver.habittracker.domain.repository.HabitRepository
import java.time.LocalDate

class ToggleHabitCompletionUseCase(
    private val habitRepository: HabitRepository
) {
    /**
     * Toggles the completion status for a habit on [date].
     *
     * @param habitId The ID of the habit
     * @param date The date to toggle completion for
     * @param today The reference current date (defaults to LocalDate.now())
     * @return true if habit is now completed, false if untoggled
     * @throws IllegalArgumentException if date is in the future
     * @throws NoSuchElementException if habit does not exist
     */
    suspend operator fun invoke(
        habitId: String,
        date: LocalDate,
        today: LocalDate = LocalDate.now()
    ): Boolean {
        // 1. Guard against future dates
        require(!date.isAfter(today)) {
            "Future date completion is strictly disallowed: date $date is after today $today"
        }

        // 2. Verify habit exists
        habitRepository.getHabitById(habitId)
            ?: throw NoSuchElementException("Habit with id $habitId not found")

        // 3. Check existing completion log
        val existingLog = habitRepository.getCompletionLog(habitId, date)
        return if (existingLog != null) {
            // Untoggle
            habitRepository.deleteCompletionLog(habitId, date)
            false
        } else {
            // Toggle complete
            val log = HabitCompletionLog(
                habitId = habitId,
                completedDate = date
            )
            habitRepository.insertCompletionLog(log)
            true
        }
    }

    /**
     * Explicitly and idempotently sets completion status.
     * If [completed] is true, records completion if not already recorded.
     * If [completed] is false, removes completion if recorded.
     */
    suspend fun setCompletion(
        habitId: String,
        date: LocalDate,
        completed: Boolean,
        today: LocalDate = LocalDate.now()
    ): Boolean {
        require(!date.isAfter(today)) {
            "Future date completion is strictly disallowed: date $date is after today $today"
        }

        habitRepository.getHabitById(habitId)
            ?: throw NoSuchElementException("Habit with id $habitId not found")

        val existingLog = habitRepository.getCompletionLog(habitId, date)
        if (completed) {
            if (existingLog == null) {
                habitRepository.insertCompletionLog(
                    HabitCompletionLog(habitId = habitId, completedDate = date)
                )
            }
            return true
        } else {
            if (existingLog != null) {
                habitRepository.deleteCompletionLog(habitId, date)
            }
            return false
        }
    }
}
