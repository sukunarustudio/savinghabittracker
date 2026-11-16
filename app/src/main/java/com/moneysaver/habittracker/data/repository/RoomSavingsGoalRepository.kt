package com.moneysaver.habittracker.data.repository

import com.moneysaver.habittracker.data.local.dao.SavingsGoalDao
import com.moneysaver.habittracker.data.local.entity.SavingsGoalEntity
import com.moneysaver.habittracker.domain.model.SavingsGoal
import com.moneysaver.habittracker.domain.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun SavingsGoalEntity.toDomain(): SavingsGoal = SavingsGoal(
    id = id,
    title = title,
    targetAmount = targetAmount,
    savedAmount = savedAmount,
    targetDate = targetDate,
    status = status
)

fun SavingsGoal.toEntity(): SavingsGoalEntity = SavingsGoalEntity(
    id = id,
    title = title,
    targetAmount = targetAmount,
    savedAmount = savedAmount,
    targetDate = targetDate,
    status = status
)

class RoomSavingsGoalRepository(
    private val savingsGoalDao: SavingsGoalDao
) : SavingsGoalRepository {

    override suspend fun insertGoal(goal: SavingsGoal): Long {
        return savingsGoalDao.insert(goal.toEntity())
    }

    override suspend fun updateGoal(goal: SavingsGoal) {
        savingsGoalDao.update(goal.toEntity())
    }

    override suspend fun deleteGoal(goal: SavingsGoal) {
        savingsGoalDao.delete(goal.toEntity())
    }

    override suspend fun getGoalById(id: Long): SavingsGoal? {
        return savingsGoalDao.getSavingsGoalById(id)?.toDomain()
    }

    override fun getGoals(): Flow<List<SavingsGoal>> {
        return savingsGoalDao.getAllSavingsGoals().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAllGoals(): List<SavingsGoal> {
        return savingsGoalDao.getAllSavingsGoalsList().map { it.toDomain() }
    }

    override suspend fun updateSavedAmount(goalId: Long, newSavedAmount: Long) {
        savingsGoalDao.updateSavedAmount(goalId, newSavedAmount)
    }
}
