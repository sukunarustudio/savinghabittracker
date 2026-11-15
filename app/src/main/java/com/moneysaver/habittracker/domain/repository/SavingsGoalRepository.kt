package com.moneysaver.habittracker.domain.repository

import com.moneysaver.habittracker.domain.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

interface SavingsGoalRepository {
    suspend fun insertGoal(goal: SavingsGoal): Long
    suspend fun updateGoal(goal: SavingsGoal)
    suspend fun deleteGoal(goal: SavingsGoal)
    suspend fun getGoalById(id: Long): SavingsGoal?
    fun getGoals(): Flow<List<SavingsGoal>>
    suspend fun getAllGoals(): List<SavingsGoal>
    suspend fun updateSavedAmount(goalId: Long, newSavedAmount: Long)
}
