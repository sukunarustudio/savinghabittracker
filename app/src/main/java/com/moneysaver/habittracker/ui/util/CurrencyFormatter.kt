package com.moneysaver.habittracker.ui.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {

    /**
     * Formats an amount in minor units (cents) into a localized currency string.
     * E.g., 5000000 cents with symbol "Rp" -> "Rp 50.000"
     * E.g., 1250 cents with symbol "$" -> "$12.50"
     * E.g., -450 cents with symbol "$" -> "-$4.50"
     */
    fun formatCents(cents: Long, symbol: String = "Rp", includeSign: Boolean = false): String {
        val isNegative = cents < 0L
        val absCents = abs(cents)
        val dollars = absCents / 100
        val remainingCents = absCents % 100

        val trimmedSymbol = symbol.trim()
        val isRupiah = trimmedSymbol.equals("Rp", ignoreCase = true) || trimmedSymbol.equals("IDR", ignoreCase = true)

        val symbols = DecimalFormatSymbols(if (isRupiah) Locale("id", "ID") else Locale.US).apply {
            if (isRupiah) {
                groupingSeparator = '.'
                decimalSeparator = ','
            }
        }
        val formatter = DecimalFormat("#,##0", symbols)
        val formattedDollars = formatter.format(dollars)

        val base = if (isRupiah) {
            val prefix = if (trimmedSymbol.isEmpty()) "Rp " else if (trimmedSymbol.endsWith(" ")) trimmedSymbol else "$trimmedSymbol "
            if (remainingCents != 0L) {
                val formattedCents = String.format(Locale.US, "%02d", remainingCents)
                "$prefix$formattedDollars,$formattedCents"
            } else {
                "$prefix$formattedDollars"
            }
        } else {
            val formattedCents = String.format(Locale.US, "%02d", remainingCents)
            "$trimmedSymbol$formattedDollars.$formattedCents"
        }

        return when {
            isNegative -> "-$base"
            includeSign && cents > 0L -> "+$base"
            else -> base
        }
    }

    /**
     * Parses a currency/decimal string (e.g. "50000", "50.000", "Rp 50.000", "12.50", "12")
     * into minor units (cents, e.g. 5000000L, 1250L).
     * Returns null if parsing fails or result is negative.
     */
    fun parseToCents(input: String): Long? {
        val trimmed = input.trim()
            .replace("Rp", "", ignoreCase = true)
            .replace("IDR", "", ignoreCase = true)
            .replace("$", "")
            .replace(" ", "")
        if (trimmed.isBlank()) return null

        return try {
            val sanitized = if (trimmed.contains(".") && !trimmed.contains(",")) {
                val parts = trimmed.split(".")
                if (parts.size > 1 && parts.drop(1).all { it.length == 3 }) {
                    trimmed.replace(".", "")
                } else if (parts.size == 2 && parts[1].length != 3) {
                    trimmed
                } else {
                    trimmed.replace(".", "")
                }
            } else if (trimmed.contains(",")) {
                val dotCleaned = trimmed.replace(".", "")
                dotCleaned.replace(",", ".")
            } else {
                trimmed
            }

            val doubleVal = sanitized.toDouble()
            if (doubleVal < 0) null else (doubleVal * 100).toLong()
        } catch (e: Exception) {
            null
        }
    }
}
