package com.boox.atomic.habits.data.dao

import androidx.room.*
import com.boox.atomic.habits.data.entity.IdentityGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface IdentityGoalDao {
    @Query("SELECT * FROM identity_goals ORDER BY sortOrder")
    fun getAll(): Flow<List<IdentityGoal>>

    @Query("SELECT * FROM identity_goals WHERE id = :id")
    suspend fun getById(id: Long): IdentityGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: IdentityGoal): Long

    @Update
    suspend fun update(goal: IdentityGoal)

    @Delete
    suspend fun delete(goal: IdentityGoal)
}