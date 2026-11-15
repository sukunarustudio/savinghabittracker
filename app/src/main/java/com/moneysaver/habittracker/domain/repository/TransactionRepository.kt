package com.moneysaver.habittracker.domain.repository

import com.moneysaver.habittracker.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun getTransactionById(id: Long): Transaction?
    fun getTransactions(): Flow<List<Transaction>>
    suspend fun getAllTransactions(): List<Transaction>
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>
    suspend fun getTransactionsByGoalId(goalId: Long): List<Transaction>
    suspend fun deleteTransactionsByGoalId(goalId: Long)
}
