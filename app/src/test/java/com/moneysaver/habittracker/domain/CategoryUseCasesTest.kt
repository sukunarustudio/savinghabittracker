package com.moneysaver.habittracker.domain

import com.moneysaver.habittracker.domain.exception.EntityNotFoundException
import com.moneysaver.habittracker.domain.fake.FakeCategoryRepository
import com.moneysaver.habittracker.domain.model.Category
import com.moneysaver.habittracker.domain.model.CategoryType
import com.moneysaver.habittracker.domain.usecase.category.CreateCategoryUseCase
import com.moneysaver.habittracker.domain.usecase.category.DeleteCategoryWithReassignmentUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class CategoryUseCasesTest {

    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var createCategoryUseCase: CreateCategoryUseCase
    private lateinit var deleteCategoryWithReassignmentUseCase: DeleteCategoryWithReassignmentUseCase

    @Before
    fun setUp() {
        categoryRepository = FakeCategoryRepository()
        createCategoryUseCase = CreateCategoryUseCase(categoryRepository)
        deleteCategoryWithReassignmentUseCase = DeleteCategoryWithReassignmentUseCase(categoryRepository)
    }

    @Test
    fun `createCategory validates blank name`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                createCategoryUseCase(Category(name = ""))
            }
        }

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                createCategoryUseCase(Category(name = "   "))
            }
        }
    }

    @Test
    fun `createCategory saves category with ID`() = runTest {
        val catId = createCategoryUseCase(
            Category(name = "Groceries", colorHex = "#FF5722", type = CategoryType.EXPENSE)
        )
        val created = categoryRepository.getCategoryById(catId)
        assertNotNull(created)
        assertEquals("Groceries", created?.name)
        assertEquals(CategoryType.EXPENSE, created?.type)
    }

    @Test
    fun `deleteCategoryWithReassignment rejects same target category`() = runTest {
        val catId = createCategoryUseCase(Category(name = "Dining", type = CategoryType.EXPENSE))
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                deleteCategoryWithReassignmentUseCase(
                    categoryIdToDelete = catId,
                    targetCategoryId = catId
                )
            }
        }
    }

    @Test
    fun `deleteCategoryWithReassignment rejects missing categories`() = runTest {
        val cat1 = createCategoryUseCase(Category(name = "Dining", type = CategoryType.EXPENSE))

        assertThrows(EntityNotFoundException::class.java) {
            kotlinx.coroutines.runBlocking {
                deleteCategoryWithReassignmentUseCase(
                    categoryIdToDelete = 999L,
                    targetCategoryId = cat1
                )
            }
        }

        assertThrows(EntityNotFoundException::class.java) {
            kotlinx.coroutines.runBlocking {
                deleteCategoryWithReassignmentUseCase(
                    categoryIdToDelete = cat1,
                    targetCategoryId = 999L
                )
            }
        }
    }

    @Test
    fun `deleteCategoryWithReassignment rejects type mismatch`() = runTest {
        val expenseCat = createCategoryUseCase(Category(name = "Dining", type = CategoryType.EXPENSE))
        val incomeCat = createCategoryUseCase(Category(name = "Salary", type = CategoryType.INCOME))

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                deleteCategoryWithReassignmentUseCase(
                    categoryIdToDelete = expenseCat,
                    targetCategoryId = incomeCat
                )
            }
        }
    }

    @Test
    fun `deleteCategoryWithReassignment successfully reassigns and deletes`() = runTest {
        val oldCatId = createCategoryUseCase(Category(name = "Fast Food", type = CategoryType.EXPENSE))
        val newCatId = createCategoryUseCase(Category(name = "General Food", type = CategoryType.EXPENSE))

        deleteCategoryWithReassignmentUseCase(
            categoryIdToDelete = oldCatId,
            targetCategoryId = newCatId
        )

        // Old category is deleted
        assertNull(categoryRepository.getCategoryById(oldCatId))
        // Target category is intact
        assertNotNull(categoryRepository.getCategoryById(newCatId))
        // Reassignment event logged
        assertEquals(1, categoryRepository.reassignedEvents.size)
        assertEquals(oldCatId to newCatId, categoryRepository.reassignedEvents.first())
    }
}
