package com.moneysaver.habittracker.data.repository

import com.moneysaver.habittracker.data.local.dao.TransactionDao
import com.moneysaver.habittracker.data.local.entity.TransactionEntity
import com.moneysaver.habittracker.domain.model.Transaction
import com.moneysaver.habittracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    amount = amount,
    type = type,
    categoryId = categoryId,
    savingsGoalId = savingsGoalId,
    isInternalTransfer = isInternalTransfer,
    timestamp = timestamp,
    note = note
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    amount = amount,
    type = type,
    categoryId = categoryId,
    savingsGoalId = savingsGoalId,
    isInternalTransfer = isInternalTransfer,
    timestamp = timestamp,
    note = note
)

class RoomTransactionRepository(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insert(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.update(transaction.toEntity())
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.delete(transaction.toEntity())
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)?.toDomain()
    }

    override fun getTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAllTransactions(): List<Transaction> {
        return transactionDao.getAllTransactionsAsc().map { it.toDomain() }
    }

    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsBetween(startDate, endDate).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTransactionsByGoalId(goalId: Long): List<Transaction> {
        return transactionDao.getTransactionsBySavingsGoalList(goalId).map { it.toDomain() }
    }

    override suspend fun deleteTransactionsByGoalId(goalId: Long) {
        transactionDao.deleteBySavingsGoalId(goalId)
    }
}
