package com.moneysaver.habittracker.domain

import com.moneysaver.habittracker.domain.engine.FinancialLedgerEngine
import com.moneysaver.habittracker.domain.exception.EntityNotFoundException
import com.moneysaver.habittracker.domain.exception.InsufficientBalanceException
import com.moneysaver.habittracker.domain.fake.FakeSavingsGoalRepository
import com.moneysaver.habittracker.domain.fake.FakeTransactionRepository
import com.moneysaver.habittracker.domain.model.SavingsGoal
import com.moneysaver.habittracker.domain.model.Transaction
import com.moneysaver.habittracker.domain.model.TransactionType
import com.moneysaver.habittracker.domain.usecase.transaction.CreateTransactionUseCase
import com.moneysaver.habittracker.domain.usecase.transaction.DeleteTransactionUseCase
import com.moneysaver.habittracker.domain.usecase.transaction.GetTransactionsUseCase
import com.moneysaver.habittracker.domain.usecase.transaction.UpdateTransactionUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class TransactionUseCasesTest {

    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var savingsGoalRepository: FakeSavingsGoalRepository
    private lateinit var ledgerEngine: FinancialLedgerEngine

    private lateinit var createTransactionUseCase: CreateTransactionUseCase
    private lateinit var updateTransactionUseCase: UpdateTransactionUseCase
    private lateinit var deleteTransactionUseCase: DeleteTransactionUseCase
    private lateinit var getTransactionsUseCase: GetTransactionsUseCase

    @Before
    fun setUp() {
        transactionRepository = FakeTransactionRepository()
        savingsGoalRepository = FakeSavingsGoalRepository()
        ledgerEngine = FinancialLedgerEngine()

        createTransactionUseCase = CreateTransactionUseCase(transactionRepository, savingsGoalRepository, ledgerEngine)
        updateTransactionUseCase = UpdateTransactionUseCase(transactionRepository, savingsGoalRepository, ledgerEngine)
        deleteTransactionUseCase = DeleteTransactionUseCase(transactionRepository, savingsGoalRepository, ledgerEngine)
        getTransactionsUseCase = GetTransactionsUseCase(transactionRepository)
    }

    @Test
    fun `createTransaction rejects non-positive amounts`() = runTest {
        val zeroTx = Transaction(amount = 0L, type = TransactionType.INCOME, timestamp = 1000L)
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { createTransactionUseCase(zeroTx) }
        }

        val negativeTx = Transaction(amount = -500L, type = TransactionType.INCOME, timestamp = 1000L)
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { createTransactionUseCase(negativeTx) }
        }
    }

    @Test
    fun `createTransaction rejects expense when available balance is insufficient`() = runTest {
        // Balance is 0 initially. Trying to spend 1000 cents.
        val expenseTx = Transaction(amount = 1000L, type = TransactionType.EXPENSE, timestamp = 1000L)
        assertThrows(InsufficientBalanceException::class.java) {
            kotlinx.coroutines.runBlocking { createTransactionUseCase(expenseTx) }
        }
    }

    @Test
    fun `createTransaction succeeds when funds are available`() = runTest {
        val income = Transaction(amount = 50000L, type = TransactionType.INCOME, timestamp = 1000L)
        val incomeId = createTransactionUseCase(income)
        assertEquals(1L, incomeId)

        val expense = Transaction(amount = 20000L, type = TransactionType.EXPENSE, timestamp = 2000L)
        val expenseId = createTransactionUseCase(expense)
        assertEquals(2L, expenseId)

        val all = transactionRepository.getAllTransactions()
        assertEquals(2, all.size)
    }

    @Test
    fun `createTransaction accounts for active savings goals reducing available balance`() = runTest {
        // Income = 50000 cents ($500)
        createTransactionUseCase(Transaction(amount = 50000L, type = TransactionType.INCOME, timestamp = 1000L))

        // Active savings goal holds 40000 cents ($400)
        savingsGoalRepository.insertGoal(SavingsGoal(id = 1L, title = "House", targetAmount = 100000L, savedAmount = 40000L))

        // Net available balance is now 50000 - 40000 = 10000 cents ($100)
        // Trying to spend 15000 cents should fail
        val expense = Transaction(amount = 15000L, type = TransactionType.EXPENSE, timestamp = 2000L)
        assertThrows(InsufficientBalanceException::class.java) {
            kotlinx.coroutines.runBlocking { createTransactionUseCase(expense) }
        }

        // Spending 10000 cents should succeed
        val validExpense = Transaction(amount = 10000L, type = TransactionType.EXPENSE, timestamp = 2000L)
        createTransactionUseCase(validExpense)
        assertEquals(2, transactionRepository.getAllTransactions().size)
    }

    @Test
    fun `createTransaction rejects backdated expense causing point-in-time overdraft`() = runTest {
        // Timeline:
        // t=1000: +10000 (bal = 10000)
        // t=2000: -8000  (bal = 2000)
        // t=3000: +30000 (bal = 32000)
        createTransactionUseCase(Transaction(amount = 10000L, type = TransactionType.INCOME, timestamp = 1000L))
        createTransactionUseCase(Transaction(amount = 8000L, type = TransactionType.EXPENSE, timestamp = 2000L))
        createTransactionUseCase(Transaction(amount = 30000L, type = TransactionType.INCOME, timestamp = 3000L))

        // Lifetime available is 32000.
        // If user backdates an expense of 5000 at t=1500:
        // at t=2000, balance would dip to 10000 - 5000 - 8000 = -3000 < 0!
        val backdatedExpense = Transaction(amount = 5000L, type = TransactionType.EXPENSE, timestamp = 1500L)
        assertThrows(InsufficientBalanceException::class.java) {
            kotlinx.coroutines.runBlocking { createTransactionUseCase(backdatedExpense) }
        }
    }

    @Test
    fun `updateTransaction enforces point-in-time validation`() = runTest {
        createTransactionUseCase(Transaction(amount = 10000L, type = TransactionType.INCOME, timestamp = 1000L))
        val expId = createTransactionUseCase(Transaction(amount = 5000L, type = TransactionType.EXPENSE, timestamp = 2000L))
        createTransactionUseCase(Transaction(amount = 4000L, type = TransactionType.EXPENSE, timestamp = 3000L))

        // Balance after t=3000 is 10000 - 5000 - 4000 = 1000.
        // Trying to increase expense at t=2000 from 5000 to 7000 (causes overdraft of -1000 at t=3000)
        val updatedExpense = Transaction(id = expId, amount = 7000L, type = TransactionType.EXPENSE, timestamp = 2000L)
        assertThrows(InsufficientBalanceException::class.java) {
            kotlinx.coroutines.runBlocking { updateTransactionUseCase(updatedExpense) }
        }

        // Updating expense to 5500 leaves balance of 500 >= 0, should succeed
        val validUpdate = Transaction(id = expId, amount = 5500L, type = TransactionType.EXPENSE, timestamp = 2000L)
        updateTransactionUseCase(validUpdate)

        val retrieved = transactionRepository.getTransactionById(expId)
        assertNotNull(retrieved)
        assertEquals(5500L, retrieved?.amount)
    }

    @Test
    fun `updateTransaction throws EntityNotFoundException for unknown ID`() = runTest {
        val nonExistent = Transaction(id = 999L, amount = 1000L, type = TransactionType.INCOME, timestamp = 1000L)
        assertThrows(EntityNotFoundException::class.java) {
            kotlinx.coroutines.runBlocking { updateTransactionUseCase(nonExistent) }
        }
    }

    @Test
    fun `deleteTransaction prevents overdraft from deleting income`() = runTest {
        val incomeId = createTransactionUseCase(Transaction(amount = 10000L, type = TransactionType.INCOME, timestamp = 1000L))
        createTransactionUseCase(Transaction(amount = 8000L, type = TransactionType.EXPENSE, timestamp = 2000L))

        // Deleting the income of 10000 would cause subsequent balance to be -8000
        assertThrows(InsufficientBalanceException::class.java) {
            kotlinx.coroutines.runBlocking { deleteTransactionUseCase(incomeId) }
        }

        // Transaction is not deleted
        assertNotNull(transactionRepository.getTransactionById(incomeId))
    }

    @Test
    fun `deleteTransaction successfully deletes expense`() = runTest {
        createTransactionUseCase(Transaction(amount = 10000L, type = TransactionType.INCOME, timestamp = 1000L))
        val expId = createTransactionUseCase(Transaction(amount = 3000L, type = TransactionType.EXPENSE, timestamp = 2000L))

        deleteTransactionUseCase(expId)
        assertEquals(null, transactionRepository.getTransactionById(expId))
        assertEquals(1, transactionRepository.getAllTransactions().size)
    }

    @Test
    fun `getTransactionsUseCase filters by date range, type, and category`() = runTest {
        createTransactionUseCase(Transaction(amount = 10000L, type = TransactionType.INCOME, categoryId = 1L, timestamp = 100L))
        createTransactionUseCase(Transaction(amount = 2000L, type = TransactionType.EXPENSE, categoryId = 2L, timestamp = 200L))
        createTransactionUseCase(Transaction(amount = 3000L, type = TransactionType.EXPENSE, categoryId = 3L, timestamp = 300L))
        createTransactionUseCase(Transaction(amount = 5000L, type = TransactionType.INCOME, categoryId = 1L, timestamp = 400L))

        // Filter by date range [150..350]
        val dateFiltered = getTransactionsUseCase(startDate = 150L, endDate = 350L).first()
        assertEquals(2, dateFiltered.size)

        // Filter by type EXPENSE
        val typeFiltered = getTransactionsUseCase(type = TransactionType.EXPENSE).first()
        assertEquals(2, typeFiltered.size)

        // Filter by categoryId = 1
        val categoryFiltered = getTransactionsUseCase(categoryId = 1L).first()
        assertEquals(2, categoryFiltered.size)

        // Synchronous filtering
        val syncFiltered = getTransactionsUseCase.executeSync(categoryId = 2L)
        assertEquals(1, syncFiltered.size)
        assertEquals(2000L, syncFiltered.first().amount)
    }
}
