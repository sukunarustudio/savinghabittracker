package com.moneysaver.habittracker.ui.viewmodel

import com.moneysaver.habittracker.data.repository.UserPreferences
import com.moneysaver.habittracker.domain.engine.FinancialLedgerEngine
import com.moneysaver.habittracker.domain.fake.FakeHabitRepository
import com.moneysaver.habittracker.domain.fake.FakeSavingsGoalRepository
import com.moneysaver.habittracker.domain.fake.FakeTransactionRepository
import com.moneysaver.habittracker.domain.fake.FakeUserPreferencesRepository
import com.moneysaver.habittracker.domain.model.GoalStatus
import com.moneysaver.habittracker.domain.model.PeriodFilter
import com.moneysaver.habittracker.domain.model.SavingsGoal
import com.moneysaver.habittracker.domain.model.Transaction
import com.moneysaver.habittracker.domain.model.TransactionType
import com.moneysaver.habittracker.domain.model.habit.FrequencyType
import com.moneysaver.habittracker.domain.model.habit.Habit
import com.moneysaver.habittracker.domain.model.habit.HabitCompletionLog
import com.moneysaver.habittracker.domain.model.habit.HabitSchedule
import com.moneysaver.habittracker.domain.usecase.habit.GetDailyHabitsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var savingsGoalRepository: FakeSavingsGoalRepository
    private lateinit var habitRepository: FakeHabitRepository
    private lateinit var userPreferencesRepository: FakeUserPreferencesRepository
    private lateinit var ledgerEngine: FinancialLedgerEngine
    private lateinit var getDailyHabitsUseCase: GetDailyHabitsUseCase
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        transactionRepository = FakeTransactionRepository()
        savingsGoalRepository = FakeSavingsGoalRepository()
        habitRepository = FakeHabitRepository()
        userPreferencesRepository = FakeUserPreferencesRepository(
            UserPreferences(currencySymbol = "$", periodFilter = PeriodFilter.WEEKLY)
        )
        ledgerEngine = FinancialLedgerEngine()
        getDailyHabitsUseCase = GetDailyHabitsUseCase(habitRepository)

        viewModel = DashboardViewModel(
            transactionRepository = transactionRepository,
            savingsGoalRepository = savingsGoalRepository,
            habitRepository = habitRepository,
            userPreferencesRepository = userPreferencesRepository,
            ledgerEngine = ledgerEngine,
            getDailyHabitsUseCase = getDailyHabitsUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state handles zero transactions and zero habits safely`() = runTest {
        val state = viewModel.uiState.value
        assertEquals(0L, state.availableBalance)
        assertEquals(0L, state.periodIncome)
        assertEquals(0L, state.periodExpense)
        assertEquals(0L, state.netCashFlow)
        assertEquals(0, state.todayHabitCount)
        assertEquals(0, state.completedHabitCount)
        assertEquals(0f, state.habitCompletionRatio, 0.001f)
        assertTrue(state.recentTransactions.isEmpty())
        assertTrue(state.activeSavingsGoals.isEmpty())
    }

    @Test
    fun `available balance reflects total income minus expenses minus active allocated savings`() = runTest {
        val now = System.currentTimeMillis()
        transactionRepository.insertTransaction(
            Transaction(amount = 100000L, type = TransactionType.INCOME, timestamp = now - 1000)
        )
        transactionRepository.insertTransaction(
            Transaction(amount = 25000L, type = TransactionType.EXPENSE, timestamp = now - 500)
        )
        savingsGoalRepository.insertGoal(
            SavingsGoal(title = "Vacation", targetAmount = 50000L, savedAmount = 15000L, status = GoalStatus.IN_PROGRESS)
        )

        val state = viewModel.uiState.first { it.availableBalance > 0L }
        // 100000 - 25000 - 15000 = 60000L
        assertEquals(60000L, state.availableBalance)
    }

    @Test
    fun `habit completion ratio accurately handles partial completion and zero division`() = runTest {
        val today = LocalDate.now()

        // 2 habits due today
        habitRepository.insertHabit(
            Habit(id = "h1", name = "Habit 1", schedule = HabitSchedule(FrequencyType.DAILY), createdAt = today.minusDays(5))
        )
        habitRepository.insertHabit(
            Habit(id = "h2", name = "Habit 2", schedule = HabitSchedule(FrequencyType.DAILY), createdAt = today.minusDays(5))
        )

        // Complete 1 habit today
        habitRepository.insertCompletionLog(
            HabitCompletionLog(habitId = "h1", completedDate = today)
        )

        val state = viewModel.uiState.first { it.todayHabitCount == 2 }
        assertEquals(2, state.todayHabitCount)
        assertEquals(1, state.completedHabitCount)
        assertEquals(0.5f, state.habitCompletionRatio, 0.001f)

        // Complete second habit
        habitRepository.insertCompletionLog(
            HabitCompletionLog(habitId = "h2", completedDate = today)
        )

        val updatedState = viewModel.uiState.first { it.completedHabitCount == 2 }
        assertEquals(2, updatedState.completedHabitCount)
        assertEquals(1.0f, updatedState.habitCompletionRatio, 0.001f)
    }

    @Test
    fun `calculatePeriodBounds adheres strictly to ISO-8601 weekly bounds`() {
        val referenceDate = LocalDate.of(2026, 11, 18) // Wednesday
        val (startMillis, endMillis, text) = DashboardViewModel.calculatePeriodBounds(
            PeriodFilter.WEEKLY,
            referenceDate
        )

        val zone = ZoneId.systemDefault()
        val expectedMonday = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val expectedSunday = referenceDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val expectedStart = expectedMonday.atStartOfDay(zone).toInstant().toEpochMilli()

        assertEquals(expectedStart, startMillis)
        assertTrue(endMillis > startMillis)
        assertTrue(text.contains("Nov 16") && text.contains("Nov 22"))
    }

    @Test
    fun `setPeriodFilter updates filter in state and persistence`() = runTest {
        viewModel.setPeriodFilter(PeriodFilter.MONTHLY)
        val state = viewModel.uiState.first { it.periodFilter == PeriodFilter.MONTHLY }
        assertEquals(PeriodFilter.MONTHLY, state.periodFilter)
        assertEquals(PeriodFilter.MONTHLY, userPreferencesRepository.userPreferences.value.periodFilter)
    }
}
