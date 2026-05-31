package com.inkhabits.util

import com.inkhabits.data.entity.Frequency
import com.inkhabits.data.entity.Habit
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Streak and completion math, evaluated against each habit's schedule
 * (consecutive scheduled occurrences — not raw calendar days).
 */
object Streaks {

    fun isCompletedOn(date: LocalDate, completed: Set<String>): Boolean =
        date.toString() in completed

    /**
     * Consecutive completed scheduled occurrences ending at the most recent
     * occurrence on/before today. Today being not-yet-done does not break the
     * streak; it just isn't counted until completed.
     */
    fun computeStreak(habit: Habit, completed: Set<String>, today: LocalDate): Int {
        if (habit.frequencyType == Frequency.WEEKLY_COUNT) {
            return weeklyStreak(habit, completed, today)
        }
        var d: LocalDate? = if (Schedule.isDueOn(habit, today)) today
        else Schedule.previousOccurrence(habit, today)
        if (d == today && today.toString() !in completed) {
            d = Schedule.previousOccurrence(habit, today)
        }
        var count = 0
        var guard = 0
        while (d != null && guard < 366) {
            if (d.toString() in completed) {
                count++
                d = Schedule.previousOccurrence(habit, d)
            } else break
            guard++
        }
        return count
    }

    /** Longest run of consecutive completed scheduled occurrences over history. */
    fun bestStreak(habit: Habit, completed: Set<String>, today: LocalDate): Int {
        if (completed.isEmpty()) return 0
        val earliest = completed.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.minOrNull()
            ?: return 0
        var d = earliest
        var best = 0
        var run = 0
        var guard = 0
        while (!d.isAfter(today) && guard < 2000) {
            if (Schedule.isDueOn(habit, d)) {
                if (d.toString() in completed) {
                    run++; if (run > best) best = run
                } else {
                    run = 0
                }
            }
            d = d.plusDays(1)
            guard++
        }
        return best
    }

    /**
     * State for each of the last [days] days ending today (oldest first):
     * 0 = not scheduled, 1 = completed, 2 = scheduled but missed.
     */
    fun dayStates(habit: Habit, completed: Set<String>, today: LocalDate, days: Int): IntArray {
        val out = IntArray(days)
        for (i in 0 until days) {
            val date = today.minusDays((days - 1 - i).toLong())
            out[i] = when {
                !Schedule.isDueOn(habit, date) -> 0
                date.toString() in completed -> 1
                else -> 2
            }
        }
        return out
    }

    /**
     * "Never miss twice" risk: true when the user has already missed the previous
     * scheduled occurrence and the next one is due now (and not yet done). For
     * weekly-target habits, true when last week missed the target and this week
     * has not yet reached it.
     */
    fun atRiskOfMissingTwice(habit: Habit, completed: Set<String>, today: LocalDate): Boolean {
        if (habit.frequencyType == Frequency.WEEKLY_COUNT) {
            val target = habit.weeklyTarget.coerceAtLeast(1)
            val thisWeek = weeklyCount(completed, today)
            val lastWeek = weeklyCount(completed, today.minusWeeks(1))
            return lastWeek < target && thisWeek < target
        }
        if (!Schedule.isDueOn(habit, today)) return false
        if (today.toString() in completed) return false
        val prev = Schedule.previousOccurrence(habit, today) ?: return false
        return prev.toString() !in completed
    }

    /**
     * Headline "perfect-day" streak: consecutive days on which every habit that
     * was due was completed. An incomplete today does not break it (counting just
     * starts from yesterday); days with nothing due are skipped, not broken.
     */
    fun perfectDayStreak(
        habits: List<Habit>,
        completedByHabit: Map<Long, Set<String>>,
        today: LocalDate
    ): Int {
        if (habits.isEmpty()) return 0
        fun allDoneOn(day: LocalDate): Boolean? {
            val due = habits.filter { Schedule.isDueOn(it, day) }
            if (due.isEmpty()) return null // nothing due
            return due.all { (completedByHabit[it.id] ?: emptySet()).contains(day.toString()) }
        }

        var day = today
        if (allDoneOn(today) != true) day = today.minusDays(1)

        var count = 0
        var guard = 0
        while (guard < 1000) {
            when (allDoneOn(day)) {
                true -> { count++; day = day.minusDays(1) }
                null -> { day = day.minusDays(1) } // skip empty days
                false -> break
            }
            guard++
        }
        return count
    }

    /**
     * Cumulative count of perfect days over all history (not necessarily
     * consecutive): every day on which at least one habit was due and all due
     * habits were completed. Used for identity progress goals.
     */
    fun totalPerfectDays(
        habits: List<Habit>,
        completedByHabit: Map<Long, Set<String>>,
        today: LocalDate
    ): Int {
        if (habits.isEmpty()) return 0
        val earliest = completedByHabit.values
            .flatMap { it }
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .minOrNull() ?: return 0
        var day = earliest
        var count = 0
        var guard = 0
        while (!day.isAfter(today) && guard < 5000) {
            val due = habits.filter { Schedule.isDueOn(it, day) }
            if (due.isNotEmpty() &&
                due.all { (completedByHabit[it.id] ?: emptySet()).contains(day.toString()) }) {
                count++
            }
            day = day.plusDays(1)
            guard++
        }
        return count
    }

    /** Number of completions in the calendar week containing [date]. */
    fun weeklyCount(completed: Set<String>, date: LocalDate): Int {
        val wf = WeekFields.of(Locale.getDefault())
        val week = date.get(wf.weekOfWeekBasedYear())
        val year = date.get(wf.weekBasedYear())
        return completed.count {
            val d = runCatching { LocalDate.parse(it) }.getOrNull() ?: return@count false
            d.get(wf.weekOfWeekBasedYear()) == week && d.get(wf.weekBasedYear()) == year
        }
    }

    /** For weekly-target habits: count consecutive prior weeks that hit the target. */
    private fun weeklyStreak(habit: Habit, completed: Set<String>, today: LocalDate): Int {
        val target = habit.weeklyTarget.coerceAtLeast(1)
        var weekRef = today
        var count = 0
        var guard = 0
        // current week counts only if target already met
        if (weeklyCount(completed, weekRef) >= target) count++
        weekRef = weekRef.minusWeeks(1)
        while (guard < 260) {
            if (weeklyCount(completed, weekRef) >= target) {
                count++
                weekRef = weekRef.minusWeeks(1)
            } else break
            guard++
        }
        return count
    }
}
