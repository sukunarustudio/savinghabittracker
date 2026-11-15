package com.moneysaver.habittracker.domain.model

enum class TransactionType {
    INCOME,
    EXPENSE,
    SAVINGS_DEPOSIT,
    SAVINGS_WITHDRAWAL;

    val isInternalTransfer: Boolean
        get() = this == SAVINGS_DEPOSIT || this == SAVINGS_WITHDRAWAL
}
