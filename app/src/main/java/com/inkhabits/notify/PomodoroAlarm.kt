package com.inkhabits.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Schedules the one-shot alarm that fires when the running Pomodoro phase ends, so the
 * end alert (vibration + "complete" notification) happens even if the app is in the
 * background or its process was killed. The countdown itself is wall-clock anchored
 * ([endAtMillis]), so this alarm is all that's needed to keep a timer "running" while away.
 */
object PomodoroAlarm {

    const val ACTION_FIRE = "com.inkhabits.action.POMODORO_END"
    const val EXTRA_TITLE = "title"
    private const val REQUEST = 7001

    private fun pendingIntent(context: Context, title: String?): PendingIntent {
        val intent = Intent(context, PomodoroEndReceiver::class.java).apply {
            action = ACTION_FIRE
            if (title != null) putExtra(EXTRA_TITLE, title)
        }
        return PendingIntent.getBroadcast(
            context, REQUEST, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun schedule(context: Context, endAtMillis: Long, title: String) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(context, title)
        try {
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, pi)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, pi)
        }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(pendingIntent(context, null))
    }
}
