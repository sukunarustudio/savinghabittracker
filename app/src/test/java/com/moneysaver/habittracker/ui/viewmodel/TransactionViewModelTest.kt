package com.moneysaver.habittracker.ui.viewmodel

import com.moneysaver.habittracker.data.repository.UserPreferences
import com.moneysaver.habittracker.domain.engine.FinancialLedgerEngine
import com.moneysaver.habittracker.domain.fake.FakeCategoryRepository
import com.moneysaver.habittracker.domain.fake.FakeSavingsGoalRepository
import com.moneysaver.habittracker.domain.fake.FakeTransactionRepository
import com.moneysaver.habittracker.domain.fake.FakeUserPreferencesRepository
import com.moneysaver.habittracker.domain.model.Category
import com.moneysaver.habittracker.domain.model.CategoryType
import com.moneysaver.habittracker.domain.model.Transaction
import com.moneysaver.habittracker.domain.model.TransactionType
import com.moneysaver.habittracker.domain.usecase.category.CreateCategoryUseCase
import com.moneysaver.habittracker.domain.usecase.category.DeleteCategoryWithReassignmentUseCase
import com.moneysaver.habittracker.domain.usecase.transaction.CreateTransactionUseCase
import com.moneysaver.habittracker.domain.usecase.transaction.DeleteTransactionUseCase
import com.moneysaver.habittracker.domain.usecase.transaction.UpdateTransactionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var savingsGoalRepository: FakeSavingsGoalRepository
    private lateinit var userPreferencesRepository: FakeUserPreferencesRepository
    private lateinit var ledgerEngine: FinancialLedgerEngine
    private lateinit var viewModel: TransactionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        transactionRepository = FakeTransactionRepository()
        categoryRepository = FakeCategoryRepository()
        savingsGoalRepository = FakeSavingsGoalRepository()
        userPreferencesRepository = FakeUserPreferencesRepository(UserPreferences(currencySymbol = "$"))
        ledgerEngine = FinancialLedgerEngine()

        viewModel = TransactionViewModel(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            savingsGoalRepository = savingsGoalRepository,
            userPreferencesRepository = userPreferencesRepository,
            ledgerEngine = ledgerEngine
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `save income transaction increases available balance`() = runTest {
        viewModel.saveTransaction(
            amount = 50000L,
            type = TransactionType.INCOME,
            categoryId = null,
            note = "Salary"
        )

        val state = viewModel.uiState.first { it.availableBalance == 50000L }
        assertEquals(50000L, state.availableBalance)
        assertEquals(1, state.transactions.size)
        assertEquals("Salary", state.transactions.first().note)
    }

    @Test
    fun `save expense exceeding available balance triggers overdraft warning dialog`() = runTest {
        // Add $20 income
        viewModel.saveTransaction(amount = 2000L, type = TransactionType.INCOME, categoryId = null, note = "Gift")
        val stateAfterIncome = viewModel.uiState.first { it.availableBalance == 2000L }
        assertEquals(2000L, stateAfterIncome.availableBalance)

        // Attempt to spend $50
        viewModel.saveTransaction(amount = 5000L, type = TransactionType.EXPENSE, categoryId = null, note = "Dinner")

        val state = viewModel.uiState.value
        assertTrue("Overdraft warning dialog should be visible", state.showOverdraftWarning)
        assertTrue(state.overdraftWarningMessage.contains("exceeds your available balance"))

        // Verify transaction was NOT added
        assertEquals(1, transactionRepository.getAllTransactions().size)

        // Dismiss warning
        viewModel.dismissOverdraftWarning()
        assertFalse(viewModel.uiState.value.showOverdraftWarning)
    }

    @Test
    fun `filter transactions by type`() = runTest {
        val now = System.currentTimeMillis()
        transactionRepository.insertTransaction(Transaction(amount = 10000L, type = TransactionType.INCOME, timestamp = now))
        transactionRepository.insertTransaction(Transaction(amount = 3000L, type = TransactionType.EXPENSE, timestamp = now + 10))

        viewModel.setFilter(TransactionType.EXPENSE)
        val state = viewModel.uiState.first { it.filterType == TransactionType.EXPENSE }
        assertEquals(1, state.transactions.size)
        assertEquals(TransactionType.EXPENSE, state.transactions.first().type)

        viewModel.setFilter(null)
        val allState = viewModel.uiState.first { it.filterType == null }
        assertEquals(2, allState.transactions.size)
    }

    @Test
    fun `create and manage categories`() = runTest {
        viewModel.createCategory(
            name = "Coffee",
            type = CategoryType.EXPENSE,
            colorHex = "#795548"
        )

        val state = viewModel.uiState.first { it.categories.any { cat -> cat.name == "Coffee" } }
        assertTrue(state.categories.any { it.name == "Coffee" })
    }
}
