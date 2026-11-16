package com.moneysaver.habittracker.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormatter {

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
    private val SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    private val DAY_OF_WEEK_FORMATTER = DateTimeFormatter.ofPattern("EEEE", Locale.US)

    fun formatDate(epochMillis: Long): String {
        val localDate = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return localDate.format(DATE_FORMATTER)
    }

    fun formatShortDate(epochMillis: Long): String {
        val localDate = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return localDate.format(SHORT_DATE_FORMATTER)
    }

    fun formatLocalDate(date: LocalDate): String {
        return date.format(DATE_FORMATTER)
    }

    fun formatDayOfWeek(date: LocalDate): String {
        return date.format(DAY_OF_WEEK_FORMATTER)
    }

    fun formatTime(time: LocalTime?): String {
        return time?.format(TIME_FORMATTER) ?: ""
    }
}
