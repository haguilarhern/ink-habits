package com.inkhabits.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.inkhabits.data.entity.IdentityGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface IdentityGoalDao {
    @Query("SELECT * FROM identity_goals ORDER BY sortOrder, createdAt")
    fun observeAll(): Flow<List<IdentityGoal>>

    @Query("SELECT * FROM identity_goals ORDER BY sortOrder, createdAt")
    suspend fun getAll(): List<IdentityGoal>

    @Query("SELECT COUNT(*) FROM identity_goals")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: IdentityGoal): Long

    @Update
    suspend fun update(goal: IdentityGoal)

    @Delete
    suspend fun delete(goal: IdentityGoal)
}
