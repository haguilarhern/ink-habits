package com.inkhabits.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-defined list/classification for to-dos (e.g. "Work", "Home", "Errands").
 * Tasks reference a list by id; [ToDo.listId] == 0 means the unlisted "Inbox".
 */
@Entity(tableName = "task_lists")
data class TaskList(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    /** Accent color for the list's chip/dot, as a #RRGGBB string. */
    val colorHex: String = "#8C1D1D",
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
