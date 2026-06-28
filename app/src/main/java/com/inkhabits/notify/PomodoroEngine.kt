package com.inkhabits.notify

import android.content.Context
import kotlin.math.ceil

/**
 * Canonical Pomodoro timer state, backed by the "pomodoro" SharedPreferences so the
 * foreground screen (PomodoroActivity) and the background notification actions
 * ([PomodoroActionReceiver]) operate on one source of truth. These operations only
 * mutate state; the caller decides whether to (re)post the notification and (re)schedule
 * the end alarm, because that differs between foreground and background.
 *
 * The prefs keys, durations, round logic and phase progression mirror PomodoroActivity
 * exactly so the two stay in lock-step.
 */
object PomodoroEngine {

    enum class Mode { FOCUS, SHORT, LONG }

    private fun p(c: Context) = c.getSharedPreferences("pomodoro", Context.MODE_PRIVATE)

    fun mode(c: Context): Mode =
        runCatching { Mode.valueOf(p(c).getString("mode", Mode.FOCUS.name)!!) }.getOrDefault(Mode.FOCUS)
    fun running(c: Context): Boolean = p(c).getBoolean("running", false)
    fun endAt(c: Context): Long = p(c).getLong("endAt", 0L)
    fun remainingMin(c: Context): Int = p(c).getInt("remainingMin", 0)
    fun completedFocus(c: Context): Int = p(c).getInt("completedFocus", 0)
    fun rounds(c: Context): Int = p(c).getInt("rounds", 4).coerceAtLeast(1)

    fun durationFor(c: Context, m: Mode): Int = p(c).getInt(
        when (m) { Mode.FOCUS -> "focus"; Mode.SHORT -> "short"; Mode.LONG -> "long" },
        when (m) { Mode.FOCUS -> 25; Mode.SHORT -> 5; Mode.LONG -> 15 }
    ).coerceAtLeast(1)

    fun phaseName(m: Mode): String = when (m) {
        Mode.FOCUS -> "Focus"; Mode.SHORT -> "Short break"; Mode.LONG -> "Long break"
    }

    fun computeRemaining(c: Context): Int {
        val ms = endAt(c) - System.currentTimeMillis()
        return ceil(ms / 60_000.0).toInt().coerceAtLeast(0)
    }

    private fun save(c: Context, mode: Mode, running: Boolean, paused: Boolean, endAt: Long, remainingMin: Int, completedFocus: Int) {
        p(c).edit()
            .putString("mode", mode.name)
            .putBoolean("running", running)
            .putBoolean("paused", paused)
            .putLong("endAt", endAt)
            .putInt("remainingMin", remainingMin)
            .putInt("completedFocus", completedFocus)
            .apply()
    }

    /** Begin / resume counting from the remaining minutes (or a full phase if none). */
    fun start(c: Context) {
        val m = mode(c)
        val rem = remainingMin(c).takeIf { it > 0 } ?: durationFor(c, m)
        save(c, m, true, false, System.currentTimeMillis() + rem * 60_000L, rem, completedFocus(c))
    }

    /** Freeze the remaining whole minutes (still on the current phase). */
    fun pause(c: Context) {
        val rem = computeRemaining(c).coerceAtLeast(1)
        save(c, mode(c), false, true, 0L, rem, completedFocus(c))
    }

    /** Restart the current phase from the top and keep it running. */
    fun restart(c: Context) {
        val m = mode(c)
        val rem = durationFor(c, m)
        save(c, m, true, false, System.currentTimeMillis() + rem * 60_000L, rem, completedFocus(c))
    }

    /** End the current phase and queue the next (focus → break → focus …), not running. */
    fun skip(c: Context) {
        val m = mode(c)
        val cf = completedFocus(c)
        val next: Mode
        val ncf: Int
        if (m == Mode.FOCUS) {
            ncf = cf + 1
            next = if (ncf % rounds(c) == 0) Mode.LONG else Mode.SHORT
        } else {
            ncf = cf
            next = Mode.FOCUS
        }
        save(c, next, false, false, 0L, durationFor(c, next), ncf)
    }
}
