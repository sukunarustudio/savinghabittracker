package com.moneysaver.habittracker.data.alarm

import com.moneysaver.habittracker.domain.model.habit.Habit

/**
 * In-memory test double for AlarmScheduler.
 */
class FakeAlarmScheduler(
    var canScheduleExact: Boolean = true
) : AlarmScheduler {

    val scheduledHabits = mutableListOf<Habit>()
    val cancelledHabitIds = mutableListOf<String>()

    override fun schedule(habit: Habit): Boolean {
        if (habit.isArchived || habit.schedule.reminderTime == null) {
            cancel(habit.id)
            return false
        }
        scheduledHabits.removeAll { it.id == habit.id }
        scheduledHabits.add(habit)
        return true
    }

    override fun cancel(habitId: String) {
        cancelledHabitIds.add(habitId)
        scheduledHabits.removeAll { it.id == habitId }
    }

    override fun rescheduleAll(habits: List<Habit>) {
        habits.forEach { habit ->
            if (habit.isArchived || habit.schedule.reminderTime == null) {
                cancel(habit.id)
            } else {
                schedule(habit)
            }
        }
    }

    override fun canScheduleExactAlarms(): Boolean = canScheduleExact
}
