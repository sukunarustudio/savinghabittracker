package com.moneysaver.habittracker.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.moneysaver.habittracker.data.alarm.AlarmScheduler
import com.moneysaver.habittracker.data.alarm.HabitAlarmDependencyProvider
import com.moneysaver.habittracker.data.notification.NotificationChannelManager
import com.moneysaver.habittracker.domain.repository.HabitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver responsible for rescheduling all active habit reminder alarms
 * across device reboots, app updates, and manual or network clock/timezone adjustments.
 *
 * Registered for:
 * - android.intent.action.BOOT_COMPLETED
 * - android.intent.action.MY_PACKAGE_REPLACED
 * - android.intent.action.TIME_SET
 * - android.intent.action.TIMEZONE_CHANGED
 */
class SystemEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        if (action !in SUPPORTED_ACTIONS) {
            Log.d(TAG, "Ignored unsupported broadcast action: $action")
            return
        }

        Log.i(TAG, "Received system broadcast: $action. Triggering habit alarm rescheduling.")

        // Ensure notification channels exist after system updates or boots
        NotificationChannelManager.createReminderChannel(context)

        val pendingResult = try {
            goAsync()
        } catch (e: Exception) {
            null
        }

        CoroutineScope(HabitAlarmDependencyProvider.coroutineDispatcher).launch {
            try {
                rescheduleAlarms(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule alarms after system action: $action", e)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    /**
     * Resolves dependencies from context and triggers alarm rescheduling.
     */
    suspend fun rescheduleAlarms(context: Context) {
        val repository = HabitAlarmDependencyProvider.getRepository(context)
        val scheduler = HabitAlarmDependencyProvider.getScheduler(context)
        rescheduleAlarms(repository, scheduler)
    }

    /**
     * Direct logic implementation enabling pure JVM unit test execution.
     */
    suspend fun rescheduleAlarms(repository: HabitRepository?, scheduler: AlarmScheduler) {
        if (repository == null) {
            Log.w(TAG, "HabitRepository not provided; skipping alarm rescheduling.")
            return
        }

        val habits = repository.getHabits().first()
        val activeWithReminders = habits.filter { !it.isArchived && it.schedule.reminderTime != null }
        Log.i(TAG, "Rescheduling ${activeWithReminders.size} active habit alarms (out of ${habits.size} total habits).")

        scheduler.rescheduleAll(habits)
    }

    companion object {
        private const val TAG = "SystemEventReceiver"

        val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
    }
}
