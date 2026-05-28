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

        val title = "Don't miss twice"
        val body = if (atRisk.size == 1) {
            val name = habits.first { it.id == atRisk[0].id }.name.ifBlank { "a habit" }
            "You skipped last time — keep the chain alive and do $name today."
        } else {
            "You have ${atRisk.size} habits you skipped last time. Don't let them slip twice — do them today."
        }
        NotificationHelper.showNeverMissTwice(applicationContext, title, body)
        return Result.success()
    }
}
