package com.example.domain

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.TransactionType
import java.util.Calendar

data class MonthlyFinancialSummary(
    val totalBalance: Double,
    val incomeThisMonth: Double,
    val expensesThisMonth: Double,
    val savedThisMonth: Double,
    val savingsRate: Double // e.g. 0.35 = 35%
)

data class CategorySpending(
    val categoryId: Long,
    val categoryName: String,
    val colorHex: String,
    val iconName: String,
    val totalSpent: Double,
    val percentageOfTotal: Float
)

enum class BudgetStatus {
    WITHIN_BUDGET,
    NEAR_BUDGET,
    OVER_BUDGET
}

data class BudgetProgress(
    val budget: BudgetEntity,
    val category: CategoryEntity?,
    val spentAmount: Double,
    val remainingAmount: Double,
    val progressFraction: Float, // 0.0 to 1.0+
    val status: BudgetStatus
)

data class MonthTrendData(
    val monthLabel: String,
    val monthIndex: Int,
    val year: Int,
    val income: Double,
    val expense: Double
)

object FinancialCalculations {

    fun calculateTotalBalance(accounts: List<AccountEntity>): Double {
        return accounts
            .filter { !it.isArchived && it.isActive }
            .sumOf { it.currentBalance }
    }

    fun calculateMonthlySummary(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        month: Int, // 1 to 12
        year: Int
    ): MonthlyFinancialSummary {
        val totalBal = calculateTotalBalance(accounts)

        val (startOfMonth, endOfMonth) = getMonthRange(month, year)

        val monthTransactions = transactions.filter { it.date in startOfMonth..endOfMonth }

        var income = 0.0
        var expenses = 0.0

        for (tx in monthTransactions) {
            when (tx.type) {
                TransactionType.INCOME -> income += tx.amount
                TransactionType.EXPENSE -> expenses += tx.amount
                TransactionType.TRANSFER -> { /* Transfers don't affect net income/expense */ }
            }
        }

        val saved = income - expenses
        val savingsRate = if (income > 0) (saved / income).coerceIn(-1.0, 1.0) else 0.0

        return MonthlyFinancialSummary(
            totalBalance = totalBal,
            incomeThisMonth = income,
            expensesThisMonth = expenses,
            savedThisMonth = saved,
            savingsRate = savingsRate
        )
    }

    fun calculateCategorySpending(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        startDate: Long,
        endDate: Long
    ): List<CategorySpending> {
        val categoryMap = categories.associateBy { it.id }

        val expenseTx = transactions.filter {
            it.type == TransactionType.EXPENSE && it.date in startDate..endDate
        }

        val totalExpense = expenseTx.sumOf { it.amount }
        if (totalExpense <= 0.0) return emptyList()

        val grouped = expenseTx.groupBy { it.categoryId ?: -1L }

        return grouped.map { (catId, txList) ->
            val cat = categoryMap[catId]
            val catTotal = txList.sumOf { it.amount }
            val fraction = (catTotal / totalExpense).toFloat()

            CategorySpending(
                categoryId = catId,
                categoryName = cat?.name ?: "Uncategorized",
                colorHex = cat?.colorHex ?: "#78909C",
                iconName = cat?.iconName ?: "category",
                totalSpent = catTotal,
                percentageOfTotal = fraction
            )
        }.sortedByDescending { it.totalSpent }
    }

    fun calculateBudgetProgress(
        budgets: List<BudgetEntity>,
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        month: Int,
        year: Int
    ): List<BudgetProgress> {
        val (start, end) = getMonthRange(month, year)
        val monthExpenseTx = transactions.filter {
            it.type == TransactionType.EXPENSE && it.date in start..end
        }

        val categoryMap = categories.associateBy { it.id }

        return budgets.filter { it.month == month && it.year == year }.map { budget ->
            val spent = monthExpenseTx
                .filter { it.categoryId == budget.categoryId }
                .sumOf { it.amount }

            val remaining = budget.amount - spent
            val fraction = if (budget.amount > 0) (spent / budget.amount).toFloat() else 0f

            val status = when {
                fraction > 1.0f -> BudgetStatus.OVER_BUDGET
                fraction >= 0.80f -> BudgetStatus.NEAR_BUDGET
                else -> BudgetStatus.WITHIN_BUDGET
            }

            BudgetProgress(
                budget = budget,
                category = categoryMap[budget.categoryId],
                spentAmount = spent,
                remainingAmount = remaining,
                progressFraction = fraction,
                status = status
            )
        }.sortedByDescending { it.progressFraction }
    }

    fun calculateLast6MonthsTrends(
        transactions: List<TransactionEntity>
    ): List<MonthTrendData> {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1

        val list = mutableListOf<MonthTrendData>()

        for (i in 5 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -i)
            val m = cal.get(Calendar.MONTH) + 1
            val y = cal.get(Calendar.YEAR)
            val (start, end) = getMonthRange(m, y)

            val monthTxs = transactions.filter { it.date in start..end }
            val inc = monthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val exp = monthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

            val monthName = getShortMonthName(m)
            list.add(
                MonthTrendData(
                    monthLabel = monthName,
                    monthIndex = m,
                    year = y,
                    income = inc,
                    expense = exp
                )
            )
        }

        return list
    }

    fun getMonthRange(month: Int, year: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return Pair(start, end)
    }

    fun getDayRange(timestamp: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return Pair(start, end)
    }

    fun getWeekRange(timestamp: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.add(Calendar.DAY_OF_WEEK, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return Pair(start, end)
    }

    fun getShortMonthName(month: Int): String {
        return when (month) {
            1 -> "Jan"
            2 -> "Feb"
            3 -> "Mar"
            4 -> "Apr"
            5 -> "May"
            6 -> "Jun"
            7 -> "Jul"
            8 -> "Aug"
            9 -> "Sep"
            10 -> "Oct"
            11 -> "Nov"
            12 -> "Dec"
            else -> ""
        }
    }
}
