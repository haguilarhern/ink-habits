package com.inkhabits.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.inkhabits.data.entity.TaskStage
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskStageDao {
    @Query("SELECT * FROM task_stages ORDER BY sortOrder, createdAt")
    fun observeAll(): Flow<List<TaskStage>>

    @Query("SELECT * FROM task_stages ORDER BY sortOrder, createdAt")
    suspend fun getAll(): List<TaskStage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stage: TaskStage): Long

    @Update
    suspend fun update(stage: TaskStage)

    @Delete
    suspend fun delete(stage: TaskStage)
}
