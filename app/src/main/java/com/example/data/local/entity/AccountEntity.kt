package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.AccountType

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["isArchived"]),
        Index(value = ["isActive"]),
        Index(value = ["accountType"])
    ]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val accountType: AccountType,
    val openingBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val currency: String = "GH₵",
    val colorHex: String = "#00796B",
    val iconName: String = "wallet",
    val isActive: Boolean = true,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
