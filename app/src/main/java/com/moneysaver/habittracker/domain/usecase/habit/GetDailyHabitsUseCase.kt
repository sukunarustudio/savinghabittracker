package com.moneysaver.habittracker.domain.usecase.habit

import com.moneysaver.habittracker.domain.model.habit.Habit
import com.moneysaver.habittracker.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class GetDailyHabitsUseCase(
    private val habitRepository: HabitRepository
) {
    /**
     * Observes active habits scheduled for the given [date].
     *
     * Filtering rules:
     * 1. createdAt <= date
     * 2. archivedAt == null || archivedAt > date (hides archived habits from subsequent checklist dates)
     * 3. Habit is scheduled on date's day of the week
     */
    operator fun invoke(date: LocalDate): Flow<List<Habit>> {
        return habitRepository.getHabits().map { habits ->
            filterHabitsForDate(habits, date)
        }
    }

    /**
     * Pure filtering function evaluating whether each habit is due on [date].
     */
    fun filterHabitsForDate(habits: List<Habit>, date: LocalDate): List<Habit> {
        return habits.filter { habit ->
            habit.isDueOn(date)
        }
    }
}
