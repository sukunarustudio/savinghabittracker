package com.moneysaver.habittracker.domain

import com.moneysaver.habittracker.domain.model.habit.FrequencyType
import com.moneysaver.habittracker.domain.model.habit.HabitSchedule
import com.moneysaver.habittracker.domain.repository.FakeHabitRepository
import com.moneysaver.habittracker.domain.usecase.habit.ArchiveHabitUseCase
import com.moneysaver.habittracker.domain.usecase.habit.CreateHabitUseCase
import com.moneysaver.habittracker.domain.usecase.habit.DeleteHabitUseCase
import com.moneysaver.habittracker.domain.usecase.habit.GetDailyHabitsUseCase
import com.moneysaver.habittracker.domain.usecase.habit.ToggleHabitCompletionUseCase
import com.moneysaver.habittracker.domain.usecase.habit.UpdateHabitScheduleUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HabitUseCasesTest {

    private lateinit var repository: FakeHabitRepository
    private lateinit var createHabitUseCase: CreateHabitUseCase
    private lateinit var updateHabitScheduleUseCase: UpdateHabitScheduleUseCase
    private lateinit var archiveHabitUseCase: ArchiveHabitUseCase
    private lateinit var deleteHabitUseCase: DeleteHabitUseCase
    private lateinit var toggleHabitCompletionUseCase: ToggleHabitCompletionUseCase
    private lateinit var getDailyHabitsUseCase: GetDailyHabitsUseCase

    @Before
    fun setUp() {
        repository = FakeHabitRepository()
        createHabitUseCase = CreateHabitUseCase(repository)
        updateHabitScheduleUseCase = UpdateHabitScheduleUseCase(repository)
        archiveHabitUseCase = ArchiveHabitUseCase(repository)
        deleteHabitUseCase = DeleteHabitUseCase(repository)
        toggleHabitCompletionUseCase = ToggleHabitCompletionUseCase(repository)
        getDailyHabitsUseCase = GetDailyHabitsUseCase(repository)
    }

    @Test
    fun createHabit_persistsValidDailyAndCustomDayHabits() = runTest {
        // Daily habit
        val daily = createHabitUseCase(
            name = "Morning Stretch",
            description = "10 minutes of yoga",
            schedule = HabitSchedule(frequencyType = FrequencyType.DAILY),
            createdAt = LocalDate.of(2026, 11, 1)
        )
        assertNotNull(daily.id)
        assertEquals("Morning Stretch", daily.name)
        assertEquals(FrequencyType.DAILY, daily.schedule.frequencyType)
        assertEquals(daily, repository.getHabitById(daily.id))

        // Custom days habit
        val custom = createHabitUseCase(
            name = "Weightlifting",
            schedule = HabitSchedule(
                frequencyType = FrequencyType.CUSTOM_DAYS,
                targetDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)
            )
        )
        assertEquals(FrequencyType.CUSTOM_DAYS, custom.schedule.frequencyType)
        assertEquals(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), custom.schedule.targetDays)
    }

    @Test
    fun createHabit_validatesInputs() = runTest {
        // Blank name
        assertThrows(IllegalArgumentException::class.java) {
            runTest {
                createHabitUseCase(name = "   ")
            }
        }

        // Empty target days for CUSTOM_DAYS
        assertThrows(IllegalArgumentException::class.java) {
            runTest {
                createHabitUseCase(
                    name = "Invalid Habit",
                    schedule = HabitSchedule(
                        frequencyType = FrequencyType.CUSTOM_DAYS,
                        targetDays = emptySet()
                    )
                )
            }
        }
    }

    @Test
    fun toggleHabitCompletion_togglesAndUntogglesSafely() = runTest {
        val today = LocalDate.of(2026, 11, 15)
        val habit = createHabitUseCase(name = "Read", createdAt = today)

        // 1. Toggle completion -> returns true (completed)
        val isCompleted = toggleHabitCompletionUseCase(habit.id, today, today = today)
        assertTrue(isCompleted)
        val log = repository.getCompletionLog(habit.id, today)
        assertNotNull(log)
        assertEquals(today, log?.completedDate)

        // 2. Untoggle completion -> returns false (uncompleted)
        val isUntoggled = toggleHabitCompletionUseCase(habit.id, today, today = today)
        assertFalse(isUntoggled)
        val logAfterUntoggle = repository.getCompletionLog(habit.id, today)
        assertNull(logAfterUntoggle)
    }

    @Test
    fun toggleHabitCompletion_strictlyRejectsFutureDates() = runTest {
        val today = LocalDate.of(2026, 11, 15)
        val futureDate = today.plusDays(1)
        val habit = createHabitUseCase(name = "Code Review", createdAt = today)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runTest {
                toggleHabitCompletionUseCase(habit.id, futureDate, today = today)
            }
        }
        assertTrue(exception.message?.contains("Future date completion is strictly disallowed") == true)
    }

    @Test
    fun toggleHabitCompletion_idempotentSetCompletion() = runTest {
        val today = LocalDate.of(2026, 11, 15)
        val habit = createHabitUseCase(name = "Drink Water", createdAt = today)

        // Set true twice
        val firstSet = toggleHabitCompletionUseCase.setCompletion(habit.id, today, true, today)
        assertTrue(firstSet)
        val secondSet = toggleHabitCompletionUseCase.setCompletion(habit.id, today, true, today)
        assertTrue(secondSet)

        // Only one log should exist
        val logs = repository.getCompletionLogsOnce(habit.id)
        assertEquals(1, logs.size)

        // Set false twice
        val firstUnset = toggleHabitCompletionUseCase.setCompletion(habit.id, today, false, today)
        assertFalse(firstUnset)
        val secondUnset = toggleHabitCompletionUseCase.setCompletion(habit.id, today, false, today)
        assertFalse(secondUnset)
        val logsAfter = repository.getCompletionLogsOnce(habit.id)
        assertEquals(0, logsAfter.size)
    }

    @Test
    fun updateHabitSchedule_updatesProspectivelyAndPreservesHistory() = runTest {
        val created = LocalDate.of(2026, 11, 1)
        val habit = createHabitUseCase(
            name = "Running",
            schedule = HabitSchedule(frequencyType = FrequencyType.DAILY, effectiveFrom = created),
            createdAt = created
        )

        val newEffectiveDate = LocalDate.of(2026, 11, 10)
        val newSchedule = HabitSchedule(
            frequencyType = FrequencyType.CUSTOM_DAYS,
            targetDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            effectiveFrom = newEffectiveDate
        )

        val updated = updateHabitScheduleUseCase(habit.id, newSchedule, effectiveFrom = newEffectiveDate)

        assertEquals(FrequencyType.CUSTOM_DAYS, updated.schedule.frequencyType)
        assertEquals(1, updated.scheduleHistory.size)
        assertEquals(FrequencyType.DAILY, updated.scheduleHistory[0].frequencyType)
    }

    @Test
    fun archiveHabit_hidesFromSubsequentChecklistDates_whilePreservingHistoricalLogs() = runTest {
        val created = LocalDate.of(2026, 11, 1)
        val habit = createHabitUseCase(name = "Daily Journal", createdAt = created)

        // Log completion on Nov 5
        val logDate = LocalDate.of(2026, 11, 5)
        toggleHabitCompletionUseCase(habit.id, logDate, today = logDate)
        assertEquals(1, repository.getCompletionLogsOnce(habit.id).size)

        // Archive habit on Nov 10
        val archiveDate = LocalDate.of(2026, 11, 10)
        archiveHabitUseCase(habit.id, archivedAt = archiveDate)

        val archivedHabit = repository.getHabitById(habit.id)
        assertNotNull(archivedHabit)
        assertTrue(archivedHabit!!.isArchived)
        assertEquals(archiveDate, archivedHabit.archivedAt)

        // Historical completion log is preserved!
        val logs = repository.getCompletionLogsOnce(habit.id)
        assertEquals(1, logs.size)
        assertEquals(logDate, logs[0].completedDate)

        // Checklist on Nov 9 (prior to archive date): habit is visible
        val habitsBefore = getDailyHabitsUseCase(LocalDate.of(2026, 11, 9)).first()
        assertTrue(habitsBefore.any { it.id == habit.id })

        // Checklist on Nov 10 (archive date): habit is hidden
        val habitsOnArchive = getDailyHabitsUseCase(LocalDate.of(2026, 11, 10)).first()
        assertFalse(habitsOnArchive.any { it.id == habit.id })

        // Checklist on Nov 11 (subsequent date): habit is hidden
        val habitsAfter = getDailyHabitsUseCase(LocalDate.of(2026, 11, 11)).first()
        assertFalse(habitsAfter.any { it.id == habit.id })
    }

    @Test
    fun getDailyHabits_filtersCorrectly() = runTest {
        val monday = LocalDate.of(2026, 11, 2) // Monday
        val tuesday = LocalDate.of(2026, 11, 3) // Tuesday

        // Habit 1: Daily, created on Monday
        val dailyHabit = createHabitUseCase(
            name = "Daily Walk",
            schedule = HabitSchedule(frequencyType = FrequencyType.DAILY),
            createdAt = monday
        )

        // Habit 2: Mon/Wed only, created on Monday
        val mwHabit = createHabitUseCase(
            name = "Mon-Wed Session",
            schedule = HabitSchedule(
                frequencyType = FrequencyType.CUSTOM_DAYS,
                targetDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
            ),
            createdAt = monday
        )

        // Habit 3: Daily, created in the future (Tuesday)
        val futureHabit = createHabitUseCase(
            name = "Future Goal",
            schedule = HabitSchedule(frequencyType = FrequencyType.DAILY),
            createdAt = tuesday
        )

        // Checklist for Monday:
        // - dailyHabit: visible (created Mon <= Mon, scheduled Mon)
        // - mwHabit: visible (created Mon <= Mon, scheduled Mon)
        // - futureHabit: hidden (created Tue > Mon)
        val mondayChecklist = getDailyHabitsUseCase(monday).first()
        assertEquals(2, mondayChecklist.size)
        assertTrue(mondayChecklist.any { it.id == dailyHabit.id })
        assertTrue(mondayChecklist.any { it.id == mwHabit.id })
        assertFalse(mondayChecklist.any { it.id == futureHabit.id })

        // Checklist for Tuesday:
        // - dailyHabit: visible (scheduled Tue)
        // - mwHabit: hidden (Tue not in Mon/Wed)
        // - futureHabit: visible (created Tue <= Tue, scheduled Tue)
        val tuesdayChecklist = getDailyHabitsUseCase(tuesday).first()
        assertEquals(2, tuesdayChecklist.size)
        assertTrue(tuesdayChecklist.any { it.id == dailyHabit.id })
        assertFalse(tuesdayChecklist.any { it.id == mwHabit.id })
        assertTrue(tuesdayChecklist.any { it.id == futureHabit.id })
    }

    @Test
    fun deleteHabit_removesHabitAndLogsFromRepository() = runTest {
        val today = LocalDate.of(2026, 11, 15)
        val habit = createHabitUseCase(name = "Temp Habit", createdAt = today)
        toggleHabitCompletionUseCase(habit.id, today, today = today)

        deleteHabitUseCase(habit.id)

        assertNull(repository.getHabitById(habit.id))
        assertEquals(0, repository.getCompletionLogsOnce(habit.id).size)
    }
}
