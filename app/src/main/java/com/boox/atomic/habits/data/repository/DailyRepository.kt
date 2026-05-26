package com.boox.atomic.habits.data.repository

import com.boox.atomic.habits.data.dao.DashboardItem
import com.boox.atomic.habits.data.entity.ToDo
import kotlinx.coroutines.flow.Flow

class DailyRepository(
    private val habitRepository: HabitRepository,
    private val todoRepository: TodoRepository
) {

    fun getFullDashboard(): Flow<Pair<List<DashboardItem>, List<ToDo>>> {
        // Combine both flows into a single Flow of pairs
        return combineFlows(
            habitRepository.getFullDashboard(),
            todoRepository.getActiveToDos()
        )
    }

    private fun combineFlows(
        dashboardFlow: Flow<List<DashboardItem>>,
        todoFlow: Flow<List<ToDo>>
    ): Flow<Pair<List<DashboardItem>, List<ToDo>>> {
        return kotlinx.coroutines.flow.combine(dashboardFlow, todoFlow) { items, todos ->
            Pair(items, todos)
        }
    }
}