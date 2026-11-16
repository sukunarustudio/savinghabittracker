package com.moneysaver.habittracker

import android.app.Application
import com.moneysaver.habittracker.data.alarm.HabitAlarmDependencyProvider
import com.moneysaver.habittracker.data.local.MoneyHabitDatabase
import com.moneysaver.habittracker.data.repository.RoomCategoryRepository
import com.moneysaver.habittracker.data.repository.RoomHabitRepository
import com.moneysaver.habittracker.data.repository.RoomSavingsGoalRepository
import com.moneysaver.habittracker.data.repository.RoomTransactionRepository
import com.moneysaver.habittracker.data.repository.UserPreferencesRepository
import com.moneysaver.habittracker.domain.engine.FinancialLedgerEngine
import com.moneysaver.habittracker.domain.engine.habit.HabitStreakCalculator
import com.moneysaver.habittracker.domain.repository.CategoryRepository
import com.moneysaver.habittracker.domain.repository.HabitRepository
import com.moneysaver.habittracker.domain.repository.SavingsGoalRepository
import com.moneysaver.habittracker.domain.repository.TransactionRepository

class MoneyHabitApplication : Application() {

    val database: MoneyHabitDatabase by lazy {
        MoneyHabitDatabase.getDatabase(this)
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(this)
    }

    val categoryRepository: CategoryRepository by lazy {
        RoomCategoryRepository(database.categoryDao())
    }

    val transactionRepository: TransactionRepository by lazy {
        RoomTransactionRepository(database.transactionDao())
    }

    val savingsGoalRepository: SavingsGoalRepository by lazy {
        RoomSavingsGoalRepository(database.savingsGoalDao())
    }

    val habitRepository: HabitRepository by lazy {
        RoomHabitRepository(database.habitDao(), database.habitCompletionLogDao())
    }

    val financialLedgerEngine: FinancialLedgerEngine by lazy {
        FinancialLedgerEngine()
    }

    val habitStreakCalculator: HabitStreakCalculator by lazy {
        HabitStreakCalculator()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        HabitAlarmDependencyProvider.repositoryProvider = { habitRepository }
    }

    companion object {
        lateinit var instance: MoneyHabitApplication
            private set
    }
}
