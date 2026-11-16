package com.moneysaver.habittracker

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.moneysaver.habittracker.ui.screens.NavigationHost
import com.moneysaver.habittracker.ui.theme.MoneyHabitTheme
import com.moneysaver.habittracker.ui.viewmodel.DashboardViewModel
import com.moneysaver.habittracker.ui.viewmodel.HabitViewModel
import com.moneysaver.habittracker.ui.viewmodel.SavingsGoalViewModel
import com.moneysaver.habittracker.ui.viewmodel.TransactionViewModel
import com.moneysaver.habittracker.ui.viewmodel.ViewModelFactory
import java.util.Locale

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
        val userPrefsRepo = (application as MoneyHabitApplication).userPreferencesRepository

        setContent {
            val userPrefs by userPrefsRepo.userPreferencesFlow.collectAsState(initial = userPrefsRepo.userPreferences.value)

            val locale = if (userPrefs.language.startsWith("en", ignoreCase = true)) {
                Locale.US
            } else {
                Locale("in", "ID")
            }

            val config = Configuration(LocalConfiguration.current).apply {
                setLocale(locale)
                setLayoutDirection(locale)
            }
            val localizedContext = LocalContext.current.createConfigurationContext(config)

            CompositionLocalProvider(
                LocalConfiguration provides config,
                LocalContext provides localizedContext
            ) {
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
}
