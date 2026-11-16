package com.moneysaver.habittracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneysaver.habittracker.domain.engine.habit.HabitStreakCalculator
import com.moneysaver.habittracker.domain.model.habit.FrequencyType
import com.moneysaver.habittracker.domain.model.habit.Habit
import com.moneysaver.habittracker.domain.model.habit.HabitCompletionLog
import com.moneysaver.habittracker.domain.model.habit.HabitSchedule
import com.moneysaver.habittracker.domain.model.habit.HabitStreakInfo
import com.moneysaver.habittracker.domain.repository.HabitRepository
import com.moneysaver.habittracker.domain.usecase.habit.ArchiveHabitUseCase
import com.moneysaver.habittracker.domain.usecase.habit.CalculateHabitStreakUseCase
import com.moneysaver.habittracker.domain.usecase.habit.CreateHabitUseCase
import com.moneysaver.habittracker.domain.usecase.habit.DeleteHabitUseCase
import com.moneysaver.habittracker.domain.usecase.habit.GetDailyHabitsUseCase
import com.moneysaver.habittracker.domain.usecase.habit.ToggleHabitCompletionUseCase
import com.moneysaver.habittracker.domain.usecase.habit.UpdateHabitScheduleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class HabitChecklistItem(
    val habit: Habit,
    val isCompleted: Boolean,
    val streakInfo: HabitStreakInfo
)

data class HeatmapDay(
    val date: LocalDate,
    val isCompleted: Boolean,
    val isDue: Boolean
)

data class HabitDetailData(
    val habit: Habit,
    val streakInfo: HabitStreakInfo,
    val heatmapDays: List<HeatmapDay>,
    val totalCompletions: Int
)

data class HabitUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val today: LocalDate = LocalDate.now(),
    val isFutureDate: Boolean = false,
    val checklistItems: List<HabitChecklistItem> = emptyList(),
    val completionRatio: Float = 0f,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val selectedHabitDetail: HabitDetailData? = null,
    val isAddHabitOpen: Boolean = false,
    val isEditScheduleOpen: Boolean = false,
    val habitToEdit: Habit? = null,
    val showArchiveConfirm: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val habitTargetForAction: Habit? = null,
    val error: String? = null,
    val isLoading: Boolean = false
)

class HabitViewModel(
    private val habitRepository: HabitRepository,
    private val streakCalculator: HabitStreakCalculator = HabitStreakCalculator(),
    private val getDailyHabitsUseCase: GetDailyHabitsUseCase = GetDailyHabitsUseCase(habitRepository),
    private val toggleHabitCompletionUseCase: ToggleHabitCompletionUseCase = ToggleHabitCompletionUseCase(habitRepository),
    private val calculateHabitStreakUseCase: CalculateHabitStreakUseCase = CalculateHabitStreakUseCase(
        habitRepository,
        streakCalculator
    ),
    private val createHabitUseCase: CreateHabitUseCase = CreateHabitUseCase(habitRepository),
    private val updateHabitScheduleUseCase: UpdateHabitScheduleUseCase = UpdateHabitScheduleUseCase(habitRepository),
    private val archiveHabitUseCase: ArchiveHabitUseCase = ArchiveHabitUseCase(habitRepository),
    private val deleteHabitUseCase: DeleteHabitUseCase = DeleteHabitUseCase(habitRepository)
) : ViewModel() {

    private val selectedDateState = MutableStateFlow(LocalDate.now())
    private val selectedHabitDetailIdState = MutableStateFlow<String?>(null)
    private val isAddHabitOpenState = MutableStateFlow(false)
    private val isEditScheduleOpenState = MutableStateFlow(false)
    private val habitToEditState = MutableStateFlow<Habit?>(null)
    private val showArchiveConfirmState = MutableStateFlow<Habit?>(null)
    private val showDeleteConfirmState = MutableStateFlow<Habit?>(null)
    private val errorState = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HabitUiState> = combine(
        habitRepository.getHabits(),
        habitRepository.getAllCompletionLogs(),
        selectedDateState,
        selectedHabitDetailIdState
    ) { habits, allLogs, selectedDate, detailHabitId ->
        val today = LocalDate.now()
        val isFuture = selectedDate.isAfter(today)

        // 1. Filter habits due for selectedDate (past-date archival rule: archivedAt > selectedDate)
        val dueHabits = getDailyHabitsUseCase.filterHabitsForDate(habits, selectedDate)

        // Logs on selected date
        val logsOnDate = allLogs.filter { it.completedDate == selectedDate }
        val completedIdsOnDate = logsOnDate.map { it.habitId }.toSet()

        // Build checklist items with streak info
        val checklistItems = dueHabits.map { habit ->
            val habitLogs = allLogs.filter { it.habitId == habit.id }
            val streak = streakCalculator.calculateStreak(habit, habitLogs, today)
            HabitChecklistItem(
                habit = habit,
                isCompleted = completedIdsOnDate.contains(habit.id),
                streakInfo = streak
            )
        }

        val completedCount = checklistItems.count { it.isCompleted }
        val totalCount = checklistItems.size
        val ratio = if (totalCount > 0) (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) else 0f

        // 2. Build Detail & Heatmap if a habit is selected
        val detailData = if (detailHabitId != null) {
            val habit = habits.find { it.id == detailHabitId }
            if (habit != null) {
                val habitLogs = allLogs.filter { it.habitId == habit.id }
                val streak = streakCalculator.calculateStreak(habit, habitLogs, today)
                val completedDates = habitLogs.map { it.completedDate }.toSet()

                // Heatmap for the last 70 days (10 weeks) up to today
                val heatmapDays = (0..69).map { offset ->
                    val date = today.minusDays((69 - offset).toLong())
                    HeatmapDay(
                        date = date,
                        isCompleted = completedDates.contains(date),
                        isDue = habit.isDueOn(date)
                    )
                }

                HabitDetailData(
                    habit = habit,
                    streakInfo = streak,
                    heatmapDays = heatmapDays,
                    totalCompletions = completedDates.size
                )
            } else null
        } else null

        HabitUiState(
            selectedDate = selectedDate,
            today = today,
            isFutureDate = isFuture,
            checklistItems = checklistItems,
            completionRatio = ratio,
            completedCount = completedCount,
            totalCount = totalCount,
            selectedHabitDetail = detailData,
            isAddHabitOpen = isAddHabitOpenState.value,
            isEditScheduleOpen = isEditScheduleOpenState.value,
            habitToEdit = habitToEditState.value,
            showArchiveConfirm = showArchiveConfirmState.value != null,
            showDeleteConfirm = showDeleteConfirmState.value != null,
            habitTargetForAction = showArchiveConfirmState.value ?: showDeleteConfirmState.value,
            error = errorState.value,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HabitUiState(isLoading = true)
    )

    fun selectDate(date: LocalDate) {
        selectedDateState.value = date
        errorState.value = null
    }

    fun goToPreviousDay() {
        selectDate(selectedDateState.value.minusDays(1))
    }

    fun goToNextDay() {
        selectDate(selectedDateState.value.plusDays(1))
    }

    fun goToToday() {
        selectDate(LocalDate.now())
    }

    fun toggleHabit(habitId: String) {
        val selectedDate = selectedDateState.value
        val today = LocalDate.now()

        // Future-date read-only guard:
        if (selectedDate.isAfter(today)) {
            errorState.value = "Future dates are read-only: habit completions cannot be recorded in advance."
            return
        }

        viewModelScope.launch {
            try {
                toggleHabitCompletionUseCase(habitId, selectedDate, today)
            } catch (e: Exception) {
                errorState.value = e.message ?: "Failed to update habit completion"
            }
        }
    }

    fun loadHabitDetail(habitId: String) {
        selectedHabitDetailIdState.value = habitId
    }

    fun closeHabitDetail() {
        selectedHabitDetailIdState.value = null
    }

    fun openAddHabit() {
        isAddHabitOpenState.value = true
        errorState.value = null
    }

    fun closeAddHabit() {
        isAddHabitOpenState.value = false
        errorState.value = null
    }

    fun createHabit(
        name: String,
        description: String = "",
        frequencyType: FrequencyType = FrequencyType.DAILY,
        targetDays: Set<DayOfWeek> = emptySet(),
        reminderTime: LocalTime? = null
    ) {
        if (name.isBlank()) {
            errorState.value = "Habit name cannot be blank"
            return
        }
        if (frequencyType == FrequencyType.CUSTOM_DAYS && targetDays.isEmpty()) {
            errorState.value = "Please select at least one day for custom frequency"
            return
        }

        viewModelScope.launch {
            try {
                val schedule = HabitSchedule(
                    frequencyType = frequencyType,
                    targetDays = targetDays,
                    reminderTime = reminderTime,
                    effectiveFrom = LocalDate.now()
                )
                createHabitUseCase(
                    name = name.trim(),
                    description = description.trim(),
                    schedule = schedule,
                    createdAt = LocalDate.now()
                )
                closeAddHabit()
            } catch (e: Exception) {
                errorState.value = e.message ?: "Failed to create habit"
            }
        }
    }

    fun openEditSchedule(habit: Habit) {
        habitToEditState.value = habit
        isEditScheduleOpenState.value = true
        errorState.value = null
    }

    fun closeEditSchedule() {
        isEditScheduleOpenState.value = false
        habitToEditState.value = null
        errorState.value = null
    }

    fun updateSchedule(
        frequencyType: FrequencyType,
        targetDays: Set<DayOfWeek>,
        reminderTime: LocalTime?
    ) {
        val habit = habitToEditState.value ?: return
        if (frequencyType == FrequencyType.CUSTOM_DAYS && targetDays.isEmpty()) {
            errorState.value = "Please select at least one day for custom frequency"
            return
        }

        viewModelScope.launch {
            try {
                val newSchedule = HabitSchedule(
                    frequencyType = frequencyType,
                    targetDays = targetDays,
                    reminderTime = reminderTime,
                    effectiveFrom = LocalDate.now()
                )
                updateHabitScheduleUseCase(
                    habitId = habit.id,
                    newSchedule = newSchedule,
                    effectiveFrom = LocalDate.now()
                )
                closeEditSchedule()
            } catch (e: Exception) {
                errorState.value = e.message ?: "Failed to update schedule"
            }
        }
    }

    fun promptArchiveHabit(habit: Habit) {
        showArchiveConfirmState.value = habit
    }

    fun cancelArchiveHabit() {
        showArchiveConfirmState.value = null
    }

    fun confirmArchiveHabit() {
        val habit = showArchiveConfirmState.value ?: return
        viewModelScope.launch {
            try {
                archiveHabitUseCase(habit.id, LocalDate.now())
                showArchiveConfirmState.value = null
                if (selectedHabitDetailIdState.value == habit.id) {
                    closeHabitDetail()
                }
            } catch (e: Exception) {
                errorState.value = e.message ?: "Failed to archive habit"
            }
        }
    }

    fun promptDeleteHabit(habit: Habit) {
        showDeleteConfirmState.value = habit
    }

    fun cancelDeleteHabit() {
        showDeleteConfirmState.value = null
    }

    fun confirmDeleteHabit() {
        val habit = showDeleteConfirmState.value ?: return
        viewModelScope.launch {
            try {
                deleteHabitUseCase(habit.id)
                showDeleteConfirmState.value = null
                if (selectedHabitDetailIdState.value == habit.id) {
                    closeHabitDetail()
                }
            } catch (e: Exception) {
                errorState.value = e.message ?: "Failed to delete habit"
            }
        }
    }

    fun clearError() {
        errorState.value = null
    }
}
