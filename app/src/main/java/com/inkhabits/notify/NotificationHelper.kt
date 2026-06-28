package com.inkhabits.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.inkhabits.MainActivity
import com.inkhabits.R

object NotificationHelper {

    private const val CHANNEL_ID = "never_miss_twice"
    const val CHANNEL_REMINDERS = "habit_reminders"
    const val CHANNEL_POMODORO = "pomodoro"
    private const val NOTIFICATION_ID = 1001
    private const val POMODORO_ID = 4000

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(NotificationChannel(
                CHANNEL_ID,
                "Never miss twice",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Nudges when a habit is at risk of being missed twice" })
            mgr.createNotificationChannel(NotificationChannel(
                CHANNEL_REMINDERS,
                "Habit reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Reminders to do your habits at their set times" })
            // Low importance: the ongoing timer should be silent (no ding when it appears);
            // the end alert uses vibration from the receiver instead.
            mgr.createNotificationChannel(NotificationChannel(
                CHANNEL_POMODORO,
                "Pomodoro timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the running focus timer and its countdown"
                setShowBadge(false)
            })
        }
    }

    private fun pomodoroOpenIntent(context: Context): PendingIntent {
        val intent = Intent(context, com.inkhabits.ui.pomodoro.PomodoroActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context, 4, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun pomodoroAction(context: Context, label: String, action: String): NotificationCompat.Action {
        val pi = PendingIntent.getBroadcast(
            context, action.hashCode(),
            Intent(context, PomodoroActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Icon is ignored for standard notification actions on modern Android (label only).
        return NotificationCompat.Action.Builder(R.drawable.ic_notification, label, pi).build()
    }

    /**
     * Ongoing notification with a live count-down to [endAtMillis] (the system renders the
     * chronometer, so it stays accurate with no per-second updates from us). Tapping it
     * reopens the Pomodoro screen. Shown while a timer is running so it keeps "running" and
     * visible even after you leave the screen.
     */
    fun showPomodoroRunning(context: Context, endAtMillis: Long, title: String) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_POMODORO)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setWhen(endAtMillis)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pomodoroOpenIntent(context))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(pomodoroAction(context, "Pause", PomodoroActionReceiver.ACTION_PAUSE))
            .addAction(pomodoroAction(context, "Skip", PomodoroActionReceiver.ACTION_SKIP))
            .addAction(pomodoroAction(context, "Reset", PomodoroActionReceiver.ACTION_RESET))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(POMODORO_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    /** Paused timer: shows the frozen remaining minutes with a Resume / Skip / Reset row. */
    fun showPomodoroPaused(context: Context, title: String, remainingMin: Int) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_POMODORO)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$title (paused)")
            .setContentText("$remainingMin min left")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pomodoroOpenIntent(context))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(pomodoroAction(context, "Resume", PomodoroActionReceiver.ACTION_RESUME))
            .addAction(pomodoroAction(context, "Skip", PomodoroActionReceiver.ACTION_SKIP))
            .addAction(pomodoroAction(context, "Reset", PomodoroActionReceiver.ACTION_RESET))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(POMODORO_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    /** Replace the ongoing timer with a dismissible "phase complete" notification. */
    fun showPomodoroComplete(context: Context, title: String) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_POMODORO)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("Tap to continue")
            .setContentIntent(pomodoroOpenIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(POMODORO_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    fun cancelPomodoro(context: Context) {
        NotificationManagerCompat.from(context).cancel(POMODORO_ID)
    }

    /** Per-habit reminder fired at the habit's set time on a day it's due. */
    fun showHabitReminder(context: Context, habitId: Long, habitName: String, subtitle: String) {
        ensureChannel(context)
        val openPi = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val name = habitName.ifBlank { "your habit" }
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to $name")
            .setContentText(subtitle)
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(3000 + habitId.toInt(), notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted; silently skip.
        }
    }

    fun showRewardUnlocked(context: Context, rewardTitle: String, rewardId: Long) {
        ensureChannel(context)
        val openPi = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = rewardTitle.ifBlank { "Open the app to see your reward." }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Reward unlocked")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(2000 + rewardId.toInt(), notification)
        } catch (_: SecurityException) {
        }
    }

    fun showNeverMissTwice(context: Context, title: String, body: String) {
        ensureChannel(context)
        val openPi = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted; silently skip.
        }
    }
}
