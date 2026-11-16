package com.moneysaver.habittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.moneysaver.habittracker.ui.screens.NavigationHost
import com.moneysaver.habittracker.ui.theme.MoneyHabitTheme
import com.moneysaver.habittracker.ui.viewmodel.DashboardViewModel
import com.moneysaver.habittracker.ui.viewmodel.HabitViewModel
import com.moneysaver.habittracker.ui.viewmodel.SavingsGoalViewModel
import com.moneysaver.habittracker.ui.viewmodel.TransactionViewModel
import com.moneysaver.habittracker.ui.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {

    private val factory by lazy {
        ViewModelFactory(application as MoneyHabitApplication)
    }

    private val dashboardViewModel: DashboardViewModel by viewModels { factory }
    private val transactionViewModel: TransactionViewModel by viewModels { factory }
    private val savingsGoalViewModel: SavingsGoalViewModel by viewModels { factory }
    private val habitViewModel: HabitViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MoneyHabitTheme {
                NavigationHost(
                    dashboardViewModel = dashboardViewModel,
                    transactionViewModel = transactionViewModel,
                    savingsGoalViewModel = savingsGoalViewModel,
                    habitViewModel = habitViewModel
                )
            }
        }
    }
}
