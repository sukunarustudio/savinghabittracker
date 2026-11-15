package com.moneysaver.habittracker.domain.usecase.habit

import com.moneysaver.habittracker.domain.model.habit.FrequencyType
import com.moneysaver.habittracker.domain.model.habit.Habit
import com.moneysaver.habittracker.domain.model.habit.HabitSchedule
import com.moneysaver.habittracker.domain.repository.HabitRepository
import java.time.LocalDate

class CreateHabitUseCase(
    private val habitRepository: HabitRepository
) {
    suspend operator fun invoke(
        name: String,
        description: String = "",
        schedule: HabitSchedule = HabitSchedule(),
        createdAt: LocalDate = LocalDate.now()
    ): Habit {
        require(name.isNotBlank()) { "Habit name cannot be blank" }
        if (schedule.frequencyType == FrequencyType.CUSTOM_DAYS) {
            require(schedule.targetDays.isNotEmpty()) {
                "Custom days schedule must include at least one target day"
            }
        }

        val initialSchedule = if (schedule.effectiveFrom == LocalDate.MIN) {
            schedule.copy(effectiveFrom = createdAt)
        } else {
            schedule
        }

        val habit = Habit(
            name = name.trim(),
            description = description.trim(),
            schedule = initialSchedule,
            scheduleHistory = emptyList(),
            createdAt = createdAt,
            archivedAt = null,
            isArchived = false
        )

        habitRepository.insertHabit(habit)
        return habit
    }
}
