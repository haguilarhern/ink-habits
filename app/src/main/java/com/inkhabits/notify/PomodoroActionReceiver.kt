package com.inkhabits.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Handles the Pause / Resume / Reset / Skip buttons on the Pomodoro notification while the
 * timer screen isn't in the foreground. Each action mutates the shared [PomodoroEngine]
 * state, then re-points the end alarm and re-renders (or clears) the notification to match.
 * When the user reopens the screen, PomodoroActivity re-syncs from the same state.
 */
class PomodoroActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PAUSE -> {
                PomodoroEngine.pause(context)
                PomodoroAlarm.cancel(context)
                NotificationHelper.showPomodoroPaused(
                    context,
                    PomodoroEngine.phaseName(PomodoroEngine.mode(context)),
                    PomodoroEngine.remainingMin(context)
                )
            }
            ACTION_RESUME -> {
                PomodoroEngine.start(context)
                PomodoroAlarm.schedule(
                    context, PomodoroEngine.endAt(context),
                    "${PomodoroEngine.phaseName(PomodoroEngine.mode(context))} complete"
                )
                NotificationHelper.showPomodoroRunning(
                    context, PomodoroEngine.endAt(context),
                    PomodoroEngine.phaseName(PomodoroEngine.mode(context))
                )
            }
            ACTION_RESET -> {
                // Restart the current phase from the top and keep it running so the
                // notification stays with a fresh countdown.
                PomodoroEngine.restart(context)
                PomodoroAlarm.schedule(
                    context, PomodoroEngine.endAt(context),
                    "${PomodoroEngine.phaseName(PomodoroEngine.mode(context))} complete"
                )
                NotificationHelper.showPomodoroRunning(
                    context, PomodoroEngine.endAt(context),
                    PomodoroEngine.phaseName(PomodoroEngine.mode(context))
                )
            }
            ACTION_SKIP -> {
                PomodoroEngine.skip(context)
                PomodoroAlarm.cancel(context)
                NotificationHelper.cancelPomodoro(context)
            }
        }
    }

    companion object {
        const val ACTION_PAUSE = "com.inkhabits.action.POMO_PAUSE"
        const val ACTION_RESUME = "com.inkhabits.action.POMO_RESUME"
        const val ACTION_RESET = "com.inkhabits.action.POMO_RESET"
        const val ACTION_SKIP = "com.inkhabits.action.POMO_SKIP"
    }
}
