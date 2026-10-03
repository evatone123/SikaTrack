package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.local.AppDatabase
import com.example.data.local.DataStoreManager
import com.example.data.local.UserPreferences
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.CategoryType
import com.example.data.model.DateFilterPeriod
import com.example.data.model.SortOrder
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.domain.BudgetProgress
import com.example.domain.CategorySpending
import com.example.domain.FinancialCalculations
import com.example.domain.MonthTrendData
import com.example.domain.MonthlyFinancialSummary
import com.example.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

data class TxFilterState(
    val query: String = "",
    val type: TransactionType? = null,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val period: DateFilterPeriod = DateFilterPeriod.ALL,
    val sort: SortOrder = SortOrder.NEWEST
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val repository = FinanceRepository(database)
    val dataStoreManager = DataStoreManager(application)
    val backupManager = BackupManager(application, repository)
    val notificationHelper = NotificationHelper(application)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.example.data.local.seedDefaultCategories(database.categoryDao())
                com.example.data.local.seedDefaultAccounts(database.accountDao())
            } catch (_: Exception) {
                // Ignore to avoid crashing
            }
        }
    }

    // Current month and year for budget and reports
    private val calendar = Calendar.getInstance()
    private val _selectedMonth = MutableStateFlow(calendar.get(Calendar.MONTH) + 1)
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(calendar.get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    // Preferences
    val userPreferences: StateFlow<UserPreferences> = dataStoreManager.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    // Lock screen state
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    fun unlockApp() {
        _isUnlocked.value = true
    }

    fun lockApp() {
        _isUnlocked.value = false
    }

    // Snackbars / UI Messages
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    fun showMessage(message: String) {
        viewModelScope.launch {
            _userMessage.emit(message)
        }
    }

    // Core Data Streams
    val activeAccounts: StateFlow<List<AccountEntity>> = repository.activeAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAccounts: StateFlow<List<AccountEntity>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCategories: StateFlow<List<CategoryEntity>> = repository.activeCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<TransactionEntity>> = repository.getRecentTransactions(8)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBudgets: StateFlow<List<BudgetEntity>> = repository.getAllBudgets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSavingsGoals: StateFlow<List<SavingsGoalEntity>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month Selector actions
    fun setMonthYear(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
    }

    fun previousMonth() {
        if (_selectedMonth.value == 1) {
            _selectedMonth.value = 12
            _selectedYear.value -= 1
        } else {
            _selectedMonth.value -= 1
        }
    }

    fun nextMonth() {
        if (_selectedMonth.value == 12) {
            _selectedMonth.value = 1
            _selectedYear.value += 1
        } else {
            _selectedMonth.value += 1
        }
    }

    // Monthly financial summary
    val monthlySummary: StateFlow<MonthlyFinancialSummary> = combine(
        activeAccounts,
        allTransactions,
        selectedMonth,
        selectedYear
    ) { accounts, transactions, month, year ->
        FinancialCalculations.calculateMonthlySummary(accounts, transactions, month, year)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MonthlyFinancialSummary(0.0, 0.0, 0.0, 0.0, 0.0)
    )

    // Budgets progress for current selected month
    val budgetProgressList: StateFlow<List<BudgetProgress>> = combine(
        allBudgets,
        activeCategories,
        allTransactions,
        selectedMonth,
        selectedYear
    ) { budgets, categories, transactions, month, year ->
        FinancialCalculations.calculateBudgetProgress(budgets, categories, transactions, month, year)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category Spending for current selected month
    val categorySpendings: StateFlow<List<CategorySpending>> = combine(
        allTransactions,
        activeCategories,
        selectedMonth,
        selectedYear
    ) { transactions, categories, month, year ->
        val (start, end) = FinancialCalculations.getMonthRange(month, year)
        FinancialCalculations.calculateCategorySpending(transactions, categories, start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Last 6 months trend
    val monthlyTrends: StateFlow<List<MonthTrendData>> = allTransactions.map { txs ->
        FinancialCalculations.calculateLast6MonthsTrends(txs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transaction History Filters
    val searchQuery = MutableStateFlow("")
    val filterType = MutableStateFlow<TransactionType?>(null)
    val filterAccountId = MutableStateFlow<Long?>(null)
    val filterCategoryId = MutableStateFlow<Long?>(null)
    val filterDatePeriod = MutableStateFlow(DateFilterPeriod.ALL)
    val customStartDate = MutableStateFlow<Long?>(null)
    val customEndDate = MutableStateFlow<Long?>(null)
    val sortOrder = MutableStateFlow(SortOrder.NEWEST)

    private val filterState = combine(
        combine(searchQuery, filterType) { q, t -> Pair(q, t) },
        combine(filterAccountId, filterCategoryId) { a, c -> Pair(a, c) },
        combine(filterDatePeriod, sortOrder) { p, s -> Pair(p, s) }
    ) { (q, t), (a, c), (p, s) ->
        TxFilterState(q, t, a, c, p, s)
    }

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        filterState
    ) { txList, filter ->
        val now = System.currentTimeMillis()
        val dateRange: Pair<Long, Long>? = when (filter.period) {
            DateFilterPeriod.ALL -> null
            DateFilterPeriod.TODAY -> FinancialCalculations.getDayRange(now)
            DateFilterPeriod.THIS_WEEK -> FinancialCalculations.getWeekRange(now)
            DateFilterPeriod.THIS_MONTH -> {
                val c = Calendar.getInstance()
                FinancialCalculations.getMonthRange(c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR))
            }
            DateFilterPeriod.LAST_MONTH -> {
                val c = Calendar.getInstance()
                c.add(Calendar.MONTH, -1)
                FinancialCalculations.getMonthRange(c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR))
            }
            DateFilterPeriod.CUSTOM -> {
                val start = customStartDate.value ?: 0L
                val end = customEndDate.value ?: Long.MAX_VALUE
                Pair(start, end)
            }
        }

        var result = txList.asSequence()

        if (filter.query.isNotBlank()) {
            val q = filter.query.trim().lowercase()
            result = result.filter {
                it.description.lowercase().contains(q) ||
                        (it.notes?.lowercase()?.contains(q) == true)
            }
        }

        if (filter.type != null) {
            result = result.filter { it.type == filter.type }
        }

        if (filter.accountId != null) {
            result = result.filter { it.accountId == filter.accountId || it.destinationAccountId == filter.accountId }
        }

        if (filter.categoryId != null) {
            result = result.filter { it.categoryId == filter.categoryId }
        }

        if (dateRange != null) {
            result = result.filter { it.date in dateRange.first..dateRange.second }
        }

        val filtered = result.toList()

        when (filter.sort) {
            SortOrder.NEWEST -> filtered.sortedWith(compareByDescending<TransactionEntity> { it.date }.thenByDescending { it.id })
            SortOrder.OLDEST -> filtered.sortedWith(compareBy<TransactionEntity> { it.date }.thenBy { it.id })
            SortOrder.HIGHEST_AMOUNT -> filtered.sortedByDescending { it.amount }
            SortOrder.LOWEST_AMOUNT -> filtered.sortedBy { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Database Actions
    fun saveTransaction(
        id: Long = 0,
        type: TransactionType,
        amount: Double,
        accountId: Long,
        destAccountId: Long? = null,
        categoryId: Long? = null,
        description: String,
        date: Long,
        notes: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (id == 0L || id == -1L) {
                    val entity = TransactionEntity(
                        type = type,
                        amount = amount,
                        accountId = accountId,
                        destinationAccountId = if (type == TransactionType.TRANSFER) destAccountId else null,
                        categoryId = if (type != TransactionType.TRANSFER) categoryId else null,
                        description = description.ifBlank {
                            when (type) {
                                TransactionType.EXPENSE -> "Expense"
                                TransactionType.INCOME -> "Income"
                                TransactionType.TRANSFER -> "Transfer"
                            }
                        },
                        date = date,
                        notes = notes
                    )
                    repository.createTransaction(entity)
                    showMessage("Transaction saved successfully")
                } else {
                    val entity = TransactionEntity(
                        id = id,
                        type = type,
                        amount = amount,
                        accountId = accountId,
                        destinationAccountId = if (type == TransactionType.TRANSFER) destAccountId else null,
                        categoryId = if (type != TransactionType.TRANSFER) categoryId else null,
                        description = description,
                        date = date,
                        notes = notes
                    )
                    repository.updateTransaction(entity)
                    showMessage("Transaction updated successfully")
                }
                onSuccess()
            } catch (e: Exception) {
                showMessage("Failed to save transaction: ${e.localizedMessage}")
            }
        }
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(transactionId)
                showMessage("Transaction deleted")
            } catch (e: Exception) {
                showMessage("Error deleting transaction: ${e.localizedMessage}")
            }
        }
    }

    // Accounts
    fun saveAccount(
        id: Long = 0,
        name: String,
        type: AccountType,
        openingBalance: Double,
        colorHex: String,
        iconName: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (id == 0L) {
                    val acc = AccountEntity(
                        name = name,
                        accountType = type,
                        openingBalance = openingBalance,
                        currentBalance = openingBalance,
                        colorHex = colorHex,
                        iconName = iconName
                    )
                    repository.createAccount(acc)
                    showMessage("Account added")
                } else {
                    val existing = allAccounts.value.find { it.id == id }
                    if (existing != null) {
                        val updated = existing.copy(
                            name = name,
                            accountType = type,
                            colorHex = colorHex,
                            iconName = iconName
                        )
                        repository.updateAccount(updated)
                        showMessage("Account updated")
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                showMessage("Error saving account: ${e.localizedMessage}")
            }
        }
    }

    fun archiveAccount(accountId: Long, isArchived: Boolean) {
        viewModelScope.launch {
            repository.archiveAccount(accountId, isArchived)
            showMessage(if (isArchived) "Account archived" else "Account unarchived")
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
            showMessage("Account deleted")
        }
    }

    // Budgets
    fun saveBudget(categoryId: Long, amount: Double, month: Int, year: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val budget = BudgetEntity(
                    categoryId = categoryId,
                    amount = amount,
                    month = month,
                    year = year
                )
                repository.saveBudget(budget)
                showMessage("Budget saved")
                onSuccess()
            } catch (e: Exception) {
                showMessage("Error saving budget: ${e.localizedMessage}")
            }
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
            showMessage("Budget deleted")
        }
    }

    // Savings Goals
    fun saveSavingsGoal(
        id: Long = 0,
        name: String,
        targetAmount: Double,
        currentAmount: Double = 0.0,
        description: String? = null,
        targetDate: Long? = null,
        colorHex: String = "#FFB300",
        iconName: String = "savings",
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (id == 0L) {
                    val goal = SavingsGoalEntity(
                        name = name,
                        targetAmount = targetAmount,
                        currentAmount = currentAmount,
                        description = description,
                        targetDate = targetDate,
                        colorHex = colorHex,
                        iconName = iconName
                    )
                    repository.saveGoal(goal)
                    showMessage("Savings goal created")
                } else {
                    val existing = allSavingsGoals.value.find { it.id == id }
                    if (existing != null) {
                        val updated = existing.copy(
                            name = name,
                            targetAmount = targetAmount,
                            description = description,
                            targetDate = targetDate,
                            colorHex = colorHex,
                            iconName = iconName
                        )
                        repository.updateGoal(updated)
                        showMessage("Savings goal updated")
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                showMessage("Error saving goal: ${e.localizedMessage}")
            }
        }
    }

    fun addMoneyToGoal(goalId: Long, amount: Double, accountId: Long?) {
        viewModelScope.launch {
            try {
                repository.addMoneyToGoal(goalId, amount, accountId)
                showMessage("Added money to savings goal")
            } catch (e: Exception) {
                showMessage("Error updating goal: ${e.localizedMessage}")
            }
        }
    }

    fun withdrawMoneyFromGoal(goalId: Long, amount: Double, accountId: Long?) {
        viewModelScope.launch {
            try {
                repository.withdrawMoneyFromGoal(goalId, amount, accountId)
                showMessage("Withdrew money from savings goal")
            } catch (e: Exception) {
                showMessage("Error withdrawing from goal: ${e.localizedMessage}")
            }
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
            showMessage("Savings goal deleted")
        }
    }

    // Settings actions
    fun setCurrency(currency: String) {
        viewModelScope.launch {
            dataStoreManager.updateCurrency(currency)
            showMessage("Currency set to $currency")
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            dataStoreManager.updateThemeMode(mode)
        }
    }

    fun setHideBalances(hide: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setHideBalances(hide)
        }
    }

    fun setPin(enabled: Boolean, code: String) {
        viewModelScope.launch {
            dataStoreManager.setPin(enabled, code)
            showMessage(if (enabled) "PIN lock enabled" else "PIN lock disabled")
        }
    }

    fun setDailyReminder(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setDailyReminder(enabled)
            if (enabled) {
                notificationHelper.showDailyReminderNotification()
                showMessage("Daily reminder enabled")
            } else {
                showMessage("Daily reminder disabled")
            }
        }
    }

    fun setBudgetAlerts(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setBudgetAlerts(enabled)
            showMessage(if (enabled) "Budget alerts enabled" else "Budget alerts disabled")
        }
    }

    // Backup & Restore
    fun exportBackup(uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.exportBackupToFile(uri)
            if (result.isSuccess) {
                onResult(true, "Backup exported successfully")
            } else {
                onResult(false, result.exceptionOrNull()?.localizedMessage ?: "Export failed")
            }
        }
    }

    fun exportToInternalFile(onResult: (Boolean, File?) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.exportToLocalAppDirectory()
            if (result.isSuccess) {
                showMessage("Backup saved to local storage")
                onResult(true, result.getOrNull())
            } else {
                showMessage("Backup failed: ${result.exceptionOrNull()?.localizedMessage}")
                onResult(false, null)
            }
        }
    }

    fun restoreBackup(uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.restoreBackupFromUri(uri)
            if (result.isSuccess) {
                val summary = result.getOrNull() ?: "Data restored"
                showMessage(summary)
                onResult(true, summary)
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Restore failed"
                showMessage("Restore error: $err")
                onResult(false, err)
            }
        }
    }

    fun loadDemoData() {
        viewModelScope.launch {
            try {
                repository.populateDemoData()
                showMessage("Demo financial data loaded")
            } catch (e: Exception) {
                showMessage("Error loading demo data: ${e.localizedMessage}")
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            try {
                repository.clearAllData()
                // Re-seed default categories & accounts
                com.example.data.local.seedDefaultCategories(database.categoryDao())
                com.example.data.local.seedDefaultAccounts(database.accountDao())
                showMessage("Database reset to defaults")
            } catch (e: Exception) {
                showMessage("Error resetting data: ${e.localizedMessage}")
            }
        }
    }
}
