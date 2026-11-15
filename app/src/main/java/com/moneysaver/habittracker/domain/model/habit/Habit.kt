package com.moneysaver.habittracker.domain.model.habit

import java.time.LocalDate
import java.util.UUID

data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val schedule: HabitSchedule = HabitSchedule(),
    val scheduleHistory: List<HabitSchedule> = emptyList(),
    val createdAt: LocalDate = LocalDate.now(),
    val archivedAt: LocalDate? = null,
    val isArchived: Boolean = false
) {
    init {
        require(name.isNotBlank()) { "Habit name cannot be blank" }
    }

    /**
     * Resolves the effective schedule for a given date based on schedule history
     * and prospective schedule modifications.
     */
    fun getScheduleFor(date: LocalDate): HabitSchedule {
        val all = (scheduleHistory + schedule).sortedBy { it.effectiveFrom }
        return all.lastOrNull { !it.effectiveFrom.isAfter(date) }
            ?: all.firstOrNull()
            ?: schedule
    }

    /**
     * Checks if the habit is scheduled on the given date, accounting for:
     * - Date being on or after habit creation date
     * - The prospective schedule active on that date
     */
    fun isScheduledOn(date: LocalDate): Boolean {
        if (date.isBefore(createdAt)) return false
        return getScheduleFor(date).isScheduledOn(date.dayOfWeek)
    }

    /**
     * Checks whether the habit should appear on a daily checklist for [date].
     * Rules:
     * 1. createdAt <= date
     * 2. archivedAt == null || archivedAt > date
     * 3. Scheduled on that day of the week
     */
    fun isDueOn(date: LocalDate): Boolean {
        if (date.isBefore(createdAt)) return false
        if (archivedAt != null && !archivedAt.isAfter(date)) return false
        return isScheduledOn(date)
    }
}
