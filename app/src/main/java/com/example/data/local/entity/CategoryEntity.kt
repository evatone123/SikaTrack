package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.CategoryType

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["type"]),
        Index(value = ["isActive"]),
        Index(value = ["name"])
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: CategoryType,
    val iconName: String = "category",
    val colorHex: String = "#0288D1",
    val isDefault: Boolean = false,
    val isActive: Boolean = true
)
