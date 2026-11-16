package com.moneysaver.habittracker.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.moneysaver.habittracker.ui.viewmodel.DashboardViewModel
import com.moneysaver.habittracker.ui.viewmodel.HabitViewModel
import com.moneysaver.habittracker.ui.viewmodel.SavingsGoalViewModel
import com.moneysaver.habittracker.ui.viewmodel.TransactionViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Transactions : Screen("transactions", "Transactions", Icons.Default.ReceiptLong)
    object SavingsGoals : Screen("savings_goals", "Goals", Icons.Default.Savings)
    object Habits : Screen("habits", "Habits", Icons.Default.CheckCircle)
    object HabitDetail : Screen("habit_detail/{habitId}", "Habit Detail", Icons.Default.CheckCircle) {
        fun createRoute(habitId: String) = "habit_detail/$habitId"
    }
}

val BottomNavigationItems = listOf(
    Screen.Dashboard,
    Screen.Transactions,
    Screen.SavingsGoals,
    Screen.Habits
)

@Composable
fun NavigationHost(
    dashboardViewModel: DashboardViewModel,
    transactionViewModel: TransactionViewModel,
    savingsGoalViewModel: SavingsGoalViewModel,
    habitViewModel: HabitViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom bar for top-level screens
    val showBottomBar = BottomNavigationItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomNavigationItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) }
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToTransactions = {
                        navController.navigate(Screen.Transactions.route)
                    },
                    onNavigateToGoals = {
                        navController.navigate(Screen.SavingsGoals.route)
                    },
                    onNavigateToHabits = {
                        navController.navigate(Screen.Habits.route)
                    },
                    onAddTransaction = {
                        navController.navigate(Screen.Transactions.route)
                        transactionViewModel.openAddTransaction()
                    },
                    onAddGoal = {
                        navController.navigate(Screen.SavingsGoals.route)
                        savingsGoalViewModel.openAddGoal()
                    },
                    onAddHabit = {
                        navController.navigate(Screen.Habits.route)
                        habitViewModel.openAddHabit()
                    }
                )
            }

            composable(Screen.Transactions.route) {
                TransactionsScreen(viewModel = transactionViewModel)
            }

            composable(Screen.SavingsGoals.route) {
                SavingsGoalsScreen(viewModel = savingsGoalViewModel)
            }

            composable(Screen.Habits.route) {
                HabitChecklistScreen(
                    viewModel = habitViewModel,
                    onNavigateToDetail = { habitId ->
                        navController.navigate(Screen.HabitDetail.createRoute(habitId))
                    }
                )
            }

            composable(
                route = Screen.HabitDetail.route,
                arguments = listOf(navArgument("habitId") { type = NavType.StringType })
            ) { backStackEntry ->
                val habitId = backStackEntry.arguments?.getString("habitId") ?: ""
                HabitDetailScreen(
                    habitId = habitId,
                    viewModel = habitViewModel,
                    onNavigateBack = {
                        habitViewModel.closeHabitDetail()
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
