package com.moneysaver.habittracker.domain

import com.moneysaver.habittracker.domain.engine.habit.HabitStreakCalculator
import com.moneysaver.habittracker.domain.model.habit.FrequencyType
import com.moneysaver.habittracker.domain.model.habit.Habit
import com.moneysaver.habittracker.domain.model.habit.HabitSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HabitStreakCalculatorTest {

    private val calculator = HabitStreakCalculator()

    @Test
    fun dailyHabit_streakIncrementsOnContinuousCompletion() {
        val created = LocalDate.of(2026, 11, 1)
        val habit = Habit(
            name = "Morning Run",
            schedule = HabitSchedule(frequencyType = FrequencyType.DAILY),
            createdAt = created
        )
        val completed = setOf(
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 2),
            LocalDate.of(2026, 11, 3)
        )
        val today = LocalDate.of(2026, 11, 3)

        val info = calculator.calculateStreak(habit, completed, referenceDate = today)

        assertEquals(3, info.currentStreak)
        assertEquals(3, info.longestStreak)
        assertTrue(info.isCompletedToday)
        assertEquals(today, info.lastCompletedDate)
    }

    @Test
    fun dailyHabit_preservesStreakDuringTodayInProgress() {
        val created = LocalDate.of(2026, 11, 1)
        val habit = Habit(
            name = "Read Book",
            schedule = HabitSchedule(frequencyType = FrequencyType.DAILY),
            createdAt = created
        )
        // Nov 1 and Nov 2 completed; Nov 3 (today) in-progress
        val completed = setOf(
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 2)
        )
        val today = LocalDate.of(2026, 11, 3)

        val info = calculator.calculateStreak(habit, completed, referenceDate = today)

        // Previous streak of 2 is preserved while today is in progress
        assertEquals(2, info.currentStreak)
        assertEquals(2, info.longestStreak)
        assertFalse(info.isCompletedToday)
        assertEquals(LocalDate.of(2026, 11, 2), info.lastCompletedDate)
    }

    @Test
    fun dailyHabit_completingTodayIncrementsStreak_andUntogglingRevertsGracefully() {
        val created = LocalDate.of(2026, 11, 1)
        val habit = Habit(
            name = "Drink Water",
            schedule = HabitSchedule(frequencyType = FrequencyType.DAILY),
            createdAt = created
        )
        val today = LocalDate.of(2026, 11, 3)

        // 1. In-progress today
        val initialCompleted = setOf(
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 2)
        )
        val inProgressInfo = calculator.calculateStreak(habit, initialCompleted, today)
        assertEquals(2, inProgressInfo.currentStreak)
        assertEquals(2, inProgressInfo.longestStreak)

        // 2. Complete today -> streak increments to 3
        val completedToday = initialCompleted + today
        val completedInfo = calculator.calculateStreak(habit, completedToday, today)
        assertEquals(3, completedInfo.currentStreak)
        assertEquals(3, completedInfo.longestStreak)
        assertTrue(completedInfo.isCompletedToday)

        // 3. Untoggle today -> streak safely reverts to 2
        val untoggled = completedToday - today
        val untoggledInfo = calculator.calculateStreak(habit, untoggled, today)
        assertEquals(2, untoggledInfo.currentStreak)
        assertEquals(2, untoggledInfo.longestStreak)
        assertFalse(untoggledInfo.isCompletedToday)
    }

    @Test
    fun dailyHabit_resetsStreakToZero_whenScheduledDayFullyElapsedWithoutCompletion() {
        val created = LocalDate.of(2026, 11, 1)
        val habit = Habit(
            name = "Meditate",
            schedule = HabitSchedule(frequencyType = FrequencyType.DAILY),
            createdAt = created
        )
        // Nov 1 completed; Nov 2 missed (elapsed!); Nov 3 is today
        val completed = setOf(
            LocalDate.of(2026, 11, 1)
        )
        val today = LocalDate.of(2026, 11, 3)

        val info = calculator.calculateStreak(habit, completed, referenceDate = today)

        // Nov 2 elapsed without completion -> current streak resets to 0
        assertEquals(0, info.currentStreak)
        // Historical longest streak of 1 is preserved
        assertEquals(1, info.longestStreak)

        // When today (Nov 3) is completed, new streak starts at 1
        val completedToday = completed + today
        val infoAfterToday = calculator.calculateStreak(habit, completedToday, referenceDate = today)
        assertEquals(1, infoAfterToday.currentStreak)
        assertEquals(1, infoAfterToday.longestStreak)
    }

    @Test
    fun customDayHabit_ignoresUnscheduledDays_andPreservesStreak() {
        // Mon, Wed, Fri habit
        val created = LocalDate.of(2026, 11, 2) // Monday
        val habit = Habit(
            name = "Gym Workout",
            schedule = HabitSchedule(
                frequencyType = FrequencyType.CUSTOM_DAYS,
                targetDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
            ),
            createdAt = created
        )
        val completedMon = setOf(LocalDate.of(2026, 11, 2))

        // Tuesday Nov 3 is unscheduled: must not reset streak
        val tuesday = LocalDate.of(2026, 11, 3)
        val tuesdayInfo = calculator.calculateStreak(habit, completedMon, referenceDate = tuesday)
        assertEquals(1, tuesdayInfo.currentStreak)
        assertEquals(1, tuesdayInfo.longestStreak)
        assertFalse(tuesdayInfo.isCompletedToday)

        // Wednesday Nov 4 is scheduled, in-progress before completion: preserves streak of 1
        val wednesday = LocalDate.of(2026, 11, 4)
        val wednesdayInProgress = calculator.calculateStreak(habit, completedMon, referenceDate = wednesday)
        assertEquals(1, wednesdayInProgress.currentStreak)
        assertEquals(1, wednesdayInProgress.longestStreak)

        // Complete Wednesday: streak becomes 2
        val completedWed = completedMon + wednesday
        val wednesdayCompleted = calculator.calculateStreak(habit, completedWed, referenceDate = wednesday)
        assertEquals(2, wednesdayCompleted.currentStreak)
        assertEquals(2, wednesdayCompleted.longestStreak)

        // Thursday Nov 5 is unscheduled: preserves streak of 2
        val thursday = LocalDate.of(2026, 11, 5)
        val thursdayInfo = calculator.calculateStreak(habit, completedWed, referenceDate = thursday)
        assertEquals(2, thursdayInfo.currentStreak)
        assertEquals(2, thursdayInfo.longestStreak)
    }

    @Test
    fun customDayHabit_missedScheduledDay_resetsStreakToZero() {
        // Mon, Wed, Fri habit
        val created = LocalDate.of(2026, 11, 2) // Monday
        val habit = Habit(
            name = "Gym Workout",
            schedule = HabitSchedule(
                frequencyType = FrequencyType.CUSTOM_DAYS,
                targetDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
            ),
            createdAt = created
        )
        // Mon completed; Wed Nov 4 missed (elapsed!)
        val completed = setOf(LocalDate.of(2026, 11, 2))

        // Friday Nov 6 in-progress: Wed fully elapsed without completion -> current streak is 0
        val friday = LocalDate.of(2026, 11, 6)
        val fridayInProgress = calculator.calculateStreak(habit, completed, referenceDate = friday)
        assertEquals(0, fridayInProgress.currentStreak)
        assertEquals(1, fridayInProgress.longestStreak)

        // Complete Friday: starts new streak of 1
        val completedFri = completed + friday
        val fridayCompleted = calculator.calculateStreak(habit, completedFri, referenceDate = friday)
        assertEquals(1, fridayCompleted.currentStreak)
        assertEquals(1, fridayCompleted.longestStreak)
    }

    @Test
    fun prospectiveScheduleChange_preservesHistoricalStreaksWithoutInvalidation() {
        // Habit created Nov 1 (Sunday) as DAILY
        val created = LocalDate.of(2026, 11, 1)
        val initialSchedule = HabitSchedule(
            frequencyType = FrequencyType.DAILY,
            effectiveFrom = created
        )
        val habit = Habit(
            name = "Yoga",
            schedule = initialSchedule,
            createdAt = created
        )
        // Completed Nov 1 (Sun), Nov 2 (Mon), Nov 3 (Tue), Nov 4 (Wed)
        val completedDays = setOf(
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 2),
            LocalDate.of(2026, 11, 3),
            LocalDate.of(2026, 11, 4)
        )

        // On Nov 5 (Thu), schedule prospectively changes to Mon, Wed, Fri
        val newSchedule = HabitSchedule(
            frequencyType = FrequencyType.CUSTOM_DAYS,
            targetDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            effectiveFrom = LocalDate.of(2026, 11, 5)
        )
        val updatedHabit = habit.copy(
            schedule = newSchedule,
            scheduleHistory = listOf(initialSchedule)
        )

        // Thursday Nov 5 is NOT scheduled under the new schedule -> streak is 4 (not broken)
        val thursday = LocalDate.of(2026, 11, 5)
        val thuInfo = calculator.calculateStreak(updatedHabit, completedDays, referenceDate = thursday)
        assertEquals(4, thuInfo.currentStreak)
        assertEquals(4, thuInfo.longestStreak)

        // Friday Nov 6 is scheduled under the new schedule:
        // Before completion (in-progress): preserves streak of 4
        val friday = LocalDate.of(2026, 11, 6)
        val friInProgress = calculator.calculateStreak(updatedHabit, completedDays, referenceDate = friday)
        assertEquals(4, friInProgress.currentStreak)
        assertEquals(4, friInProgress.longestStreak)

        // Complete Friday: streak increments to 5!
        val completedWithFri = completedDays + friday
        val friCompleted = calculator.calculateStreak(updatedHabit, completedWithFri, referenceDate = friday)
        assertEquals(5, friCompleted.currentStreak)
        assertEquals(5, friCompleted.longestStreak)
    }

    @Test
    fun habitCreatedToday_handlesBoundaryConditions() {
        val today = LocalDate.of(2026, 11, 15)
        val habit = Habit(
            name = "Journaling",
            schedule = HabitSchedule(frequencyType = FrequencyType.DAILY),
            createdAt = today
        )

        // Before completion
        val before = calculator.calculateStreak(habit, emptySet(), referenceDate = today)
        assertEquals(0, before.currentStreak)
        assertEquals(0, before.longestStreak)
        assertFalse(before.isCompletedToday)

        // After completion
        val after = calculator.calculateStreak(habit, setOf(today), referenceDate = today)
        assertEquals(1, after.currentStreak)
        assertEquals(1, after.longestStreak)
        assertTrue(after.isCompletedToday)

        // Reference date before createdAt
        val beforeCreated = calculator.calculateStreak(habit, setOf(today), referenceDate = today.minusDays(1))
        assertEquals(0, beforeCreated.currentStreak)
        assertEquals(0, beforeCreated.longestStreak)
    }

    @Test
    fun longestStreak_preservedAcrossMultipleStreakCycles() {
        val created = LocalDate.of(2026, 11, 1)
        val habit = Habit(
            name = "Floss",
            schedule = HabitSchedule(frequencyType = FrequencyType.DAILY),
            createdAt = created
        )
        // Streak 1: Nov 1..Nov 5 (5 days)
        // Nov 6 missed
        // Streak 2: Nov 7..Nov 8 (2 days)
        val completed = setOf(
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 11, 2),
            LocalDate.of(2026, 11, 3),
            LocalDate.of(2026, 11, 4),
            LocalDate.of(2026, 11, 5),
            LocalDate.of(2026, 11, 7),
            LocalDate.of(2026, 11, 8)
        )
        val today = LocalDate.of(2026, 11, 8)

        val info = calculator.calculateStreak(habit, completed, referenceDate = today)
        assertEquals(2, info.currentStreak)
        assertEquals(5, info.longestStreak)
    }
}
