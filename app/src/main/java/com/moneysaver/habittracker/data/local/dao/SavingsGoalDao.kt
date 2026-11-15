package com.moneysaver.habittracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneysaver.habittracker.data.local.entity.SavingsGoalEntity
import com.moneysaver.habittracker.domain.model.GoalStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(savingsGoal: SavingsGoalEntity): Long

    @Update
    suspend fun update(savingsGoal: SavingsGoalEntity)

    @Delete
    suspend fun delete(savingsGoal: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getSavingsGoalById(id: Long): SavingsGoalEntity?

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    fun getSavingsGoalByIdFlow(id: Long): Flow<SavingsGoalEntity?>

    @Query("SELECT * FROM savings_goals ORDER BY targetDate ASC, id ASC")
    fun getAllSavingsGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals ORDER BY targetDate ASC, id ASC")
    suspend fun getAllSavingsGoalsList(): List<SavingsGoalEntity>

    @Query("SELECT * FROM savings_goals WHERE status = :status ORDER BY targetDate ASC")
    fun getSavingsGoalsByStatus(status: GoalStatus): Flow<List<SavingsGoalEntity>>

    @Query("UPDATE savings_goals SET savedAmount = :savedAmount WHERE id = :id")
    suspend fun updateSavedAmount(id: Long, savedAmount: Long)

    @Query("UPDATE savings_goals SET savedAmount = savedAmount + :delta WHERE id = :id")
    suspend fun adjustSavedAmount(id: Long, delta: Long)

    @Query("UPDATE savings_goals SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: GoalStatus)

    @Query("SELECT COALESCE(SUM(savedAmount), 0) FROM savings_goals WHERE status = 'IN_PROGRESS'")
    fun getTotalActiveAllocatedSavings(): Flow<Long>

    @Query("SELECT COALESCE(SUM(savedAmount), 0) FROM savings_goals WHERE status = 'IN_PROGRESS'")
    suspend fun getTotalActiveAllocatedSavingsValue(): Long
}
