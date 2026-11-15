package com.moneysaver.habittracker.data.notification

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NotificationChannelManagerTest {

    @Test
    fun channelConstants_areProperlyConfigured() {
        assertEquals("habit_reminders_channel", NotificationChannelManager.CHANNEL_ID_HABIT_REMINDERS)
        assertEquals("Habit Reminders", NotificationChannelManager.CHANNEL_NAME_HABIT_REMINDERS)
        assertNotNull(NotificationChannelManager.CHANNEL_DESC_HABIT_REMINDERS)
        assertArrayEquals(
            longArrayOf(0, 250, 250, 250),
            NotificationChannelManager.DEFAULT_VIBRATION_PATTERN
        )
    }
}
