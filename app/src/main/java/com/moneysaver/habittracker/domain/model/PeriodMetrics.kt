package com.moneysaver.habittracker.domain.model

data class PeriodMetrics(
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val netCashFlow: Long = 0L,
    val transactionCount: Int = 0
) {
    val netSavings: Long
        get() = netCashFlow
}
