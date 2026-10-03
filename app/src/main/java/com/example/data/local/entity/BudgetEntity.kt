package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["categoryId", "month", "year"], unique = true),
        Index(value = ["month", "year"]),
        Index(value = ["categoryId"])
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val amount: Double,
    val month: Int, // 1 to 12
    val year: Int,  // e.g. 2026
    val createdAt: Long = System.currentTimeMillis()
)
