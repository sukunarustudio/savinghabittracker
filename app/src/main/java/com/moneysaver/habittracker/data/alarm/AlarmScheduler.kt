package com.moneysaver.habittracker.data.alarm

import com.moneysaver.habittracker.domain.model.habit.Habit

/**
 * Interface contract for scheduling and cancelling habit reminder alarms.
 */
interface AlarmScheduler {
    /**
     * Schedules an alarm for the habit's next reminder occurrence.
     * @return true if successfully scheduled, false otherwise.
     */
    fun schedule(habit: Habit): Boolean

    /**
     * Cancels any scheduled reminder alarm for the given habit id.
     */
    fun cancel(habitId: String)

    /**
     * Reschedules reminder alarms for all active habits in the provided list.
     */
    fun rescheduleAll(habits: List<Habit>)

    /**
     * Verifies if the application has permission to schedule exact alarms (Android 12+ / API 31+).
     */
    fun canScheduleExactAlarms(): Boolean
}
