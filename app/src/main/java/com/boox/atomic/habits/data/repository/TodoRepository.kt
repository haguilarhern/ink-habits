package com.boox.atomic.habits.data.repository

import com.boox.atomic.habits.data.dao.ToDoDao
import com.boox.atomic.habits.data.entity.ToDo
import kotlinx.coroutines.flow.Flow

class TodoRepository(
    private val toDoDao: ToDoDao
) {

    fun getActiveToDos(): Flow<List<ToDo>> {
        return toDoDao.getActiveToDos()
    }

    suspend fun createToDo(title: String): Long {
        return toDoDao.insert(ToDo(title = title))
    }

    suspend fun toggleComplete(id: Long) {
        val currentTime = System.currentTimeMillis()
        // Complete it first; if already completed, uncomplete it
        toDoDao.complete(id, currentTime)
    }

    suspend fun toggle(id: Long) {
        toDoDao.complete(id)
    }
}