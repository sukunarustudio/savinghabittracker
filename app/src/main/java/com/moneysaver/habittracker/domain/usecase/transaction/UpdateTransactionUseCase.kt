package com.moneysaver.habittracker.domain.usecase.transaction

import com.moneysaver.habittracker.domain.engine.FinancialLedgerEngine
import com.moneysaver.habittracker.domain.exception.EntityNotFoundException
import com.moneysaver.habittracker.domain.exception.InsufficientBalanceException
import com.moneysaver.habittracker.domain.model.Transaction
import com.moneysaver.habittracker.domain.repository.SavingsGoalRepository
import com.moneysaver.habittracker.domain.repository.TransactionRepository

class UpdateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val ledgerEngine: FinancialLedgerEngine
) {
    suspend operator fun invoke(updatedTransaction: Transaction) {
        require(updatedTransaction.amount > 0L) { "Transaction amount must be greater than 0" }

        val existing = transactionRepository.getTransactionById(updatedTransaction.id)
            ?: throw EntityNotFoundException("Transaction with ID ${updatedTransaction.id} not found")

        val allTransactions = transactionRepository.getAllTransactions()
        val allGoals = savingsGoalRepository.getAllGoals()

        // Remove existing transaction to simulate the updated state
        val historyWithoutExisting = allTransactions.filter { it.id != existing.id }

        // Check lifetime available balance with updated transaction
        val simulatedTransactions = historyWithoutExisting + updatedTransaction
        val lifetimeBalance = ledgerEngine.calculateLifetimeAvailableBalance(simulatedTransactions, allGoals)
        if (lifetimeBalance < 0L) {
            throw InsufficientBalanceException("Updating transaction would result in negative available balance: $lifetimeBalance")
        }

        // Point-in-time running balance validation
        val isValidRunning = ledgerEngine.validateRunningBalance(
            mutatedTimestamp = updatedTransaction.timestamp,
            delta = updatedTransaction.signedAmount,
            history = historyWithoutExisting
        )
        if (!isValidRunning) {
            throw InsufficientBalanceException("Updating transaction causes point-in-time overdraft")
        }

        transactionRepository.updateTransaction(updatedTransaction)
    }
}
