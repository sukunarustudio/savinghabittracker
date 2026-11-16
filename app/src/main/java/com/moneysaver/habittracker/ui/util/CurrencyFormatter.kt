package com.moneysaver.habittracker.ui.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {

    /**
     * Formats an amount in minor units (cents) into a localized currency string.
     * E.g., 1250 cents with symbol "$" -> "$12.50"
     * E.g., -450 cents with symbol "$" -> "-$4.50"
     */
    fun formatCents(cents: Long, symbol: String = "$", includeSign: Boolean = false): String {
        val isNegative = cents < 0L
        val absCents = abs(cents)
        val dollars = absCents / 100
        val remainingCents = absCents % 100

        val symbols = DecimalFormatSymbols(Locale.US)
        val formatter = DecimalFormat("#,##0", symbols)
        val formattedDollars = formatter.format(dollars)
        val formattedCents = String.format(Locale.US, "%02d", remainingCents)

        val base = "$symbol$formattedDollars.$formattedCents"
        return when {
            isNegative -> "-$base"
            includeSign && cents > 0L -> "+$base"
            else -> base
        }
    }

    /**
     * Parses a decimal string (e.g. "12.50" or "12") into minor units (cents, e.g. 1250L).
     * Returns null if parsing fails or result is negative.
     */
    fun parseToCents(input: String): Long? {
        val trimmed = input.trim().replace("$", "").replace(",", "")
        if (trimmed.isBlank()) return null
        return try {
            val doubleVal = trimmed.toDouble()
            if (doubleVal < 0) null else (doubleVal * 100).toLong()
        } catch (e: Exception) {
            null
        }
    }
}
