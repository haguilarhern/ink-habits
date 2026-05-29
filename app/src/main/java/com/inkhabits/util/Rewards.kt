package com.inkhabits.util

import android.content.Context
import com.inkhabits.data.AppDatabase
import com.inkhabits.data.entity.Reward
import com.inkhabits.notify.NotificationHelper
import java.time.LocalDate

/**
 * Gamification: unlock self-rewards when a habit reaches its target streak.
 * "Hit a streak of doing a habit X times" = the longest current per-habit streak.
 */
object Rewards {

    /** Longest current streak across all active habits (the unlock metric). */
    suspend fun maxHabitStreak(db: AppDatabase, today: LocalDate = LocalDate.now()): Int {
        var max = 0
        for (h in db.habitDao().getActive()) {
            val completed = db.habitCompletionDao().getForHabit(h.id).map { it.date }.toSet()
            val s = Streaks.computeStreak(h, completed, today)
            if (s > max) max = s
        }
        return max
    }

    /**
     * Unlock any locked rewards whose target streak has been reached, and notify.
     * Safe to call after every completion toggle (app or widget).
     */
    suspend fun checkAndUnlock(context: Context) {
        val db = AppDatabase.get(context)
        val reached = maxHabitStreak(db)
        if (reached <= 0) return
        db.rewardDao().getAll()
            .filter { !it.unlocked && it.targetStreak in 1..reached }
            .forEach { r ->
                db.rewardDao().update(r.copy(unlocked = true, unlockedAt = System.currentTimeMillis()))
                NotificationHelper.showRewardUnlocked(context, r.title, r.id)
            }
    }
}
