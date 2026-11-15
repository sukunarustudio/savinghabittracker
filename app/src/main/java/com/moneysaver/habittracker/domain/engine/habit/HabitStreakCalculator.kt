package com.moneysaver.habittracker.domain.engine.habit

import com.moneysaver.habittracker.domain.model.habit.Habit
import com.moneysaver.habittracker.domain.model.habit.HabitCompletionLog
import com.moneysaver.habittracker.domain.model.habit.HabitStreakInfo
import java.time.LocalDate

class HabitStreakCalculator {

    /**
     * Calculates streak information for a habit from completion logs.
     */
    fun calculateStreak(
        habit: Habit,
        logs: List<HabitCompletionLog>,
        referenceDate: LocalDate = LocalDate.now()
    ): HabitStreakInfo {
        val completedDates = logs.map { it.completedDate }.toSet()
        return calculateStreak(habit, completedDates, referenceDate)
    }

    /**
     * Calculates streak information for a habit given a set of completed dates.
     *
     * Invariants:
     * 1. Streaks evaluate consistency strictly against scheduled days starting from [Habit.createdAt].
     * 2. Unscheduled days do not reset streaks and are ignored.
     * 3. Today in-progress preserves previous streak before completion.
     * 4. Increments streak when today is completed; reverts gracefully on untoggle.
     * 5. Resets streak to 0 only when a scheduled day has fully elapsed without completion.
     * 6. Prospective schedule modifications apply from their effective date without
     *    retroactively invalidating historical streaks.
     */
    fun calculateStreak(
        habit: Habit,
        completedDates: Set<LocalDate>,
        referenceDate: LocalDate = LocalDate.now()
    ): HabitStreakInfo {
        if (habit.createdAt.isAfter(referenceDate)) {
            return HabitStreakInfo(
                currentStreak = 0,
                longestStreak = 0,
                lastCompletedDate = null,
                isCompletedToday = false
            )
        }

        // 1. Collect all scheduled dates from createdAt up to referenceDate
        val scheduledDates = generateSequence(habit.createdAt) { it.plusDays(1) }
            .takeWhile { !it.isAfter(referenceDate) }
            .filter { habit.isScheduledOn(it) }
            .toList()

        if (scheduledDates.isEmpty()) {
            return HabitStreakInfo(
                currentStreak = 0,
                longestStreak = 0,
                lastCompletedDate = null,
                isCompletedToday = false
            )
        }

        val isTodayScheduled = habit.isScheduledOn(referenceDate)
        val isCompletedToday = isTodayScheduled && completedDates.contains(referenceDate)
        val lastCompleted = scheduledDates.filter { completedDates.contains(it) }.lastOrNull()

        // 2. Scheduled days strictly before referenceDate
        val pastScheduledDates = if (isTodayScheduled) {
            scheduledDates.dropLast(1)
        } else {
            scheduledDates
        }

        // 3. Current streak calculation:
        // If today is scheduled and completed: 1 + consecutive completed past scheduled days
        // If today is scheduled and in-progress (not completed yet): preserve previous streak
        // If today is not scheduled: preserve previous streak from most recent elapsed scheduled day
        // Note: countConsecutiveBackwards returns 0 if the most recent past scheduled day was missed
        val currentStreak = if (isTodayScheduled && isCompletedToday) {
            1 + countConsecutiveBackwards(pastScheduledDates, completedDates)
        } else {
            countConsecutiveBackwards(pastScheduledDates, completedDates)
        }

        // 4. Longest streak calculation:
        // Today in-progress is not an elapsed day and cannot be counted as missed.
        val evalDatesForLongest = if (isTodayScheduled && !isCompletedToday) {
            pastScheduledDates
        } else {
            scheduledDates
        }

        var maxStreak = 0
        var runningStreak = 0
        for (date in evalDatesForLongest) {
            if (completedDates.contains(date)) {
                runningStreak++
                if (runningStreak > maxStreak) {
                    maxStreak = runningStreak
                }
            } else {
                runningStreak = 0
            }
        }

        val longestStreak = maxOf(maxStreak, currentStreak)

        return HabitStreakInfo(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            lastCompletedDate = lastCompleted,
            isCompletedToday = isCompletedToday
        )
    }

    private fun countConsecutiveBackwards(
        scheduledDates: List<LocalDate>,
        completedDates: Set<LocalDate>
    ): Int {
        var count = 0
        for (i in scheduledDates.indices.reversed()) {
            if (completedDates.contains(scheduledDates[i])) {
                count++
            } else {
                break
            }
        }
        return count
    }
}
