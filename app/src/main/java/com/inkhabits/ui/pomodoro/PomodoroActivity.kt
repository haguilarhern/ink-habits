package com.inkhabits.ui.pomodoro

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.widget.TextView
import com.inkhabits.databinding.ActivityPomodoroBinding
import com.inkhabits.eink.EInk
import com.inkhabits.eink.EInkActivity
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
    private var remainingMin = 0
    private var endAtMillis = 0L
    private var completedFocus = 0

    private val handler = Handler(Looper.getMainLooper())
    private val tick = Runnable { onTick() }

    private val prefs by lazy { getSharedPreferences("pomodoro", MODE_PRIVATE) }

    private val FOCUS_COLOR = Color.parseColor("#8C1D1D")
    private val SHORT_COLOR = Color.parseColor("#2E7D32")
    private val LONG_COLOR = Color.parseColor("#2E5E8C")
    private val INK = Color.parseColor("#1A1A1A")
    private val MUTED = Color.parseColor("#6B6B6B")
    private val HAIRLINE = Color.parseColor("#CFCBC0")

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

        remainingMin = durationFor(mode)
        updateUi()
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
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
            // Pause: freeze the remaining whole minutes.
            running = false
            handler.removeCallbacks(tick)
            remainingMin = computeRemaining()
            if (remainingMin <= 0) remainingMin = durationFor(mode)
            updateUi()
            EInk.clean(binding.root)
        } else {
            if (remainingMin <= 0) remainingMin = durationFor(mode)
            endAtMillis = System.currentTimeMillis() + remainingMin * 60_000L
            running = true
            updateUi()
            EInk.clean(binding.root)
            handler.postDelayed(tick, 60_000L)
        }
    }

    private fun reset() {
        running = false
        handler.removeCallbacks(tick)
        remainingMin = durationFor(mode)
        updateUi()
        EInk.clean(binding.root)
    }

    private fun switchMode(m: Mode) {
        running = false
        handler.removeCallbacks(tick)
        mode = m
        remainingMin = durationFor(m)
        updateUi()
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

    private fun finishPhase() {
        running = false
        handler.removeCallbacks(tick)
        buzz()
        // Queue the next phase without auto-starting it.
        mode = when (mode) {
            Mode.FOCUS -> {
                completedFocus++
                if (completedFocus % rounds() == 0) Mode.LONG else Mode.SHORT
            }
            else -> Mode.FOCUS
        }
        remainingMin = durationFor(mode)
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
