package com.boox.atomic.habits.data.dao

import androidx.room.*
import com.boox.atomic.habits.data.entity.HabitCompletion

@Dao
interface HabitCompletionDao {
    @Query("SELECT date FROM habit_completions WHERE habitId = :habitId ORDER BY date DESC")
    suspend fun getCompletionDates(habitId: Long): List<String>

    @Query("SELECT COUNT(*) FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun isCompleted(habitId: Long, date: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(completion: HabitCompletion): Long

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun removeCompletion(habitId: Long, date: String)

    @Query("SELECT date FROM habit_completions WHERE habitId = :habitId AND date >= :sinceDate ORDER BY date DESC")
    suspend fun getStreakWindow(habitId: Long, sinceDate: String): List<String>
}