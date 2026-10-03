package com.example

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.CategoryType
import com.example.data.model.TransactionType
import com.example.domain.BudgetStatus
import com.example.domain.CurrencyFormatter
import com.example.domain.FinancialCalculations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {

    @Test
    fun testCurrencyFormatting() {
        val formattedCedi = CurrencyFormatter.format(1250.50, "GH₵")
        assertEquals("GH₵ 1,250.50", formattedCedi)

        val formattedNegative = CurrencyFormatter.format(-450.00, "GH₵")
        assertEquals("-GH₵ 450.00", formattedNegative)

        val masked = CurrencyFormatter.format(5000.0, "GH₵", hideBalances = true)
        assertEquals("••••••", masked)
    }

    @Test
    fun testTotalBalanceCalculation() {
        val accounts = listOf(
            AccountEntity(id = 1, name = "Cash", accountType = AccountType.CASH, currentBalance = 500.0),
            AccountEntity(id = 2, name = "MoMo", accountType = AccountType.MOBILE_MONEY, currentBalance = 1200.0),
            AccountEntity(id = 3, name = "Bank", accountType = AccountType.BANK, currentBalance = 3500.0),
            AccountEntity(id = 4, name = "Old Account", accountType = AccountType.OTHER, currentBalance = 1000.0, isArchived = true)
        )

        val total = FinancialCalculations.calculateTotalBalance(accounts)
        assertEquals(5200.0, total, 0.001) // Archived account (1000) is excluded
    }

    @Test
    fun testMonthlyFinancialSummary() {
        val accounts = listOf(
            AccountEntity(id = 1, name = "Bank", accountType = AccountType.BANK, currentBalance = 4000.0)
        )

        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentYear = cal.get(Calendar.YEAR)

        val transactions = listOf(
            TransactionEntity(
                id = 1,
                type = TransactionType.INCOME,
                amount = 5000.0,
                accountId = 1,
                date = cal.timeInMillis
            ),
            TransactionEntity(
                id = 2,
                type = TransactionType.EXPENSE,
                amount = 1500.0,
                accountId = 1,
                date = cal.timeInMillis
            ),
            TransactionEntity(
                id = 3,
                type = TransactionType.EXPENSE,
                amount = 500.0,
                accountId = 1,
                date = cal.timeInMillis
            )
        )

        val summary = FinancialCalculations.calculateMonthlySummary(accounts, transactions, currentMonth, currentYear)

        assertEquals(4000.0, summary.totalBalance, 0.001)
        assertEquals(5000.0, summary.incomeThisMonth, 0.001)
        assertEquals(2000.0, summary.expensesThisMonth, 0.001)
        assertEquals(3000.0, summary.savedThisMonth, 0.001)
        assertEquals(0.60, summary.savingsRate, 0.001)
    }

    @Test
    fun testBudgetProgressStatus() {
        val cal = Calendar.getInstance()
        val m = cal.get(Calendar.MONTH) + 1
        val y = cal.get(Calendar.YEAR)

        val budgets = listOf(
            BudgetEntity(id = 1, categoryId = 10, amount = 1000.0, month = m, year = y),
            BudgetEntity(id = 2, categoryId = 20, amount = 500.0, month = m, year = y),
            BudgetEntity(id = 3, categoryId = 30, amount = 200.0, month = m, year = y)
        )

        val categories = listOf(
            CategoryEntity(id = 10, name = "Food", type = CategoryType.EXPENSE),
            CategoryEntity(id = 20, name = "Transport", type = CategoryType.EXPENSE),
            CategoryEntity(id = 30, name = "Utilities", type = CategoryType.EXPENSE)
        )

        val transactions = listOf(
            // Food: spent 500 out of 1000 -> 50% -> WITHIN_BUDGET
            TransactionEntity(type = TransactionType.EXPENSE, amount = 500.0, accountId = 1, categoryId = 10, date = cal.timeInMillis),
            // Transport: spent 450 out of 500 -> 90% -> NEAR_BUDGET
            TransactionEntity(type = TransactionType.EXPENSE, amount = 450.0, accountId = 1, categoryId = 20, date = cal.timeInMillis),
            // Utilities: spent 250 out of 200 -> 125% -> OVER_BUDGET
            TransactionEntity(type = TransactionType.EXPENSE, amount = 250.0, accountId = 1, categoryId = 30, date = cal.timeInMillis)
        )

        val progressList = FinancialCalculations.calculateBudgetProgress(budgets, categories, transactions, m, y)

        val foodProgress = progressList.find { it.budget.categoryId == 10L }!!
        assertEquals(BudgetStatus.WITHIN_BUDGET, foodProgress.status)
        assertEquals(500.0, foodProgress.remainingAmount, 0.001)

        val transportProgress = progressList.find { it.budget.categoryId == 20L }!!
        assertEquals(BudgetStatus.NEAR_BUDGET, transportProgress.status)
        assertEquals(50.0, transportProgress.remainingAmount, 0.001)

        val utilProgress = progressList.find { it.budget.categoryId == 30L }!!
        assertEquals(BudgetStatus.OVER_BUDGET, utilProgress.status)
        assertEquals(-50.0, utilProgress.remainingAmount, 0.001)
    }
}
