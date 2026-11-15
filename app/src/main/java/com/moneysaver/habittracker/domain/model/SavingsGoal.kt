package com.moneysaver.habittracker.domain.model

data class SavingsGoal(
    val id: Long = 0L,
    val title: String,
    val targetAmount: Long, // represented in minor units (cents)
    val savedAmount: Long = 0L, // represented in minor units (cents)
    val targetDate: Long? = null,
    val status: GoalStatus = GoalStatus.IN_PROGRESS
) {
    val remainingAmount: Long
        get() = (targetAmount - savedAmount).coerceAtLeast(0L)

    val progress: Float
        get() = if (targetAmount > 0L) {
            (savedAmount.toFloat() / targetAmount.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val isAchieved: Boolean
        get() = savedAmount >= targetAmount
}
