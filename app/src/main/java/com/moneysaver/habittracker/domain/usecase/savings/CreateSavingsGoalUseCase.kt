package com.moneysaver.habittracker.domain.usecase.savings

import com.moneysaver.habittracker.domain.model.GoalStatus
import com.moneysaver.habittracker.domain.model.SavingsGoal
import com.moneysaver.habittracker.domain.repository.SavingsGoalRepository

class CreateSavingsGoalUseCase(
    private val savingsGoalRepository: SavingsGoalRepository
) {
    suspend operator fun invoke(goal: SavingsGoal): Long {
        require(goal.title.isNotBlank()) { "Goal title cannot be blank" }
        require(goal.targetAmount > 0L) { "Target amount must be greater than 0" }
        require(goal.savedAmount >= 0L) { "Saved amount cannot be negative" }

        val status = when {
            goal.savedAmount >= goal.targetAmount -> GoalStatus.ACHIEVED
            else -> goal.status
        }

        return savingsGoalRepository.insertGoal(goal.copy(status = status))
    }
}
