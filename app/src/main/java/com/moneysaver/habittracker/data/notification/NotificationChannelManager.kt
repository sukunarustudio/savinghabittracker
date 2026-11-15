package com.moneysaver.habittracker.data.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

/**
 * Manages Notification Channels for Android 8.0+ (API 26+).
 * Creates high-importance notification channels for habit reminders with sound,
 * vibration, and badge support.
 */
class NotificationChannelManager(
    private val context: Context,
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
) {

    /**
     * Initializes all application notification channels.
     */
    fun createChannels() {
        createHabitReminderChannel()
    }

    /**
     * Creates and registers the high-importance reminder channel with sound and vibration.
     * Idempotent on API 26+; no-op on pre-Oreo.
     */
    fun createHabitReminderChannel(): NotificationChannel? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val channel = NotificationChannel(
                CHANNEL_ID_HABIT_REMINDERS,
                CHANNEL_NAME_HABIT_REMINDERS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC_HABIT_REMINDERS
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                vibrationPattern = DEFAULT_VIBRATION_PATTERN
                setSound(soundUri, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(channel)
            return channel
        }
        return null
    }

    companion object {
        const val CHANNEL_ID_HABIT_REMINDERS = "habit_reminders_channel"
        const val CHANNEL_NAME_HABIT_REMINDERS = "Habit Reminders"
        const val CHANNEL_DESC_HABIT_REMINDERS = "High-importance reminder notifications for scheduled habits"
        val DEFAULT_VIBRATION_PATTERN = longArrayOf(0, 250, 250, 250)

        /**
         * Convenience helper to create the reminder channel directly.
         */
        fun createReminderChannel(context: Context): NotificationChannel? {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return null
            return NotificationChannelManager(context, manager).createHabitReminderChannel()
        }
    }
}
