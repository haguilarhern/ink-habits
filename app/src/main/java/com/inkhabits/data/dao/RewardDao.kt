package com.inkhabits.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.inkhabits.data.entity.Reward
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardDao {
    @Query("SELECT * FROM rewards ORDER BY unlocked, targetStreak, sortOrder, createdAt")
    fun observeAll(): Flow<List<Reward>>

    @Query("SELECT * FROM rewards")
    suspend fun getAll(): List<Reward>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reward: Reward): Long

    @Update
    suspend fun update(reward: Reward)

    @Delete
    suspend fun delete(reward: Reward)
}
