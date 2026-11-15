package com.moneysaver.habittracker.domain.model.habit

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class HabitCompletionLog(
    val id: String = UUID.randomUUID().toString(),
    val habitId: String,
    val completedDate: LocalDate,
    val completedAt: Instant = Instant.now()
)

typealias HabitRecord = HabitCompletionLog
