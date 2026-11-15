package com.moneysaver.habittracker.data.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.moneysaver.habittracker.data.alarm.AlarmScheduler
import com.moneysaver.habittracker.data.alarm.HabitAlarmDependencyProvider
import com.moneysaver.habittracker.data.alarm.HabitAlarmScheduler
import com.moneysaver.habittracker.data.notification.NotificationChannelManager
import com.moneysaver.habittracker.domain.repository.HabitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered when a habit reminder alarm fires.
 * Posts a high-importance notification and schedules the habit's next occurrence.
 */
class HabitAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val habitId = intent.getStringExtra(HabitAlarmScheduler.EXTRA_HABIT_ID) ?: return
        val habitName = intent.getStringExtra(HabitAlarmScheduler.EXTRA_HABIT_NAME) ?: "Habit Reminder"
        val habitDescription = intent.getStringExtra(HabitAlarmScheduler.EXTRA_HABIT_DESCRIPTION) ?: ""

        Log.d(TAG, "Habit alarm triggered for habit: $habitId ($habitName)")

        // 1. Post notification for the triggered habit
        postNotification(context, habitId, habitName, habitDescription)

        // 2. Reschedule next occurrence asynchronously
        val pendingResult = try {
            goAsync()
        } catch (e: Exception) {
            null
        }

        CoroutineScope(HabitAlarmDependencyProvider.coroutineDispatcher).launch {
            try {
                val repository = HabitAlarmDependencyProvider.getRepository(context)
                val scheduler = HabitAlarmDependencyProvider.getScheduler(context)
                rescheduleNextOccurrence(habitId, repository, scheduler)
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating/scheduling next occurrence for habit $habitId", e)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    /**
     * Reschedules the next occurrence for the specified habit if active and configured.
     */
    suspend fun rescheduleNextOccurrence(
        habitId: String,
        repository: HabitRepository?,
        scheduler: AlarmScheduler
    ) {
        if (repository != null) {
            val habit = repository.getHabitById(habitId)
            if (habit != null && !habit.isArchived && habit.schedule.reminderTime != null) {
                scheduler.schedule(habit)
                Log.d(TAG, "Successfully rescheduled next occurrence for habit: $habitId")
            } else {
                scheduler.cancel(habitId)
            }
        }
    }

    /**
     * Constructs and displays the reminder notification with high priority,
     * sound, vibration, and tap action to open MainActivity.
     */
    fun postNotification(
        context: Context,
        habitId: String,
        habitName: String,
        habitDescription: String
    ): Boolean {
        return try {
            NotificationChannelManager.createReminderChannel(context)

            val notificationManagerCompat = NotificationManagerCompat.from(context)
            if (!notificationManagerCompat.areNotificationsEnabled()) {
                Log.w(TAG, "Notifications are disabled. Skipping notification for habit $habitId")
                return false
            }

            val launchIntent = context.packageManager?.getLaunchIntentForPackage(context.packageName)
                ?: Intent().setClassName(context.packageName, "com.moneysaver.habittracker.MainActivity")

            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            launchIntent.putExtra(HabitAlarmScheduler.EXTRA_HABIT_ID, habitId)

            val contentPendingIntent = PendingIntent.getActivity(
                context,
                habitId.hashCode(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val contentText = if (habitDescription.isNotBlank()) {
                habitDescription
            } else {
                "Time to complete your habit: $habitName"
            }

            val notification = NotificationCompat.Builder(
                context,
                NotificationChannelManager.CHANNEL_ID_HABIT_REMINDERS
            )
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(habitName)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            notificationManagerCompat.notify(habitId.hashCode(), notification)
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing POST_NOTIFICATIONS permission when posting for habit $habitId", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display notification for habit $habitId", e)
            false
        }
    }

    companion object {
        private const val TAG = "HabitAlarmReceiver"
    }
}
