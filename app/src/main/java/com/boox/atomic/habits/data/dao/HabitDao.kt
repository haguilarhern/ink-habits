package com.boox.atomic.habits.data.dao

import androidx.room.*
import com.boox.atomic.habits.data.entity.Habit
import kotlinx.coroutines.flow.Flow

data class DashboardItem(
    val id: Long,
    val name: String,
    val identityStatement: String,
    val icon: String,
    val goalSortOrder: Int,
    val habitId: Long?,
    val habitName: String?,
    val habitStrokeData: String?,
    val frequencyType: String?,
    val intervalDays: Int?,
    val daysOfWeek: String?,
    val habitSortOrder: Int?,
    val isCompletedToday: Int
)

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE identityGoalId = :goalId AND isActive = 1 ORDER BY sortOrder")
    fun getHabitsForGoal(goalId: Long): Flow<List<Habit>>

    @Query("""
        SELECT g.*, h.id as habitId, h.name as habitName,
               h.strokeData as habitStrokeData,
               h.frequencyType, h.intervalDays, h.daysOfWeek,
               h.sortOrder as habitSortOrder,
               CASE WHEN hc.id IS NOT NULL THEN 1 ELSE 0 END as isCompletedToday
        FROM identity_goals g
        LEFT JOIN habits h ON h.identityGoalId = g.id AND h.isActive = 1
        LEFT JOIN habit_completions hc ON hc.habitId = h.id AND hc.date = :today
        ORDER BY g.sortOrder, h.sortOrder
    """)
    fun getFullDashboard(today: String): Flow<List<DashboardItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: Habit): Long

    @Update
    suspend fun update(habit: Habit)

    @Delete
    suspend fun delete(habit: Habit)
}