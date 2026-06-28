package com.inkhabits.ui.pomodoro

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.inkhabits.data.AppDatabase
import com.inkhabits.data.entity.ToDo
import com.inkhabits.databinding.ActivityPomodoroBinding
import com.inkhabits.eink.EInk
import com.inkhabits.eink.EInkActivity
import com.inkhabits.notify.NotificationHelper
import com.inkhabits.notify.PomodoroAlarm
import com.inkhabits.ui.widget.CheckBoxView
import com.inkhabits.util.StrokeRenderer
import com.inkhabits.util.TaskRecurrence
import com.inkhabits.util.YearTally
import kotlinx.coroutines.launch
import kotlin.math.ceil

/**
 * E-ink optimized Pomodoro timer. The countdown is displayed and animated in WHOLE
 * MINUTES, not seconds: the screen only repaints (and does a clean refresh) once per
 * minute, which avoids the constant ghosting a per-second sweep would cause on the
 * Boox panel. Time is anchored to the wall clock so pausing/resuming stays accurate.
 *
 * Batches are configurable (focus / short break / long break / rounds-per-long-break).
 * When a phase ends the next phase is queued (not auto-started), so the user controls
 * each transition — again, fewer surprise repaints.
 */
class PomodoroActivity : EInkActivity() {

    private lateinit var binding: ActivityPomodoroBinding

    private enum class Mode { FOCUS, SHORT, LONG }
    private var mode = Mode.FOCUS
    private var running = false
    private var paused = false          // not running, but a phase is frozen mid-way
    private var remainingMin = 0
    private var endAtMillis = 0L
    private var completedFocus = 0

    private val handler = Handler(Looper.getMainLooper())
    private val tick = Runnable { onTick() }

    /** While the screen is visible, mirror state changes made by the notification's
     *  action buttons (pause/resume/reset/skip) so the on-screen timer stays in sync. */
    private val prefsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        val before = listOf(running, paused, endAtMillis, mode, remainingMin, completedFocus)
        reloadState()
        if (listOf(running, paused, endAtMillis, mode, remainingMin, completedFocus) == before) return@OnSharedPreferenceChangeListener
        PomodoroAlarm.cancel(this)   // foreground owns the end via the on-screen ticker
        handler.removeCallbacks(tick)
        if (running && endAtMillis > 0L && remainingMin > 0) handler.postDelayed(tick, 60_000L)
        updateUi()
        EInk.clean(binding.root)
    }

    private val prefs by lazy { getSharedPreferences("pomodoro", MODE_PRIVATE) }
    private val db by lazy { AppDatabase.get(this) }

    private var todos = listOf<ToDo>()
    /** Task ids chosen to work on this session. */
    private val sessionIds = linkedSetOf<Long>()

    private val FOCUS_COLOR = Color.parseColor("#2A4A8C")
    private val SHORT_COLOR = Color.parseColor("#2A4A8C")
    private val LONG_COLOR = Color.parseColor("#2A4A8C")
    private val INK = Color.parseColor("#0B0B0C")
    private val MUTED = Color.parseColor("#5C5C5C")
    private val HAIRLINE = Color.parseColor("#D9D9DE")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPomodoroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }
        binding.settingsButton.setOnClickListener { showSettings() }
        binding.startButton.setOnClickListener { startOrPause() }
        binding.resetButton.setOnClickListener { reset() }
        binding.tabFocus.setOnClickListener { switchMode(Mode.FOCUS) }
        binding.tabShort.setOnClickListener { switchMode(Mode.SHORT) }
        binding.tabLong.setOnClickListener { switchMode(Mode.LONG) }
        binding.addTaskButton.setOnClickListener { chooseTasks() }

        // Restore the previously chosen session tasks.
        prefs.getString("session", "")?.split(",")
            ?.mapNotNull { it.trim().toLongOrNull() }?.let { sessionIds.addAll(it) }

        // Restore a timer that may still be running (or paused) from a previous visit, a
        // process death, or a notification action, so reopening picks up where it left off.
        reloadState()
        if (running && endAtMillis > 0L && remainingMin <= 0) {
            // Ended while we were gone (the alarm already alerted) — settle to next phase.
            finishPhase(alert = false)
        }
        updateUi()
        syncNotification()
        loadTasks()
    }

    override fun onResume() {
        super.onResume()
        // The on-screen ticker owns the countdown while visible, so drop the background end
        // alarm (the notification itself stays — it shows the live timer on any screen).
        // Re-sync first in case a notification action changed the state while we were away.
        PomodoroAlarm.cancel(this)
        reloadState()
        if (running && endAtMillis > 0L) {
            if (remainingMin <= 0) {
                finishPhase(alert = false)
            } else {
                updateUi()
                handler.removeCallbacks(tick)
                handler.postDelayed(tick, 60_000L)
            }
        } else {
            updateUi()
        }
        syncNotification()
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        loadTasks()
    }

    override fun onPause() {
        super.onPause()
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        // Leaving the screen while a timer runs: arm the end alarm so it still fires when
        // the activity is gone. The notification is already showing (kept across screens).
        if (running && endAtMillis > 0L) {
            handler.removeCallbacks(tick)
            PomodoroAlarm.schedule(this, endAtMillis, "${phaseName()} complete")
            persist()
        }
    }

    /** Post / update / clear the notification to match the current timer state. Kept in
     *  sync on every screen so the running timer is always visible in the shade. */
    private fun syncNotification() {
        when {
            running && endAtMillis > 0L -> NotificationHelper.showPomodoroRunning(this, endAtMillis, phaseName())
            paused -> NotificationHelper.showPomodoroPaused(this, phaseName(), remainingMin)
            else -> NotificationHelper.cancelPomodoro(this)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    private fun phaseName(): String = when (mode) {
        Mode.FOCUS -> "Focus"; Mode.SHORT -> "Short break"; Mode.LONG -> "Long break"
    }

    /** Persist timer state so it survives leaving the screen, process death, and is shared
     *  with the notification action buttons ([com.inkhabits.notify.PomodoroEngine]). */
    private fun persist() {
        prefs.edit()
            .putBoolean("running", running)
            .putBoolean("paused", paused)
            .putLong("endAt", endAtMillis)
            .putString("mode", mode.name)
            .putInt("remainingMin", remainingMin)
            .putInt("completedFocus", completedFocus)
            .apply()
    }

    /** Pull the canonical state from prefs into the in-memory fields. A notification action
     *  (pause/resume/reset/skip) may have changed it while we were away. */
    private fun reloadState() {
        mode = runCatching { Mode.valueOf(prefs.getString("mode", Mode.FOCUS.name)!!) }
            .getOrDefault(Mode.FOCUS)
        completedFocus = prefs.getInt("completedFocus", 0)
        running = prefs.getBoolean("running", false)
        paused = prefs.getBoolean("paused", false)
        endAtMillis = prefs.getLong("endAt", 0L)
        remainingMin = if (running && endAtMillis > 0L) computeRemaining()
        else prefs.getInt("remainingMin", 0).takeIf { it > 0 } ?: durationFor(mode)
    }

    // ── settings ──

    private fun durationFor(m: Mode): Int = when (m) {
        Mode.FOCUS -> prefs.getInt("focus", 25)
        Mode.SHORT -> prefs.getInt("short", 5)
        Mode.LONG -> prefs.getInt("long", 15)
    }.coerceAtLeast(1)

    private fun rounds(): Int = prefs.getInt("rounds", 4).coerceAtLeast(1)

    private fun showSettings() {
        val ctx = this
        val box = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        fun field(label: String, value: Int): android.widget.EditText {
            box.addView(TextView(ctx).apply {
                text = label; setTextColor(MUTED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(10), 0, dp(2))
            })
            val et = android.widget.EditText(ctx).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                filters = arrayOf(android.text.InputFilter.LengthFilter(3))
                setText(value.toString())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            }
            box.addView(et); return et
        }
        val f = field("Focus minutes", durationFor(Mode.FOCUS))
        val s = field("Short break minutes", durationFor(Mode.SHORT))
        val l = field("Long break minutes", durationFor(Mode.LONG))
        val r = field("Focus rounds before a long break", rounds())

        com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this, com.inkhabits.R.style.ThemeOverlay_InkHabits_Dialog)
            .setTitle("Timer settings")
            .setView(android.widget.ScrollView(ctx).apply { addView(box) })
            .setPositiveButton("Save") { _, _ ->
                fun read(et: android.widget.EditText, def: Int) =
                    (et.text?.toString()?.trim()?.toIntOrNull() ?: def).coerceIn(1, 180)
                prefs.edit()
                    .putInt("focus", read(f, 25))
                    .putInt("short", read(s, 5))
                    .putInt("long", read(l, 15))
                    .putInt("rounds", read(r, 4))
                    .apply()
                if (!running) { remainingMin = durationFor(mode); updateUi(); EInk.clean(binding.root) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── timer control ──

    private fun startOrPause() {
        if (running) {
            // Pause: freeze the remaining whole minutes (notification switches to paused).
            running = false
            paused = true
            handler.removeCallbacks(tick)
            remainingMin = computeRemaining()
            if (remainingMin <= 0) remainingMin = durationFor(mode)
            PomodoroAlarm.cancel(this)
            persist()
            updateUi()
            syncNotification()
            EInk.clean(binding.root)
        } else {
            if (remainingMin <= 0) remainingMin = durationFor(mode)
            endAtMillis = System.currentTimeMillis() + remainingMin * 60_000L
            running = true
            paused = false
            persist()
            updateUi()
            syncNotification()      // show the running notification immediately, on any screen
            EInk.clean(binding.root)
            handler.postDelayed(tick, 60_000L)
        }
    }

    private fun reset() {
        running = false
        paused = false
        handler.removeCallbacks(tick)
        remainingMin = durationFor(mode)
        endAtMillis = 0L
        PomodoroAlarm.cancel(this)
        persist()
        updateUi()
        syncNotification()          // idle → clears the notification
        EInk.clean(binding.root)
    }

    private fun switchMode(m: Mode) {
        running = false
        paused = false
        handler.removeCallbacks(tick)
        mode = m
        remainingMin = durationFor(m)
        endAtMillis = 0L
        PomodoroAlarm.cancel(this)
        persist()
        updateUi()
        syncNotification()
        EInk.clean(binding.root)
    }

    private fun computeRemaining(): Int {
        val ms = endAtMillis - System.currentTimeMillis()
        return ceil(ms / 60_000.0).toInt().coerceAtLeast(0)
    }

    private fun onTick() {
        if (!running) return
        remainingMin = computeRemaining()
        if (remainingMin <= 0) {
            finishPhase()
        } else {
            updateUi()
            EInk.clean(binding.root) // once-a-minute clean refresh keeps the panel crisp
            handler.postDelayed(tick, 60_000L)
        }
    }

    private fun finishPhase(alert: Boolean = true) {
        running = false
        paused = false
        endAtMillis = 0L
        handler.removeCallbacks(tick)
        PomodoroAlarm.cancel(this)
        if (alert) buzz()
        // Queue the next phase without auto-starting it.
        mode = when (mode) {
            Mode.FOCUS -> {
                completedFocus++
                if (completedFocus % rounds() == 0) Mode.LONG else Mode.SHORT
            }
            else -> Mode.FOCUS
        }
        remainingMin = durationFor(mode)
        persist()
        syncNotification()      // next phase queued, not running → clears the notification
        updateUi()
        EInk.clean(binding.root)
    }

    private fun buzz() {
        try {
            val v = getSystemService(android.os.Vibrator::class.java) ?: return
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                v.vibrate(android.os.VibrationEffect.createOneShot(
                    400, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else @Suppress("DEPRECATION") v.vibrate(400)
        } catch (_: Throwable) {}
    }

    // ── session tasks ──

    private fun loadTasks() {
        lifecycleScope.launch {
            todos = db.toDoDao().getAll()
            // Forget any chosen ids whose task no longer exists.
            val existing = todos.map { it.id }.toSet()
            if (sessionIds.retainAll(existing)) saveSession()
            renderTasks()
        }
    }

    private fun saveSession() {
        prefs.edit().putString("session", sessionIds.joinToString(",")).apply()
    }

    private fun renderTasks() {
        val box = binding.taskList
        box.removeAllViews()
        val session = sessionIds.mapNotNull { id -> todos.find { it.id == id } }
        if (session.isEmpty()) {
            box.addView(TextView(this).apply {
                text = "No tasks selected. Tap “+ Choose tasks” to pick what to work on."
                setTextColor(MUTED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(8), 0, 0)
            })
            return
        }
        session.sortedBy { it.isDone }.forEach { box.addView(taskRow(it)) }
    }

    private fun taskRow(todo: ToDo): android.view.View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = getDrawable(com.inkhabits.R.drawable.pill_bg)
            setPadding(dp(14), dp(8), dp(6), dp(8))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(10); layoutParams = lp
        }
        row.addView(taskLabel(todo))
        // Remove from this session (does not delete the task).
        row.addView(TextView(this).apply {
            text = "✕"
            setTextColor(MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener {
                sessionIds.remove(todo.id); saveSession(); renderTasks(); EInk.clean(binding.root)
            }
        })
        val cb = CheckBoxView(this).apply {
            checked = todo.isDone
            onToggle = { done -> toggleTaskDone(todo, done) }
        }
        row.addView(cb, LinearLayout.LayoutParams(dp(40), dp(40)))
        return row
    }

    /** Rendered task name (handwriting if present, else text), weighted to fill the row. */
    private fun taskLabel(todo: ToDo): android.view.View =
        if (StrokeRenderer.hasInk(todo.titleStrokes)) {
            ImageView(this).apply {
                scaleType = ImageView.ScaleType.FIT_START
                alpha = if (todo.isDone) 0.45f else 1f
                layoutParams = LinearLayout.LayoutParams(0, dp(26), 1f)
                post {
                    setImageBitmap(StrokeRenderer.renderToBitmap(
                        todo.titleStrokes, width.coerceAtLeast(1), dp(26), maxScale = 1f))
                }
            }
        } else {
            TextView(this).apply {
                text = todo.title.ifBlank { "Task" }
                setTextColor(if (todo.isDone) MUTED else INK)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                if (todo.isDone) paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
        }

    private fun toggleTaskDone(todo: ToDo, done: Boolean) {
        YearTally.add(this, if (done) 1 else -1)
        lifecycleScope.launch {
            db.toDoDao().update(todo.copy(isDone = done))
            // Completing a recurring task spawns its next occurrence (mirrors the To-Do screen).
            if (done && TaskRecurrence.isRecurring(todo)) {
                TaskRecurrence.nextDue(todo)?.let { next ->
                    db.toDoDao().insert(todo.copy(
                        id = 0, isDone = false, dueEpochDay = next.toEpochDay(),
                        createdAt = System.currentTimeMillis()))
                }
            }
            com.inkhabits.widget.WidgetCommon.updateAll(this@PomodoroActivity)
            loadTasks()
        }
    }

    /** Pick which active to-dos to work on this session. */
    private fun chooseTasks() {
        val active = todos.filter { !it.isDone }
        if (active.isEmpty()) {
            android.widget.Toast.makeText(
                this, "No active tasks — add some in the To-Do screen.",
                android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), 0)
        }
        val checks = HashMap<Long, CheckBoxView>()
        active.forEach { t ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, dp(6))
            }
            row.addView(taskLabel(t))
            val cb = CheckBoxView(this).apply { checked = sessionIds.contains(t.id) }
            checks[t.id] = cb
            row.addView(cb, LinearLayout.LayoutParams(dp(36), dp(36)))
            row.setOnClickListener { cb.checked = !cb.checked }
            container.addView(row)
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this, com.inkhabits.R.style.ThemeOverlay_InkHabits_Dialog)
            .setTitle("Choose tasks")
            .setView(android.widget.ScrollView(this).apply { addView(container) })
            .setPositiveButton("Done") { _, _ ->
                val activeIds = active.map { it.id }.toSet()
                sessionIds.removeAll(activeIds)
                active.forEach { if (checks[it.id]?.checked == true) sessionIds.add(it.id) }
                saveSession(); renderTasks(); EInk.clean(binding.root)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── rendering ──

    private fun modeColor() = when (mode) {
        Mode.FOCUS -> FOCUS_COLOR; Mode.SHORT -> SHORT_COLOR; Mode.LONG -> LONG_COLOR
    }

    private fun updateUi() {
        val total = durationFor(mode)
        binding.minutesText.text = remainingMin.toString()
        binding.minutesLabel.text = if (running) "minutes left" else "minutes"
        binding.ring.ringColor = modeColor()
        binding.ring.progress = remainingMin / total.toFloat()
        binding.minutesText.setTextColor(INK)

        binding.phaseHint.text = when (mode) {
            Mode.FOCUS -> "Time to focus"
            Mode.SHORT -> "Short break — step away"
            Mode.LONG -> "Long break — rest well"
        }
        binding.startButton.text = if (running) "PAUSE" else "START"
        binding.roundText.text =
            "Round ${(completedFocus % rounds()) + 1} of ${rounds()}  ·  $completedFocus done"

        styleTab(binding.tabFocus, mode == Mode.FOCUS)
        styleTab(binding.tabShort, mode == Mode.SHORT)
        styleTab(binding.tabLong, mode == Mode.LONG)
    }

    private fun styleTab(tv: TextView, selected: Boolean) {
        tv.background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            if (selected) setColor(modeColor()) else {
                setColor(Color.WHITE); setStroke(dp(1).coerceAtLeast(1), HAIRLINE)
            }
        }
        tv.setTextColor(if (selected) Color.WHITE else INK)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
