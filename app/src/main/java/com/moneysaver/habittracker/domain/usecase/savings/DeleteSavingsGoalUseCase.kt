package com.moneysaver.habittracker.domain.usecase.savings

import com.moneysaver.habittracker.domain.exception.EntityNotFoundException
import com.moneysaver.habittracker.domain.model.SavingsGoal
import com.moneysaver.habittracker.domain.model.Transaction
import com.moneysaver.habittracker.domain.model.TransactionType
import com.moneysaver.habittracker.domain.repository.SavingsGoalRepository
import com.moneysaver.habittracker.domain.repository.TransactionRepository

sealed class GoalDeletionStrategy {
    data class RefundToBalance(
        val timestamp: Long = System.currentTimeMillis()
    ) : GoalDeletionStrategy()

    data class RealizeAsExpense(
        val categoryId: Long,
        val timestamp: Long = System.currentTimeMillis(),
        val note: String = ""
    ) : GoalDeletionStrategy()
}

class DeleteSavingsGoalUseCase(
    private val savingsGoalRepository: SavingsGoalRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        goalId: Long,
        strategy: GoalDeletionStrategy = GoalDeletionStrategy.RefundToBalance()
    ) {
        val goal = savingsGoalRepository.getGoalById(goalId)
            ?: throw EntityNotFoundException("Savings goal with ID $goalId not found")

        if (goal.savedAmount > 0L) {
            when (strategy) {
                is GoalDeletionStrategy.RefundToBalance -> {
                    val refundTx = Transaction(
                        amount = goal.savedAmount,
                        type = TransactionType.SAVINGS_WITHDRAWAL,
                        savingsGoalId = goalId,
                        isInternalTransfer = true,
                        timestamp = strategy.timestamp,
                        note = "Refund from deleted savings goal: ${goal.title}"
                    )
                    transactionRepository.insertTransaction(refundTx)
                }
                is GoalDeletionStrategy.RealizeAsExpense -> {
                    val expenseTx = Transaction(
                        amount = goal.savedAmount,
                        type = TransactionType.EXPENSE,
                        categoryId = strategy.categoryId,
                        savingsGoalId = goalId,
                        isInternalTransfer = false,
                        timestamp = strategy.timestamp,
                        note = strategy.note.ifBlank { "Realized expense from goal: ${goal.title}" }
                    )
                    transactionRepository.insertTransaction(expenseTx)
                }
            }
        }

        savingsGoalRepository.deleteGoal(goal)
    }
}
