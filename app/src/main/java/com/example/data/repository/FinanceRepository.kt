package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.CategoryType
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.math.RoundingMode

class FinanceRepository(private val database: AppDatabase) {

    private val accountDao = database.accountDao()
    private val categoryDao = database.categoryDao()
    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()
    private val savingsGoalDao = database.savingsGoalDao()

    // Accounts
    val activeAccounts: Flow<List<AccountEntity>> = accountDao.getActiveAccounts()
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    fun getAccountById(id: Long): Flow<AccountEntity?> = accountDao.getAccountById(id)

    suspend fun createAccount(account: AccountEntity): Long {
        return database.withTransaction {
            val roundedOpening = roundMoney(account.openingBalance)
            val toInsert = account.copy(
                openingBalance = roundedOpening,
                currentBalance = roundedOpening
            )
            accountDao.insertAccount(toInsert)
        }
    }

    suspend fun updateAccount(account: AccountEntity) {
        database.withTransaction {
            accountDao.updateAccount(account)
            // Recompute balance based on opening balance and transactions
            recomputeAccountBalance(account.id)
        }
    }

    suspend fun archiveAccount(accountId: Long, archive: Boolean) {
        database.withTransaction {
            val account = accountDao.getAccountByIdOnce(accountId) ?: return@withTransaction
            accountDao.updateAccount(account.copy(isArchived = archive))
        }
    }

    suspend fun deleteAccount(account: AccountEntity) {
        database.withTransaction {
            accountDao.deleteAccount(account)
        }
    }

    // Categories
    val activeCategories: Flow<List<CategoryEntity>> = categoryDao.getAllActiveCategories()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    fun getCategoriesByType(type: CategoryType): Flow<List<CategoryEntity>> =
        categoryDao.getCategoriesByType(type)

    fun getCategoryById(id: Long): Flow<CategoryEntity?> = categoryDao.getCategoryById(id)

    suspend fun createCategory(category: CategoryEntity): Long =
        categoryDao.insertCategory(category)

    suspend fun updateCategory(category: CategoryEntity) =
        categoryDao.updateCategory(category)

    suspend fun softDeleteCategory(category: CategoryEntity) {
        // Soft-delete to preserve transaction history
        categoryDao.updateCategory(category.copy(isActive = false))
    }

    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getRecentTransactions(limit: Int = 10): Flow<List<TransactionEntity>> =
        transactionDao.getRecentTransactions(limit)

    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByAccount(accountId)

    fun getTransactionsBetweenDates(start: Long, end: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsBetweenDates(start, end)

    fun getTransactionById(id: Long): Flow<TransactionEntity?> =
        transactionDao.getTransactionById(id)

    suspend fun createTransaction(transaction: TransactionEntity): Long {
        return database.withTransaction {
            val validAmount = roundMoney(transaction.amount)
            val newTx = transaction.copy(
                amount = validAmount,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val insertedId = transactionDao.insertTransaction(newTx)

            // Apply financial balance change
            applyBalanceChange(
                type = newTx.type,
                amount = newTx.amount,
                sourceAccountId = newTx.accountId,
                destAccountId = newTx.destinationAccountId,
                isReversal = false
            )

            insertedId
        }
    }

    suspend fun updateTransaction(newTx: TransactionEntity) {
        database.withTransaction {
            val oldTx = transactionDao.getTransactionByIdOnce(newTx.id) ?: return@withTransaction
            val validAmount = roundMoney(newTx.amount)
            val updated = newTx.copy(
                amount = validAmount,
                updatedAt = System.currentTimeMillis()
            )

            // First reverse old transaction effects
            applyBalanceChange(
                type = oldTx.type,
                amount = oldTx.amount,
                sourceAccountId = oldTx.accountId,
                destAccountId = oldTx.destinationAccountId,
                isReversal = true
            )

            // Then save updated record
            transactionDao.updateTransaction(updated)

            // Apply new transaction effects
            applyBalanceChange(
                type = updated.type,
                amount = updated.amount,
                sourceAccountId = updated.accountId,
                destAccountId = updated.destinationAccountId,
                isReversal = false
            )
        }
    }

    suspend fun deleteTransaction(transactionId: Long) {
        database.withTransaction {
            val tx = transactionDao.getTransactionByIdOnce(transactionId) ?: return@withTransaction

            // Reverse balance effect
            applyBalanceChange(
                type = tx.type,
                amount = tx.amount,
                sourceAccountId = tx.accountId,
                destAccountId = tx.destinationAccountId,
                isReversal = true
            )

            transactionDao.deleteTransaction(tx)
        }
    }

    private suspend fun applyBalanceChange(
        type: TransactionType,
        amount: Double,
        sourceAccountId: Long,
        destAccountId: Long?,
        isReversal: Boolean
    ) {
        val sign = if (isReversal) -1.0 else 1.0

        when (type) {
            TransactionType.INCOME -> {
                val sourceAcc = accountDao.getAccountByIdOnce(sourceAccountId) ?: return
                val delta = safeMultiply(amount, sign)
                val newBal = roundMoney(sourceAcc.currentBalance + delta)
                accountDao.updateBalance(sourceAccountId, newBal)
            }
            TransactionType.EXPENSE -> {
                val sourceAcc = accountDao.getAccountByIdOnce(sourceAccountId) ?: return
                val delta = safeMultiply(amount, sign)
                val newBal = roundMoney(sourceAcc.currentBalance - delta)
                accountDao.updateBalance(sourceAccountId, newBal)
            }
            TransactionType.TRANSFER -> {
                val sourceAcc = accountDao.getAccountByIdOnce(sourceAccountId)
                val destAcc = destAccountId?.let { accountDao.getAccountByIdOnce(it) }

                if (sourceAcc != null) {
                    val delta = safeMultiply(amount, sign)
                    val newSourceBal = roundMoney(sourceAcc.currentBalance - delta)
                    accountDao.updateBalance(sourceAccountId, newSourceBal)
                }
                if (destAcc != null && destAccountId != null) {
                    val delta = safeMultiply(amount, sign)
                    val newDestBal = roundMoney(destAcc.currentBalance + delta)
                    accountDao.updateBalance(destAccountId, newDestBal)
                }
            }
        }
    }

    suspend fun recomputeAccountBalance(accountId: Long) {
        val account = accountDao.getAccountByIdOnce(accountId) ?: return
        val allTx = transactionDao.getTransactionsByAccountSync(accountId)

        var runningBalance = BigDecimal.valueOf(account.openingBalance)

        for (tx in allTx) {
            val txAmount = BigDecimal.valueOf(tx.amount)
            when (tx.type) {
                TransactionType.INCOME -> {
                    if (tx.accountId == accountId) {
                        runningBalance = runningBalance.add(txAmount)
                    }
                }
                TransactionType.EXPENSE -> {
                    if (tx.accountId == accountId) {
                        runningBalance = runningBalance.subtract(txAmount)
                    }
                }
                TransactionType.TRANSFER -> {
                    if (tx.accountId == accountId) {
                        runningBalance = runningBalance.subtract(txAmount)
                    }
                    if (tx.destinationAccountId == accountId) {
                        runningBalance = runningBalance.add(txAmount)
                    }
                }
            }
        }

        accountDao.updateBalance(accountId, runningBalance.setScale(2, RoundingMode.HALF_EVEN).toDouble())
    }

    suspend fun recomputeAllBalances() {
        database.withTransaction {
            val accounts = accountDao.getAllAccountsSync()
            for (acc in accounts) {
                recomputeAccountBalance(acc.id)
            }
        }
    }

    // Budgets
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<BudgetEntity>> =
        budgetDao.getBudgetsForMonth(month, year)

    fun getAllBudgets(): Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

    suspend fun saveBudget(budget: BudgetEntity): Long {
        return database.withTransaction {
            val existing = budgetDao.getBudgetByCategoryOnce(budget.categoryId, budget.month, budget.year)
            if (existing != null) {
                budgetDao.updateBudget(budget.copy(id = existing.id, amount = roundMoney(budget.amount)))
                existing.id
            } else {
                budgetDao.insertBudget(budget.copy(amount = roundMoney(budget.amount)))
            }
        }
    }

    suspend fun deleteBudget(budget: BudgetEntity) = budgetDao.deleteBudget(budget)

    suspend fun deleteBudgetById(id: Long) = budgetDao.deleteBudgetById(id)

    // Savings Goals
    val allGoals: Flow<List<SavingsGoalEntity>> = savingsGoalDao.getAllGoals()
    val activeGoals: Flow<List<SavingsGoalEntity>> = savingsGoalDao.getActiveGoals()

    fun getGoalById(id: Long): Flow<SavingsGoalEntity?> = savingsGoalDao.getGoalById(id)

    suspend fun saveGoal(goal: SavingsGoalEntity): Long =
        savingsGoalDao.insertGoal(goal.copy(
            targetAmount = roundMoney(goal.targetAmount),
            currentAmount = roundMoney(goal.currentAmount)
        ))

    suspend fun updateGoal(goal: SavingsGoalEntity) =
        savingsGoalDao.updateGoal(goal.copy(
            targetAmount = roundMoney(goal.targetAmount),
            currentAmount = roundMoney(goal.currentAmount)
        ))

    suspend fun addMoneyToGoal(goalId: Long, addAmount: Double, accountId: Long?) {
        database.withTransaction {
            val goal = savingsGoalDao.getGoalByIdOnce(goalId) ?: return@withTransaction
            val newAmount = roundMoney(goal.currentAmount + addAmount)
            val isNowCompleted = newAmount >= goal.targetAmount
            savingsGoalDao.updateGoal(goal.copy(currentAmount = newAmount, isCompleted = isNowCompleted))

            // If account is linked, record an expense or deduction
            if (accountId != null) {
                val acc = accountDao.getAccountByIdOnce(accountId)
                if (acc != null) {
                    val deduction = roundMoney(addAmount)
                    val newBal = roundMoney(acc.currentBalance - deduction)
                    accountDao.updateBalance(accountId, newBal)

                    val tx = TransactionEntity(
                        type = TransactionType.EXPENSE,
                        amount = deduction,
                        accountId = accountId,
                        description = "Savings: ${goal.name}",
                        notes = "Contribution to savings goal"
                    )
                    transactionDao.insertTransaction(tx)
                }
            }
        }
    }

    suspend fun withdrawMoneyFromGoal(goalId: Long, withdrawAmount: Double, accountId: Long?) {
        database.withTransaction {
            val goal = savingsGoalDao.getGoalByIdOnce(goalId) ?: return@withTransaction
            val newAmount = roundMoney(maxOf(0.0, goal.currentAmount - withdrawAmount))
            savingsGoalDao.updateGoal(goal.copy(currentAmount = newAmount, isCompleted = false))

            // If account is linked, record as income to that account
            if (accountId != null) {
                val acc = accountDao.getAccountByIdOnce(accountId)
                if (acc != null) {
                    val addition = roundMoney(withdrawAmount)
                    val newBal = roundMoney(acc.currentBalance + addition)
                    accountDao.updateBalance(accountId, newBal)

                    val tx = TransactionEntity(
                        type = TransactionType.INCOME,
                        amount = addition,
                        accountId = accountId,
                        description = "Withdrawal: ${goal.name}",
                        notes = "Funds withdrawn from savings goal"
                    )
                    transactionDao.insertTransaction(tx)
                }
            }
        }
    }

    suspend fun deleteGoal(goal: SavingsGoalEntity) = savingsGoalDao.deleteGoal(goal)

    // Demo Data Seeder and Database Reset
    suspend fun clearAllData() {
        database.withTransaction {
            transactionDao.clearAll()
            budgetDao.clearAll()
            savingsGoalDao.clearAll()
            categoryDao.clearAll()
            accountDao.clearAll()
        }
    }

    suspend fun populateDemoData() {
        database.withTransaction {
            clearAllData()

            // 1. Categories
            val expenseCats = listOf(
                CategoryEntity(name = "Food & Dining", type = CategoryType.EXPENSE, iconName = "restaurant", colorHex = "#FF5722", isDefault = true),
                CategoryEntity(name = "Transportation", type = CategoryType.EXPENSE, iconName = "directions_car", colorHex = "#0288D1", isDefault = true),
                CategoryEntity(name = "Housing & Rent", type = CategoryType.EXPENSE, iconName = "home", colorHex = "#5E35B1", isDefault = true),
                CategoryEntity(name = "Utilities", type = CategoryType.EXPENSE, iconName = "bolt", colorHex = "#FBC02D", isDefault = true),
                CategoryEntity(name = "Education", type = CategoryType.EXPENSE, iconName = "school", colorHex = "#3949AB", isDefault = true),
                CategoryEntity(name = "Healthcare", type = CategoryType.EXPENSE, iconName = "local_hospital", colorHex = "#E53935", isDefault = true),
                CategoryEntity(name = "Shopping", type = CategoryType.EXPENSE, iconName = "shopping_bag", colorHex = "#D81B60", isDefault = true),
                CategoryEntity(name = "Entertainment", type = CategoryType.EXPENSE, iconName = "movie", colorHex = "#8E24AA", isDefault = true),
                CategoryEntity(name = "Airtime & Data", type = CategoryType.EXPENSE, iconName = "phone_android", colorHex = "#00897B", isDefault = true),
                CategoryEntity(name = "Bills & Fees", type = CategoryType.EXPENSE, iconName = "receipt_long", colorHex = "#7CB342", isDefault = true),
                CategoryEntity(name = "Family & Personal", type = CategoryType.EXPENSE, iconName = "people", colorHex = "#FB8C00", isDefault = true),
                CategoryEntity(name = "Other Expense", type = CategoryType.EXPENSE, iconName = "more_horiz", colorHex = "#78909C", isDefault = true)
            )

            val incomeCats = listOf(
                CategoryEntity(name = "Salary", type = CategoryType.INCOME, iconName = "payments", colorHex = "#2E7D32", isDefault = true),
                CategoryEntity(name = "Business", type = CategoryType.INCOME, iconName = "storefront", colorHex = "#00796B", isDefault = true),
                CategoryEntity(name = "Freelance & Gigs", type = CategoryType.INCOME, iconName = "laptop", colorHex = "#1565C0", isDefault = true),
                CategoryEntity(name = "Investment", type = CategoryType.INCOME, iconName = "trending_up", colorHex = "#6A1B9A", isDefault = true),
                CategoryEntity(name = "Gift", type = CategoryType.INCOME, iconName = "card_giftcard", colorHex = "#AD1457", isDefault = true),
                CategoryEntity(name = "Other Income", type = CategoryType.INCOME, iconName = "account_balance_wallet", colorHex = "#37474F", isDefault = true)
            )
            val catIds = categoryDao.insertCategories(expenseCats + incomeCats)
            val foodCatId = catIds[0]
            val transCatId = catIds[1]
            val utilCatId = catIds[3]
            val shopCatId = catIds[6]
            val salaryCatId = catIds[12]

            // 2. Accounts
            val accIds = accountDao.insertAccounts(listOf(
                AccountEntity(name = "Cash", accountType = AccountType.CASH, openingBalance = 500.0, currentBalance = 500.0, colorHex = "#43A047", iconName = "attach_money"),
                AccountEntity(name = "MTN Mobile Money", accountType = AccountType.MOBILE_MONEY, openingBalance = 1200.0, currentBalance = 1200.0, colorHex = "#FF8F00", iconName = "smartphone"),
                AccountEntity(name = "Bank Account", accountType = AccountType.BANK, openingBalance = 5000.0, currentBalance = 5000.0, colorHex = "#1E88E5", iconName = "account_balance")
            ))
            val cashAccId = accIds[0]
            val momoAccId = accIds[1]
            val bankAccId = accIds[2]

            // 3. Transactions
            val now = System.currentTimeMillis()
            val day = 86400000L

            createTransaction(TransactionEntity(
                type = TransactionType.INCOME,
                amount = 5000.0,
                accountId = bankAccId,
                categoryId = salaryCatId,
                description = "Monthly Salary",
                date = now - 15 * day
            ))

            createTransaction(TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 150.0,
                accountId = cashAccId,
                categoryId = foodCatId,
                description = "Groceries & Market",
                date = now - 4 * day
            ))

            createTransaction(TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 80.0,
                accountId = momoAccId,
                categoryId = transCatId,
                description = "TroTro & Ride Hailing",
                date = now - 3 * day
            ))

            createTransaction(TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 250.0,
                accountId = momoAccId,
                categoryId = utilCatId,
                description = "Electricity (ECG Pre-paid)",
                date = now - 2 * day
            ))

            createTransaction(TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 320.0,
                accountId = bankAccId,
                categoryId = shopCatId,
                description = "Office Supplies & Books",
                date = now - 1 * day
            ))

            createTransaction(TransactionEntity(
                type = TransactionType.TRANSFER,
                amount = 400.0,
                accountId = bankAccId,
                destinationAccountId = momoAccId,
                description = "Bank to MoMo Transfer",
                date = now - 1 * day
            ))

            // 4. Budgets for current month
            val calendar = java.util.Calendar.getInstance()
            val currentMonth = calendar.get(java.util.Calendar.MONTH) + 1
            val currentYear = calendar.get(java.util.Calendar.YEAR)

            budgetDao.insertBudgets(listOf(
                BudgetEntity(categoryId = foodCatId, amount = 1000.0, month = currentMonth, year = currentYear),
                BudgetEntity(categoryId = transCatId, amount = 500.0, month = currentMonth, year = currentYear),
                BudgetEntity(categoryId = utilCatId, amount = 400.0, month = currentMonth, year = currentYear),
                BudgetEntity(categoryId = shopCatId, amount = 600.0, month = currentMonth, year = currentYear)
            ))

            // 5. Savings Goals
            savingsGoalDao.insertGoals(listOf(
                SavingsGoalEntity(
                    name = "Emergency Fund",
                    targetAmount = 10000.0,
                    currentAmount = 6500.0,
                    description = "6 months of living expenses reserve",
                    colorHex = "#00897B",
                    iconName = "shield"
                ),
                SavingsGoalEntity(
                    name = "New Laptop",
                    targetAmount = 7500.0,
                    currentAmount = 3000.0,
                    description = "Work development machine",
                    colorHex = "#3949AB",
                    iconName = "laptop"
                )
            ))
        }
    }

    // Direct access for Backup/Restore
    suspend fun getAllAccountsDirect() = accountDao.getAllAccountsSync()
    suspend fun getAllCategoriesDirect() = categoryDao.getAllCategoriesSync()
    suspend fun getAllTransactionsDirect() = transactionDao.getAllTransactionsSync()
    suspend fun getAllBudgetsDirect() = budgetDao.getAllBudgetsSync()
    suspend fun getAllSavingsGoalsDirect() = savingsGoalDao.getAllGoalsSync()

    suspend fun restoreDatabase(
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        savingsGoals: List<SavingsGoalEntity>
    ) {
        database.withTransaction {
            clearAllData()
            if (categories.isNotEmpty()) categoryDao.insertCategories(categories)
            if (accounts.isNotEmpty()) accountDao.insertAccounts(accounts)
            if (transactions.isNotEmpty()) transactionDao.insertTransactions(transactions)
            if (budgets.isNotEmpty()) budgetDao.insertBudgets(budgets)
            if (savingsGoals.isNotEmpty()) savingsGoalDao.insertGoals(savingsGoals)
            recomputeAllBalances()
        }
    }

    companion object {
        fun roundMoney(value: Double): Double {
            return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_EVEN)
                .toDouble()
        }

        private fun safeMultiply(value: Double, factor: Double): Double {
            return BigDecimal.valueOf(value)
                .multiply(BigDecimal.valueOf(factor))
                .setScale(2, RoundingMode.HALF_EVEN)
                .toDouble()
        }
    }
}
