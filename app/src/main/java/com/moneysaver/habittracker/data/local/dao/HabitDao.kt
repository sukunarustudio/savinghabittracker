package com.moneysaver.habittracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneysaver.habittracker.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    @Delete
    suspend fun delete(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Long): HabitEntity?

    @Query("SELECT * FROM habits WHERE id = :id")
    fun getHabitByIdFlow(id: Long): Flow<HabitEntity?>

    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    suspend fun getAllHabitsList(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAt ASC")
    fun getActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAt ASC")
    suspend fun getActiveHabitsList(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE isArchived = 1 ORDER BY archivedAt DESC")
    fun getArchivedHabits(): Flow<List<HabitEntity>>

    @Query("UPDATE habits SET isArchived = 1, archivedAt = :archivedAt WHERE id = :id")
    suspend fun archiveHabit(id: Long, archivedAt: Long = System.currentTimeMillis())

    @Query("UPDATE habits SET isArchived = 0, archivedAt = NULL WHERE id = :id")
    suspend fun unarchiveHabit(id: Long)
}
