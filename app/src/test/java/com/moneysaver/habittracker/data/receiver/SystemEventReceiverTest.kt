package com.moneysaver.habittracker.data.receiver

import android.content.Intent
import com.moneysaver.habittracker.data.alarm.FakeAlarmScheduler
import com.moneysaver.habittracker.domain.model.habit.FrequencyType
import com.moneysaver.habittracker.domain.model.habit.Habit
import com.moneysaver.habittracker.domain.model.habit.HabitSchedule
import com.moneysaver.habittracker.domain.repository.FakeHabitRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class SystemEventReceiverTest {

    private lateinit var fakeRepository: FakeHabitRepository
    private lateinit var fakeScheduler: FakeAlarmScheduler
    private lateinit var receiver: SystemEventReceiver

    @Before
    fun setUp() {
        fakeRepository = FakeHabitRepository()
        fakeScheduler = FakeAlarmScheduler()
        receiver = SystemEventReceiver()
    }

    @Test
    fun supportedActions_containsAllRequiredSystemEvents() {
        val actions = SystemEventReceiver.SUPPORTED_ACTIONS
        assertTrue(actions.contains(Intent.ACTION_BOOT_COMPLETED))
        assertTrue(actions.contains(Intent.ACTION_MY_PACKAGE_REPLACED))
        assertTrue(actions.contains(Intent.ACTION_TIME_SET))
        assertTrue(actions.contains(Intent.ACTION_TIMEZONE_CHANGED))
        assertEquals(4, actions.size)
    }

    @Test
    fun rescheduleAlarms_reschedulesActiveHabitsWithReminders() = runTest {
        val activeWithReminder = Habit(
            id = "active-with-reminder",
            name = "Morning Stretch",
            schedule = HabitSchedule(
                frequencyType = FrequencyType.DAILY,
                reminderTime = LocalTime.of(7, 30)
            ),
            createdAt = LocalDate.of(2026, 11, 1)
        )
        val activeWithoutReminder = Habit(
            id = "active-no-reminder",
            name = "Read News",
            schedule = HabitSchedule(
                frequencyType = FrequencyType.DAILY,
                reminderTime = null
            ),
            createdAt = LocalDate.of(2026, 11, 1)
        )
        val archivedHabit = Habit(
            id = "archived-habit",
            name = "Old Habit",
            schedule = HabitSchedule(
                frequencyType = FrequencyType.DAILY,
                reminderTime = LocalTime.of(8, 0)
            ),
            isArchived = true,
            createdAt = LocalDate.of(2026, 10, 1)
        )

        fakeRepository.insertHabit(activeWithReminder)
        fakeRepository.insertHabit(activeWithoutReminder)
        fakeRepository.insertHabit(archivedHabit)

        receiver.rescheduleAlarms(fakeRepository, fakeScheduler)

        // Only active habit with reminder should be scheduled
        assertEquals(1, fakeScheduler.scheduledHabits.size)
        assertEquals("active-with-reminder", fakeScheduler.scheduledHabits.first().id)

        // Others should have cancel called
        assertTrue(fakeScheduler.cancelledHabitIds.contains("active-no-reminder"))
        assertTrue(fakeScheduler.cancelledHabitIds.contains("archived-habit"))
    }

    @Test
    fun rescheduleAlarms_handlesNullRepositoryGracefully() = runTest {
        // When repository is null, it should safely log and return without throwing
        receiver.rescheduleAlarms(repository = null, scheduler = fakeScheduler)
        assertTrue(fakeScheduler.scheduledHabits.isEmpty())
        assertTrue(fakeScheduler.cancelledHabitIds.isEmpty())
    }
}
