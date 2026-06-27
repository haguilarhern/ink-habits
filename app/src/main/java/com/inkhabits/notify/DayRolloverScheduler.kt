package com.inkhabits.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Fires an exact alarm just after midnight each day so the home-screen widgets roll
 * over to the new day's habits/to-dos automatically. Widget contents are derived from
 * [LocalDate.now], so without this they keep showing yesterday's list (and yesterday's
 * done/undone marks) until the app is opened or a widget is tapped.
 *
 * The alarm is one-shot; [DayRolloverReceiver] re-arms it for the next midnight after
 * each fire, and also re-arms on boot.
 */
object DayRolloverScheduler {

    private const val REQUEST_CODE = 9100

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DayRolloverReceiver::class.java).apply {
            action = DayRolloverReceiver.ACTION_ROLLOVER
        }
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Next epoch-milli a few seconds past the upcoming midnight. */
    private fun nextTriggerMillis(): Long {
        val next = LocalDate.now().plusDays(1).atTime(LocalTime.of(0, 0, 5))
        return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun schedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(context)
        val at = nextTriggerMillis()
        try {
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }
}
