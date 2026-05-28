package com.inkhabits.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A standalone to-do task. Separate from the core habit flow.
 * Title may be typed text or handwritten ink.
 */
@Entity(tableName = "todos")
data class ToDo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val titleStrokes: String = "",
    val isDone: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
