package com.example.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Dashboard : Screen("dashboard", "Dashboard")
    object Transactions : Screen("transactions", "Transactions")
    object Budgets : Screen("budgets", "Budgets")
    object Goals : Screen("goals", "Savings Goals")
    object Accounts : Screen("accounts", "Accounts")
    object Reports : Screen("reports", "Reports & Analytics")
    object Settings : Screen("settings", "Settings")
    object AddTransaction : Screen("add_transaction?type={type}&id={id}", "Transaction") {
        fun createRoute(type: String = "EXPENSE", id: Long = -1L): String {
            return "add_transaction?type=$type&id=$id"
        }
    }
    object AccountDetail : Screen("account_detail/{accountId}", "Account Details") {
        fun createRoute(accountId: Long): String {
            return "account_detail/$accountId"
        }
    }
}
