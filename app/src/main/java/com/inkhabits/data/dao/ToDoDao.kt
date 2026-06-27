package com.inkhabits.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.inkhabits.data.entity.ToDo
import kotlinx.coroutines.flow.Flow

@Dao
interface ToDoDao {
    @Query("SELECT * FROM todos ORDER BY isDone, sortOrder, createdAt")
    fun observeAll(): Flow<List<ToDo>>

    @Query("SELECT * FROM todos ORDER BY isDone, sortOrder, createdAt")
    suspend fun getAll(): List<ToDo>

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getById(id: Long): ToDo?

    /** Clear a list reference from its tasks (used before deleting the list). */
    @Query("UPDATE todos SET listId = 0 WHERE listId = :listId")
    suspend fun clearList(listId: Long)

    /** Clear a stage reference from its tasks (used before deleting the stage). */
    @Query("UPDATE todos SET stageId = 0 WHERE stageId = :stageId")
    suspend fun clearStage(stageId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: ToDo): Long

    @Update
    suspend fun update(todo: ToDo)

    @Delete
    suspend fun delete(todo: ToDo)
}
