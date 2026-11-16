package com.moneysaver.habittracker.data.repository

import com.moneysaver.habittracker.data.local.dao.CategoryDao
import com.moneysaver.habittracker.data.local.entity.CategoryEntity
import com.moneysaver.habittracker.domain.model.Category
import com.moneysaver.habittracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    iconRes = iconRes.toIntOrNull() ?: 0,
    colorHex = colorHex,
    type = type,
    isDefault = isDefault
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    iconRes = iconRes.toString(),
    colorHex = colorHex,
    type = type,
    isDefault = isDefault
)

class RoomCategoryRepository(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override suspend fun insertCategory(category: Category): Long {
        return categoryDao.insert(category.toEntity())
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.update(category.toEntity())
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.delete(category.toEntity())
    }

    override suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getCategoryById(id)?.toDomain()
    }

    override fun getCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAllCategories(): List<Category> {
        return categoryDao.getAllCategoriesList().map { it.toDomain() }
    }

    override suspend fun reassignCategory(oldCategoryId: Long, newCategoryId: Long) {
        categoryDao.reassignTransactions(oldCategoryId, newCategoryId)
    }

    override suspend fun countTransactionsForCategory(categoryId: Long): Int {
        return categoryDao.countLinkedTransactions(categoryId)
    }
}
