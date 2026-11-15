package com.moneysaver.habittracker.domain.repository

import com.moneysaver.habittracker.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    suspend fun insertCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
    suspend fun getCategoryById(id: Long): Category?
    fun getCategories(): Flow<List<Category>>
    suspend fun getAllCategories(): List<Category>
    suspend fun reassignCategory(oldCategoryId: Long, newCategoryId: Long)
    suspend fun countTransactionsForCategory(categoryId: Long): Int
}
