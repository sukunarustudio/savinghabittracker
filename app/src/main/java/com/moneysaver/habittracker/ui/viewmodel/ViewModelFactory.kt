package com.moneysaver.habittracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.moneysaver.habittracker.MoneyHabitApplication

class ViewModelFactory(
    private val app: MoneyHabitApplication = MoneyHabitApplication.instance
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(
                    transactionRepository = app.transactionRepository,
                    savingsGoalRepository = app.savingsGoalRepository,
                    habitRepository = app.habitRepository,
                    userPreferencesRepository = app.userPreferencesRepository,
                    ledgerEngine = app.financialLedgerEngine
                ) as T
            }
            modelClass.isAssignableFrom(TransactionViewModel::class.java) -> {
                TransactionViewModel(
                    transactionRepository = app.transactionRepository,
                    categoryRepository = app.categoryRepository,
                    savingsGoalRepository = app.savingsGoalRepository,
                    userPreferencesRepository = app.userPreferencesRepository,
                    ledgerEngine = app.financialLedgerEngine
                ) as T
            }
            modelClass.isAssignableFrom(SavingsGoalViewModel::class.java) -> {
                SavingsGoalViewModel(
                    savingsGoalRepository = app.savingsGoalRepository,
                    transactionRepository = app.transactionRepository,
                    categoryRepository = app.categoryRepository,
                    userPreferencesRepository = app.userPreferencesRepository,
                    ledgerEngine = app.financialLedgerEngine
                ) as T
            }
            modelClass.isAssignableFrom(HabitViewModel::class.java) -> {
                HabitViewModel(
                    habitRepository = app.habitRepository,
                    streakCalculator = app.habitStreakCalculator
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
