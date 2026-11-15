package com.moneysaver.habittracker.data.alarm

import com.moneysaver.habittracker.domain.model.habit.Habit

/**
 * In-memory test double for AlarmManagerWrapper to verify exact and fallback alarm scheduling.
 */
class FakeAlarmManagerWrapper(
    var canScheduleExactValue: Boolean = true,
    var throwSecurityExceptionOnExact: Boolean = false
) : AlarmManagerWrapper {

    data class ScheduledAlarm(
        val triggerAtMillis: Long,
        val habit: Habit,
        val isExact: Boolean
    )

    val scheduledAlarms = mutableListOf<ScheduledAlarm>()
    val cancelledHabitIds = mutableListOf<String>()

    override fun canScheduleExact(): Boolean = canScheduleExactValue

    override fun setExactAndAllowWhileIdle(triggerAtMillis: Long, habit: Habit) {
        if (throwSecurityExceptionOnExact) {
            throw SecurityException("Caller not allowed to schedule exact alarms.")
        }
        scheduledAlarms.add(ScheduledAlarm(triggerAtMillis, habit, isExact = true))
    }

    override fun setAndAllowWhileIdle(triggerAtMillis: Long, habit: Habit) {
        scheduledAlarms.add(ScheduledAlarm(triggerAtMillis, habit, isExact = false))
    }

    override fun cancel(habitId: String) {
        cancelledHabitIds.add(habitId)
        scheduledAlarms.removeAll { it.habit.id == habitId }
    }
}
