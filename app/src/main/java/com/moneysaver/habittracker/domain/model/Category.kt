package com.moneysaver.habittracker.domain.model

data class Category(
    val id: Long = 0L,
    val name: String,
    val iconRes: Int = 0,
    val colorHex: String = "#4CAF50",
    val type: CategoryType = CategoryType.EXPENSE,
    val isDefault: Boolean = false
)
