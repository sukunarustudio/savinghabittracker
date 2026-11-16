package com.moneysaver.habittracker.data.repository

import com.moneysaver.habittracker.data.local.dao.HabitCompletionLogDao
import com.moneysaver.habittracker.data.local.dao.HabitDao
import com.moneysaver.habittracker.data.local.entity.HabitCompletionLogEntity
import com.moneysaver.habittracker.data.local.entity.HabitEntity
import com.moneysaver.habittracker.domain.model.habit.FrequencyType
import com.moneysaver.habittracker.domain.model.habit.Habit
import com.moneysaver.habittracker.domain.model.habit.HabitCompletionLog
import com.moneysaver.habittracker.domain.model.habit.HabitSchedule
import com.moneysaver.habittracker.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

fun HabitEntity.toDomain(): Habit {
    val targetDays = mutableSetOf<DayOfWeek>()
    for (day in DayOfWeek.values()) {
        if ((scheduledDaysBitmask and (1 shl (day.value - 1))) != 0) {
            targetDays.add(day)
        }
    }

    val remTime = reminderTime?.let {
        val totalSecs = (it / 1000).toInt()
        val hour = (totalSecs / 3600) % 24
        val min = (totalSecs % 3600) / 60
        LocalTime.of(hour, min)
    }

    val createdDate = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
    val archDate = archivedAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }

    return Habit(
        id = id.toString(),
        name = name,
        description = "",
        schedule = HabitSchedule(
            frequencyType = frequencyType,
            targetDays = if (frequencyType == FrequencyType.DAILY) emptySet() else targetDays,
            reminderTime = remTime,
            effectiveFrom = createdDate
        ),
        scheduleHistory = emptyList(),
        createdAt = createdDate,
        archivedAt = archDate,
        isArchived = isArchived
    )
}

fun Habit.toEntity(): HabitEntity {
    val bitmask = when (schedule.frequencyType) {
        FrequencyType.DAILY -> 127
        FrequencyType.CUSTOM_DAYS -> {
            var mask = 0
            for (day in schedule.targetDays) {
                mask = mask or (1 shl (day.value - 1))
            }
            mask
        }
    }

    val remMillis = schedule.reminderTime?.let {
        ((it.hour * 3600L) + (it.minute * 60L) + it.second) * 1000L
    }

    val createdMillis = createdAt.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val archMillis = archivedAt?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

    return HabitEntity(
        id = id.toLongOrNull() ?: 0L,
        name = name,
        frequencyType = schedule.frequencyType,
        scheduledDaysBitmask = bitmask,
        reminderTime = remMillis,
        isArchived = isArchived,
        archivedAt = archMillis,
        createdAt = createdMillis
    )
}

fun HabitCompletionLogEntity.toDomain(): HabitCompletionLog = HabitCompletionLog(
    id = id.toString(),
    habitId = habitId.toString(),
    completedDate = LocalDate.parse(completedDate),
    completedAt = Instant.ofEpochMilli(completedAt)
)

fun HabitCompletionLog.toEntity(): HabitCompletionLogEntity = HabitCompletionLogEntity(
    id = id.toLongOrNull() ?: 0L,
    habitId = habitId.toLongOrNull() ?: 0L,
    completedDate = completedDate.toString(),
    completedAt = completedAt.toEpochMilli()
)

class RoomHabitRepository(
    private val habitDao: HabitDao,
    private val habitCompletionLogDao: HabitCompletionLogDao
) : HabitRepository {

    override fun getHabits(): Flow<List<Habit>> {
        return habitDao.getAllHabits().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getHabitById(id: String): Habit? {
        val longId = id.toLongOrNull() ?: return null
        return habitDao.getHabitById(longId)?.toDomain()
    }

    override suspend fun insertHabit(habit: Habit) {
        val entity = habit.toEntity()
        val generatedId = habitDao.insert(entity)
    }

    override suspend fun updateHabit(habit: Habit) {
        habitDao.update(habit.toEntity())
    }

    override suspend fun deleteHabit(id: String) {
        val longId = id.toLongOrNull() ?: return
        habitDao.deleteById(longId)
        habitCompletionLogDao.deleteLogsForHabit(longId)
    }

    override fun getCompletionLogs(habitId: String): Flow<List<HabitCompletionLog>> {
        val longId = habitId.toLongOrNull() ?: 0L
        return habitCompletionLogDao.getLogsForHabit(longId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getCompletionLogsOnce(habitId: String): List<HabitCompletionLog> {
        val longId = habitId.toLongOrNull() ?: 0L
        return habitCompletionLogDao.getLogsForHabitList(longId).map { it.toDomain() }
    }

    override suspend fun getCompletionLog(habitId: String, date: LocalDate): HabitCompletionLog? {
        val longId = habitId.toLongOrNull() ?: 0L
        val logs = habitCompletionLogDao.getLogsForDateList(date.toString())
        return logs.firstOrNull { it.habitId == longId }?.toDomain()
    }

    override suspend fun insertCompletionLog(log: HabitCompletionLog) {
        habitCompletionLogDao.insert(log.toEntity())
    }

    override suspend fun deleteCompletionLog(habitId: String, date: LocalDate) {
        val longId = habitId.toLongOrNull() ?: return
        habitCompletionLogDao.deleteByDate(longId, date.toString())
    }

    override fun getAllCompletionLogs(): Flow<List<HabitCompletionLog>> {
        return habitCompletionLogDao.getAllLogs().map { list -> list.map { it.toDomain() } }
    }
}
