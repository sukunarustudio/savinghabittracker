package com.moneysaver.habittracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneysaver.habittracker.data.repository.UserPreferencesRepository
import com.moneysaver.habittracker.domain.engine.FinancialLedgerEngine
import com.moneysaver.habittracker.domain.model.PeriodFilter
import com.moneysaver.habittracker.domain.model.SavingsGoal
import com.moneysaver.habittracker.domain.model.Transaction
import com.moneysaver.habittracker.domain.model.habit.Habit
import com.moneysaver.habittracker.domain.repository.HabitRepository
import com.moneysaver.habittracker.domain.repository.SavingsGoalRepository
import com.moneysaver.habittracker.domain.repository.TransactionRepository
import com.moneysaver.habittracker.domain.usecase.habit.GetDailyHabitsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

data class DashboardUiState(
    val availableBalance: Long = 0L,
    val periodIncome: Long = 0L,
    val periodExpense: Long = 0L,
    val netCashFlow: Long = 0L,
    val periodFilter: PeriodFilter = PeriodFilter.WEEKLY,
    val periodDateRangeText: String = "",
    val todayHabitCount: Int = 0,
    val completedHabitCount: Int = 0,
    val habitCompletionRatio: Float = 0f,
    val recentTransactions: List<Transaction> = emptyList(),
    val activeSavingsGoals: List<SavingsGoal> = emptyList(),
    val currencySymbol: String = "$",
    val isLoading: Boolean = false
)

class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val habitRepository: HabitRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val ledgerEngine: FinancialLedgerEngine = FinancialLedgerEngine(),
    private val getDailyHabitsUseCase: GetDailyHabitsUseCase = GetDailyHabitsUseCase(habitRepository)
) : ViewModel() {

    private val selectedPeriodFilter = MutableStateFlow(PeriodFilter.WEEKLY)

    val uiState: StateFlow<DashboardUiState> = combine(
        transactionRepository.getTransactions(),
        savingsGoalRepository.getGoals(),
        habitRepository.getHabits(),
        habitRepository.getAllCompletionLogs(),
        combine(selectedPeriodFilter, userPreferencesRepository.userPreferencesFlow) { filter, prefs ->
            filter to prefs
        }
    ) { transactions, goals, habits, logs, filterAndPrefs ->
        val (periodFilter, userPrefs) = filterAndPrefs
        // 1. Calculate Lifetime Net Available Balance
        val availableBalance = ledgerEngine.calculateLifetimeAvailableBalance(transactions, goals)

        // 2. Calculate Period Metrics using ISO-8601 weekly bounds
        val referenceDate = LocalDate.now()
        val (startMillis, endMillis, rangeText) = calculatePeriodBounds(periodFilter, referenceDate)
        val periodMetrics = ledgerEngine.calculatePeriodMetrics(transactions, startMillis, endMillis)

        // 3. Calculate Today's Habit Completion Ratio
        val todayHabits = getDailyHabitsUseCase.filterHabitsForDate(habits, referenceDate)
        val todayLogs = logs.filter { it.completedDate == referenceDate }
        val completedTodayIds = todayLogs.map { it.habitId }.toSet()
        val completedCount = todayHabits.count { completedTodayIds.contains(it.id) }
        val totalTodayCount = todayHabits.size

        // Zero-division guard:
        val completionRatio = if (totalTodayCount > 0) {
            (completedCount.toFloat() / totalTodayCount.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        // 4. Recent transactions (sorted by timestamp descending, top 5)
        val recentTx = transactions
            .sortedByDescending { it.timestamp }
            .take(5)

        // 5. Active savings goals (top 3)
        val activeGoals = goals
            .filter { it.status.isActive }
            .take(3)

        DashboardUiState(
            availableBalance = availableBalance,
            periodIncome = periodMetrics.totalIncome,
            periodExpense = periodMetrics.totalExpense,
            netCashFlow = periodMetrics.netCashFlow,
            periodFilter = periodFilter,
            periodDateRangeText = rangeText,
            todayHabitCount = totalTodayCount,
            completedHabitCount = completedCount,
            habitCompletionRatio = completionRatio,
            recentTransactions = recentTx,
            activeSavingsGoals = activeGoals,
            currencySymbol = userPrefs.currencySymbol,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun setPeriodFilter(filter: PeriodFilter) {
        selectedPeriodFilter.value = filter
        viewModelScope.launch {
            userPreferencesRepository.setPeriodFilter(filter)
        }
    }

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d")

        /**
         * Computes start and end epoch milliseconds for period filtering.
         * Explicitly enforces ISO-8601 weekly bounds: Monday 00:00:00 to Sunday 23:59:59.999.
         */
        fun calculatePeriodBounds(
            filter: PeriodFilter,
            referenceDate: LocalDate = LocalDate.now()
        ): Triple<Long, Long, String> {
            val zone = ZoneId.systemDefault()
            return when (filter) {
                PeriodFilter.WEEKLY -> {
                    val monday = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    val sunday = referenceDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                    val start = monday.atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = sunday.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
                    val text = "${monday.format(DATE_FORMATTER)} - ${sunday.format(DATE_FORMATTER)}"
                    Triple(start, end, text)
                }
                PeriodFilter.DAILY -> {
                    val start = referenceDate.atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = referenceDate.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
                    Triple(start, end, referenceDate.format(DATE_FORMATTER))
                }
                PeriodFilter.MONTHLY -> {
                    val firstDay = referenceDate.withDayOfMonth(1)
                    val lastDay = referenceDate.with(TemporalAdjusters.lastDayOfMonth())
                    val start = firstDay.atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = lastDay.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
                    val text = "${firstDay.format(DATE_FORMATTER)} - ${lastDay.format(DATE_FORMATTER)}"
                    Triple(start, end, text)
                }
                PeriodFilter.YEARLY -> {
                    val firstDay = referenceDate.withDayOfYear(1)
                    val lastDay = referenceDate.with(TemporalAdjusters.lastDayOfYear())
                    val start = firstDay.atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = lastDay.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
                    Triple(start, end, referenceDate.year.toString())
                }
                PeriodFilter.ALL, PeriodFilter.CUSTOM -> {
                    Triple(0L, Long.MAX_VALUE, "All Time")
                }
            }
        }
    }
}
