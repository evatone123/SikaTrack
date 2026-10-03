package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Defines the core 5 destinations accessible from the BottomNavigationBar.
 */
sealed class BottomNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
) {
    object Dashboard : BottomNavDestination(
        route = Screen.Dashboard.route,
        label = "Dashboard",
        icon = Icons.Default.Dashboard,
        testTag = "nav_item_dashboard"
    )

    object Transactions : BottomNavDestination(
        route = Screen.Transactions.route,
        label = "Transactions",
        icon = Icons.Default.ReceiptLong,
        testTag = "nav_item_transactions"
    )

    object Budgets : BottomNavDestination(
        route = Screen.Budgets.route,
        label = "Budgets",
        icon = Icons.Default.PieChart,
        testTag = "nav_item_budgets"
    )

    object Goals : BottomNavDestination(
        route = Screen.Goals.route,
        label = "Goals",
        icon = Icons.Default.Savings,
        testTag = "nav_item_goals"
    )

    object Accounts : BottomNavDestination(
        route = Screen.Accounts.route,
        label = "Accounts",
        icon = Icons.Default.AccountBalanceWallet,
        testTag = "nav_item_accounts"
    )

    companion object {
        val items = listOf(Dashboard, Transactions, Budgets, Goals, Accounts)
    }
}
