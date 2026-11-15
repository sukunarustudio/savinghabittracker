package com.moneysaver.habittracker.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.moneysaver.habittracker.data.receiver.HabitAlarmReceiver
import com.moneysaver.habittracker.domain.model.habit.Habit
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Abstraction over AlarmManager operations for reliable execution and unit testability.
 */
interface AlarmManagerWrapper {
    fun canScheduleExact(): Boolean
    fun setExactAndAllowWhileIdle(triggerAtMillis: Long, habit: Habit)
    fun setAndAllowWhileIdle(triggerAtMillis: Long, habit: Habit)
    fun cancel(habitId: String)
}

/**
 * Concrete AlarmScheduler for habit reminders.
 * Supports Android 8.0+ up to 14+ (API 26-34+).
 * Handles Android 12+ (API 31+) canScheduleExactAlarms() checks and gracefully falls back
 * from setExactAndAllowWhileIdle to setAndAllowWhileIdle upon SecurityException or lack of permission.
 */
class HabitAlarmScheduler internal constructor(
    private val context: Context?,
    private val wrapper: AlarmManagerWrapper
) : AlarmScheduler {

    /**
     * Production constructor using system services.
     */
    constructor(
        context: Context,
        alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    ) : this(
        context,
        RealAlarmManagerWrapper(context, alarmManager)
    )

    /**
     * Checks if exact alarm scheduling is allowed by the system on Android 12+ (API 31+).
     * On pre-API 31, exact alarms do not require SCHEDULE_EXACT_ALARM runtime permission.
     */
    override fun canScheduleExactAlarms(): Boolean {
        return try {
            wrapper.canScheduleExact()
        } catch (e: Exception) {
            Log.w(TAG, "Error checking canScheduleExact, returning false", e)
            false
        }
    }

    /**
     * Schedules the next reminder alarm for a habit.
     * Uses setExactAndAllowWhileIdle() when permitted, otherwise gracefully falls back to
     * setAndAllowWhileIdle() preventing SecurityException.
     */
    override fun schedule(habit: Habit): Boolean {
        if (habit.isArchived || habit.schedule.reminderTime == null) {
            cancel(habit.id)
            return false
        }

        val triggerAtMillis = calculateNextTriggerMillis(habit) ?: run {
            cancel(habit.id)
            return false
        }

        return if (canScheduleExactAlarms()) {
            try {
                wrapper.setExactAndAllowWhileIdle(triggerAtMillis, habit)
                true
            } catch (e: SecurityException) {
                Log.w(
                    TAG,
                    "SecurityException scheduling exact alarm for habit ${habit.id}. " +
                            "Gracefully falling back to setAndAllowWhileIdle().",
                    e
                )
                try {
                    wrapper.setAndAllowWhileIdle(triggerAtMillis, habit)
                    true
                } catch (fallbackEx: Exception) {
                    Log.e(TAG, "Fallback setAndAllowWhileIdle failed for habit ${habit.id}", fallbackEx)
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error scheduling alarm for habit ${habit.id}", e)
                false
            }
        } else {
            try {
                wrapper.setAndAllowWhileIdle(triggerAtMillis, habit)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling inexact alarm for habit ${habit.id}", e)
                false
            }
        }
    }

    /**
     * Cancels any pending reminder alarm for the specified habit id.
     */
    override fun cancel(habitId: String) {
        try {
            wrapper.cancel(habitId)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel alarm for habitId: $habitId", e)
        }
    }

    /**
     * Reschedules reminder alarms for all habits in the provided list.
     * Cancels alarms for archived habits or habits without a configured reminder time.
     */
    override fun rescheduleAll(habits: List<Habit>) {
        habits.forEach { habit ->
            if (habit.isArchived || habit.schedule.reminderTime == null) {
                cancel(habit.id)
            } else {
                schedule(habit)
            }
        }
    }

    /**
     * Calculates the next trigger timestamp in epoch milliseconds.
     * Accounts for habit creation date, archive date, effective schedule, day-of-week target days,
     * and reminder time.
     *
     * @param habit The habit to evaluate.
     * @param referenceDateTime The reference point in time (defaulting to now in the specified zone).
     * @param zoneId Time zone used for conversion (defaulting to system default).
     * @return Epoch millisecond timestamp of the next occurrence, or null if no upcoming reminder is scheduled.
     */
    fun calculateNextTriggerMillis(
        habit: Habit,
        referenceDateTime: LocalDateTime = LocalDateTime.now(zoneId),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long? {
        if (habit.isArchived) return null
        if (habit.archivedAt != null && !habit.archivedAt.isAfter(referenceDateTime.toLocalDate())) {
            return null
        }

        val referenceDate = referenceDateTime.toLocalDate()
        val referenceTime = referenceDateTime.toLocalTime()

        // 1. Check if the reminder is due today and still in the future
        if (habit.isDueOn(referenceDate)) {
            val todaySchedule = habit.getScheduleFor(referenceDate)
            val todayReminder = todaySchedule.reminderTime
            if (todayReminder != null && todayReminder.isAfter(referenceTime)) {
                return referenceDate.atTime(todayReminder)
                    .atZone(zoneId)
                    .toInstant()
                    .toEpochMilli()
            }
        }

        // 2. Scan forward up to 366 days for the next scheduled occurrence
        var candidateDate = referenceDate.plusDays(1)
        val maxDate = referenceDate.plusDays(366)

        while (!candidateDate.isAfter(maxDate)) {
            if (habit.isDueOn(candidateDate)) {
                val candidateSchedule = habit.getScheduleFor(candidateDate)
                val candidateReminder = candidateSchedule.reminderTime
                if (candidateReminder != null) {
                    return candidateDate.atTime(candidateReminder)
                        .atZone(zoneId)
                        .toInstant()
                        .toEpochMilli()
                }
            }
            candidateDate = candidateDate.plusDays(1)
        }

        return null
    }

    companion object {
        private const val TAG = "HabitAlarmScheduler"
        const val ACTION_HABIT_REMINDER = "com.moneysaver.habittracker.ACTION_HABIT_REMINDER"
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_HABIT_NAME = "extra_habit_name"
        const val EXTRA_HABIT_DESCRIPTION = "extra_habit_description"
    }
}

/**
 * Production implementation of AlarmManagerWrapper delegating to Android AlarmManager.
 */
internal class RealAlarmManagerWrapper(
    private val context: Context,
    private val alarmManager: AlarmManager
) : AlarmManagerWrapper {

    override fun canScheduleExact(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    override fun setExactAndAllowWhileIdle(triggerAtMillis: Long, habit: Habit) {
        val pendingIntent = createPendingIntent(habit)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    override fun setAndAllowWhileIdle(triggerAtMillis: Long, habit: Habit) {
        val pendingIntent = createPendingIntent(habit)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    override fun cancel(habitId: String) {
        val pendingIntent = createCancelPendingIntent(habitId)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun createPendingIntent(habit: Habit): PendingIntent {
        val intent = Intent(context, HabitAlarmReceiver::class.java).apply {
            action = HabitAlarmScheduler.ACTION_HABIT_REMINDER
            data = Uri.parse("habit://${habit.id}")
            putExtra(HabitAlarmScheduler.EXTRA_HABIT_ID, habit.id)
            putExtra(HabitAlarmScheduler.EXTRA_HABIT_NAME, habit.name)
            putExtra(HabitAlarmScheduler.EXTRA_HABIT_DESCRIPTION, habit.description)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, habit.id.hashCode(), intent, flags)
    }

    private fun createCancelPendingIntent(habitId: String): PendingIntent {
        val intent = Intent(context, HabitAlarmReceiver::class.java).apply {
            action = HabitAlarmScheduler.ACTION_HABIT_REMINDER
            data = Uri.parse("habit://$habitId")
            putExtra(HabitAlarmScheduler.EXTRA_HABIT_ID, habitId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, habitId.hashCode(), intent, flags)
    }
}
