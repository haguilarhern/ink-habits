package com.inkhabits.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.inkhabits.data.AppDatabase
import com.inkhabits.data.entity.Habit
import com.inkhabits.util.Schedule
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Schedules per-habit reminder notifications via [AlarmManager]. Each enabled habit
 * gets one exact daily alarm at its reminder time; when it fires, [HabitReminderReceiver]
 * checks whether the habit is actually due (and not yet done) before notifying, then
 * re-arms the alarm for the next day (exact alarms are one-shot).
 *
 * Reminders are opt-in per habit ([Habit.reminderEnabled]) and only apply when the
 * habit has a real reminder time set (not "Any time").
 */
object HabitReminderScheduler {

    /** Resolve a habit's reminder time to a minute-of-day, or null when it has none. */
    fun reminderMinuteOfDay(habit: Habit): Int? = when (habit.reminderMinutes) {
        Schedule.TIME_ANY -> null
        Schedule.TIME_MORNING -> 8 * 60
        Schedule.TIME_AFTERNOON -> 14 * 60
        Schedule.TIME_EVENING -> 20 * 60
        else -> if (habit.reminderMinutes < 0) null else habit.reminderMinutes
    }

    private fun eligible(habit: Habit): Boolean =
        habit.isActive && habit.reminderEnabled && reminderMinuteOfDay(habit) != null

    private fun pendingIntent(context: Context, habitId: Long): PendingIntent {
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = HabitReminderReceiver.ACTION_FIRE
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
            // Make the PendingIntent unique per habit so they don't collapse.
            data = android.net.Uri.parse("inkhabits://reminder/$habitId")
        }
        return PendingIntent.getBroadcast(
            context, habitId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Next epoch-milli at [minuteOfDay] that is strictly in the future. */
    private fun nextTriggerMillis(minuteOfDay: Int): Long {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(LocalTime.of(minuteOfDay / 60, minuteOfDay % 60))
        if (!next.isAfter(now)) next = next.plusDays(1)
        return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun scheduleHabit(context: Context, habit: Habit) {
        val minute = reminderMinuteOfDay(habit) ?: return
        if (!eligible(habit)) return
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(context, habit.id)
        val at = nextTriggerMillis(minute)
        try {
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancelHabit(context: Context, habitId: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(pendingIntent(context, habitId))
    }

    /** Re-arm alarms for every active habit (cancel-then-schedule). Call on app start,
     *  after edits, and on boot. Alarms for deleted/disabled habits self-clean: they
     *  fire once, find the habit gone/ineligible, notify nothing and don't re-arm. */
    suspend fun rescheduleAll(context: Context) {
        val habits = AppDatabase.get(context).habitDao().getActive()
        for (h in habits) {
            cancelHabit(context, h.id)
            if (eligible(h)) scheduleHabit(context, h)
        }
    }
}
