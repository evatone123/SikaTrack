package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.BudgetDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.SavingsGoalDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.CategoryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        SavingsGoalEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE savings_goals ADD COLUMN linkedAccountId INTEGER DEFAULT NULL")
                } catch (_: Exception) {}

                try {
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_savings_goals_isCompleted ON savings_goals(isCompleted)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_savings_goals_linkedAccountId ON savings_goals(linkedAccountId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_isArchived ON accounts(isArchived)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_isActive ON accounts(isActive)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_accountType ON accounts(accountType)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_categories_type ON categories(type)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_categories_isActive ON categories(isActive)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_categories_name ON categories(name)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_month_year ON budgets(month, year)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_categoryId ON budgets(categoryId)")
                } catch (_: Exception) {}
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pocket_ledger_database.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Initial seeding will be safely verified in ViewModel init
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

suspend fun seedDefaultCategories(dao: CategoryDao) {
    if (dao.getCategoryCount() > 0) return

    val expenseCategories = listOf(
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

    val incomeCategories = listOf(
        CategoryEntity(name = "Salary", type = CategoryType.INCOME, iconName = "payments", colorHex = "#2E7D32", isDefault = true),
        CategoryEntity(name = "Business", type = CategoryType.INCOME, iconName = "storefront", colorHex = "#00796B", isDefault = true),
        CategoryEntity(name = "Freelance & Gigs", type = CategoryType.INCOME, iconName = "laptop", colorHex = "#1565C0", isDefault = true),
        CategoryEntity(name = "Investment", type = CategoryType.INCOME, iconName = "trending_up", colorHex = "#6A1B9A", isDefault = true),
        CategoryEntity(name = "Gift", type = CategoryType.INCOME, iconName = "card_giftcard", colorHex = "#AD1457", isDefault = true),
        CategoryEntity(name = "Other Income", type = CategoryType.INCOME, iconName = "account_balance_wallet", colorHex = "#37474F", isDefault = true)
    )

    dao.insertCategories(expenseCategories + incomeCategories)
}

suspend fun seedDefaultAccounts(dao: AccountDao) {
    if (dao.getAllAccountsSync().isNotEmpty()) return

    val defaultAccounts = listOf(
        AccountEntity(name = "Cash", accountType = AccountType.CASH, openingBalance = 0.0, currentBalance = 0.0, colorHex = "#43A047", iconName = "attach_money"),
        AccountEntity(name = "Mobile Money", accountType = AccountType.MOBILE_MONEY, openingBalance = 0.0, currentBalance = 0.0, colorHex = "#FF8F00", iconName = "smartphone"),
        AccountEntity(name = "Bank Account", accountType = AccountType.BANK, openingBalance = 0.0, currentBalance = 0.0, colorHex = "#1E88E5", iconName = "account_balance")
    )
    dao.insertAccounts(defaultAccounts)
}
