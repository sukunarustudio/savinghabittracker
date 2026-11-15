package com.moneysaver.habittracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.moneysaver.habittracker.domain.model.habit.FrequencyType

@Entity(
    tableName = "habits",
    indices = [
        Index(value = ["isArchived"]),
        Index(value = ["createdAt"])
    ]
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val frequencyType: FrequencyType = FrequencyType.DAILY,
    val scheduledDaysBitmask: Int = 127,
    val reminderTime: Long? = null,
    val isArchived: Boolean = false,
    val archivedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
