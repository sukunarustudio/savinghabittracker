package com.moneysaver.habittracker.domain.usecase.category

import com.moneysaver.habittracker.domain.model.Category
import com.moneysaver.habittracker.domain.repository.CategoryRepository

class CreateCategoryUseCase(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(category: Category): Long {
        require(category.name.isNotBlank()) { "Category name cannot be blank" }
        return categoryRepository.insertCategory(category)
    }
}
