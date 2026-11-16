package com.moneysaver.habittracker.ui.viewmodel

import com.moneysaver.habittracker.domain.engine.habit.HabitStreakCalculator
import com.moneysaver.habittracker.domain.fake.FakeHabitRepository
import com.moneysaver.habittracker.domain.model.habit.FrequencyType
import com.moneysaver.habittracker.domain.model.habit.Habit
import com.moneysaver.habittracker.domain.model.habit.HabitCompletionLog
import com.moneysaver.habittracker.domain.model.habit.HabitSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HabitViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var habitRepository: FakeHabitRepository
    private lateinit var streakCalculator: HabitStreakCalculator
    private lateinit var viewModel: HabitViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        habitRepository = FakeHabitRepository()
        streakCalculator = HabitStreakCalculator()

        viewModel = HabitViewModel(
            habitRepository = habitRepository,
            streakCalculator = streakCalculator
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `today checklist shows due habits and tracks completion state`() = runTest {
        val today = LocalDate.now()
        val habit = Habit(
            id = "habit-1",
            name = "Morning Stretch",
            schedule = HabitSchedule(FrequencyType.DAILY),
            createdAt = today.minusDays(7)
        )
        habitRepository.insertHabit(habit)

        val state = viewModel.uiState.first { it.checklistItems.isNotEmpty() }
        assertEquals(1, state.checklistItems.size)
        assertFalse(state.checklistItems.first().isCompleted)
        assertEquals(0f, state.completionRatio, 0.001f)

        // Toggle completion
        viewModel.toggleHabit("habit-1")

        val stateAfterToggle = viewModel.uiState.first { it.completedCount == 1 }
        assertTrue(stateAfterToggle.checklistItems.first().isCompleted)
        assertEquals(1f, stateAfterToggle.completionRatio, 0.001f)
    }

    @Test
    fun `untoggle action removes existing completion log`() = runTest {
        val today = LocalDate.now()
        val habit = Habit(
            id = "habit-2",
            name = "Drink Water",
            schedule = HabitSchedule(FrequencyType.DAILY),
            createdAt = today.minusDays(2)
        )
        habitRepository.insertHabit(habit)
        habitRepository.insertCompletionLog(HabitCompletionLog(habitId = "habit-2", completedDate = today))

        val initial = viewModel.uiState.first { it.completedCount == 1 }
        assertTrue(initial.checklistItems.first().isCompleted)

        // Untoggle
        viewModel.toggleHabit("habit-2")

        val stateAfterUntoggle = viewModel.uiState.first { it.completedCount == 0 }
        assertFalse(stateAfterUntoggle.checklistItems.first().isCompleted)
        assertNull(habitRepository.getCompletionLog("habit-2", today))
    }

    @Test
    fun `future date read-only state blocks toggles and sets warning`() = runTest {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val habit = Habit(
            id = "habit-3",
            name = "Read Book",
            schedule = HabitSchedule(FrequencyType.DAILY),
            createdAt = today.minusDays(5)
        )
        habitRepository.insertHabit(habit)

        // Navigate to tomorrow
        viewModel.selectDate(tomorrow)

        val state = viewModel.uiState.first { it.selectedDate == tomorrow }
        assertTrue(state.isFutureDate)

        // Attempt to toggle
        viewModel.toggleHabit("habit-3")

        val stateAfterAttempt = viewModel.uiState.value
        assertNotNull(stateAfterAttempt.error)
        assertTrue(stateAfterAttempt.error!!.contains("Future dates are read-only"))
        assertNull(habitRepository.getCompletionLog("habit-3", tomorrow))
    }

    @Test
    fun `past date archival filter includes habits archived after that date and excludes habits archived before`() = runTest {
        val today = LocalDate.now()
        val threeDaysAgo = today.minusDays(3)

        // Habit A: Created 10 days ago, archived 2 days ago (was ACTIVE 3 days ago)
        val habitA = Habit(
            id = "habit-a",
            name = "Active in past",
            schedule = HabitSchedule(FrequencyType.DAILY),
            createdAt = today.minusDays(10),
            archivedAt = today.minusDays(2),
            isArchived = true
        )
        // Habit B: Created 10 days ago, archived 5 days ago (was ALREADY ARCHIVED 3 days ago)
        val habitB = Habit(
            id = "habit-b",
            name = "Archived earlier",
            schedule = HabitSchedule(FrequencyType.DAILY),
            createdAt = today.minusDays(10),
            archivedAt = today.minusDays(5),
            isArchived = true
        )
        habitRepository.insertHabit(habitA)
        habitRepository.insertHabit(habitB)

        // Inspect 3 days ago
        viewModel.selectDate(threeDaysAgo)

        val state = viewModel.uiState.first { it.selectedDate == threeDaysAgo }
        val habitIds = state.checklistItems.map { it.habit.id }
        assertTrue("Habit A should be visible because archivedAt > selectedDate", habitIds.contains("habit-a"))
        assertFalse("Habit B should NOT be visible because archivedAt <= selectedDate", habitIds.contains("habit-b"))
    }

    @Test
    fun `load habit detail calculates streak and generates 70-day heatmap`() = runTest {
        val today = LocalDate.now()
        val habit = Habit(
            id = "habit-4",
            name = "Meditate",
            schedule = HabitSchedule(FrequencyType.DAILY),
            createdAt = today.minusDays(10)
        )
        habitRepository.insertHabit(habit)

        // Log completions for past 3 days (streak = 3)
        habitRepository.insertCompletionLog(HabitCompletionLog(habitId = "habit-4", completedDate = today))
        habitRepository.insertCompletionLog(HabitCompletionLog(habitId = "habit-4", completedDate = today.minusDays(1)))
        habitRepository.insertCompletionLog(HabitCompletionLog(habitId = "habit-4", completedDate = today.minusDays(2)))

        viewModel.loadHabitDetail("habit-4")

        val state = viewModel.uiState.first { it.selectedHabitDetail != null }
        val detail = state.selectedHabitDetail!!

        assertEquals("Meditate", detail.habit.name)
        assertEquals(3, detail.streakInfo.currentStreak)
        assertEquals(70, detail.heatmapDays.size)
        assertEquals(3, detail.totalCompletions)

        val todayHeatmap = detail.heatmapDays.find { it.date == today }
        assertNotNull(todayHeatmap)
        assertTrue(todayHeatmap!!.isCompleted)
    }
}
