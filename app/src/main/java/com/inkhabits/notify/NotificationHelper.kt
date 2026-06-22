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
    private const val NOTIFICATION_ID = 1001

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
        }
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
            .setContentTitle("🎁 Reward unlocked!")
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
