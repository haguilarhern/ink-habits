package com.inkhabits.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inkhabits.data.entity.HabitCompletion
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitCompletionDao {
    @Query("SELECT * FROM habit_completions")
    fun observeAll(): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY date")
    suspend fun getForHabit(habitId: Long): List<HabitCompletion>

    @Query("SELECT * FROM habit_completions WHERE date = :date")
    suspend fun getForDate(date: String): List<HabitCompletion>

    @Query("SELECT EXISTS(SELECT 1 FROM habit_completions WHERE habitId = :habitId AND date = :date)")
    suspend fun isCompleted(habitId: Long, date: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(completion: HabitCompletion): Long

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun delete(habitId: Long, date: String)
}
