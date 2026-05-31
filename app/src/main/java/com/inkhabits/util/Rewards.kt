package com.inkhabits.util

import android.content.Context
import com.inkhabits.data.AppDatabase
import com.inkhabits.data.entity.Reward
import com.inkhabits.notify.NotificationHelper
import java.time.LocalDate

/**
 * Gamification: unlock self-rewards when their target streak is reached.
 *
 * A reward can track three bases:
 *  - "Any habit"  (habitId == 0 && identityId == 0): the longest current streak
 *    across all active habits.
 *  - a specific habit (habitId > 0): that habit's current streak.
 *  - an identity (identityId > 0): that identity's current perfect-day streak.
 */
object Rewards {

    /** Longest current streak across all active habits. */
    suspend fun maxHabitStreak(db: AppDatabase, today: LocalDate = LocalDate.now()): Int {
        var max = 0
        for (h in db.habitDao().getActive()) {
            val completed = db.habitCompletionDao().getForHabit(h.id).map { it.date }.toSet()
            val s = Streaks.computeStreak(h, completed, today)
            if (s > max) max = s
        }
        return max
    }

    /** Current streak for a single habit. */
    suspend fun habitStreak(db: AppDatabase, habitId: Long, today: LocalDate = LocalDate.now()): Int {
        val habit = db.habitDao().getById(habitId) ?: return 0
        val completed = db.habitCompletionDao().getForHabit(habitId).map { it.date }.toSet()
        return Streaks.computeStreak(habit, completed, today)
    }

    /** Current perfect-day streak for one identity (all its due habits completed). */
    suspend fun identityStreak(db: AppDatabase, identityId: Long, today: LocalDate = LocalDate.now()): Int {
        val habits = db.habitDao().getActive().filter { it.identityGoalId == identityId }
        if (habits.isEmpty()) return 0
        val byHabit = habits.associate { h ->
            h.id to db.habitCompletionDao().getForHabit(h.id).map { it.date }.toSet()
        }
        return Streaks.perfectDayStreak(habits, byHabit, today)
    }

    /** The current streak value that [reward] is measured against. */
    suspend fun streakFor(db: AppDatabase, reward: Reward, today: LocalDate = LocalDate.now()): Int = when {
        reward.habitId > 0 -> habitStreak(db, reward.habitId, today)
        reward.identityId > 0 -> identityStreak(db, reward.identityId, today)
        else -> maxHabitStreak(db, today)
    }

    /**
     * Unlock any locked rewards whose target streak has been reached, and notify.
     * Safe to call after every completion toggle (app or widget).
     */
    suspend fun checkAndUnlock(context: Context) {
        val db = AppDatabase.get(context)
        val today = LocalDate.now()
        db.rewardDao().getAll()
            .filter { !it.unlocked && it.targetStreak >= 1 }
            .forEach { r ->
                if (streakFor(db, r, today) >= r.targetStreak) {
                    db.rewardDao().update(r.copy(unlocked = true, unlockedAt = System.currentTimeMillis()))
                    NotificationHelper.showRewardUnlocked(context, r.title, r.id)
                }
            }
    }
}
