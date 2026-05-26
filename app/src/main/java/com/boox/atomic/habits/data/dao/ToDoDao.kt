package com.boox.atomic.habits.data.dao

import androidx.room.*
import com.boox.atomic.habits.data.entity.ToDo
import kotlinx.coroutines.flow.Flow

@Dao
interface ToDoDao {
    @Query("SELECT * FROM todos WHERE isCompleted = 0 ORDER BY sortOrder")
    fun getActiveToDos(): Flow<List<ToDo>>

    @Query("SELECT * FROM todos ORDER BY isCompleted ASC, sortOrder ASC")
    fun getAllToDos(): Flow<List<ToDo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: ToDo): Long

    @Update
    suspend fun update(todo: ToDo)

    @Delete
    suspend fun delete(todo: ToDo)

    @Query("UPDATE todos SET isCompleted = 1, completedAt = :now WHERE id = :id")
    suspend fun complete(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE todos SET isCompleted = 0, completedAt = NULL WHERE id = :id")
    suspend fun uncomplete(id: Long)
}