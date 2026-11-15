package com.moneysaver.habittracker.domain.model

data class Transaction(
    val id: Long = 0L,
    val amount: Long, // represented in minor units (cents), must be > 0
    val type: TransactionType,
    val categoryId: Long? = null,
    val savingsGoalId: Long? = null,
    val isInternalTransfer: Boolean = (type == TransactionType.SAVINGS_DEPOSIT || type == TransactionType.SAVINGS_WITHDRAWAL),
    val timestamp: Long,
    val note: String = ""
) {
    val signedAmount: Long
        get() = when (type) {
            TransactionType.INCOME -> amount
            TransactionType.SAVINGS_WITHDRAWAL -> amount
            TransactionType.EXPENSE -> -amount
            TransactionType.SAVINGS_DEPOSIT -> -amount
        }
}
