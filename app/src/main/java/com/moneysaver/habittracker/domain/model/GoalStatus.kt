package com.moneysaver.habittracker.domain.model

enum class GoalStatus {
    IN_PROGRESS,
    ACHIEVED,
    OVERDUE,
    CANCELLED;

    val isActive: Boolean
        get() = this == IN_PROGRESS || this == ACHIEVED || this == OVERDUE
}
