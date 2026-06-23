package com.factory.habitharmony.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.factory.habitharmony.data.entity.HabitCompletionEntity
import com.factory.habitharmony.data.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    // ── Habit CRUD ────────────────────────────────────────────────────────────

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAt ASC")
    fun getAllActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabitById(id: Long): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("UPDATE habits SET isArchived = 1 WHERE id = :habitId")
    suspend fun archiveHabit(habitId: Long)

    // ── Completion CRUD ───────────────────────────────────────────────────────

    @Query(
        "SELECT * FROM habit_completions " +
        "WHERE completedDate >= :sinceDate " +
        "ORDER BY habitId, completedDate DESC"
    )
    fun getCompletionsSince(sinceDate: Long): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE completedDate = :date")
    fun getCompletionsForDate(date: Long): Flow<List<HabitCompletionEntity>>

    @Query(
        "SELECT * FROM habit_completions WHERE habitId = :habitId " +
        "ORDER BY completedDate DESC"
    )
    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletionEntity>>

    @Query(
        "SELECT * FROM habit_completions WHERE habitId = :habitId " +
        "ORDER BY completedDate DESC LIMIT :limit"
    )
    suspend fun getRecentCompletions(habitId: Long, limit: Int = 365): List<HabitCompletionEntity>

    @Query(
        "SELECT * FROM habit_completions WHERE habitId = :habitId AND completedDate = :date LIMIT 1"
    )
    suspend fun getCompletion(habitId: Long, date: Long): HabitCompletionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletion(completion: HabitCompletionEntity)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND completedDate = :date")
    suspend fun deleteCompletion(habitId: Long, date: Long)

    @Query("SELECT COUNT(*) FROM habit_completions WHERE habitId = :habitId")
    suspend fun getTotalCompletions(habitId: Long): Int
}
