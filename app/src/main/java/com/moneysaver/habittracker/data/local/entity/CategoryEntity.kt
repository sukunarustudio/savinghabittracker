package com.moneysaver.habittracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.moneysaver.habittracker.domain.model.CategoryType

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["type"]),
        Index(value = ["name"])
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val iconRes: String,
    val colorHex: String,
    val type: CategoryType,
    val isDefault: Boolean = false
)
