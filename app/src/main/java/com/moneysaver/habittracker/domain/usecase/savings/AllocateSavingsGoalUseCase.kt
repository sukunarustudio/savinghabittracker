package com.moneysaver.habittracker.domain.usecase.savings

import com.moneysaver.habittracker.domain.engine.FinancialLedgerEngine
import com.moneysaver.habittracker.domain.exception.EntityNotFoundException
import com.moneysaver.habittracker.domain.exception.InsufficientBalanceException
import com.moneysaver.habittracker.domain.model.GoalStatus
import com.moneysaver.habittracker.domain.model.Transaction
import com.moneysaver.habittracker.domain.model.TransactionType
import com.moneysaver.habittracker.domain.repository.SavingsGoalRepository
import com.moneysaver.habittracker.domain.repository.TransactionRepository

class AllocateSavingsGoalUseCase(
    private val savingsGoalRepository: SavingsGoalRepository,
    private val transactionRepository: TransactionRepository,
    private val ledgerEngine: FinancialLedgerEngine
) {
    suspend operator fun invoke(
        goalId: Long,
        amount: Long,
        timestamp: Long = System.currentTimeMillis(),
        note: String = ""
    ): Transaction {
        require(amount > 0L) { "Allocation amount must be greater than 0" }

        val goal = savingsGoalRepository.getGoalById(goalId)
            ?: throw EntityNotFoundException("Savings goal with ID $goalId not found")

        val allTransactions = transactionRepository.getAllTransactions()
        val allGoals = savingsGoalRepository.getAllGoals()

        val available = ledgerEngine.calculateLifetimeAvailableBalance(allTransactions, allGoals)
        if (!ledgerEngine.validateExpenseAllowed(available, amount)) {
            throw InsufficientBalanceException("Cannot allocate $amount: net available balance is $available")
        }

        // Point-in-time check: allocation draws from available balance (delta = -amount)
        val isValidRunning = ledgerEngine.validateRunningBalance(
            mutatedTimestamp = timestamp,
            delta = -amount,
            history = allTransactions
        )
        if (!isValidRunning) {
            throw InsufficientBalanceException("Allocation of $amount at timestamp $timestamp causes running balance overdraft")
        }

        // 1. Create internal SAVINGS_DEPOSIT transaction
        val depositTx = Transaction(
            amount = amount,
            type = TransactionType.SAVINGS_DEPOSIT,
            savingsGoalId = goalId,
            isInternalTransfer = true,
            timestamp = timestamp,
            note = note.ifBlank { "Allocated to savings goal: ${goal.title}" }
        )
        val txId = transactionRepository.insertTransaction(depositTx)
        val createdTx = depositTx.copy(id = txId)

        // 2. Update goal savedAmount and status
        val newSavedAmount = goal.savedAmount + amount
        val newStatus = if (newSavedAmount >= goal.targetAmount) GoalStatus.ACHIEVED else goal.status
        savingsGoalRepository.updateGoal(goal.copy(savedAmount = newSavedAmount, status = newStatus))

        return createdTx
    }
}
