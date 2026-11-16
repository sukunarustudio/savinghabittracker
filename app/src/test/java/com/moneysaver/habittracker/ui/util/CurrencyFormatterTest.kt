package com.moneysaver.habittracker.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun `formatCents with Rp formats standard Rupiah without decimals`() {
        val result = CurrencyFormatter.formatCents(5000000L, "Rp")
        assertEquals("Rp 50.000", result)
    }

    @Test
    fun `formatCents with Rp formats millions properly with dot separator`() {
        val result = CurrencyFormatter.formatCents(150000000L, "Rp")
        assertEquals("Rp 1.500.000", result)
    }

    @Test
    fun `formatCents with Rp formats remaining cents with comma`() {
        val result = CurrencyFormatter.formatCents(5000050L, "Rp")
        assertEquals("Rp 50.000,50", result)
    }

    @Test
    fun `formatCents with US dollar formats standard dollar and cents`() {
        val result = CurrencyFormatter.formatCents(1250L, "$")
        assertEquals("$12.50", result)
    }

    @Test
    fun `formatCents handles negative values and signs`() {
        val negativeRp = CurrencyFormatter.formatCents(-5000000L, "Rp")
        assertEquals("-Rp 50.000", negativeRp)

        val positiveRpWithSign = CurrencyFormatter.formatCents(5000000L, "Rp", includeSign = true)
        assertEquals("+Rp 50.000", positiveRpWithSign)
    }

    @Test
    fun `parseToCents parses plain number into cents`() {
        val result = CurrencyFormatter.parseToCents("50000")
        assertNotNull(result)
        assertEquals(5000000L, result)
    }

    @Test
    fun `parseToCents parses Indonesian dot grouping correctly`() {
        val result = CurrencyFormatter.parseToCents("50.000")
        assertNotNull(result)
        assertEquals(5000000L, result)

        val million = CurrencyFormatter.parseToCents("Rp 1.500.000")
        assertNotNull(million)
        assertEquals(150000000L, million)
    }

    @Test
    fun `parseToCents parses US format correctly`() {
        val result = CurrencyFormatter.parseToCents("$12.50")
        assertNotNull(result)
        assertEquals(1250L, result)
    }

    @Test
    fun `parseToCents parses Indonesian comma decimal correctly`() {
        val result = CurrencyFormatter.parseToCents("50.000,50")
        assertNotNull(result)
        assertEquals(5000050L, result)
    }

    @Test
    fun `parseToCents returns null for blank or invalid input`() {
        assertNull(CurrencyFormatter.parseToCents(""))
        assertNull(CurrencyFormatter.parseToCents("   "))
        assertNull(CurrencyFormatter.parseToCents("abc"))
    }
}
