package com.moneysaver.habittracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_completion_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId", "completedDate"], unique = true),
        Index(value = ["habitId"]),
        Index(value = ["completedDate"])
    ]
)
data class HabitCompletionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val habitId: Long,
    val completedDate: String, // ISO date string YYYY-MM-DD
    val completedAt: Long = System.currentTimeMillis()
)
