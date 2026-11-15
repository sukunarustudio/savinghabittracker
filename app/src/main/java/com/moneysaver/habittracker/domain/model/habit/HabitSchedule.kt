package com.moneysaver.habittracker.domain.model.habit

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class HabitSchedule(
    val frequencyType: FrequencyType = FrequencyType.DAILY,
    val targetDays: Set<DayOfWeek> = emptySet(),
    val reminderTime: LocalTime? = null,
    val effectiveFrom: LocalDate = LocalDate.MIN
) {
    init {
        if (frequencyType == FrequencyType.CUSTOM_DAYS) {
            require(targetDays.isNotEmpty()) {
                "Custom days schedule must specify at least one target day of the week"
            }
        }
    }

    /**
     * Checks if the given day of the week is scheduled under this configuration.
     */
    fun isScheduledOn(dayOfWeek: DayOfWeek): Boolean {
        return when (frequencyType) {
            FrequencyType.DAILY -> true
            FrequencyType.CUSTOM_DAYS -> targetDays.contains(dayOfWeek)
        }
    }
}
