package com.moneysaver.habittracker.domain

import com.moneysaver.habittracker.domain.engine.FinancialLedgerEngine
import com.moneysaver.habittracker.domain.model.GoalStatus
import com.moneysaver.habittracker.domain.model.SavingsGoal
import com.moneysaver.habittracker.domain.model.Transaction
import com.moneysaver.habittracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FinancialLedgerEngineTest {

    private lateinit var engine: FinancialLedgerEngine

    @Before
    fun setUp() {
        engine = FinancialLedgerEngine()
    }

    @Test
    fun `calculateLifetimeAvailableBalance correctly handles minor units and active savings`() {
        // Income = $1000.00 (100,000 cents)
        val tx1 = Transaction(id = 1L, amount = 100000L, type = TransactionType.INCOME, timestamp = 1000L)
        // Expense = $250.50 (25,050 cents)
        val tx2 = Transaction(id = 2L, amount = 25050L, type = TransactionType.EXPENSE, timestamp = 2000L)
        // Active Savings Goal with $200.00 saved
        val goal1 = SavingsGoal(id = 1L, title = "Emergency Fund", targetAmount = 50000L, savedAmount = 20000L, status = GoalStatus.IN_PROGRESS)
        // Cancelled goal should not count toward active allocated savings
        val goal2 = SavingsGoal(id = 2L, title = "Old Goal", targetAmount = 10000L, savedAmount = 5000L, status = GoalStatus.CANCELLED)

        val transactions = listOf(tx1, tx2)
        val goals = listOf(goal1, goal2)

        // Expected available balance = 100000 - 25050 - 20000 = 54950 ($549.50)
        val balance = engine.calculateLifetimeAvailableBalance(transactions, goals)
        assertEquals(54950L, balance)
    }

    @Test
    fun `calculateLifetimeAvailableBalance derives allocated savings from internal transfers when goals list empty`() {
        val txs = listOf(
            Transaction(id = 1L, amount = 50000L, type = TransactionType.INCOME, timestamp = 1000L),
            Transaction(id = 2L, amount = 15000L, type = TransactionType.SAVINGS_DEPOSIT, timestamp = 1500L),
            Transaction(id = 3L, amount = 5000L, type = TransactionType.SAVINGS_WITHDRAWAL, timestamp = 2000L),
            Transaction(id = 4L, amount = 10000L, type = TransactionType.EXPENSE, timestamp = 2500L)
        )

        // Income = 50000, Expense = 10000, Net Savings = 15000 - 5000 = 10000
        // Expected available = 50000 - 10000 - 10000 = 30000
        val balance = engine.calculateLifetimeAvailableBalance(txs)
        assertEquals(30000L, balance)
    }

    @Test
    fun `calculatePeriodMetrics strictly excludes internal transfers`() {
        val txs = listOf(
            Transaction(id = 1L, amount = 100000L, type = TransactionType.INCOME, timestamp = 1000L),
            Transaction(id = 2L, amount = 30000L, type = TransactionType.EXPENSE, timestamp = 2000L),
            Transaction(id = 3L, amount = 20000L, type = TransactionType.SAVINGS_DEPOSIT, timestamp = 3000L),
            Transaction(id = 4L, amount = 5000L, type = TransactionType.SAVINGS_WITHDRAWAL, timestamp = 4000L)
        )

        val metrics = engine.calculatePeriodMetrics(
            transactions = txs,
            startDate = 500L,
            endDate = 5000L
        )

        assertEquals(100000L, metrics.totalIncome)
        assertEquals(30000L, metrics.totalExpense)
        assertEquals(70000L, metrics.netCashFlow)
        assertEquals(70000L, metrics.netSavings)
        assertEquals(2, metrics.transactionCount) // tx1 and tx2 only
    }

    @Test
    fun `calculatePeriodMetrics filters by date range inclusively`() {
        val txs = listOf(
            Transaction(id = 1L, amount = 5000L, type = TransactionType.INCOME, timestamp = 100L),
            Transaction(id = 2L, amount = 8000L, type = TransactionType.INCOME, timestamp = 200L),
            Transaction(id = 3L, amount = 3000L, type = TransactionType.EXPENSE, timestamp = 300L),
            Transaction(id = 4L, amount = 4000L, type = TransactionType.INCOME, timestamp = 400L)
        )

        val metrics = engine.calculatePeriodMetrics(txs, startDate = 200L, endDate = 300L)
        assertEquals(8000L, metrics.totalIncome)
        assertEquals(3000L, metrics.totalExpense)
        assertEquals(5000L, metrics.netCashFlow)
        assertEquals(2, metrics.transactionCount)
    }

    @Test
    fun `validateExpenseAllowed requires positive expense and sufficient available balance`() {
        assertTrue(engine.validateExpenseAllowed(currentAvailable = 10000L, expenseAmount = 5000L))
        assertTrue(engine.validateExpenseAllowed(currentAvailable = 5000L, expenseAmount = 5000L))
        assertFalse(engine.validateExpenseAllowed(currentAvailable = 4999L, expenseAmount = 5000L))
        assertFalse(engine.validateExpenseAllowed(currentAvailable = 10000L, expenseAmount = 0L))
        assertFalse(engine.validateExpenseAllowed(currentAvailable = 10000L, expenseAmount = -100L))
    }

    @Test
    fun `validateRunningBalance succeeds for valid chronologically ordered transactions`() {
        val history = listOf(
            Transaction(id = 1L, amount = 10000L, type = TransactionType.INCOME, timestamp = 1000L),
            Transaction(id = 2L, amount = 4000L, type = TransactionType.EXPENSE, timestamp = 2000L),
            Transaction(id = 3L, amount = 3000L, type = TransactionType.EXPENSE, timestamp = 3000L)
        )

        // Adding income delta = +2000 at timestamp 1500
        assertTrue(engine.validateRunningBalance(mutatedTimestamp = 1500L, delta = 2000L, history = history))

        // Adding expense delta = -5000 at timestamp 3500 (balance after 3000 is 3000; 3000 - 5000 = -2000 < 0)
        assertFalse(engine.validateRunningBalance(mutatedTimestamp = 3500L, delta = -5000L, history = history))

        // Adding expense delta = -2000 at timestamp 3500 (3000 - 2000 = 1000 >= 0)
        assertTrue(engine.validateRunningBalance(mutatedTimestamp = 3500L, delta = -2000L, history = history))
    }

    @Test
    fun `validateRunningBalance detects point-in-time overdraft for backdated expense`() {
        // Timeline:
        // t=1000: +10000 (bal = 10000)
        // t=2000: -8000  (bal = 2000)
        // t=3000: +50000 (bal = 52000)
        val history = listOf(
            Transaction(id = 1L, amount = 10000L, type = TransactionType.INCOME, timestamp = 1000L),
            Transaction(id = 2L, amount = 8000L, type = TransactionType.EXPENSE, timestamp = 2000L),
            Transaction(id = 3L, amount = 50000L, type = TransactionType.INCOME, timestamp = 3000L)
        )

        // Backdating expense of 5000 at t=1500:
        // at t=1500: bal = 10000 - 5000 = 5000 >= 0
        // at t=2000: bal = 5000 - 8000 = -3000 < 0! (OVERDRAFT)
        // at t=3000: bal would be 47000 >= 0, but dip at t=2000 is invalid
        val isValid = engine.validateRunningBalance(mutatedTimestamp = 1500L, delta = -5000L, history = history)
        assertFalse(isValid)

        // Backdating expense of 1500 at t=1500:
        // at t=1500: bal = 10000 - 1500 = 8500 >= 0
        // at t=2000: bal = 8500 - 8000 = 500 >= 0
        // at t=3000: bal = 50500 >= 0
        val isSmallExpenseValid = engine.validateRunningBalance(mutatedTimestamp = 1500L, delta = -1500L, history = history)
        assertTrue(isSmallExpenseValid)
    }

    @Test
    fun `validateRunningBalance handles empty history and pre-history timestamp`() {
        val emptyHistory = emptyList<Transaction>()

        assertTrue(engine.validateRunningBalance(mutatedTimestamp = 1000L, delta = 5000L, history = emptyHistory))
        assertFalse(engine.validateRunningBalance(mutatedTimestamp = 1000L, delta = -100L, history = emptyHistory))

        val singleTxHistory = listOf(
            Transaction(id = 1L, amount = 10000L, type = TransactionType.INCOME, timestamp = 2000L)
        )

        // Delta before first transaction with negative amount must fail (cannot spend before earning)
        assertFalse(engine.validateRunningBalance(mutatedTimestamp = 1000L, delta = -500L, history = singleTxHistory))
        // Delta before first transaction with positive amount is fine
        assertTrue(engine.validateRunningBalance(mutatedTimestamp = 1000L, delta = 500L, history = singleTxHistory))
    }
}
