package com.moneysaver.habittracker.domain.usecase.habit

import com.moneysaver.habittracker.domain.model.habit.FrequencyType
import com.moneysaver.habittracker.domain.model.habit.Habit
import com.moneysaver.habittracker.domain.model.habit.HabitSchedule
import com.moneysaver.habittracker.domain.repository.HabitRepository
import java.time.LocalDate

class UpdateHabitScheduleUseCase(
    private val habitRepository: HabitRepository
) {
    /**
     * Updates habit schedule prospectively from [effectiveFrom].
     * Historical streaks prior to [effectiveFrom] continue using the historical schedule.
     */
    suspend operator fun invoke(
        habitId: String,
        newSchedule: HabitSchedule,
        effectiveFrom: LocalDate = LocalDate.now()
    ): Habit {
        val habit = habitRepository.getHabitById(habitId)
            ?: throw NoSuchElementException("Habit with id $habitId not found")

        if (newSchedule.frequencyType == FrequencyType.CUSTOM_DAYS) {
            require(newSchedule.targetDays.isNotEmpty()) {
                "Custom days schedule must include at least one target day"
            }
        }

        // Archive current schedule into history if effectiveFrom is after current schedule's effectiveFrom
        val updatedHistory = if (effectiveFrom.isAfter(habit.schedule.effectiveFrom)) {
            habit.scheduleHistory + habit.schedule
        } else {
            habit.scheduleHistory
        }

        val updatedHabit = habit.copy(
            schedule = newSchedule.copy(effectiveFrom = effectiveFrom),
            scheduleHistory = updatedHistory
        )

        habitRepository.updateHabit(updatedHabit)
        return updatedHabit
    }
}
