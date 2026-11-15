package com.moneysaver.habittracker.domain.model.habit

import java.time.LocalDate

data class HabitStreakInfo(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedDate: LocalDate? = null,
    val isCompletedToday: Boolean = false
)
