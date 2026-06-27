package com.inkhabits.util

import android.content.Context
import com.inkhabits.data.AppDatabase
import com.inkhabits.data.entity.EconomyState
import com.inkhabits.data.entity.Habit
import com.inkhabits.data.entity.StreakFreeze
import java.time.LocalDate

/**
 * Protective totems (Duolingo-style streak freezes).
 *
 * A totem bought in the shop ([Economy]) is consumed automatically by [reconcile] when a
 * scheduled day is missed on a live streak, recording a [StreakFreeze] for that day.
 * Streak math then treats a frozen day as if it had been completed — callers merge the
 * frozen dates into each habit's completion set via [effectiveCompletions] (habit
 * totems) and pass identity-frozen days as "forced perfect" days (identity totems).
 *
 * Earned aura ignores freezes, so a frozen day never pays out the perfect-day bonus.
 */
object Freezes {

    // ---- Read helpers (for streak rendering) ----

    /** Each active habit's completions merged with its habit-totem freezes. */
    suspend fun effectiveCompletions(db: AppDatabase): Map<Long, Set<String>> {
        val frozen = db.streakFreezeDao().getAll().filter { it.habitId > 0 }
            .groupBy { it.habitId }.mapValues { e -> e.value.map { it.date }.toSet() }
        return db.habitDao().getActive().associate { h ->
            val done = db.habitCompletionDao().getForHabit(h.id).map { it.date }.toSet()
            h.id to (done + (frozen[h.id] ?: emptySet()))
        }
    }

    /** A single habit's completions merged with its freezes. */
    suspend fun effectiveCompletionsFor(db: AppDatabase, habitId: Long): Set<String> {
        val done = db.habitCompletionDao().getForHabit(habitId).map { it.date }.toSet()
        val frozen = db.streakFreezeDao().getForHabit(habitId).map { it.date }.toSet()
        return done + frozen
    }

    /** Days an identity totem has protected (treated as perfect days). */
    suspend fun identityForcedPerfect(db: AppDatabase, identityId: Long): Set<String> =
        db.streakFreezeDao().getForIdentity(identityId).map { it.date }.toSet()

    // ---- Overnight reconcile (auto-consume) ----

    /**
     * Spend owned totems to protect streaks broken by a miss. Runs at the day rollover
     * and on app start. Conservative by design: it only protects the single most recent
     * *passed* occurrence per habit/identity, and only when the prior occurrence was
     * satisfied (so there is a real streak worth saving) and a matching totem is owned.
     * Running daily, this still bridges consecutive misses one day at a time.
     */
    suspend fun reconcile(context: Context, today: LocalDate = LocalDate.now()) {
        val db = AppDatabase.get(context)
        var state = Economy.state(db)

        // --- Habit totems ---
        for (h in db.habitDao().getActive()) {
            if (state.habitTotems <= 0) break
            val last = Schedule.previousOccurrence(h, today) ?: continue
            val ds = last.toString()
            val done = db.habitCompletionDao().isCompleted(h.id, ds)
            val frozen = db.streakFreezeDao().habitFrozenOn(h.id, ds)
            if (done || frozen) continue // not a miss
            // Only protect if the occurrence before it was satisfied (live streak).
            val prior = Schedule.previousOccurrence(h, last) ?: continue
            val priorOk = db.habitCompletionDao().isCompleted(h.id, prior.toString()) ||
                db.streakFreezeDao().habitFrozenOn(h.id, prior.toString())
            if (!priorOk) continue
            db.streakFreezeDao().insert(StreakFreeze(habitId = h.id, date = ds))
            state = state.copy(habitTotems = state.habitTotems - 1)
            db.economyDao().upsert(state)
        }

        // --- Identity totems (perfect-day streaks) ---
        val activeHabits = db.habitDao().getActive()
        val byHabitEff = effectiveCompletions(db)
        for (identity in db.identityGoalDao().getAll()) {
            if (state.identityTotems <= 0) break
            val idHabits = activeHabits.filter { it.identityGoalId == identity.id }
            if (idHabits.isEmpty()) continue
            val last = previousIdentityOccurrence(idHabits, today) ?: continue
            val ds = last.toString()
            if (db.streakFreezeDao().identityFrozenOn(identity.id, ds)) continue
            if (isPerfectDay(idHabits, byHabitEff, last)) continue // not a miss
            val prior = previousIdentityOccurrence(idHabits, last) ?: continue
            val priorOk = isPerfectDay(idHabits, byHabitEff, prior) ||
                db.streakFreezeDao().identityFrozenOn(identity.id, prior.toString())
            if (!priorOk) continue
            db.streakFreezeDao().insert(StreakFreeze(identityId = identity.id, date = ds))
            state = state.copy(identityTotems = state.identityTotems - 1)
            db.economyDao().upsert(state)
        }
    }

    /** Latest day strictly before [date] on which any of [idHabits] is due. */
    private fun previousIdentityOccurrence(idHabits: List<Habit>, date: LocalDate): LocalDate? {
        var d = date.minusDays(1)
        var guard = 0
        while (guard < 366) {
            if (idHabits.any { Schedule.isDueOn(it, d) }) return d
            d = d.minusDays(1)
            guard++
        }
        return null
    }

    /** True when every due habit on [day] is completed (freezes already merged in). */
    private fun isPerfectDay(
        idHabits: List<Habit>,
        byHabitEff: Map<Long, Set<String>>,
        day: LocalDate
    ): Boolean {
        val due = idHabits.filter { Schedule.isDueOn(it, day) }
        if (due.isEmpty()) return false
        val ds = day.toString()
        return due.all { (byHabitEff[it.id] ?: emptySet()).contains(ds) }
    }
}
