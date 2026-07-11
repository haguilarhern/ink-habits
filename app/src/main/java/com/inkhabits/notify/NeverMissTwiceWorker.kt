package com.inkhabits.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.inkhabits.data.AppDatabase
import com.inkhabits.util.Streaks
import java.time.LocalDate

/**
 * Daily check for habits at risk of being missed twice. Posts a single nudge
 * summarizing what still needs attention today.
 */
class NeverMissTwiceWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.get(applicationContext)
        val today = LocalDate.now()

        val habits = db.habitDao().getActive()
        val atRisk = habits.filter { habit ->
            val completed = db.habitCompletionDao().getForHabit(habit.id).map { it.date }.toSet()
            Streaks.atRiskOfMissingTwice(habit, completed, today)
        }
        if (atRisk.isEmpty()) return Result.success()

        // Lead with a warm coach line (same voice as the freeze-saved nudge).
        val coach = com.inkhabits.util.Comebacks.random()
        val title = "Don't miss twice"
        val body = if (atRisk.size == 1) {
            val name = habits.first { it.id == atRisk[0].id }.name.ifBlank { "a habit" }
            "$coach Do $name today and keep the chain alive."
        } else {
            "$coach You have ${atRisk.size} habits to pick back up today — don't let them slip twice."
        }
        NotificationHelper.showNeverMissTwice(applicationContext, title, body)
        return Result.success()
    }
}
