package com.inkhabits.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Recurrence cadence for a to-do. NONE = a normal one-off task. The others mirror
 * the habit [Frequency] model so recurring tasks regenerate on a familiar schedule.
 */
object Recur {
    const val NONE = "NONE"
    const val DAILY = "DAILY"
    const val DAYS_OF_WEEK = "DAYS_OF_WEEK"
    const val INTERVAL = "INTERVAL"
}

/**
 * Eisenhower-matrix importance score (0 = unset). The quadrant is (urgent, important):
 *  1 = Do      (urgent & important)
 *  2 = Schedule(important, not urgent)
 *  3 = Delegate(urgent, not important)
 *  4 = Drop    (neither)
 */
object Priority {
    const val NONE = 0
    const val DO = 1
    const val SCHEDULE = 2
    const val DELEGATE = 3
    const val DROP = 4
}

/**
 * A standalone to-do task. Separate from the core habit flow. Title may be typed text
 * or handwritten ink. Tasks can be classified into a [TaskList], given a [dueEpochDay]
 * and an Eisenhower [priority], and set to recur on a cadence (regenerating a fresh
 * instance when completed).
 */
@Entity(tableName = "todos")
data class ToDo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val titleStrokes: String = "",
    val isDone: Boolean = false,
    /** Owning list; 0 = the unlisted "Inbox". */
    val listId: Long = 0,
    /** Due date as an epoch day; 0 = no due date. */
    val dueEpochDay: Long = 0,
    /** Eisenhower importance 0..4 (see [Priority]); 0 = unset. */
    val priority: Int = Priority.NONE,
    /** Recurrence cadence (see [Recur]). */
    val recurType: String = Recur.NONE,
    /** Every N days, for [Recur.INTERVAL]. */
    val recurInterval: Int = 1,
    /** Comma-separated ISO day numbers (Mon=1..Sun=7), for [Recur.DAYS_OF_WEEK]. */
    val recurDaysOfWeek: String = "",
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
