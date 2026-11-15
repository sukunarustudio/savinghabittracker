package com.moneysaver.habittracker.domain.usecase.habit

import com.moneysaver.habittracker.domain.repository.HabitRepository

class DeleteHabitUseCase(
    private val habitRepository: HabitRepository
) {
    suspend operator fun invoke(habitId: String) {
        habitRepository.deleteHabit(habitId)
    }
}
