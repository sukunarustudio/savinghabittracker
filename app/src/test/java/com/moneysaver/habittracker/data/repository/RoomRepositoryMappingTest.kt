package com.moneysaver.habittracker.data.repository

import com.moneysaver.habittracker.data.local.entity.CategoryEntity
import com.moneysaver.habittracker.data.local.entity.HabitCompletionLogEntity
import com.moneysaver.habittracker.data.local.entity.HabitEntity
import com.moneysaver.habittracker.data.local.entity.SavingsGoalEntity
import com.moneysaver.habittracker.data.local.entity.TransactionEntity
import com.moneysaver.habittracker.domain.model.Category
import com.moneysaver.habittracker.domain.model.CategoryType
import com.moneysaver.habittracker.domain.model.GoalStatus
import com.moneysaver.habittracker.domain.model.SavingsGoal
import com.moneysaver.habittracker.domain.model.Transaction
import com.moneysaver.habittracker.domain.model.TransactionType
import com.moneysaver.habittracker.domain.model.habit.FrequencyType
import com.moneysaver.habittracker.domain.model.habit.Habit
import com.moneysaver.habittracker.domain.model.habit.HabitCompletionLog
import com.moneysaver.habittracker.domain.model.habit.HabitSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class RoomRepositoryMappingTest {

    @Test
    fun testCategoryEntityMapping() {
        val entity = CategoryEntity(
            id = 5L,
            name = "Groceries",
            iconRes = "123",
            colorHex = "#FF0000",
            type = CategoryType.EXPENSE,
            isDefault = true
        )
        val domain = entity.toDomain()
        assertEquals(5L, domain.id)
        assertEquals("Groceries", domain.name)
        assertEquals(123, domain.iconRes)
        assertEquals("#FF0000", domain.colorHex)
        assertEquals(CategoryType.EXPENSE, domain.type)
        assertTrue(domain.isDefault)

        val backToEntity = domain.toEntity()
        assertEquals(entity.id, backToEntity.id)
        assertEquals(entity.name, backToEntity.name)
        assertEquals(entity.colorHex, backToEntity.colorHex)
        assertEquals(entity.type, backToEntity.type)
    }

    @Test
    fun testTransactionEntityMapping() {
        val domain = Transaction(
            id = 10L,
            amount = 5000L,
            type = TransactionType.EXPENSE,
            categoryId = 2L,
            savingsGoalId = null,
            isInternalTransfer = false,
            timestamp = 1700000000000L,
            note = "Coffee"
        )
        val entity = domain.toEntity()
        assertEquals(10L, entity.id)
        assertEquals(5000L, entity.amount)
        assertEquals(TransactionType.EXPENSE, entity.type)
        assertEquals(2L, entity.categoryId)
        assertEquals("Coffee", entity.note)

        val backToDomain = entity.toDomain()
        assertEquals(domain.id, backToDomain.id)
        assertEquals(domain.amount, backToDomain.amount)
        assertEquals(domain.type, backToDomain.type)
    }

    @Test
    fun testSavingsGoalEntityMapping() {
        val domain = SavingsGoal(
            id = 3L,
            title = "New Phone",
            targetAmount = 80000L,
            savedAmount = 40000L,
            targetDate = 1750000000000L,
            status = GoalStatus.IN_PROGRESS
        )
        val entity = domain.toEntity()
        assertEquals(3L, entity.id)
        assertEquals("New Phone", entity.title)
        assertEquals(80000L, entity.targetAmount)
        assertEquals(40000L, entity.savedAmount)

        val backToDomain = entity.toDomain()
        assertEquals(domain.id, backToDomain.id)
        assertEquals(domain.targetAmount, backToDomain.targetAmount)
        assertEquals(domain.savedAmount, backToDomain.savedAmount)
    }

    @Test
    fun testHabitEntityMapping() {
        val habit = Habit(
            id = "42",
            name = "Workout",
            schedule = HabitSchedule(
                frequencyType = FrequencyType.CUSTOM_DAYS,
                targetDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                reminderTime = LocalTime.of(7, 30)
            ),
            createdAt = LocalDate.of(2026, 1, 1),
            isArchived = false
        )

        val entity = habit.toEntity()
        assertEquals(42L, entity.id)
        assertEquals("Workout", entity.name)
        assertEquals(FrequencyType.CUSTOM_DAYS, entity.frequencyType)
        // Bitmask for Mon(1), Wed(4), Fri(16) = 21
        assertEquals(21, entity.scheduledDaysBitmask)
        // Reminder time for 07:30 = (7*3600 + 30*60)*1000 = 27,000,000L
        assertEquals(27000000L, entity.reminderTime)

        val backToDomain = entity.toDomain()
        assertEquals("42", backToDomain.id)
        assertEquals("Workout", backToDomain.name)
        assertEquals(FrequencyType.CUSTOM_DAYS, backToDomain.schedule.frequencyType)
        assertTrue(backToDomain.schedule.targetDays.contains(DayOfWeek.MONDAY))
        assertTrue(backToDomain.schedule.targetDays.contains(DayOfWeek.WEDNESDAY))
        assertTrue(backToDomain.schedule.targetDays.contains(DayOfWeek.FRIDAY))
        assertFalse(backToDomain.schedule.targetDays.contains(DayOfWeek.TUESDAY))
        assertEquals(LocalTime.of(7, 30), backToDomain.schedule.reminderTime)
    }

    @Test
    fun testHabitCompletionLogEntityMapping() {
        val log = HabitCompletionLog(
            id = "99",
            habitId = "42",
            completedDate = LocalDate.of(2026, 11, 15),
            completedAt = Instant.ofEpochMilli(1700005000000L)
        )
        val entity = log.toEntity()
        assertEquals(99L, entity.id)
        assertEquals(42L, entity.habitId)
        assertEquals("2026-11-15", entity.completedDate)
        assertEquals(1700005000000L, entity.completedAt)

        val backToDomain = entity.toDomain()
        assertEquals("99", backToDomain.id)
        assertEquals("42", backToDomain.habitId)
        assertEquals(LocalDate.of(2026, 11, 15), backToDomain.completedDate)
        assertEquals(Instant.ofEpochMilli(1700005000000L), backToDomain.completedAt)
    }
}
