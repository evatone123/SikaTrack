package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.CategoryType
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FinanceRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("SikaTrack", appName)
    }

    @Test
    fun `test transaction impact on account balance`() = runBlocking {
        // Create Cash Account with 100 opening balance
        val accountId = repository.createAccount(
            AccountEntity(
                name = "Cash",
                accountType = AccountType.CASH,
                openingBalance = 100.0,
                currentBalance = 100.0
            )
        )

        val accInitial = repository.getAccountById(accountId).first()
        assertEquals(100.0, accInitial?.currentBalance ?: 0.0, 0.001)

        // Add Income of 50
        val incomeTxId = repository.createTransaction(
            TransactionEntity(
                type = TransactionType.INCOME,
                amount = 50.0,
                accountId = accountId,
                description = "Side Income"
            )
        )

        val accAfterIncome = repository.getAccountById(accountId).first()
        assertEquals(150.0, accAfterIncome?.currentBalance ?: 0.0, 0.001)

        // Add Expense of 30
        val expenseTxId = repository.createTransaction(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 30.0,
                accountId = accountId,
                description = "Groceries"
            )
        )

        val accAfterExpense = repository.getAccountById(accountId).first()
        assertEquals(120.0, accAfterExpense?.currentBalance ?: 0.0, 0.001)

        // Delete Expense
        repository.deleteTransaction(expenseTxId)
        val accAfterExpenseDelete = repository.getAccountById(accountId).first()
        assertEquals(150.0, accAfterExpenseDelete?.currentBalance ?: 0.0, 0.001)
    }

    @Test
    fun `test transfer updates both source and destination accounts`() = runBlocking {
        val bankId = repository.createAccount(
            AccountEntity(
                name = "Bank",
                accountType = AccountType.BANK,
                openingBalance = 1000.0,
                currentBalance = 1000.0
            )
        )
        val momoId = repository.createAccount(
            AccountEntity(
                name = "MoMo",
                accountType = AccountType.MOBILE_MONEY,
                openingBalance = 200.0,
                currentBalance = 200.0
            )
        )

        // Transfer 300 from Bank to MoMo
        val transferTxId = repository.createTransaction(
            TransactionEntity(
                type = TransactionType.TRANSFER,
                amount = 300.0,
                accountId = bankId,
                destinationAccountId = momoId,
                description = "Bank to MoMo"
            )
        )

        val bankAfter = repository.getAccountById(bankId).first()
        val momoAfter = repository.getAccountById(momoId).first()

        assertEquals(700.0, bankAfter?.currentBalance ?: 0.0, 0.001)
        assertEquals(500.0, momoAfter?.currentBalance ?: 0.0, 0.001)

        // Reversal by deleting transfer
        repository.deleteTransaction(transferTxId)

        val bankReversed = repository.getAccountById(bankId).first()
        val momoReversed = repository.getAccountById(momoId).first()

        assertEquals(1000.0, bankReversed?.currentBalance ?: 0.0, 0.001)
        assertEquals(200.0, momoReversed?.currentBalance ?: 0.0, 0.001)
    }

    @Test
    fun `test backup manager generates valid JSON`() = runBlocking {
        repository.createAccount(
            AccountEntity(name = "Wallet", accountType = AccountType.CASH, openingBalance = 50.0, currentBalance = 50.0)
        )
        repository.createCategory(
            CategoryEntity(name = "Snacks", type = CategoryType.EXPENSE)
        )

        val backupManager = BackupManager(context, repository)
        val json = backupManager.createBackupJson()

        assertNotNull(json)
        assertTrue(json.contains("SikaTrack"))
        assertTrue(json.contains("Wallet"))
        assertTrue(json.contains("Snacks"))
    }
}
