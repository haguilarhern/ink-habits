package com.inkhabits.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Frequency model for a habit.
 *
 * - DAILY: due every day.
 * - DAYS_OF_WEEK: due on specific weekdays (see [Habit.daysOfWeek]).
 * - INTERVAL: due every N days from [Habit.startDate] (every other day = 2).
 * - WEEKLY_COUNT: flexible target of N completions per calendar week,
 *   any days (see [Habit.weeklyTarget]).
 */
object Frequency {
    const val DAILY = "DAILY"
    const val DAYS_OF_WEEK = "DAYS_OF_WEEK"
    const val INTERVAL = "INTERVAL"
    const val WEEKLY_COUNT = "WEEKLY_COUNT"
}

@Entity(
    tableName = "habits",
    foreignKeys = [ForeignKey(
        entity = IdentityGoal::class,
        parentColumns = ["id"],
        childColumns = ["identityGoalId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("identityGoalId")]
)
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val identityGoalId: Long,
    val name: String = "",
    val nameStrokes: String = "",
    val frequencyType: String = Frequency.DAILY,
    /** Comma-separated ISO day numbers, Mon=1..Sun=7. Used by DAYS_OF_WEEK. */
    val daysOfWeek: String = "",
    /** Every N days. Used by INTERVAL. */
    val intervalDays: Int = 1,
    /** Completions targeted per week. Used by WEEKLY_COUNT. */
    val weeklyTarget: Int = 1,
    /** Epoch day (UTC) the habit started; anchor for INTERVAL math. */
    val startEpochDay: Long = 0,
    /** Preferred time of day in minutes after midnight; -1 = any time. */
    val reminderMinutes: Int = -1,
    /**
     * Optional habit-stacking anchor cue (e.g. "my morning coffee"). Not tracked;
     * shown as a small reminder of when/after-what to do the habit. May be typed
     * ([anchor]) or handwritten ([anchorStrokes]).
     */
    val anchor: String = "",
    val anchorStrokes: String = "",
    /**
     * Goal streak (target consecutive completions) driving this habit's progress
     * bar. 0 = inherit the parent identity's goal; > 0 = a custom per-habit goal.
     */
    val goalDays: Int = 0,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
