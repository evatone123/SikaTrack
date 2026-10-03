package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.AddEditTransactionSheet
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SavingsGoalsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.viewmodel.MainViewModel

/**
 * Jetpack Navigation NavHost routing between the 5 core screens and sub-screens.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        // 1. Dashboard Screen (Home)
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToAddTransaction = { type ->
                    navController.navigate(Screen.AddTransaction.createRoute(type = type))
                },
                onNavigateToTransactions = {
                    navController.navigate(Screen.Transactions.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToAccounts = {
                    navController.navigate(Screen.Accounts.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToReports = {
                    navController.navigate(Screen.Reports.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onTransactionClick = { txId ->
                    navController.navigate(Screen.AddTransaction.createRoute(id = txId))
                }
            )
        }

        // 2. Transactions Screen
        composable(Screen.Transactions.route) {
            TransactionsScreen(
                viewModel = viewModel,
                onNavigateToAddTransaction = {
                    navController.navigate(Screen.AddTransaction.createRoute())
                },
                onTransactionClick = { txId ->
                    navController.navigate(Screen.AddTransaction.createRoute(id = txId))
                }
            )
        }

        // 3. Budgets Screen
        composable(Screen.Budgets.route) {
            BudgetsScreen(
                viewModel = viewModel
            )
        }

        // 4. Goals Screen (Savings Goals)
        composable(Screen.Goals.route) {
            SavingsGoalsScreen(
                viewModel = viewModel
            )
        }

        // 5. Accounts Screen (Accounts & Wallets)
        composable(Screen.Accounts.route) {
            AccountsScreen(
                viewModel = viewModel,
                onTransactionClick = { txId ->
                    navController.navigate(Screen.AddTransaction.createRoute(id = txId))
                }
            )
        }

        // Secondary / Sub-Screens
        composable(Screen.Reports.route) {
            ReportsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AddTransaction.route,
            arguments = listOf(
                navArgument("type") {
                    type = NavType.StringType
                    defaultValue = "EXPENSE"
                },
                navArgument("id") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "EXPENSE"
            val id = backStackEntry.arguments?.getLong("id") ?: -1L
            AddEditTransactionSheet(
                viewModel = viewModel,
                initialType = type,
                transactionId = id,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
