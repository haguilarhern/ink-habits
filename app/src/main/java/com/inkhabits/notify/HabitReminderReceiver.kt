package com.inkhabits.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.inkhabits.data.AppDatabase
import com.inkhabits.util.Schedule
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

/**
 * Receives per-habit reminder alarms (and BOOT_COMPLETED). On a reminder alarm it
 * notifies only if the habit is still due today and not yet completed, then re-arms
 * the alarm for the next day. On boot it re-schedules every habit's reminder.
 */
class HabitReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                val pending = goAsync()
                Thread {
                    try { runBlocking { HabitReminderScheduler.rescheduleAll(context) } }
                    finally { pending.finish() }
                }.start()
            }
            ACTION_FIRE -> {
                val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
                if (habitId <= 0) return
                val pending = goAsync()
                Thread {
                    try {
                        runBlocking {
                            val db = AppDatabase.get(context)
                            val habit = db.habitDao().getById(habitId)
                            if (habit != null && habit.isActive && habit.reminderEnabled) {
                                val today = LocalDate.now()
                                val done = db.habitCompletionDao().isCompleted(habitId, today.toString())
                                if (Schedule.isDueOn(habit, today) && !done) {
                                    val identity = db.identityGoalDao().getAll()
                                        .firstOrNull { it.id == habit.identityGoalId }
                                    val who = identity?.name?.ifBlank { null }
                                    val subtitle = if (who != null) "Keep up being $who — tap to check it off."
                                        else "Tap to check it off."
                                    NotificationHelper.showHabitReminder(
                                        context, habitId, habit.name, subtitle)
                                }
                                // Re-arm for the next day regardless of today's outcome.
                                HabitReminderScheduler.scheduleHabit(context, habit)
                            }
                        }
                    } finally { pending.finish() }
                }.start()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.inkhabits.action.HABIT_REMINDER"
        const val EXTRA_HABIT_ID = "habit_id"
    }
}
