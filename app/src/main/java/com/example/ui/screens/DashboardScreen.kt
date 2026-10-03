package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.model.TransactionType
import com.example.domain.CurrencyFormatter
import com.example.domain.FinancialCalculations
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.DonutPieChart
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatCard
import com.example.ui.components.TransactionItemRow
import com.example.ui.theme.EmeraldPrimaryDark
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import com.example.ui.theme.TransferColor
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToAddTransaction: (type: String) -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val userPrefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val monthlySummary by viewModel.monthlySummary.collectAsStateWithLifecycle()
    val recentTxs by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val spendings by viewModel.categorySpendings.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()

    val categoryMap = categories.associateBy { it.id }
    val accountMap = accounts.associateBy { it.id }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_cowrie_logo),
                            contentDescription = "SikaTrack Cowrie Logo",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SikaTrack",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Offline Personal Finance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val nextMode = when (userPrefs.themeMode) {
                                "LIGHT" -> "DARK"
                                "DARK" -> "SYSTEM"
                                else -> "LIGHT"
                            }
                            viewModel.setThemeMode(nextMode)
                        },
                        modifier = Modifier.testTag("dashboard_theme_toggle_button")
                    ) {
                        val icon = when (userPrefs.themeMode) {
                            "DARK" -> Icons.Default.DarkMode
                            "LIGHT" -> Icons.Default.LightMode
                            else -> Icons.Default.BrightnessMedium
                        }
                        Icon(imageVector = icon, contentDescription = "Toggle Theme: ${userPrefs.themeMode}")
                    }
                    IconButton(
                        onClick = onNavigateToReports,
                        modifier = Modifier.testTag("dashboard_reports_button")
                    ) {
                        Icon(imageVector = Icons.Default.Analytics, contentDescription = "Reports")
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("dashboard_settings_button")
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Total Net Balance Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("total_balance_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Total Net Worth",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            IconButton(
                                onClick = { viewModel.setHideBalances(!userPrefs.hideBalances) },
                                modifier = Modifier.size(32.dp).testTag("toggle_hide_balance")
                            ) {
                                Icon(
                                    imageVector = if (userPrefs.hideBalances) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Balance Visibility",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = CurrencyFormatter.format(
                                amount = monthlySummary.totalBalance,
                                currencyCode = userPrefs.defaultCurrency,
                                hideBalances = userPrefs.hideBalances
                            ),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Across ${accounts.filter { !it.isArchived }.size} active accounts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            // 2. Month Overview Metrics (Income, Expense, Saved)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Income",
                        amount = monthlySummary.incomeThisMonth,
                        currency = userPrefs.defaultCurrency,
                        hideBalances = userPrefs.hideBalances,
                        color = IncomeColor,
                        icon = Icons.Default.ArrowDownward,
                        modifier = Modifier.weight(1f).testTag("income_stat_card")
                    )

                    StatCard(
                        title = "Expenses",
                        amount = monthlySummary.expensesThisMonth,
                        currency = userPrefs.defaultCurrency,
                        hideBalances = userPrefs.hideBalances,
                        color = ExpenseColor,
                        icon = Icons.Default.ArrowUpward,
                        modifier = Modifier.weight(1f).testTag("expense_stat_card")
                    )

                    StatCard(
                        title = "Saved",
                        amount = monthlySummary.savedThisMonth,
                        currency = userPrefs.defaultCurrency,
                        hideBalances = userPrefs.hideBalances,
                        color = if (monthlySummary.savedThisMonth >= 0) IncomeColor else ExpenseColor,
                        icon = Icons.Default.Wallet,
                        modifier = Modifier.weight(1f).testTag("saved_stat_card")
                    )
                }
            }

            // 3. Quick Actions
            item {
                Column {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionButton(
                            title = "+ Income",
                            color = IncomeColor,
                            icon = Icons.Default.ArrowDownward,
                            onClick = { onNavigateToAddTransaction("INCOME") },
                            modifier = Modifier.weight(1f).testTag("quick_add_income")
                        )
                        QuickActionButton(
                            title = "+ Expense",
                            color = ExpenseColor,
                            icon = Icons.Default.ArrowUpward,
                            onClick = { onNavigateToAddTransaction("EXPENSE") },
                            modifier = Modifier.weight(1f).testTag("quick_add_expense")
                        )
                        QuickActionButton(
                            title = "Transfer",
                            color = TransferColor,
                            icon = Icons.AutoMirrored.Filled.CompareArrows,
                            onClick = { onNavigateToAddTransaction("TRANSFER") },
                            modifier = Modifier.weight(1f).testTag("quick_add_transfer")
                        )
                        QuickActionButton(
                            title = "Accounts",
                            color = MaterialTheme.colorScheme.primary,
                            icon = Icons.Default.AccountBalanceWallet,
                            onClick = onNavigateToAccounts,
                            modifier = Modifier.weight(1f).testTag("quick_view_accounts")
                        )
                    }
                }
            }

            // 4. Spending Summary Card (Donut Chart)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Spending Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${FinancialCalculations.getShortMonthName(selectedMonth)} $selectedYear",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        DonutPieChart(
                            spendings = spendings,
                            currency = userPrefs.defaultCurrency,
                            hideBalances = userPrefs.hideBalances
                        )
                    }
                }
            }

            // 5. Recent Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = onNavigateToTransactions,
                        modifier = Modifier.testTag("see_all_transactions_button")
                    ) {
                        Text("See All")
                    }
                }
            }

            // 6. Recent Transactions List
            if (recentTxs.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "No transactions yet",
                        message = "Start tracking your money by adding your first income or expense, or load demo data to explore.",
                        buttonText = "Load Demo Data",
                        onButtonClick = { viewModel.loadDemoData() }
                    )
                }
            } else {
                items(recentTxs, key = { it.id }) { tx ->
                    TransactionItemRow(
                        transaction = tx,
                        category = categoryMap[tx.categoryId],
                        sourceAccount = accountMap[tx.accountId],
                        destAccount = tx.destinationAccountId?.let { accountMap[it] },
                        currency = userPrefs.defaultCurrency,
                        hideBalances = userPrefs.hideBalances,
                        onClick = { onTransactionClick(tx.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    lineHeight = 14.sp
                ),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
