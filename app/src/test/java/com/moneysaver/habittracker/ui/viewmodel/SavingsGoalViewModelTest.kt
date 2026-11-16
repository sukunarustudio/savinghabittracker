package com.moneysaver.habittracker.ui.viewmodel

import com.moneysaver.habittracker.data.repository.UserPreferences
import com.moneysaver.habittracker.domain.engine.FinancialLedgerEngine
import com.moneysaver.habittracker.domain.fake.FakeCategoryRepository
import com.moneysaver.habittracker.domain.fake.FakeSavingsGoalRepository
import com.moneysaver.habittracker.domain.fake.FakeTransactionRepository
import com.moneysaver.habittracker.domain.fake.FakeUserPreferencesRepository
import com.moneysaver.habittracker.domain.model.Category
import com.moneysaver.habittracker.domain.model.CategoryType
import com.moneysaver.habittracker.domain.model.GoalStatus
import com.moneysaver.habittracker.domain.model.SavingsGoal
import com.moneysaver.habittracker.domain.model.Transaction
import com.moneysaver.habittracker.domain.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SavingsGoalViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var savingsGoalRepository: FakeSavingsGoalRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var userPreferencesRepository: FakeUserPreferencesRepository
    private lateinit var ledgerEngine: FinancialLedgerEngine
    private lateinit var viewModel: SavingsGoalViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        savingsGoalRepository = FakeSavingsGoalRepository()
        transactionRepository = FakeTransactionRepository()
        categoryRepository = FakeCategoryRepository()
        userPreferencesRepository = FakeUserPreferencesRepository(UserPreferences(currencySymbol = "$"))
        ledgerEngine = FinancialLedgerEngine()

        viewModel = SavingsGoalViewModel(
            savingsGoalRepository = savingsGoalRepository,
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            userPreferencesRepository = userPreferencesRepository,
            ledgerEngine = ledgerEngine
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `save goal creates new goal and appears in list`() = runTest {
        viewModel.saveGoal(title = "Emergency Fund", targetAmount = 500000L, targetDate = null)

        val state = viewModel.uiState.first { it.goals.isNotEmpty() }
        assertEquals(1, state.goals.size)
        assertEquals("Emergency Fund", state.goals.first().title)
        assertEquals(500000L, state.goals.first().targetAmount)
        assertEquals(0L, state.goals.first().savedAmount)
    }

    @Test
    fun `allocate funds updates saved amount and achieves goal when target reached`() = runTest {
        // Provide $1000 income to available balance
        transactionRepository.insertTransaction(
            Transaction(amount = 100000L, type = TransactionType.INCOME, timestamp = System.currentTimeMillis())
        )

        val goalId = savingsGoalRepository.insertGoal(
            SavingsGoal(title = "Laptop", targetAmount = 50000L, savedAmount = 0L)
        )
        val goal = savingsGoalRepository.getGoalById(goalId)!!

        viewModel.openAllocateDialog(goal)
        viewModel.allocate(50000L, "Full purchase allocation")

        val updatedGoal = savingsGoalRepository.getGoalById(goalId)!!
        assertEquals(50000L, updatedGoal.savedAmount)
        assertEquals(GoalStatus.ACHIEVED, updatedGoal.status)
        assertTrue(updatedGoal.isAchieved)
    }

    @Test
    fun `delete goal with RefundToBalance returns saved amount to available balance`() = runTest {
        // Setup initial state: $1000 income, $300 allocated to a goal
        val now = System.currentTimeMillis()
        transactionRepository.insertTransaction(
            Transaction(amount = 100000L, type = TransactionType.INCOME, timestamp = now - 2000)
        )
        val goalId = savingsGoalRepository.insertGoal(
            SavingsGoal(title = "Flight", targetAmount = 60000L, savedAmount = 30000L)
        )
        transactionRepository.insertTransaction(
            Transaction(amount = 30000L, type = TransactionType.SAVINGS_DEPOSIT, savingsGoalId = goalId, isInternalTransfer = true, timestamp = now - 1000)
        )

        val goal = savingsGoalRepository.getGoalById(goalId)!!
        viewModel.openDeleteDialog(goal)
        viewModel.setDeletionChoice(GoalDeletionChoice.REFUND_TO_BALANCE)
        viewModel.confirmDeleteGoal()

        // Goal should be deleted
        assertNull(savingsGoalRepository.getGoalById(goalId))

        // SAVINGS_WITHDRAWAL refund transaction should have been recorded
        val txs = transactionRepository.getAllTransactions()
        val refundTx = txs.find { it.type == TransactionType.SAVINGS_WITHDRAWAL }
        assertNotNull(refundTx)
        assertEquals(30000L, refundTx?.amount)
    }

    @Test
    fun `delete goal with RealizeAsExpense records saved funds as an expense transaction`() = runTest {
        val now = System.currentTimeMillis()
        val catId = categoryRepository.insertCategory(
            Category(name = "Travel", type = CategoryType.EXPENSE)
        )
        transactionRepository.insertTransaction(
            Transaction(amount = 100000L, type = TransactionType.INCOME, timestamp = now - 2000)
        )
        val goalId = savingsGoalRepository.insertGoal(
            SavingsGoal(title = "Concert", targetAmount = 20000L, savedAmount = 20000L)
        )
        transactionRepository.insertTransaction(
            Transaction(amount = 20000L, type = TransactionType.SAVINGS_DEPOSIT, savingsGoalId = goalId, isInternalTransfer = true, timestamp = now - 1000)
        )

        val goal = savingsGoalRepository.getGoalById(goalId)!!
        viewModel.openDeleteDialog(goal)
        viewModel.setDeletionChoice(GoalDeletionChoice.REALIZE_AS_EXPENSE)
        viewModel.setSelectedRealizeCategory(catId)
        viewModel.confirmDeleteGoal("Bought concert tickets")

        // Goal deleted
        assertNull(savingsGoalRepository.getGoalById(goalId))

        // Expense transaction created
        val txs = transactionRepository.getAllTransactions()
        val expenseTx = txs.find { it.type == TransactionType.EXPENSE && it.categoryId == catId }
        assertNotNull(expenseTx)
        assertEquals(20000L, expenseTx?.amount)
        assertEquals("Bought concert tickets", expenseTx?.note)
    }
}
