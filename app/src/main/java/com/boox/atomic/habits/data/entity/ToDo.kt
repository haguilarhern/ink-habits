package com.boox.atomic.habits.data.entity

import androidx.room.*

@Entity(tableName = "todos")
data class ToDo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val strokeData: String = "",
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
