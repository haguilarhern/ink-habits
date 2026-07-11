package com.inkhabits.util

import com.inkhabits.data.AppDatabase
import com.inkhabits.data.entity.EconomyState
import java.time.LocalDate

/**
 * The aura economy.
 *
 * Aura *earned* is a pure function of completion history, so it can never be farmed by
 * toggling a completion on and off:
 *
 *   earned = [AURA_PER_COMPLETION] · (total habit completions)
 *          + [AURA_PER_PERFECT_DAY] · (total perfect days, real completions only)
 *
 * The wallet row only persists what can't be recomputed — aura spent and the current
 * totem inventory. Spending aura buys "totems" (idols) that the overnight reconcile
 * ([Freezes]) consumes to protect a streak from a missed day.
 *
 * All values are tunable here.
 */
object Economy {

    /** Aura granted per individual habit completion. */
    const val AURA_PER_COMPLETION = 2

    /** Bonus aura for a day on which every due habit was completed. */
    const val AURA_PER_PERFECT_DAY = 5

    /**
     * Cost of one habit totem (protects one missed occurrence of one habit).
     *
     * Calibrated so protection stays *scarce*: roughly five days of doing most (not all)
     * of a typical ~4–5-habit slate earns one habit totem. Example — 4 completions/day
     * with no perfect day: 4 · [AURA_PER_COMPLETION] · 5 = 40 aura ≈ one totem. Perfect
     * days accelerate it a little, but the completion path is the primary earner so a
     * "good but imperfect" week yields about one freeze, not a stockpile.
     */
    const val COST_HABIT_TOTEM = 40

    /** Cost of one identity totem — double a habit totem (protects a missed perfect-day). */
    const val COST_IDENTITY_TOTEM = COST_HABIT_TOTEM * 2

    /** Current wallet, defaulting a fresh install to an empty id=1 row. */
    suspend fun state(db: AppDatabase): EconomyState =
        db.economyDao().get() ?: EconomyState(id = 1)

    /** Total aura ever earned from completion history. */
    suspend fun earnedAura(db: AppDatabase, today: LocalDate = LocalDate.now()): Long {
        val completions = db.habitCompletionDao().count().toLong()
        val habits = db.habitDao().getActive()
        val byHabit = habits.associate { h ->
            h.id to db.habitCompletionDao().getForHabit(h.id).map { it.date }.toSet()
        }
        val perfectDays = Streaks.totalPerfectDays(habits, byHabit, today).toLong()
        return completions * AURA_PER_COMPLETION + perfectDays * AURA_PER_PERFECT_DAY
    }

    /** Spendable aura balance = earned − spent (never negative). */
    suspend fun balance(db: AppDatabase, today: LocalDate = LocalDate.now()): Long =
        (earnedAura(db, today) - state(db).auraSpent).coerceAtLeast(0)

    /**
     * Spend [cost] aura to add one totem to inventory. Returns true on success, false
     * when the balance is insufficient. [adjust] applies the inventory change.
     */
    private suspend fun buy(db: AppDatabase, cost: Int, adjust: (EconomyState) -> EconomyState): Boolean {
        if (balance(db) < cost) return false
        val s = state(db)
        db.economyDao().upsert(adjust(s).copy(id = 1, auraSpent = s.auraSpent + cost))
        return true
    }

    suspend fun buyHabitTotem(db: AppDatabase): Boolean =
        buy(db, COST_HABIT_TOTEM) { it.copy(habitTotems = it.habitTotems + 1) }

    suspend fun buyIdentityTotem(db: AppDatabase): Boolean =
        buy(db, COST_IDENTITY_TOTEM) { it.copy(identityTotems = it.identityTotems + 1) }
}
