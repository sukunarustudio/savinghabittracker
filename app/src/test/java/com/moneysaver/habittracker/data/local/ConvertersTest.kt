package com.moneysaver.habittracker.data.local

import com.moneysaver.habittracker.domain.model.CategoryType
import com.moneysaver.habittracker.domain.model.GoalStatus
import com.moneysaver.habittracker.domain.model.TransactionType
import com.moneysaver.habittracker.domain.model.habit.FrequencyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ConvertersTest {

    private lateinit var converters: Converters

    @Before
    fun setUp() {
        converters = Converters()
    }

    @Test
    fun testCategoryTypeConversion() {
        assertEquals("INCOME", converters.fromCategoryType(CategoryType.INCOME))
        assertEquals("EXPENSE", converters.fromCategoryType(CategoryType.EXPENSE))
        assertNull(converters.fromCategoryType(null))

        assertEquals(CategoryType.INCOME, converters.toCategoryType("INCOME"))
        assertEquals(CategoryType.EXPENSE, converters.toCategoryType("EXPENSE"))
        assertNull(converters.toCategoryType(null))
    }

    @Test
    fun testTransactionTypeConversion() {
        assertEquals("INCOME", converters.fromTransactionType(TransactionType.INCOME))
        assertEquals("EXPENSE", converters.fromTransactionType(TransactionType.EXPENSE))
        assertEquals("SAVINGS_DEPOSIT", converters.fromTransactionType(TransactionType.SAVINGS_DEPOSIT))
        assertEquals("SAVINGS_WITHDRAWAL", converters.fromTransactionType(TransactionType.SAVINGS_WITHDRAWAL))
        assertNull(converters.fromTransactionType(null))

        assertEquals(TransactionType.INCOME, converters.toTransactionType("INCOME"))
        assertEquals(TransactionType.EXPENSE, converters.toTransactionType("EXPENSE"))
        assertEquals(TransactionType.SAVINGS_DEPOSIT, converters.toTransactionType("SAVINGS_DEPOSIT"))
        assertEquals(TransactionType.SAVINGS_WITHDRAWAL, converters.toTransactionType("SAVINGS_WITHDRAWAL"))
        assertNull(converters.toTransactionType(null))
    }

    @Test
    fun testGoalStatusConversion() {
        assertEquals("IN_PROGRESS", converters.fromGoalStatus(GoalStatus.IN_PROGRESS))
        assertEquals("ACHIEVED", converters.fromGoalStatus(GoalStatus.ACHIEVED))
        assertEquals("OVERDUE", converters.fromGoalStatus(GoalStatus.OVERDUE))
        assertNull(converters.fromGoalStatus(null))

        assertEquals(GoalStatus.IN_PROGRESS, converters.toGoalStatus("IN_PROGRESS"))
        assertEquals(GoalStatus.ACHIEVED, converters.toGoalStatus("ACHIEVED"))
        assertEquals(GoalStatus.OVERDUE, converters.toGoalStatus("OVERDUE"))
        assertNull(converters.toGoalStatus(null))
    }

    @Test
    fun testFrequencyTypeConversion() {
        assertEquals("DAILY", converters.fromFrequencyType(FrequencyType.DAILY))
        assertEquals("CUSTOM_DAYS", converters.fromFrequencyType(FrequencyType.CUSTOM_DAYS))
        assertNull(converters.fromFrequencyType(null))

        assertEquals(FrequencyType.DAILY, converters.toFrequencyType("DAILY"))
        assertEquals(FrequencyType.CUSTOM_DAYS, converters.toFrequencyType("CUSTOM_DAYS"))
        assertNull(converters.toFrequencyType(null))
    }
}
