package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.model.CategoryType
import com.example.domain.BudgetProgress
import com.example.domain.BudgetStatus
import com.example.domain.CurrencyFormatter
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.components.GlassCard
import com.example.ui.components.MonthSelectorHeader
import com.example.ui.components.getCategoryIcon
import com.example.ui.components.glassmorphic
import com.example.ui.components.parseColorSafe
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import com.example.ui.theme.WarningColor
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val budgetProgressList by viewModel.budgetProgressList.collectAsStateWithLifecycle()
    val allCategories by viewModel.activeCategories.collectAsStateWithLifecycle()
    val userPrefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()

    val expenseCategories = allCategories.filter { it.type == CategoryType.EXPENSE }

    var showAddDialog by remember { mutableStateOf(false) }
    var budgetToEdit by remember { mutableStateOf<BudgetProgress?>(null) }
    var budgetToDelete by remember { mutableStateOf<BudgetEntity?>(null) }

    val totalBudgeted = budgetProgressList.sumOf { it.budget.amount }
    val totalSpent = budgetProgressList.sumOf { it.spentAmount }
    val totalRemaining = totalBudgeted - totalSpent

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monthly Budgets", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    budgetToEdit = null
                    showAddDialog = true
                },
                modifier = Modifier
                    .testTag("fab_add_budget")
                    .glassmorphic(
                        shape = RoundedCornerShape(16.dp),
                        elevation = 8.dp,
                        tint = MaterialTheme.colorScheme.primary,
                        accentBorder = Color.White.copy(alpha = 0.5f)
                    ),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Budget")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Month Selector
            item {
                MonthSelectorHeader(
                    month = selectedMonth,
                    year = selectedYear,
                    onPrevious = { viewModel.previousMonth() },
                    onNext = { viewModel.nextMonth() }
                )
            }

            // Overview Summary Card
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_overview_card"),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Budget",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.format(totalBudgeted, userPrefs.defaultCurrency, userPrefs.hideBalances),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Spent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.format(totalSpent, userPrefs.defaultCurrency, userPrefs.hideBalances),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalSpent > totalBudgeted) ExpenseColor else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val overallFraction = if (totalBudgeted > 0) (totalSpent / totalBudgeted).toFloat().coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(
                            progress = { overallFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (totalSpent > totalBudgeted) ExpenseColor else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${(overallFraction * 100).toInt()}% used",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (totalRemaining >= 0) {
                                    "${CurrencyFormatter.format(totalRemaining, userPrefs.defaultCurrency, userPrefs.hideBalances)} remaining"
                                } else {
                                    "${CurrencyFormatter.format(-totalRemaining, userPrefs.defaultCurrency, userPrefs.hideBalances)} over budget"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (totalRemaining >= 0) IncomeColor else ExpenseColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Categories Budget List
            if (budgetProgressList.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "No budgets set for this month",
                        message = "Set spending targets by category (e.g. Food, Transport, Utilities) to stay in control of your expenses.",
                        buttonText = "Create First Budget",
                        icon = Icons.Default.PieChart,
                        onButtonClick = {
                            budgetToEdit = null
                            showAddDialog = true
                        }
                    )
                }
            } else {
                items(budgetProgressList, key = { it.budget.id }) { item ->
                    BudgetItemCard(
                        progress = item,
                        currency = userPrefs.defaultCurrency,
                        hideBalances = userPrefs.hideBalances,
                        onEdit = {
                            budgetToEdit = item
                            showAddDialog = true
                        },
                        onDelete = { budgetToDelete = item.budget }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Add / Edit Budget Dialog
    if (showAddDialog) {
        AddEditBudgetDialog(
            budgetProgress = budgetToEdit,
            categories = expenseCategories,
            month = selectedMonth,
            year = selectedYear,
            currency = userPrefs.defaultCurrency,
            onDismiss = { showAddDialog = false },
            onSave = { categoryId, amount ->
                viewModel.saveBudget(categoryId, amount, selectedMonth, selectedYear) {
                    showAddDialog = false
                }
            }
        )
    }

    // Delete Confirmation
    if (budgetToDelete != null) {
        AlertDialog(
            onDismissRequest = { budgetToDelete = null },
            title = { Text("Delete Budget") },
            text = { Text("Are you sure you want to remove this budget?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        budgetToDelete?.let { viewModel.deleteBudget(it) }
                        budgetToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_budget")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { budgetToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BudgetItemCard(
    progress: BudgetProgress,
    currency: String,
    hideBalances: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = progress.category
    val statusColor = when (progress.status) {
        BudgetStatus.WITHIN_BUDGET -> IncomeColor
        BudgetStatus.NEAR_BUDGET -> WarningColor
        BudgetStatus.OVER_BUDGET -> ExpenseColor
    }

    val statusText = when (progress.status) {
        BudgetStatus.WITHIN_BUDGET -> "Within Budget"
        BudgetStatus.NEAR_BUDGET -> "Near Limit"
        BudgetStatus.OVER_BUDGET -> "Over Budget"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphic(
                shape = RoundedCornerShape(18.dp),
                elevation = 5.dp,
                accentBorder = statusColor.copy(alpha = 0.5f)
            )
            .testTag("budget_card_${progress.budget.id}")
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIconBadge(
                    iconName = category?.iconName ?: "category",
                    colorHex = category?.colorHex ?: "#00796B",
                    size = 42
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category?.name ?: "Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Budget", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Budget", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress.progressFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spent: ${CurrencyFormatter.format(progress.spentAmount, currency, hideBalances)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Budget: ${CurrencyFormatter.format(progress.budget.amount, currency, hideBalances)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AddEditBudgetDialog(
    budgetProgress: BudgetProgress?,
    categories: List<CategoryEntity>,
    month: Int,
    year: Int,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (categoryId: Long, amount: Double) -> Unit
) {
    var selectedCategoryId by remember {
        mutableLongStateOf(budgetProgress?.budget?.categoryId ?: (categories.firstOrNull()?.id ?: 0L))
    }
    var amountText by remember {
        mutableStateOf(budgetProgress?.budget?.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "")
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (budgetProgress == null) "Set Budget" else "Edit Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (budgetProgress == null) {
                    Text("Select Category", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories, key = { it.id }) { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id },
                                label = { Text(cat.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getCategoryIcon(cat.iconName),
                                        contentDescription = null,
                                        tint = parseColorSafe(cat.colorHex),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Category: ${budgetProgress.category?.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                            amountText = input
                            errorMessage = null
                        }
                    },
                    label = { Text("Monthly Budget Amount ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("budget_amount_input")
                )

                if (errorMessage != null) {
                    Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        errorMessage = "Please enter a valid amount"
                        return@Button
                    }
                    if (selectedCategoryId == 0L) {
                        errorMessage = "Please select a category"
                        return@Button
                    }
                    onSave(selectedCategoryId, amount)
                },
                modifier = Modifier.testTag("save_budget_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
