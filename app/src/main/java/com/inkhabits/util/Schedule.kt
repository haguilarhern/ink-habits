package com.inkhabits.util

import com.inkhabits.data.entity.Frequency
import com.inkhabits.data.entity.Habit
import java.time.LocalDate

/**
 * Schedule-aware helpers. A habit is evaluated against its own frequency,
 * not raw calendar days, so streaks and the "never miss twice" rule key off
 * scheduled occurrences.
 */
object Schedule {

    private const val ISO_DAYS = "1234567" // Mon=1 .. Sun=7

    fun parseDays(csv: String): Set<Int> =
        csv.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..7 }.toSet()

    fun formatDays(days: Set<Int>): String = days.sorted().joinToString(",")

    /**
     * Whether [habit] is scheduled on [date]. WEEKLY_COUNT is flexible (any
     * day), so it returns true here; the weekly target is enforced separately
     * using completion counts.
     */
    fun isDueOn(habit: Habit, date: LocalDate): Boolean = when (habit.frequencyType) {
        Frequency.DAILY -> true
        Frequency.DAYS_OF_WEEK -> date.dayOfWeek.value in parseDays(habit.daysOfWeek)
        Frequency.INTERVAL -> {
            val n = habit.intervalDays.coerceAtLeast(1)
            val diff = date.toEpochDay() - habit.startEpochDay
            diff >= 0 && diff % n == 0L
        }
        Frequency.WEEKLY_COUNT -> true
        else -> true
    }

    /** Latest scheduled date strictly before [date], or null if none within a year. */
    fun previousOccurrence(habit: Habit, date: LocalDate): LocalDate? {
        var d = date.minusDays(1)
        var guard = 0
        while (guard < 366) {
            if (isDueOn(habit, d)) return d
            d = d.minusDays(1)
            guard++
        }
        return null
    }

    /** Earliest scheduled date strictly after [date], or null if none within a year. */
    fun nextOccurrence(habit: Habit, date: LocalDate): LocalDate? {
        var d = date.plusDays(1)
        var guard = 0
        while (guard < 366) {
            if (isDueOn(habit, d)) return d
            d = d.plusDays(1)
            guard++
        }
        return null
    }

    // Reminder sentinels (stored in Habit.reminderMinutes): a daypart instead of an hour.
    const val TIME_ANY = -1
    const val TIME_MORNING = -2
    const val TIME_AFTERNOON = -3
    const val TIME_EVENING = -4

    /** Formats the reminder as a daypart, a 12-hour time, or "" when unset. */
    fun formatTime(minutes: Int): String = when (minutes) {
        TIME_MORNING -> "Morning"
        TIME_AFTERNOON -> "Afternoon"
        TIME_EVENING -> "Evening"
        else -> {
            if (minutes < 0) ""
            else {
                val h24 = (minutes / 60) % 24
                val m = minutes % 60
                val ampm = if (h24 < 12) "AM" else "PM"
                var h12 = h24 % 12
                if (h12 == 0) h12 = 12
                "%d:%02d %s".format(h12, m, ampm)
            }
        }
    }

    /** Short human label for the schedule, e.g. "Daily", "Mon, Wed, Fri", "Every 2 days", "3x / week". */
    fun label(habit: Habit): String = when (habit.frequencyType) {
        Frequency.DAILY -> "Daily"
        Frequency.DAYS_OF_WEEK -> {
            val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val days = parseDays(habit.daysOfWeek).sorted()
            if (days.isEmpty()) "No days" else days.joinToString(", ") { names[it - 1] }
        }
        Frequency.INTERVAL -> {
            val n = habit.intervalDays.coerceAtLeast(1)
            if (n == 1) "Daily" else if (n == 2) "Every other day" else "Every $n days"
        }
        Frequency.WEEKLY_COUNT -> "${habit.weeklyTarget.coerceAtLeast(1)}x / week"
        else -> "Daily"
    }

    /**
     * Sort key (minutes-of-day) for ordering habits chronologically. Dayparts map to
     * representative times; "any time" sorts to the very end.
     */
    fun timeKey(minutes: Int): Int = when (minutes) {
        TIME_MORNING -> 8 * 60
        TIME_AFTERNOON -> 14 * 60
        TIME_EVENING -> 20 * 60
        TIME_ANY -> Int.MAX_VALUE
        else -> if (minutes < 0) Int.MAX_VALUE else minutes
    }

    /** Section label for the time-sorted view, derived from [timeKey]. */
    fun timeSection(minutes: Int): String = when (val k = timeKey(minutes)) {
        Int.MAX_VALUE -> "Anytime"
        else -> when {
            k < 12 * 60 -> "Morning"
            k < 17 * 60 -> "Afternoon"
            else -> "Evening"
        }
    }
}
