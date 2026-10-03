package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.TransactionType
import com.example.domain.CurrencyFormatter
import com.example.domain.FinancialCalculations
import com.example.ui.components.EmptyStateView
import com.example.ui.components.GlassCard
import com.example.ui.components.TransactionItemRow
import com.example.ui.components.getCategoryIcon
import com.example.ui.components.glassmorphic
import com.example.ui.components.parseColorSafe
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: MainViewModel,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val allAccounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val userPrefs by viewModel.userPreferences.collectAsStateWithLifecycle()

    val categoryMap = allCategories.associateBy { it.id }
    val accountMap = allAccounts.associateBy { it.id }

    val activeAccounts = allAccounts.filter { !it.isArchived }
    val archivedAccounts = allAccounts.filter { it.isArchived }

    val totalBalance = FinancialCalculations.calculateTotalBalance(allAccounts)

    var showAddEditDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }
    var selectedAccountForDetail by remember { mutableStateOf<AccountEntity?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accounts & Wallets", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    accountToEdit = null
                    showAddEditDialog = true
                },
                modifier = Modifier
                    .testTag("fab_add_account")
                    .glassmorphic(
                        shape = RoundedCornerShape(16.dp),
                        elevation = 8.dp,
                        tint = MaterialTheme.colorScheme.primary,
                        accentBorder = Color.White.copy(alpha = 0.5f)
                    ),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Account")
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
            // Net Worth Card
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("accounts_total_balance_card"),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Total Balance",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = CurrencyFormatter.format(
                                amount = totalBalance,
                                currencyCode = userPrefs.defaultCurrency,
                                hideBalances = userPrefs.hideBalances
                            ),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${activeAccounts.size} Active Accounts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Active Accounts Header
            item {
                Text(
                    text = "Active Accounts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (activeAccounts.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "No active accounts",
                        message = "Add an account like Cash, Bank Account, or Mobile Money to start tracking.",
                        buttonText = "Add Account",
                        icon = Icons.Default.AccountBalanceWallet,
                        onButtonClick = {
                            accountToEdit = null
                            showAddEditDialog = true
                        }
                    )
                }
            } else {
                items(activeAccounts, key = { it.id }) { acc ->
                    AccountItemCard(
                        account = acc,
                        currency = userPrefs.defaultCurrency,
                        hideBalances = userPrefs.hideBalances,
                        onCardClick = { selectedAccountForDetail = acc },
                        onEdit = {
                            accountToEdit = acc
                            showAddEditDialog = true
                        },
                        onArchive = { viewModel.archiveAccount(acc.id, true) },
                        onDelete = { accountToDelete = acc }
                    )
                }
            }

            // Archived Accounts (if any)
            if (archivedAccounts.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Archived Accounts (${archivedAccounts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(archivedAccounts, key = { it.id }) { acc ->
                    AccountItemCard(
                        account = acc,
                        currency = userPrefs.defaultCurrency,
                        hideBalances = userPrefs.hideBalances,
                        isArchivedView = true,
                        onCardClick = { selectedAccountForDetail = acc },
                        onEdit = {
                            accountToEdit = acc
                            showAddEditDialog = true
                        },
                        onArchive = { viewModel.archiveAccount(acc.id, false) },
                        onDelete = { accountToDelete = acc }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Add / Edit Account Dialog
    if (showAddEditDialog) {
        AddEditAccountDialog(
            account = accountToEdit,
            currency = userPrefs.defaultCurrency,
            onDismiss = { showAddEditDialog = false },
            onSave = { id, name, type, openingBal, colorHex, iconName ->
                viewModel.saveAccount(
                    id = id,
                    name = name,
                    type = type,
                    openingBalance = openingBal,
                    colorHex = colorHex,
                    iconName = iconName,
                    onSuccess = { showAddEditDialog = false }
                )
            }
        )
    }

    // Delete Account Confirmation Dialog
    if (accountToDelete != null) {
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to delete '${accountToDelete?.name}'? Note: To preserve transaction history, archiving is recommended.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        accountToDelete?.let { viewModel.deleteAccount(it) }
                        accountToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_account")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Account Detail Bottom Sheet
    selectedAccountForDetail?.let { acc ->
        val accTxs = allTransactions.filter { it.accountId == acc.id || it.destinationAccountId == acc.id }
        val totalIncome = accTxs.filter { it.type == TransactionType.INCOME && it.accountId == acc.id }.sumOf { it.amount }
        val totalExpenses = accTxs.filter { it.type == TransactionType.EXPENSE && it.accountId == acc.id }.sumOf { it.amount }

        ModalBottomSheet(
            onDismissRequest = { selectedAccountForDetail = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(parseColorSafe(acc.colorHex).copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(acc.iconName),
                                contentDescription = null,
                                tint = parseColorSafe(acc.colorHex),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = acc.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = acc.accountType.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = CurrencyFormatter.format(acc.currentBalance, userPrefs.defaultCurrency, userPrefs.hideBalances),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Current Balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Total Inflow", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "+${CurrencyFormatter.format(totalIncome, userPrefs.defaultCurrency, userPrefs.hideBalances)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = IncomeColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Total Outflow", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "-${CurrencyFormatter.format(totalExpenses, userPrefs.defaultCurrency, userPrefs.hideBalances)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Account Activity (${accTxs.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (accTxs.isEmpty()) {
                    Text(
                        text = "No transactions linked to this account yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(accTxs, key = { it.id }) { tx ->
                            TransactionItemRow(
                                transaction = tx,
                                category = categoryMap[tx.categoryId],
                                sourceAccount = accountMap[tx.accountId],
                                destAccount = tx.destinationAccountId?.let { accountMap[it] },
                                currency = userPrefs.defaultCurrency,
                                hideBalances = userPrefs.hideBalances,
                                onClick = {
                                    selectedAccountForDetail = null
                                    onTransactionClick(tx.id)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun AccountItemCard(
    account: AccountEntity,
    currency: String,
    hideBalances: Boolean,
    onCardClick: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    isArchivedView: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassmorphic(
                shape = RoundedCornerShape(18.dp),
                elevation = 5.dp,
                accentBorder = parseColorSafe(account.colorHex).copy(alpha = 0.5f)
            )
            .clickable(onClick = onCardClick)
            .testTag("account_card_${account.id}")
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(parseColorSafe(account.colorHex).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(account.iconName),
                    contentDescription = null,
                    tint = parseColorSafe(account.colorHex),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = account.accountType.displayName + if (isArchivedView) " • Archived" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.format(
                        amount = account.currentBalance,
                        currencyCode = currency,
                        hideBalances = hideBalances
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = parseColorSafe(account.colorHex),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.testTag("account_menu_${account.id}")
                ) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Account Options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isArchivedView) "Unarchive" else "Archive") },
                        leadingIcon = {
                            Icon(
                                if (isArchivedView) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            showMenu = false
                            onArchive()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AddEditAccountDialog(
    account: AccountEntity?,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (id: Long, name: String, type: AccountType, openingBalance: Double, colorHex: String, iconName: String) -> Unit
) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    var selectedType by remember { mutableStateOf(account?.accountType ?: AccountType.CASH) }
    var openingBalText by remember {
        mutableStateOf(account?.openingBalance?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "0")
    }
    var selectedColor by remember { mutableStateOf(account?.colorHex ?: "#00796B") }
    var selectedIcon by remember { mutableStateOf(account?.iconName ?: "account_balance_wallet") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val colors = listOf("#00796B", "#1E88E5", "#FF8F00", "#43A047", "#5E35B1", "#E53935", "#D81B60", "#37474F")
    val icons = listOf(
        Pair("account_balance_wallet", Icons.Default.AccountBalanceWallet),
        Pair("smartphone", Icons.Default.Smartphone),
        Pair("account_balance", Icons.Default.AccountBalance),
        Pair("savings", Icons.Default.Savings),
        Pair("attach_money", Icons.Default.AttachMoney),
        Pair("credit_card", Icons.Default.CreditCard)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (account == null) "New Account" else "Edit Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Account Name") },
                    placeholder = { Text("e.g. Cash, MTN MoMo, Ecobank") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("account_name_input")
                )

                if (account == null) {
                    OutlinedTextField(
                        value = openingBalText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^-?\\d*(\\.\\d{0,2})?$"))) {
                                openingBalText = input
                            }
                        },
                        label = { Text("Opening Balance ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("account_opening_balance_input")
                    )
                }

                // Account Type Chips
                Text("Account Type", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(AccountType.values().toList()) { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.displayName) }
                        )
                    }
                }

                // Color choices
                Text("Color Accent", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parseColorSafe(hex))
                                .clickable { selectedColor = hex }
                                .padding(2.dp)
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Please enter an account name"
                        return@Button
                    }
                    val openingBal = openingBalText.toDoubleOrNull() ?: 0.0
                    onSave(account?.id ?: 0L, name.trim(), selectedType, openingBal, selectedColor, selectedIcon)
                },
                modifier = Modifier.testTag("save_account_button")
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
