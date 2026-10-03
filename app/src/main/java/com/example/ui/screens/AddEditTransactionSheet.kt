package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.model.CategoryType
import com.example.data.model.TransactionType
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.getCategoryIcon
import com.example.ui.components.parseColorSafe
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import com.example.ui.theme.TransferColor
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionSheet(
    viewModel: MainViewModel,
    initialType: String = "EXPENSE",
    transactionId: Long = -1L,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accounts by viewModel.activeAccounts.collectAsStateWithLifecycle()
    val allCategories by viewModel.activeCategories.collectAsStateWithLifecycle()
    val allTxs by viewModel.allTransactions.collectAsStateWithLifecycle()
    val userPrefs by viewModel.userPreferences.collectAsStateWithLifecycle()

    var selectedType by remember {
        mutableStateOf(
            try {
                TransactionType.valueOf(initialType)
            } catch (_: Exception) {
                TransactionType.EXPENSE
            }
        )
    }

    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableLongStateOf(0L) }
    var destinationAccountId by remember { mutableLongStateOf(0L) }
    var selectedCategoryId by remember { mutableLongStateOf(0L) }
    var transactionDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // If editing existing transaction, prefill values
    LaunchedEffect(transactionId, allTxs) {
        if (transactionId != -1L) {
            val existing = allTxs.find { it.id == transactionId }
            if (existing != null) {
                selectedType = existing.type
                amountText = if (existing.amount % 1.0 == 0.0) {
                    existing.amount.toLong().toString()
                } else {
                    existing.amount.toString()
                }
                description = existing.description
                notes = existing.notes ?: ""
                selectedAccountId = existing.accountId
                destinationAccountId = existing.destinationAccountId ?: 0L
                selectedCategoryId = existing.categoryId ?: 0L
                transactionDate = existing.date
            }
        } else if (accounts.isNotEmpty() && selectedAccountId == 0L) {
            selectedAccountId = accounts.first().id
            if (accounts.size > 1) {
                destinationAccountId = accounts[1].id
            }
        }
    }

    val availableCategories = remember(allCategories, selectedType) {
        when (selectedType) {
            TransactionType.EXPENSE -> allCategories.filter { it.type == CategoryType.EXPENSE }
            TransactionType.INCOME -> allCategories.filter { it.type == CategoryType.INCOME }
            TransactionType.TRANSFER -> emptyList()
        }
    }

    // Default select first category if none selected
    LaunchedEffect(availableCategories) {
        if (selectedCategoryId == 0L && availableCategories.isNotEmpty()) {
            selectedCategoryId = availableCategories.first().id
        }
    }

    val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())

    fun openDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = transactionDate }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                transactionDate = newCal.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (transactionId == -1L) "Add Transaction" else "Edit Transaction",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (transactionId != -1L) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("delete_tx_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type Tabs
            TabRow(
                selectedTabIndex = selectedType.ordinal,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .fillMaxWidth()
            ) {
                Tab(
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = { selectedType = TransactionType.EXPENSE },
                    text = { Text("Expense", fontWeight = FontWeight.Bold) },
                    selectedContentColor = ExpenseColor,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("tab_expense")
                )
                Tab(
                    selected = selectedType == TransactionType.INCOME,
                    onClick = { selectedType = TransactionType.INCOME },
                    text = { Text("Income", fontWeight = FontWeight.Bold) },
                    selectedContentColor = IncomeColor,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("tab_income")
                )
                Tab(
                    selected = selectedType == TransactionType.TRANSFER,
                    onClick = { selectedType = TransactionType.TRANSFER },
                    text = { Text("Transfer", fontWeight = FontWeight.Bold) },
                    selectedContentColor = TransferColor,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("tab_transfer")
                )
            }

            // Amount Input Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Enter Amount (${userPrefs.defaultCurrency})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = userPrefs.defaultCurrency,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = when (selectedType) {
                                TransactionType.EXPENSE -> ExpenseColor
                                TransactionType.INCOME -> IncomeColor
                                TransactionType.TRANSFER -> TransferColor
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                                    amountText = input
                                    errorMessage = null
                                }
                            },
                            placeholder = { Text("0.00", fontSize = 28.sp) },
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .testTag("amount_input")
                        )
                    }
                }
            }

            // Account Selector
            Column {
                Text(
                    text = if (selectedType == TransactionType.TRANSFER) "From Account" else "Account / Wallet",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(accounts, key = { it.id }) { acc ->
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = { selectedAccountId = acc.id },
                            label = { Text(acc.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = getCategoryIcon(acc.iconName),
                                    contentDescription = null,
                                    tint = parseColorSafe(acc.colorHex),
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("account_chip_${acc.id}")
                        )
                    }
                }
            }

            // Destination Account (for Transfer)
            if (selectedType == TransactionType.TRANSFER) {
                Column {
                    Text(
                        text = "To Account",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(accounts.filter { it.id != selectedAccountId }, key = { it.id }) { acc ->
                            FilterChip(
                                selected = destinationAccountId == acc.id,
                                onClick = { destinationAccountId = acc.id },
                                label = { Text(acc.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getCategoryIcon(acc.iconName),
                                        contentDescription = null,
                                        tint = parseColorSafe(acc.colorHex),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("dest_account_chip_${acc.id}")
                            )
                        }
                    }
                }
            }

            // Category Selector (Expense or Income)
            if (selectedType != TransactionType.TRANSFER) {
                Column {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        availableCategories.forEach { cat ->
                            val isSelected = selectedCategoryId == cat.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategoryId = cat.id },
                                label = { Text(cat.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getCategoryIcon(cat.iconName),
                                        contentDescription = null,
                                        tint = parseColorSafe(cat.colorHex),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("category_chip_${cat.id}")
                            )
                        }
                    }
                }
            }

            // Date Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { openDatePicker() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Select Date",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Date",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dateFormat.format(Date(transactionDate)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Text(
                    text = "Change",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Description & Suggestions
            Column {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = {
                        Text(
                            when (selectedType) {
                                TransactionType.EXPENSE -> "e.g. Lunch with team, Groceries"
                                TransactionType.INCOME -> "e.g. Monthly salary, Freelance design"
                                TransactionType.TRANSFER -> "e.g. Moved to savings"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("description_input")
                )

                // Quick suggestions
                val suggestions = when (selectedType) {
                    TransactionType.EXPENSE -> listOf("Groceries", "Lunch", "TroTro / Ride", "Fuel", "Airtime", "Electricity")
                    TransactionType.INCOME -> listOf("Salary", "Side Project", "Dividends", "Gift")
                    TransactionType.TRANSFER -> listOf("Bank to MoMo", "To Savings", "Cash Withdrawal")
                }

                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(suggestions) { sugg ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { description = sugg }
                        ) {
                            Text(
                                text = sugg,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notes_input")
            )

            // Error display
            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Save Button
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        errorMessage = "Please enter a valid amount greater than 0"
                        return@Button
                    }
                    if (selectedAccountId == 0L) {
                        errorMessage = "Please select an account"
                        return@Button
                    }
                    if (selectedType == TransactionType.TRANSFER) {
                        if (destinationAccountId == 0L || destinationAccountId == selectedAccountId) {
                            errorMessage = "Please select a different destination account"
                            return@Button
                        }
                    }

                    viewModel.saveTransaction(
                        id = if (transactionId != -1L) transactionId else 0L,
                        type = selectedType,
                        amount = amount,
                        accountId = selectedAccountId,
                        destAccountId = if (selectedType == TransactionType.TRANSFER) destinationAccountId else null,
                        categoryId = if (selectedType != TransactionType.TRANSFER) selectedCategoryId else null,
                        description = description.trim(),
                        date = transactionDate,
                        notes = notes.trim().ifBlank { null },
                        onSuccess = onNavigateBack
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (transactionId == -1L) "Save Transaction" else "Update Transaction",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction? The account balance will be updated automatically.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteTransaction(transactionId)
                        onNavigateBack()
                    },
                    modifier = Modifier.testTag("confirm_delete_tx")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
