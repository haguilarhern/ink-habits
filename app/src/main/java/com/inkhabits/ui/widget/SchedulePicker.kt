package com.inkhabits.ui.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.inkhabits.data.entity.Frequency

/** Captured schedule selection from a [SchedulePicker]. */
data class ScheduleConfig(
    val frequencyType: String,
    val daysOfWeek: String,
    val intervalDays: Int,
    val weeklyTarget: Int
)

/**
 * Compact schedule chooser: Daily / Specific days / Every N days / N× per week.
 */
class SchedulePicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var freq = Frequency.DAILY
    private val dayChips = mutableListOf<Button>()
    private val daySelected = BooleanArray(7) // Mon..Sun
    private var interval = 2
    private var weekly = 3

    private val modeRow: LinearLayout
    private val dailyChip: Button
    private val daysChip: Button
    private val intervalChip: Button
    private val weeklyChip: Button

    private val detail: LinearLayout
    private val daysRow: LinearLayout
    private val intervalRow: LinearLayout
    private val weeklyRow: LinearLayout
    private val intervalLabel: TextView
    private val weeklyLabel: TextView

    init {
        orientation = VERTICAL

        modeRow = LinearLayout(context).apply { orientation = HORIZONTAL }
        dailyChip = modeChip("Daily")
        daysChip = modeChip("Days")
        intervalChip = modeChip("Every N")
        weeklyChip = modeChip("N / week")
        modeRow.addView(dailyChip); modeRow.addView(daysChip)
        modeRow.addView(intervalChip); modeRow.addView(weeklyChip)
        addView(modeRow)

        detail = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        addView(detail)

        // Days row
        daysRow = LinearLayout(context).apply { orientation = HORIZONTAL }
        val names = listOf("M", "T", "W", "T", "F", "S", "S")
        for (i in 0..6) {
            val chip = dayChip(names[i])
            chip.setOnClickListener {
                daySelected[i] = !daySelected[i]
                styleDayChip(chip, daySelected[i])
            }
            dayChips.add(chip)
            daysRow.addView(chip)
        }

        // Interval row
        intervalRow = LinearLayout(context).apply {
            orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        }
        intervalLabel = stepperLabel()
        intervalRow.addView(stepperButton("−") { if (interval > 2) interval--; refreshLabels() })
        intervalRow.addView(intervalLabel)
        intervalRow.addView(stepperButton("+") { if (interval < 30) interval++; refreshLabels() })

        // Weekly row
        weeklyRow = LinearLayout(context).apply {
            orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        }
        weeklyLabel = stepperLabel()
        weeklyRow.addView(stepperButton("−") { if (weekly > 1) weekly--; refreshLabels() })
        weeklyRow.addView(weeklyLabel)
        weeklyRow.addView(stepperButton("+") { if (weekly < 7) weekly++; refreshLabels() })

        dailyChip.setOnClickListener { setFreq(Frequency.DAILY) }
        daysChip.setOnClickListener { setFreq(Frequency.DAYS_OF_WEEK) }
        intervalChip.setOnClickListener { setFreq(Frequency.INTERVAL) }
        weeklyChip.setOnClickListener { setFreq(Frequency.WEEKLY_COUNT) }

        refreshLabels()
        setFreq(Frequency.DAILY)
    }

    private fun setFreq(f: String) {
        freq = f
        styleModeChip(dailyChip, f == Frequency.DAILY)
        styleModeChip(daysChip, f == Frequency.DAYS_OF_WEEK)
        styleModeChip(intervalChip, f == Frequency.INTERVAL)
        styleModeChip(weeklyChip, f == Frequency.WEEKLY_COUNT)
        detail.removeAllViews()
        when (f) {
            Frequency.DAYS_OF_WEEK -> detail.addView(daysRow)
            Frequency.INTERVAL -> detail.addView(intervalRow)
            Frequency.WEEKLY_COUNT -> detail.addView(weeklyRow)
        }
    }

    private fun refreshLabels() {
        intervalLabel.text = "Every $interval days"
        weeklyLabel.text = "$weekly times / week"
    }

    /** Restore a previously captured selection (used when editing a habit). */
    fun setConfig(cfg: ScheduleConfig) {
        interval = cfg.intervalDays.coerceIn(2, 30)
        weekly = cfg.weeklyTarget.coerceIn(1, 7)
        for (i in 0..6) daySelected[i] = false
        if (cfg.daysOfWeek.isNotBlank()) cfg.daysOfWeek.split(",").forEach {
            val d = it.trim().toIntOrNull()
            if (d != null && d in 1..7) daySelected[d - 1] = true
        }
        dayChips.forEachIndexed { i, c -> styleDayChip(c, daySelected[i]) }
        refreshLabels()
        setFreq(cfg.frequencyType)
    }

    fun getConfig(): ScheduleConfig {
        val days = (0..6).filter { daySelected[it] }.joinToString(",") { (it + 1).toString() }
        // If "Days" chosen but nothing ticked, fall back to daily to stay valid.
        val effective = if (freq == Frequency.DAYS_OF_WEEK && days.isEmpty()) Frequency.DAILY else freq
        return ScheduleConfig(effective, days, interval, weekly)
    }

    // ── styling helpers ──
    private fun modeChip(label: String): Button = Button(context).apply {
        text = label; isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        minWidth = 0; minimumWidth = 0
        setPadding(dp(10), dp(2), dp(10), dp(2))
        val lp = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        lp.marginEnd = dp(4)
        layoutParams = lp
    }

    private fun dayChip(label: String): Button = Button(context).apply {
        text = label; isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        minWidth = 0; minimumWidth = 0
        setPadding(0, dp(2), 0, dp(2))
        val lp = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        lp.marginEnd = dp(3)
        layoutParams = lp
        styleDayChip(this, false)
    }

    private fun stepperButton(label: String, onClick: () -> Unit): Button = Button(context).apply {
        text = label; isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        minWidth = 0; minimumWidth = dp(48)
        setOnClickListener { onClick() }
    }

    private fun stepperLabel(): TextView = TextView(context).apply {
        setTextColor(Color.BLACK)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        gravity = Gravity.CENTER
        val lp = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        layoutParams = lp
    }

    private fun styleModeChip(b: Button, on: Boolean) {
        b.background = chipBg(on)
        b.elevation = 0f
        b.stateListAnimator = null
        b.setTextColor(if (on) Color.WHITE else Color.parseColor("#0B0B0C"))
    }

    private fun styleDayChip(b: Button, on: Boolean) {
        b.background = chipBg(on)
        b.elevation = 0f
        b.stateListAnimator = null
        b.setTextColor(if (on) Color.WHITE else Color.parseColor("#0B0B0C"))
    }

    /** Rounded pill: accent fill when selected, hairline outline when idle. */
    private fun chipBg(on: Boolean) = android.graphics.drawable.GradientDrawable().apply {
        cornerRadius = dp(12).toFloat()
        if (on) {
            setColor(ACCENT)
        } else {
            setColor(Color.WHITE)
            setStroke(dp(1).coerceAtLeast(1), Color.parseColor("#D9D9DE"))
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    companion object {
        private val ACCENT = Color.parseColor("#0B0B0C")
    }
}
