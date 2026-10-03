package com.example.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity

/**
 * 1-to-1 / N-to-1 Relationship: Transaction with its associated source account,
 * destination account (for transfers), and category.
 */
data class TransactionWithDetails(
    @Embedded
    val transaction: TransactionEntity,

    @Relation(
        parentColumn = "accountId",
        entityColumn = "id"
    )
    val account: AccountEntity?,

    @Relation(
        parentColumn = "destinationAccountId",
        entityColumn = "id"
    )
    val destinationAccount: AccountEntity?,

    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?
)

/**
 * N-to-1 Relationship: Budget with its associated spending category.
 */
data class BudgetWithCategory(
    @Embedded
    val budget: BudgetEntity,

    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?
)

/**
 * 1-to-N Relationship: Account with all its historical transactions.
 */
data class AccountWithTransactions(
    @Embedded
    val account: AccountEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "accountId"
    )
    val transactions: List<TransactionEntity>
)

/**
 * N-to-1 Relationship: Savings Goal with its optional linked funding account.
 */
data class SavingsGoalWithAccount(
    @Embedded
    val savingsGoal: SavingsGoalEntity,

    @Relation(
        parentColumn = "linkedAccountId",
        entityColumn = "id"
    )
    val linkedAccount: AccountEntity?
)
