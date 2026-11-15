package com.moneysaver.habittracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.moneysaver.habittracker.domain.model.GoalStatus

@Entity(
    tableName = "savings_goals",
    indices = [
        Index(value = ["status"]),
        Index(value = ["targetDate"])
    ]
)
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val targetAmount: Long,
    val savedAmount: Long = 0L,
    val targetDate: Long? = null,
    val status: GoalStatus = GoalStatus.IN_PROGRESS
)
