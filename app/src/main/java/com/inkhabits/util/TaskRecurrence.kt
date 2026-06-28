package com.inkhabits.util

import android.graphics.Color
import com.inkhabits.data.entity.Priority
import com.inkhabits.data.entity.Recur
import com.inkhabits.data.entity.ToDo
import java.time.LocalDate

/**
 * Helpers for recurring to-dos and Eisenhower prioritization: computing the next due
 * date when a recurring task is completed, and labels/colors for cadence, due dates,
 * and priority quadrants.
 */
object TaskRecurrence {

    fun isRecurring(t: ToDo): Boolean = t.recurType != Recur.NONE

    /**
     * Next due date for a recurring task, advancing from its current due date (or
     * [from] if it has none). Returns null for non-recurring tasks.
     */
    fun nextDue(t: ToDo, from: LocalDate = LocalDate.now()): LocalDate? {
        val base = if (t.dueEpochDay > 0) LocalDate.ofEpochDay(t.dueEpochDay) else from
        return when (t.recurType) {
            Recur.DAILY -> base.plusDays(1)
            Recur.INTERVAL -> base.plusDays(t.recurInterval.coerceAtLeast(1).toLong())
            Recur.DAYS_OF_WEEK -> {
                val days = Schedule.parseDays(t.recurDaysOfWeek)
                if (days.isEmpty()) return null
                var d = base.plusDays(1)
                var guard = 0
                while (guard < 14) {
                    if (d.dayOfWeek.value in days) return d
                    d = d.plusDays(1); guard++
                }
                null
            }
            else -> null
        }
    }

    /** Short cadence label, e.g. "Daily", "Every 3 days", "Mon, Wed, Fri". */
    fun recurLabel(t: ToDo): String = when (t.recurType) {
        Recur.DAILY -> "Daily"
        Recur.INTERVAL -> {
            val n = t.recurInterval.coerceAtLeast(1)
            if (n == 1) "Daily" else if (n == 2) "Every other day" else "Every $n days"
        }
        Recur.DAYS_OF_WEEK -> {
            val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val days = Schedule.parseDays(t.recurDaysOfWeek).sorted()
            if (days.isEmpty()) "Weekly" else days.joinToString(", ") { names[it - 1] }
        }
        else -> ""
    }

    /** Human due-date label, e.g. "Today", "Tomorrow", "Overdue · Jun 30", "Jul 4". */
    fun dueLabel(epochDay: Long, today: LocalDate = LocalDate.now()): String {
        if (epochDay <= 0) return ""
        val d = LocalDate.ofEpochDay(epochDay)
        val days = d.toEpochDay() - today.toEpochDay()
        val pretty = "${d.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)} ${d.dayOfMonth}"
        return when {
            days == 0L -> "Today"
            days == 1L -> "Tomorrow"
            days == -1L -> "Overdue · yesterday"
            days < 0 -> "Overdue · $pretty"
            else -> pretty
        }
    }

    fun isOverdue(epochDay: Long, today: LocalDate = LocalDate.now()): Boolean =
        epochDay in 1 until today.toEpochDay()

    // ---- Eisenhower priority ----

    /** Compact tag for a task's metadata row. */
    fun priorityShort(p: Int): String = when (p) {
        Priority.DO -> "Urgent + Important"
        Priority.SCHEDULE -> "Important"
        Priority.DELEGATE -> "Urgent"
        Priority.DROP -> "Low priority"
        else -> ""
    }

    /** Full urgency/importance label (Eisenhower quadrant). */
    fun priorityTitle(p: Int): String = when (p) {
        Priority.DO -> "Urgent & important"
        Priority.SCHEDULE -> "Important, not urgent"
        Priority.DELEGATE -> "Urgent, not important"
        Priority.DROP -> "Not urgent, not important"
        else -> "No priority"
    }

    fun priorityColor(p: Int): Int = when (p) {
        Priority.DO -> Color.parseColor("#0B0B0C")       // red
        Priority.SCHEDULE -> Color.parseColor("#5C5C5C") // green
        Priority.DELEGATE -> Color.parseColor("#0B0B0C") // amber
        Priority.DROP -> Color.parseColor("#5C5C5C")     // muted
        else -> Color.parseColor("#9A9AA0")
    }
}
