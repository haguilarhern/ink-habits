package com.inkhabits.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-defined Kanban stage/column (e.g. "To Do", "In Progress", "Done").
 * Tasks reference a stage by id via [ToDo.stageId]; 0 (or an unknown id) falls into
 * the first stage.
 */
@Entity(tableName = "task_stages")
data class TaskStage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
