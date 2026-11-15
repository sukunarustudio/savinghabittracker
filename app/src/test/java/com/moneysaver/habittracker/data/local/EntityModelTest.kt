package com.moneysaver.habittracker.data.local

import com.moneysaver.habittracker.data.local.entity.CategoryEntity
import com.moneysaver.habittracker.data.local.entity.HabitCompletionLogEntity
import com.moneysaver.habittracker.data.local.entity.HabitEntity
import com.moneysaver.habittracker.data.local.entity.SavingsGoalEntity
import com.moneysaver.habittracker.data.local.entity.TransactionEntity
import com.moneysaver.habittracker.domain.model.CategoryType
import com.moneysaver.habittracker.domain.model.GoalStatus
import com.moneysaver.habittracker.domain.model.TransactionType
import com.moneysaver.habittracker.domain.model.habit.FrequencyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityModelTest {

    @Test
    fun testCategoryEntityInstantiation() {
        val category = CategoryEntity(
            id = 1L,
            name = "Groceries",
            iconRes = "restaurant",
            colorHex = "#FF5722",
            type = CategoryType.EXPENSE,
            isDefault = true
        )
        assertEquals(1L, category.id)
        assertEquals("Groceries", category.name)
        assertEquals(CategoryType.EXPENSE, category.type)
        assertTrue(category.isDefault)
    }

    @Test
    fun testSavingsGoalEntityInstantiation() {
        val goal = SavingsGoalEntity(
            id = 2L,
            title = "Emergency Fund",
            targetAmount = 100000L,
            savedAmount = 25000L,
            targetDate = 1735689600000L,
            status = GoalStatus.IN_PROGRESS
        )
        assertEquals(2L, goal.id)
        assertEquals(100000L, goal.targetAmount)
        assertEquals(25000L, goal.savedAmount)
        assertEquals(GoalStatus.IN_PROGRESS, goal.status)
    }

    @Test
    fun testTransactionEntityInstantiation() {
        val transaction = TransactionEntity(
            id = 10L,
            amount = 4500L,
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            savingsGoalId = null,
            isInternalTransfer = false,
            timestamp = 1700000000000L,
            note = "Weekly groceries"
        )
        assertEquals(10L, transaction.id)
        assertEquals(4500L, transaction.amount)
        assertEquals(TransactionType.EXPENSE, transaction.type)
        assertEquals(1L, transaction.categoryId)
        assertNull(transaction.savingsGoalId)
        assertFalse(transaction.isInternalTransfer)
    }

    @Test
    fun testHabitEntityInstantiation() {
        val habit = HabitEntity(
            id = 3L,
            name = "Morning Run",
            frequencyType = FrequencyType.DAILY,
            scheduledDaysBitmask = 127,
            reminderTime = 25200000L,
            isArchived = false,
            archivedAt = null,
            createdAt = 1700000000000L
        )
        assertEquals(3L, habit.id)
        assertEquals("Morning Run", habit.name)
        assertEquals(FrequencyType.DAILY, habit.frequencyType)
        assertEquals(127, habit.scheduledDaysBitmask)
        assertFalse(habit.isArchived)
    }

    @Test
    fun testHabitCompletionLogEntityInstantiation() {
        val log = HabitCompletionLogEntity(
            id = 100L,
            habitId = 3L,
            completedDate = "2026-11-15",
            completedAt = 1700005000000L
        )
        assertEquals(100L, log.id)
        assertEquals(3L, log.habitId)
        assertEquals("2026-11-15", log.completedDate)
    }

    @Test
    fun testDefaultCategoriesList() {
        val defaultCategories = MoneyHabitDatabase.DEFAULT_CATEGORIES
        assertTrue(defaultCategories.isNotEmpty())
        assertTrue(defaultCategories.any { it.name == "Food & Dining" && it.type == CategoryType.EXPENSE })
        assertTrue(defaultCategories.any { it.name == "Salary" && it.type == CategoryType.INCOME })
        assertTrue(defaultCategories.all { it.isDefault })
    }
}
