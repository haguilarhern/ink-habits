package com.inkhabits.util

import com.inkhabits.data.entity.Habit
import com.inkhabits.data.entity.IdentityGoal

/**
 * Goal-streak resolution. Goals are configurable at both the identity and the
 * habit level; an unset value falls back to a sensible default, and a habit with
 * no goal of its own inherits its parent identity's goal.
 */
object Goals {

    /** Default goal when nothing is set — the popular "~66 days to form a habit". */
    const val DEFAULT = 66

    /** Preset choices offered in the goal pickers. */
    val PRESETS = listOf(21, 30, 66, 90, 100, 180, 365)

    /** Effective goal for an identity (its own value, or the default). */
    fun identityGoal(identity: IdentityGoal): Int =
        identity.goalDays.takeIf { it > 0 } ?: DEFAULT

    /** Effective goal for a habit: its own value, else the identity's effective goal. */
    fun habitGoal(habit: Habit, identityGoal: Int): Int =
        habit.goalDays.takeIf { it > 0 } ?: identityGoal

    /** True when the habit has no goal of its own and rides the identity's. */
    fun habitInherits(habit: Habit): Boolean = habit.goalDays <= 0
}
