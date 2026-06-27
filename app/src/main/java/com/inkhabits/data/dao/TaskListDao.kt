package com.inkhabits.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.inkhabits.data.entity.TaskList
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskListDao {
    @Query("SELECT * FROM task_lists ORDER BY sortOrder, createdAt")
    fun observeAll(): Flow<List<TaskList>>

    @Query("SELECT * FROM task_lists ORDER BY sortOrder, createdAt")
    suspend fun getAll(): List<TaskList>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: TaskList): Long

    @Update
    suspend fun update(list: TaskList)

    @Delete
    suspend fun delete(list: TaskList)
}
