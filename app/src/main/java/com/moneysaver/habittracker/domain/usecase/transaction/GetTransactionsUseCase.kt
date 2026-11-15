package com.moneysaver.habittracker.domain.usecase.transaction

import com.moneysaver.habittracker.domain.model.Transaction
import com.moneysaver.habittracker.domain.model.TransactionType
import com.moneysaver.habittracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetTransactionsUseCase(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(
        startDate: Long? = null,
        endDate: Long? = null,
        type: TransactionType? = null,
        categoryId: Long? = null
    ): Flow<List<Transaction>> {
        return transactionRepository.getTransactions().map { transactions ->
            transactions.filter { tx ->
                (startDate == null || tx.timestamp >= startDate) &&
                (endDate == null || tx.timestamp <= endDate) &&
                (type == null || tx.type == type) &&
                (categoryId == null || tx.categoryId == categoryId)
            }
        }
    }

    suspend fun executeSync(
        startDate: Long? = null,
        endDate: Long? = null,
        type: TransactionType? = null,
        categoryId: Long? = null
    ): List<Transaction> {
        return transactionRepository.getAllTransactions().filter { tx ->
            (startDate == null || tx.timestamp >= startDate) &&
            (endDate == null || tx.timestamp <= endDate) &&
            (type == null || tx.type == type) &&
            (categoryId == null || tx.categoryId == categoryId)
        }
    }
}
