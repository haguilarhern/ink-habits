package com.inkhabits.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.inkhabits.data.entity.Habit
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isActive = 1 ORDER BY sortOrder, createdAt")
    fun observeActive(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE identityGoalId = :goalId AND isActive = 1 ORDER BY sortOrder, createdAt")
    fun observeForGoal(goalId: Long): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE isActive = 1 ORDER BY sortOrder, createdAt")
    suspend fun getActive(): List<Habit>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: Long): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: Habit): Long

    @Update
    suspend fun update(habit: Habit)

    @Delete
    suspend fun delete(habit: Habit)
}
