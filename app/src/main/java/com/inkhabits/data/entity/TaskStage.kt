package com.inkhabits.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Fixed roles for the two un-deletable, un-movable Kanban stages. */
object StageRole {
    const val NONE = ""      // a normal, user-managed custom stage
    const val TODO = "TODO"  // always the first column
    const val DONE = "DONE"  // always the last column; tasks here are marked done
}

/**
 * A Kanban stage/column. Tasks reference a stage via [ToDo.stageId]; 0 (or an unknown
 * id) falls into the first stage. Two stages are seeded and fixed — "To Do" ([StageRole.TODO],
 * always first) and "Done" ([StageRole.DONE], always last). Custom stages live in between
 * and can be added, renamed, reordered, and deleted.
 */
@Entity(tableName = "task_stages")
data class TaskStage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val sortOrder: Int = 0,
    /** "" = custom; otherwise a fixed [StageRole]. */
    val role: String = StageRole.NONE,
    val createdAt: Long = System.currentTimeMillis()
)
