package com.inkhabits.util

import android.content.Context
import java.time.LocalDate

/**
 * Persistent per-year tally of completed to-dos. A cumulative achievement counter
 * (increments when a to-do is checked off, decrements only when un-checked) that
 * survives clearing/reusing the notebook lines.
 */
object YearTally {
    private const val PREFS = "todo_tally"

    private fun key(year: Int) = "done_$year"

    fun get(context: Context, year: Int = LocalDate.now().year): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(key(year), 0)

    /** Adjust this year's tally by [delta] (clamped at 0). Returns the new value. */
    fun add(context: Context, delta: Int): Int {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val y = LocalDate.now().year
        val next = (p.getInt(key(y), 0) + delta).coerceAtLeast(0)
        p.edit().putInt(key(y), next).apply()
        return next
    }
}
