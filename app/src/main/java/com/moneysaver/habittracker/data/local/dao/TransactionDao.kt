package com.moneysaver.habittracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneysaver.habittracker.data.local.entity.TransactionEntity
import com.moneysaver.habittracker.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions WHERE savingsGoalId = :savingsGoalId")
    suspend fun deleteBySavingsGoalId(savingsGoalId: Long)

    @Query("SELECT * FROM transactions WHERE savingsGoalId = :savingsGoalId ORDER BY timestamp DESC")
    suspend fun getTransactionsBySavingsGoalList(savingsGoalId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionByIdFlow(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp ASC")
    suspend fun getAllTransactionsAsc(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    suspend fun getTransactionsBetweenList(startTime: Long, endTime: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY timestamp DESC")
    fun getTransactionsByCategory(categoryId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE savingsGoalId = :savingsGoalId ORDER BY timestamp DESC")
    fun getTransactionsBySavingsGoal(savingsGoalId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY timestamp DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(
            CASE 
                WHEN type IN ('INCOME', 'SAVINGS_WITHDRAWAL') THEN amount 
                WHEN type IN ('EXPENSE', 'SAVINGS_DEPOSIT') THEN -amount 
                ELSE 0 
            END
        ), 0) 
        FROM transactions 
        WHERE timestamp <= :pointInTime
        """
    )
    suspend fun getRunningBalanceAt(pointInTime: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(
            CASE 
                WHEN type IN ('INCOME', 'SAVINGS_WITHDRAWAL') THEN amount 
                WHEN type IN ('EXPENSE', 'SAVINGS_DEPOSIT') THEN -amount 
                ELSE 0 
            END
        ), 0) 
        FROM transactions
        """
    )
    suspend fun getLifetimeBalance(): Long

    @Query(
        """
        SELECT COALESCE(SUM(
            CASE 
                WHEN type IN ('INCOME', 'SAVINGS_WITHDRAWAL') THEN amount 
                WHEN type IN ('EXPENSE', 'SAVINGS_DEPOSIT') THEN -amount 
                ELSE 0 
            END
        ), 0) 
        FROM transactions
        """
    )
    fun getLifetimeBalanceFlow(): Flow<Long>

    @Query("SELECT * FROM transactions WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getTransactionsSince(sinceTimestamp: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE timestamp <= :pointInTime ORDER BY timestamp ASC")
    suspend fun getTransactionsUpTo(pointInTime: Long): List<TransactionEntity>
}
