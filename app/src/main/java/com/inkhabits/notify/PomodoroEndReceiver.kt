package com.inkhabits.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * Fires when a backgrounded Pomodoro phase reaches its end. Alerts the user (vibration +
 * a "complete" notification) and clears the running flag so the screen reflects it on
 * return. When the Pomodoro screen is in the foreground it cancels this alarm and handles
 * the end itself, so this only runs when the user has navigated away.
 */
class PomodoroEndReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(PomodoroAlarm.EXTRA_TITLE) ?: "Pomodoro complete"
        // Mark the timer stopped so the activity shows the correct state when reopened.
        context.getSharedPreferences("pomodoro", Context.MODE_PRIVATE)
            .edit().putBoolean("running", false).apply()
        buzz(context)
        NotificationHelper.showPomodoroComplete(context, title)
    }

    private fun buzz(context: Context) {
        try {
            val v = context.getSystemService(Vibrator::class.java) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") v.vibrate(400)
            }
        } catch (_: Throwable) {
        }
    }
}
