package com.moneysaver.habittracker.domain.usecase.savings

import com.moneysaver.habittracker.domain.exception.EntityNotFoundException
import com.moneysaver.habittracker.domain.exception.InsufficientBalanceException
import com.moneysaver.habittracker.domain.model.GoalStatus
import com.moneysaver.habittracker.domain.model.Transaction
import com.moneysaver.habittracker.domain.model.TransactionType
import com.moneysaver.habittracker.domain.repository.SavingsGoalRepository
import com.moneysaver.habittracker.domain.repository.TransactionRepository

class WithdrawSavingsGoalUseCase(
    private val savingsGoalRepository: SavingsGoalRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        goalId: Long,
        amount: Long,
        timestamp: Long = System.currentTimeMillis(),
        note: String = ""
    ): Transaction {
        require(amount > 0L) { "Withdrawal amount must be greater than 0" }

        val goal = savingsGoalRepository.getGoalById(goalId)
            ?: throw EntityNotFoundException("Savings goal with ID $goalId not found")

        if (amount > goal.savedAmount) {
            throw InsufficientBalanceException(
                "Cannot withdraw $amount: goal '${goal.title}' only has ${goal.savedAmount} saved"
            )
        }

        // 1. Create internal SAVINGS_WITHDRAWAL transaction (returns to available balance)
        val withdrawalTx = Transaction(
            amount = amount,
            type = TransactionType.SAVINGS_WITHDRAWAL,
            savingsGoalId = goalId,
            isInternalTransfer = true,
            timestamp = timestamp,
            note = note.ifBlank { "Withdrawn from savings goal: ${goal.title}" }
        )
        val txId = transactionRepository.insertTransaction(withdrawalTx)
        val createdTx = withdrawalTx.copy(id = txId)

        // 2. Update goal savedAmount and status
        val newSavedAmount = goal.savedAmount - amount
        val newStatus = if (newSavedAmount < goal.targetAmount && goal.status == GoalStatus.ACHIEVED) {
            GoalStatus.IN_PROGRESS
        } else {
            goal.status
        }
        savingsGoalRepository.updateGoal(goal.copy(savedAmount = newSavedAmount, status = newStatus))

        return createdTx
    }
}
