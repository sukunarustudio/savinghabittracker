package com.moneysaver.habittracker.domain.usecase.category

import com.moneysaver.habittracker.domain.exception.EntityNotFoundException
import com.moneysaver.habittracker.domain.repository.CategoryRepository

class DeleteCategoryWithReassignmentUseCase(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(
        categoryIdToDelete: Long,
        targetCategoryId: Long
    ) {
        require(categoryIdToDelete != targetCategoryId) {
            "Target category must be different from category to delete"
        }

        val categoryToDelete = categoryRepository.getCategoryById(categoryIdToDelete)
            ?: throw EntityNotFoundException("Category to delete with ID $categoryIdToDelete not found")

        val targetCategory = categoryRepository.getCategoryById(targetCategoryId)
            ?: throw EntityNotFoundException("Target category with ID $targetCategoryId not found")

        require(categoryToDelete.type == targetCategory.type) {
            "Cannot reassign category of type ${categoryToDelete.type} to category of type ${targetCategory.type}"
        }

        // 1. Reassign linked transactions
        categoryRepository.reassignCategory(
            oldCategoryId = categoryIdToDelete,
            newCategoryId = targetCategoryId
        )

        // 2. Delete the category
        categoryRepository.deleteCategory(categoryToDelete)
    }
}
