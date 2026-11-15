package com.moneysaver.habittracker.domain

import com.moneysaver.habittracker.domain.engine.FinancialLedgerEngine
import com.moneysaver.habittracker.domain.exception.EntityNotFoundException
import com.moneysaver.habittracker.domain.exception.InsufficientBalanceException
import com.moneysaver.habittracker.domain.fake.FakeSavingsGoalRepository
import com.moneysaver.habittracker.domain.fake.FakeTransactionRepository
import com.moneysaver.habittracker.domain.model.GoalStatus
import com.moneysaver.habittracker.domain.model.SavingsGoal
import com.moneysaver.habittracker.domain.model.Transaction
import com.moneysaver.habittracker.domain.model.TransactionType
import com.moneysaver.habittracker.domain.usecase.savings.AllocateSavingsGoalUseCase
import com.moneysaver.habittracker.domain.usecase.savings.CreateSavingsGoalUseCase
import com.moneysaver.habittracker.domain.usecase.savings.DeleteSavingsGoalUseCase
import com.moneysaver.habittracker.domain.usecase.savings.GoalDeletionStrategy
import com.moneysaver.habittracker.domain.usecase.savings.WithdrawSavingsGoalUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SavingsGoalUseCasesTest {

    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var savingsGoalRepository: FakeSavingsGoalRepository
    private lateinit var ledgerEngine: FinancialLedgerEngine

    private lateinit var createSavingsGoalUseCase: CreateSavingsGoalUseCase
    private lateinit var allocateSavingsGoalUseCase: AllocateSavingsGoalUseCase
    private lateinit var withdrawSavingsGoalUseCase: WithdrawSavingsGoalUseCase
    private lateinit var deleteSavingsGoalUseCase: DeleteSavingsGoalUseCase

    @Before
    fun setUp() {
        transactionRepository = FakeTransactionRepository()
        savingsGoalRepository = FakeSavingsGoalRepository()
        ledgerEngine = FinancialLedgerEngine()

        createSavingsGoalUseCase = CreateSavingsGoalUseCase(savingsGoalRepository)
        allocateSavingsGoalUseCase = AllocateSavingsGoalUseCase(savingsGoalRepository, transactionRepository, ledgerEngine)
        withdrawSavingsGoalUseCase = WithdrawSavingsGoalUseCase(savingsGoalRepository, transactionRepository)
        deleteSavingsGoalUseCase = DeleteSavingsGoalUseCase(savingsGoalRepository, transactionRepository)
    }

    @Test
    fun `createSavingsGoal validates inputs and sets initial status`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                createSavingsGoalUseCase(SavingsGoal(title = "", targetAmount = 10000L))
            }
        }

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                createSavingsGoalUseCase(SavingsGoal(title = "Car", targetAmount = 0L))
            }
        }

        val goalId = createSavingsGoalUseCase(
            SavingsGoal(title = "Car", targetAmount = 50000L, savedAmount = 0L)
        )
        val created = savingsGoalRepository.getGoalById(goalId)
        assertNotNull(created)
        assertEquals(GoalStatus.IN_PROGRESS, created?.status)
    }

    @Test
    fun `allocateSavingsGoal creates internal SAVINGS_DEPOSIT and updates goal`() = runTest {
        // Fund account with $1000 (100,000 cents)
        transactionRepository.insertTransaction(
            Transaction(amount = 100000L, type = TransactionType.INCOME, timestamp = 1000L)
        )

        val goalId = createSavingsGoalUseCase(
            SavingsGoal(title = "Emergency", targetAmount = 50000L, savedAmount = 0L)
        )

        // Allocate 30000 cents
        val depositTx = allocateSavingsGoalUseCase(goalId = goalId, amount = 30000L, timestamp = 2000L)

        assertEquals(TransactionType.SAVINGS_DEPOSIT, depositTx.type)
        assertEquals(30000L, depositTx.amount)
        assertTrue(depositTx.isInternalTransfer)
        assertEquals(goalId, depositTx.savingsGoalId)

        // Check goal updated
        val updatedGoal = savingsGoalRepository.getGoalById(goalId)
        assertEquals(30000L, updatedGoal?.savedAmount)
        assertEquals(GoalStatus.IN_PROGRESS, updatedGoal?.status)

        // Net available balance is now 100000 - 30000 = 70000 cents
        val available = ledgerEngine.calculateLifetimeAvailableBalance(
            transactionRepository.getAllTransactions(),
            savingsGoalRepository.getAllGoals()
        )
        assertEquals(70000L, available)
    }

    @Test
    fun `allocateSavingsGoal marks goal ACHIEVED when target reached`() = runTest {
        transactionRepository.insertTransaction(
            Transaction(amount = 100000L, type = TransactionType.INCOME, timestamp = 1000L)
        )
        val goalId = createSavingsGoalUseCase(
            SavingsGoal(title = "Headphones", targetAmount = 25000L, savedAmount = 0L)
        )

        allocateSavingsGoalUseCase(goalId = goalId, amount = 25000L, timestamp = 2000L)

        val updatedGoal = savingsGoalRepository.getGoalById(goalId)
        assertEquals(25000L, updatedGoal?.savedAmount)
        assertEquals(GoalStatus.ACHIEVED, updatedGoal?.status)
    }

    @Test
    fun `allocateSavingsGoal rejects allocation exceeding available balance`() = runTest {
        transactionRepository.insertTransaction(
            Transaction(amount = 10000L, type = TransactionType.INCOME, timestamp = 1000L)
        )
        val goalId = createSavingsGoalUseCase(
            SavingsGoal(title = "Laptop", targetAmount = 50000L, savedAmount = 0L)
        )

        assertThrows(InsufficientBalanceException::class.java) {
            kotlinx.coroutines.runBlocking {
                allocateSavingsGoalUseCase(goalId = goalId, amount = 15000L, timestamp = 2000L)
            }
        }
    }

    @Test
    fun `withdrawSavingsGoal creates SAVINGS_WITHDRAWAL and returns money to available balance`() = runTest {
        transactionRepository.insertTransaction(
            Transaction(amount = 100000L, type = TransactionType.INCOME, timestamp = 1000L)
        )
        val goalId = createSavingsGoalUseCase(
            SavingsGoal(title = "Vacation", targetAmount = 50000L, savedAmount = 0L)
        )

        // Allocate 40000
        allocateSavingsGoalUseCase(goalId = goalId, amount = 40000L, timestamp = 2000L)

        // Withdraw 15000
        val withdrawalTx = withdrawSavingsGoalUseCase(goalId = goalId, amount = 15000L, timestamp = 3000L)

        assertEquals(TransactionType.SAVINGS_WITHDRAWAL, withdrawalTx.type)
        assertEquals(15000L, withdrawalTx.amount)
        assertTrue(withdrawalTx.isInternalTransfer)

        val updatedGoal = savingsGoalRepository.getGoalById(goalId)
        assertEquals(25000L, updatedGoal?.savedAmount)

        // Available balance is now 100000 - 25000 = 75000
        val available = ledgerEngine.calculateLifetimeAvailableBalance(
            transactionRepository.getAllTransactions(),
            savingsGoalRepository.getAllGoals()
        )
        assertEquals(75000L, available)
    }

    @Test
    fun `withdrawSavingsGoal rejects amount exceeding saved amount`() = runTest {
        val goalId = createSavingsGoalUseCase(
            SavingsGoal(title = "Vacation", targetAmount = 50000L, savedAmount = 10000L)
        )

        assertThrows(InsufficientBalanceException::class.java) {
            kotlinx.coroutines.runBlocking {
                withdrawSavingsGoalUseCase(goalId = goalId, amount = 15000L, timestamp = 1000L)
            }
        }
    }

    @Test
    fun `deleteSavingsGoal with refundToBalance returns funds to available balance`() = runTest {
        transactionRepository.insertTransaction(
            Transaction(amount = 100000L, type = TransactionType.INCOME, timestamp = 1000L)
        )
        val goalId = createSavingsGoalUseCase(
            SavingsGoal(title = "Old Bike", targetAmount = 30000L, savedAmount = 0L)
        )
        allocateSavingsGoalUseCase(goalId = goalId, amount = 20000L, timestamp = 2000L)

        // Available balance before deletion: 100000 - 20000 = 80000
        deleteSavingsGoalUseCase(goalId = goalId, strategy = GoalDeletionStrategy.RefundToBalance(timestamp = 3000L))

        assertNull(savingsGoalRepository.getGoalById(goalId))

        // Check refund transaction was generated
        val transactions = transactionRepository.getAllTransactions()
        val refundTx = transactions.find { it.type == TransactionType.SAVINGS_WITHDRAWAL }
        assertNotNull(refundTx)
        assertEquals(20000L, refundTx?.amount)

        // Available balance restored to 100000
        val available = ledgerEngine.calculateLifetimeAvailableBalance(
            transactionRepository.getAllTransactions(),
            savingsGoalRepository.getAllGoals()
        )
        assertEquals(100000L, available)
    }

    @Test
    fun `deleteSavingsGoal with realizeAsExpense converts savings to external expense`() = runTest {
        transactionRepository.insertTransaction(
            Transaction(amount = 100000L, type = TransactionType.INCOME, timestamp = 1000L)
        )
        val goalId = createSavingsGoalUseCase(
            SavingsGoal(title = "Laptop Purchase", targetAmount = 40000L, savedAmount = 0L)
        )
        allocateSavingsGoalUseCase(goalId = goalId, amount = 40000L, timestamp = 2000L)

        // Delete goal and realize 40000 as external expense under categoryId = 42
        deleteSavingsGoalUseCase(
            goalId = goalId,
            strategy = GoalDeletionStrategy.RealizeAsExpense(
                categoryId = 42L,
                timestamp = 3000L,
                note = "Bought laptop"
            )
        )

        assertNull(savingsGoalRepository.getGoalById(goalId))

        // Check external expense transaction exists
        val expenseTx = transactionRepository.getAllTransactions().find { it.type == TransactionType.EXPENSE }
        assertNotNull(expenseTx)
        assertEquals(40000L, expenseTx?.amount)
        assertEquals(42L, expenseTx?.categoryId)
        assertEquals(false, expenseTx?.isInternalTransfer)

        // Available balance was 60000 before deletion, and remains 60000 (100000 income - 40000 expense - 0 active savings)
        val available = ledgerEngine.calculateLifetimeAvailableBalance(
            transactionRepository.getAllTransactions(),
            savingsGoalRepository.getAllGoals()
        )
        assertEquals(60000L, available)

        // Period metrics now reflects the realized expense:
        val metrics = ledgerEngine.calculatePeriodMetrics(
            transactions = transactionRepository.getAllTransactions(),
            startDate = 500L,
            endDate = 5000L
        )
        assertEquals(100000L, metrics.totalIncome)
        assertEquals(40000L, metrics.totalExpense)
        assertEquals(60000L, metrics.netCashFlow)
    }
}
