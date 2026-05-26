package com.boox.atomic.habits.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Schedules daily reminders for habit check-ins using AlarmManager.
 *
 * Handles setting, cancelling, and rescheduling alarms for the
 * Boox Ink Habits app. The reminder fires a broadcast that
 * the app's notification system handles to display a status bar
 * notification.
 */
object ReminderScheduler {

    private const val ACTION_REMINDER = "com.boox.atomic.habits.REMINDER"

    /**
     * Schedules a repeating daily reminder at the given hour and minute.
     *
     * @param context application context
     * @param hour    hour of day (0-23)
     * @param minute  minute of hour (0-59)
     */
    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return

        val intent = Intent(ACTION_REMINDER).apply {
            `package` = context.packageName
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate the next trigger time
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If the time has passed today, schedule for tomorrow
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Schedule repeating daily alarm
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    /**
     * Cancels any previously scheduled daily reminder.
     *
     * @param context application context
     */
    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return

        val intent = Intent(ACTION_REMINDER).apply {
            `package` = context.packageName
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /**
     * Broadcast receiver for the daily reminder alarm.
     *
     * Registers with the manifest to handle the REMINDER action.
     */
    class ReminderReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_REMINDER) {
                // Show a notification reminding the user to check in
                showReminderNotification(context)
            }
        }

        private fun showReminderNotification(context: Context) {
            try {
                val channelId = "habit_reminders"
                val notificationManager = context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as android.app.NotificationManager

                // Create notification channel for Android 8+
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val channel = android.app.NotificationChannel(
                        channelId,
                        "Habit Reminders",
                        android.app.NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Daily reminders to check in on your habits"
                    }
                    notificationManager.createNotificationChannel(channel)
                }

                // Build notification
                val openIntent = context.packageManager.getLaunchIntentForPackage(
                    context.packageName
                )
                val openPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = android.app.Notification.Builder(context, channelId)
                    .setContentTitle("Time to check in!")
                    .setContentText("How are your Ink Habits going today?")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setAutoCancel(true)
                    .setContentIntent(openPendingIntent)
                    .build()

                notificationManager.notify(1001, notification)
            } catch (_: Exception) {
                // Silently fail — notification is non-critical
            }
        }
    }
}
