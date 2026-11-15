package com.moneysaver.habittracker.data.receiver

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

class HabitAlarmReceiverTest {

    private lateinit var fakeRepository: FakeHabitRepository
    private lateinit var fakeScheduler: FakeAlarmScheduler
    private lateinit var receiver: HabitAlarmReceiver

    @Before
    fun setUp() {
        fakeRepository = FakeHabitRepository()
        fakeScheduler = FakeAlarmScheduler()
        receiver = HabitAlarmReceiver()
    }

    @Test
    fun rescheduleNextOccurrence_schedulesActiveHabitWithReminder() = runTest {
        val habit = Habit(
            id = "habit-reschedule-1",
            name = "Read 30 mins",
            schedule = HabitSchedule(
                frequencyType = FrequencyType.DAILY,
                reminderTime = LocalTime.of(21, 0)
            ),
            createdAt = LocalDate.of(2026, 11, 1)
        )
        fakeRepository.insertHabit(habit)

        receiver.rescheduleNextOccurrence(habit.id, fakeRepository, fakeScheduler)

        assertEquals(1, fakeScheduler.scheduledHabits.size)
        assertEquals("habit-reschedule-1", fakeScheduler.scheduledHabits.first().id)
    }

    @Test
    fun rescheduleNextOccurrence_cancelsWhenHabitIsArchived() = runTest {
        val archivedHabit = Habit(
            id = "habit-archived-2",
            name = "Gym",
            schedule = HabitSchedule(
                frequencyType = FrequencyType.DAILY,
                reminderTime = LocalTime.of(6, 30)
            ),
            isArchived = true,
            createdAt = LocalDate.of(2026, 10, 1)
        )
        fakeRepository.insertHabit(archivedHabit)

        receiver.rescheduleNextOccurrence(archivedHabit.id, fakeRepository, fakeScheduler)

        assertTrue(fakeScheduler.scheduledHabits.isEmpty())
        assertTrue(fakeScheduler.cancelledHabitIds.contains("habit-archived-2"))
    }

    @Test
    fun rescheduleNextOccurrence_cancelsWhenHabitHasNoReminder() = runTest {
        val noReminderHabit = Habit(
            id = "habit-no-reminder-3",
            name = "Drink Water",
            schedule = HabitSchedule(
                frequencyType = FrequencyType.DAILY,
                reminderTime = null
            ),
            createdAt = LocalDate.of(2026, 11, 1)
        )
        fakeRepository.insertHabit(noReminderHabit)

        receiver.rescheduleNextOccurrence(noReminderHabit.id, fakeRepository, fakeScheduler)

        assertTrue(fakeScheduler.scheduledHabits.isEmpty())
        assertTrue(fakeScheduler.cancelledHabitIds.contains("habit-no-reminder-3"))
    }

    @Test
    fun rescheduleNextOccurrence_handlesNullRepositorySafely() = runTest {
        receiver.rescheduleNextOccurrence("any-id", repository = null, scheduler = fakeScheduler)
        assertTrue(fakeScheduler.scheduledHabits.isEmpty())
        assertTrue(fakeScheduler.cancelledHabitIds.isEmpty())
    }
}
