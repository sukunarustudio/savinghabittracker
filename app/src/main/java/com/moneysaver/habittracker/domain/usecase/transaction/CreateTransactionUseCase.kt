package com.moneysaver.habittracker.domain.usecase.transaction

import com.moneysaver.habittracker.domain.engine.FinancialLedgerEngine
import com.moneysaver.habittracker.domain.exception.InsufficientBalanceException
import com.moneysaver.habittracker.domain.model.Transaction
import com.moneysaver.habittracker.domain.model.TransactionType
import com.moneysaver.habittracker.domain.repository.SavingsGoalRepository
import com.moneysaver.habittracker.domain.repository.TransactionRepository

class CreateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val ledgerEngine: FinancialLedgerEngine
) {
    suspend operator fun invoke(transaction: Transaction): Long {
        require(transaction.amount > 0L) { "Transaction amount must be greater than 0" }

        val allTransactions = transactionRepository.getAllTransactions()
        val allGoals = savingsGoalRepository.getAllGoals()

        // Expenses and savings deposits reduce available balance:
        if (transaction.type == TransactionType.EXPENSE || transaction.type == TransactionType.SAVINGS_DEPOSIT) {
            val availableBalance = ledgerEngine.calculateLifetimeAvailableBalance(allTransactions, allGoals)
            if (!ledgerEngine.validateExpenseAllowed(availableBalance, transaction.amount)) {
                throw InsufficientBalanceException(
                    "Amount ${transaction.amount} exceeds net available balance of $availableBalance"
                )
            }
        }

        // Point-in-time running balance validation (handles backdated and current entries)
        val isValidRunning = ledgerEngine.validateRunningBalance(
            mutatedTimestamp = transaction.timestamp,
            delta = transaction.signedAmount,
            history = allTransactions
        )
        if (!isValidRunning) {
            throw InsufficientBalanceException(
                "Transaction at timestamp ${transaction.timestamp} with amount ${transaction.amount} causes point-in-time overdraft"
            )
        }

        return transactionRepository.insertTransaction(transaction)
    }
}
