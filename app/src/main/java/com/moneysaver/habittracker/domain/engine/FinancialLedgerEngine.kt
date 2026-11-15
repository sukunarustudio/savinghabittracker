package com.moneysaver.habittracker.domain.engine

import com.moneysaver.habittracker.domain.model.GoalStatus
import com.moneysaver.habittracker.domain.model.PeriodMetrics
import com.moneysaver.habittracker.domain.model.SavingsGoal
import com.moneysaver.habittracker.domain.model.Transaction
import com.moneysaver.habittracker.domain.model.TransactionType

class FinancialLedgerEngine {

    /**
     * Net Available Balance = (Total Lifetime Income - Total Lifetime Expenses - Total Active Allocated Savings)
     * All monetary values are represented in minor units (cents).
     */
    fun calculateLifetimeAvailableBalance(
        transactions: List<Transaction>,
        savingsGoals: List<SavingsGoal> = emptyList()
    ): Long {
        val totalIncome = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        val totalExpenses = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        val totalActiveAllocatedSavings = if (savingsGoals.isNotEmpty()) {
            savingsGoals
                .filter { it.status.isActive }
                .sumOf { it.savedAmount }
        } else {
            // When savingsGoals list is omitted/empty, derive allocated savings from internal ledger transfers:
            val savingsDeposited = transactions
                .filter { it.type == TransactionType.SAVINGS_DEPOSIT }
                .sumOf { it.amount }
            val savingsWithdrawn = transactions
                .filter { it.type == TransactionType.SAVINGS_WITHDRAWAL }
                .sumOf { it.amount }
            (savingsDeposited - savingsWithdrawn).coerceAtLeast(0L)
        }

        return totalIncome - totalExpenses - totalActiveAllocatedSavings
    }

    /**
     * Calculates period metrics for the given date range (inclusive),
     * strictly excluding internal transfers (SAVINGS_DEPOSIT and SAVINGS_WITHDRAWAL).
     */
    fun calculatePeriodMetrics(
        transactions: List<Transaction>,
        startDate: Long,
        endDate: Long
    ): PeriodMetrics {
        val filtered = transactions.filter { tx ->
            tx.timestamp in startDate..endDate && !tx.isInternalTransfer
        }

        val totalIncome = filtered
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        val totalExpense = filtered
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        return PeriodMetrics(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netCashFlow = totalIncome - totalExpense,
            transactionCount = filtered.size
        )
    }

    /**
     * Checks if an expense is allowed given current available balance.
     * Expense amount must be > 0 and available balance must be >= expenseAmount.
     */
    fun validateExpenseAllowed(currentAvailable: Long, expenseAmount: Long): Boolean {
        if (expenseAmount <= 0L) return false
        return currentAvailable >= expenseAmount
    }

    /**
     * Point-in-time running balance validation:
     * Verifies that applying [delta] at [mutatedTimestamp] does not cause an overdraft (< 0)
     * at [mutatedTimestamp] or at any subsequent transaction in [history].
     */
    fun validateRunningBalance(
        mutatedTimestamp: Long,
        delta: Long,
        history: List<Transaction>
    ): Boolean {
        val sorted = history.sortedWith(compareBy({ it.timestamp }, { it.id }))

        // If history is empty, starting balance is 0
        if (sorted.isEmpty()) {
            return delta >= 0L
        }

        // If mutatedTimestamp is strictly before the first transaction:
        // Balance before the first transaction was 0, so 0 + delta must be >= 0
        if (mutatedTimestamp < sorted.first().timestamp) {
            if (delta < 0L) return false
        }

        var runningBalance = 0L
        var checkedIntermediate = false

        for (tx in sorted) {
            // If mutatedTimestamp falls before this tx, verify balance at mutatedTimestamp
            if (!checkedIntermediate && mutatedTimestamp < tx.timestamp) {
                if (runningBalance + delta < 0L) {
                    return false
                }
                checkedIntermediate = true
            }

            runningBalance += tx.signedAmount

            // If this transaction is at or after mutatedTimestamp, delta shifts the running balance
            if (tx.timestamp >= mutatedTimestamp) {
                if (runningBalance + delta < 0L) {
                    return false
                }
            }
        }

        // If mutatedTimestamp is after all existing transactions in history:
        if (!checkedIntermediate && mutatedTimestamp >= sorted.last().timestamp) {
            if (runningBalance + delta < 0L) {
                return false
            }
        }

        return true
    }
}
