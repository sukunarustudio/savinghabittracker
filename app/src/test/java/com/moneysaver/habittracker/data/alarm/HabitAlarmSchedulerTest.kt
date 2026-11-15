package com.moneysaver.habittracker.data.alarm

import com.moneysaver.habittracker.domain.model.habit.FrequencyType
import com.moneysaver.habittracker.domain.model.habit.Habit
import com.moneysaver.habittracker.domain.model.habit.HabitSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class HabitAlarmSchedulerTest {

    private lateinit var fakeWrapper: FakeAlarmManagerWrapper
    private lateinit var scheduler: HabitAlarmScheduler
    private val zoneId = ZoneId.of("UTC")

    @Before
    fun setUp() {
        fakeWrapper = FakeAlarmManagerWrapper()
        scheduler = HabitAlarmScheduler(context = null, wrapper = fakeWrapper)
    }

    @Test
    fun canScheduleExactAlarms_delegatesToWrapper() {
        fakeWrapper.canScheduleExactValue = true
        assertTrue(scheduler.canScheduleExactAlarms())

        fakeWrapper.canScheduleExactValue = false
        assertFalse(scheduler.canScheduleExactAlarms())
    }

    @Test
    fun schedule_usesExactAlarm_whenPermitted() {
        fakeWrapper.canScheduleExactValue = true
        val habit = createDailyHabit(
            id = "habit-1",
            reminderTime = LocalTime.of(18, 0),
            createdAt = LocalDate.of(2026, 11, 1)
        )

        val scheduled = scheduler.schedule(habit)

        assertTrue(scheduled)
        assertEquals(1, fakeWrapper.scheduledAlarms.size)
        val alarm = fakeWrapper.scheduledAlarms.first()
        assertTrue("Expected exact alarm when permitted", alarm.isExact)
        assertEquals("habit-1", alarm.habit.id)
    }

    @Test
    fun schedule_usesFallbackInexactAlarm_whenExactNotPermitted() {
        fakeWrapper.canScheduleExactValue = false
        val habit = createDailyHabit(
            id = "habit-2",
            reminderTime = LocalTime.of(19, 0),
            createdAt = LocalDate.of(2026, 11, 1)
        )

        val scheduled = scheduler.schedule(habit)

        assertTrue(scheduled)
        assertEquals(1, fakeWrapper.scheduledAlarms.size)
        val alarm = fakeWrapper.scheduledAlarms.first()
        assertFalse("Expected inexact fallback alarm when exact is not permitted", alarm.isExact)
        assertEquals("habit-2", alarm.habit.id)
    }

    @Test
    fun schedule_catchesSecurityException_andGracefullyFallsBackToInexact() {
        fakeWrapper.canScheduleExactValue = true
        fakeWrapper.throwSecurityExceptionOnExact = true
        val habit = createDailyHabit(
            id = "habit-3",
            reminderTime = LocalTime.of(20, 0),
            createdAt = LocalDate.of(2026, 11, 1)
        )

        // Should not throw SecurityException; must gracefully fall back to setAndAllowWhileIdle
        val scheduled = scheduler.schedule(habit)

        assertTrue(scheduled)
        assertEquals(1, fakeWrapper.scheduledAlarms.size)
        val alarm = fakeWrapper.scheduledAlarms.first()
        assertFalse("Alarm should have fallen back to inexact after SecurityException", alarm.isExact)
        assertEquals("habit-3", alarm.habit.id)
    }

    @Test
    fun schedule_archivedHabit_cancelsAlarmAndReturnsFalse() {
        val habit = createDailyHabit(
            id = "archived-1",
            reminderTime = LocalTime.of(9, 0)
        ).copy(isArchived = true)

        val result = scheduler.schedule(habit)

        assertFalse(result)
        assertTrue(fakeWrapper.scheduledAlarms.isEmpty())
        assertTrue(fakeWrapper.cancelledHabitIds.contains("archived-1"))
    }

    @Test
    fun schedule_noReminderTime_cancelsAlarmAndReturnsFalse() {
        val habit = createDailyHabit(
            id = "no-reminder-1",
            reminderTime = null
        )

        val result = scheduler.schedule(habit)

        assertFalse(result)
        assertTrue(fakeWrapper.scheduledAlarms.isEmpty())
        assertTrue(fakeWrapper.cancelledHabitIds.contains("no-reminder-1"))
    }

    @Test
    fun cancel_invokesWrapperCancel() {
        scheduler.cancel("habit-cancel-test")
        assertTrue(fakeWrapper.cancelledHabitIds.contains("habit-cancel-test"))
    }

    @Test
    fun rescheduleAll_schedulesOnlyActiveHabitsWithReminders() {
        val activeWithReminder = createDailyHabit("active-1", LocalTime.of(7, 0))
        val activeNoReminder = createDailyHabit("active-2", null)
        val archivedWithReminder = createDailyHabit("archived-1", LocalTime.of(8, 0)).copy(isArchived = true)

        scheduler.rescheduleAll(listOf(activeWithReminder, activeNoReminder, archivedWithReminder))

        assertEquals(1, fakeWrapper.scheduledAlarms.size)
        assertEquals("active-1", fakeWrapper.scheduledAlarms.first().habit.id)
        assertTrue(fakeWrapper.cancelledHabitIds.contains("active-2"))
        assertTrue(fakeWrapper.cancelledHabitIds.contains("archived-1"))
    }

    @Test
    fun calculateNextTriggerMillis_todayReminderInFuture_triggersToday() {
        val habit = createDailyHabit(
            id = "habit-today-future",
            reminderTime = LocalTime.of(14, 0),
            createdAt = LocalDate.of(2026, 11, 1)
        )
        val now = LocalDateTime.of(2026, 11, 15, 10, 0, 0)

        val triggerMillis = scheduler.calculateNextTriggerMillis(habit, referenceDateTime = now, zoneId = zoneId)

        assertNotNull(triggerMillis)
        val expectedMillis = LocalDateTime.of(2026, 11, 15, 14, 0, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        assertEquals(expectedMillis, triggerMillis)
    }

    @Test
    fun calculateNextTriggerMillis_todayReminderInPast_triggersTomorrow() {
        val habit = createDailyHabit(
            id = "habit-today-past",
            reminderTime = LocalTime.of(8, 0),
            createdAt = LocalDate.of(2026, 11, 1)
        )
        // Now is 10:00 (past 08:00 reminder)
        val now = LocalDateTime.of(2026, 11, 15, 10, 0, 0)

        val triggerMillis = scheduler.calculateNextTriggerMillis(habit, referenceDateTime = now, zoneId = zoneId)

        assertNotNull(triggerMillis)
        val expectedMillis = LocalDateTime.of(2026, 11, 16, 8, 0, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        assertEquals(expectedMillis, triggerMillis)
    }

    @Test
    fun calculateNextTriggerMillis_customDays_schedulesNextScheduledTargetDay() {
        // Scheduled on Mon, Wed, Fri
        val schedule = HabitSchedule(
            frequencyType = FrequencyType.CUSTOM_DAYS,
            targetDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            reminderTime = LocalTime.of(9, 30)
        )
        val habit = Habit(
            id = "custom-days-habit",
            name = "Workout",
            schedule = schedule,
            createdAt = LocalDate.of(2026, 11, 1)
        )

        // 2026-11-17 is a TUESDAY. Next target day is WEDNESDAY (2026-11-18)
        val tuesday = LocalDateTime.of(2026, 11, 17, 10, 0, 0)
        val wednesdayTrigger = scheduler.calculateNextTriggerMillis(habit, referenceDateTime = tuesday, zoneId = zoneId)

        assertNotNull(wednesdayTrigger)
        val expectedWedMillis = LocalDateTime.of(2026, 11, 18, 9, 30, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        assertEquals(expectedWedMillis, wednesdayTrigger)

        // 2026-11-20 is a FRIDAY at 10:00 (past 09:30 reminder). Next target day is MONDAY (2026-11-23)
        val fridayEvening = LocalDateTime.of(2026, 11, 20, 10, 0, 0)
        val mondayTrigger = scheduler.calculateNextTriggerMillis(habit, referenceDateTime = fridayEvening, zoneId = zoneId)

        assertNotNull(mondayTrigger)
        val expectedMonMillis = LocalDateTime.of(2026, 11, 23, 9, 30, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        assertEquals(expectedMonMillis, mondayTrigger)
    }

    @Test
    fun calculateNextTriggerMillis_habitCreatedInFuture_triggersOnCreationDate() {
        val futureCreationDate = LocalDate.of(2026, 11, 20)
        val habit = createDailyHabit(
            id = "future-created",
            reminderTime = LocalTime.of(7, 0),
            createdAt = futureCreationDate
        )
        val now = LocalDateTime.of(2026, 11, 15, 12, 0, 0)

        val triggerMillis = scheduler.calculateNextTriggerMillis(habit, referenceDateTime = now, zoneId = zoneId)

        assertNotNull(triggerMillis)
        val expectedMillis = LocalDateTime.of(2026, 11, 20, 7, 0, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        assertEquals(expectedMillis, triggerMillis)
    }

    @Test
    fun calculateNextTriggerMillis_archivedHabit_returnsNull() {
        val habit = createDailyHabit(
            id = "archived-calc",
            reminderTime = LocalTime.of(10, 0)
        ).copy(isArchived = true)

        val now = LocalDateTime.of(2026, 11, 15, 8, 0, 0)
        assertNull(scheduler.calculateNextTriggerMillis(habit, now, zoneId))
    }

    @Test
    fun calculateNextTriggerMillis_prospectiveSchedule_usesEffectiveSchedule() {
        val baseSchedule = HabitSchedule(
            frequencyType = FrequencyType.DAILY,
            reminderTime = LocalTime.of(8, 0),
            effectiveFrom = LocalDate.of(2026, 11, 1)
        )
        val prospectiveSchedule = HabitSchedule(
            frequencyType = FrequencyType.DAILY,
            reminderTime = LocalTime.of(18, 0),
            effectiveFrom = LocalDate.of(2026, 11, 18)
        )
        val habit = Habit(
            name = "Meditation",
            schedule = prospectiveSchedule,
            scheduleHistory = listOf(baseSchedule),
            createdAt = LocalDate.of(2026, 11, 1)
        )

        // On 2026-11-15 (before prospective), base schedule (08:00) is effective
        val nowNov15 = LocalDateTime.of(2026, 11, 15, 6, 0, 0)
        val triggerNov15 = scheduler.calculateNextTriggerMillis(habit, referenceDateTime = nowNov15, zoneId = zoneId)
        val expectedNov15Millis = LocalDateTime.of(2026, 11, 15, 8, 0, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        assertEquals(expectedNov15Millis, triggerNov15)

        // On 2026-11-18 (effective date of prospective), 18:00 is effective
        val nowNov18 = LocalDateTime.of(2026, 11, 18, 10, 0, 0)
        val triggerNov18 = scheduler.calculateNextTriggerMillis(habit, referenceDateTime = nowNov18, zoneId = zoneId)
        val expectedNov18Millis = LocalDateTime.of(2026, 11, 18, 18, 0, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        assertEquals(expectedNov18Millis, triggerNov18)
    }

    private fun createDailyHabit(
        id: String,
        reminderTime: LocalTime?,
        createdAt: LocalDate = LocalDate.of(2026, 11, 1)
    ): Habit {
        return Habit(
            id = id,
            name = "Test Habit $id",
            schedule = HabitSchedule(
                frequencyType = FrequencyType.DAILY,
                reminderTime = reminderTime
            ),
            createdAt = createdAt
        )
    }
}
