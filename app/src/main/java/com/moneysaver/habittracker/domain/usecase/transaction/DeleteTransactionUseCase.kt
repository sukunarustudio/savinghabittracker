package com.moneysaver.habittracker.domain.usecase.transaction

import com.moneysaver.habittracker.domain.engine.FinancialLedgerEngine
import com.moneysaver.habittracker.domain.exception.EntityNotFoundException
import com.moneysaver.habittracker.domain.exception.InsufficientBalanceException
import com.moneysaver.habittracker.domain.repository.SavingsGoalRepository
import com.moneysaver.habittracker.domain.repository.TransactionRepository

class DeleteTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val ledgerEngine: FinancialLedgerEngine
) {
    suspend operator fun invoke(transactionId: Long) {
        val existing = transactionRepository.getTransactionById(transactionId)
            ?: throw EntityNotFoundException("Transaction with ID $transactionId not found")

        val allTransactions = transactionRepository.getAllTransactions()
        val allGoals = savingsGoalRepository.getAllGoals()

        val remainingTransactions = allTransactions.filter { it.id != existing.id }

        // Check lifetime available balance after removing transaction
        val lifetimeBalance = ledgerEngine.calculateLifetimeAvailableBalance(remainingTransactions, allGoals)
        if (lifetimeBalance < 0L) {
            throw InsufficientBalanceException(
                "Deleting transaction would result in negative available balance: $lifetimeBalance"
            )
        }

        // Check point-in-time running balance: removing transaction applies delta = -signedAmount
        val isValidRunning = ledgerEngine.validateRunningBalance(
            mutatedTimestamp = existing.timestamp,
            delta = -existing.signedAmount,
            history = allTransactions
        )
        if (!isValidRunning) {
            throw InsufficientBalanceException("Deleting transaction causes point-in-time overdraft")
        }

        transactionRepository.deleteTransaction(existing)
    }
}
