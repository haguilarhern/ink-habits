package com.inkhabits.ui.history

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.inkhabits.data.AppDatabase
import com.inkhabits.data.entity.Habit
import com.inkhabits.databinding.ActivityHistoryBinding
import com.inkhabits.eink.EInkActivity
import com.inkhabits.ui.widget.CalendarView
import com.inkhabits.util.Schedule
import com.inkhabits.util.StrokeRenderer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Calendar-based records: a month grid marks each day by completion, and tapping a
 * day shows which habits were done / missed that day.
 */
class HistoryActivity : EInkActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var db: AppDatabase

    private val today = LocalDate.now()
    private var habits: List<Habit> = emptyList()
    private var completedByHabit: Map<Long, Set<String>> = emptyMap()

    private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val dayFmt = DateTimeFormatter.ofPattern("EEEE, MMM d")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.get(this)

        binding.backButton.setOnClickListener { finish() }

        binding.calendar.month = YearMonth.from(today)
        binding.calendar.selected = today
        binding.calendar.statusProvider = { date -> statusFor(date) }
        binding.calendar.onSelect = { date -> renderDetail(date) }

        binding.prevMonth.setOnClickListener { changeMonth(-1) }
        binding.nextMonth.setOnClickListener { changeMonth(1) }

        updateMonthLabel()

        lifecycleScope.launch {
            combine(
                db.habitDao().observeActive(),
                db.habitCompletionDao().observeAll()
            ) { hs, completions ->
                hs to completions.groupBy { it.habitId }
                    .mapValues { e -> e.value.map { it.date }.toSet() }
            }.collect { (hs, byHabit) ->
                habits = hs
                completedByHabit = byHabit
                binding.emptyState.visibility = if (hs.isEmpty()) View.VISIBLE else View.GONE
                binding.calendar.invalidate()
                renderDetail(binding.calendar.selected ?: today)
            }
        }
    }

    private fun changeMonth(delta: Int) {
        val m = binding.calendar.month.plusMonths(delta.toLong())
        binding.calendar.month = m
        // Select today if it falls in view, else the 1st — keeps the detail meaningful.
        val sel = if (YearMonth.from(today) == m) today else m.atDay(1)
        binding.calendar.selected = sel
        updateMonthLabel()
        renderDetail(sel)
    }

    private fun updateMonthLabel() {
        binding.monthLabel.text = binding.calendar.month.atDay(1).format(monthFmt)
    }

    private fun dueOn(date: LocalDate): List<Habit> =
        habits.filter { it.startEpochDay <= date.toEpochDay() && Schedule.isDueOn(it, date) }

    private fun statusFor(date: LocalDate): Int {
        if (date.isAfter(today)) return CalendarView.FUTURE
        val due = dueOn(date)
        if (due.isEmpty()) return CalendarView.NONE_DUE
        val ds = date.toString()
        val done = due.count { completedByHabit[it.id]?.contains(ds) == true }
        return when {
            done == due.size -> CalendarView.ALL_DONE
            done > 0 -> CalendarView.PARTIAL
            else -> CalendarView.DUE_NONE
        }
    }

    private fun renderDetail(date: LocalDate) {
        binding.dayTitle.text = date.format(dayFmt)
        val box = binding.dayDetail
        box.removeAllViews()
        val due = dueOn(date)
        if (due.isEmpty()) {
            box.addView(infoText(
                if (date.isAfter(today)) "Upcoming — nothing logged yet."
                else "Nothing scheduled this day."))
            return
        }
        val ds = date.toString()
        for (h in due) {
            val done = completedByHabit[h.id]?.contains(ds) == true
            box.addView(detailRow(h, done, date.isAfter(today)))
        }
    }

    private fun detailRow(h: Habit, done: Boolean, future: Boolean): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(7), 0, dp(7))
        }
        val mark = TextView(this).apply {
            text = if (done) "✓" else if (future) "·" else "○"
            setTextColor(if (done) ACCENT else MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            width = dp(28)
            gravity = Gravity.CENTER
        }
        row.addView(mark)

        if (StrokeRenderer.hasInk(h.nameStrokes)) {
            val img = ImageView(this).apply {
                scaleType = ImageView.ScaleType.FIT_START
                val lp = LinearLayout.LayoutParams(0, dp(26), 1f)
                lp.marginStart = dp(8)
                layoutParams = lp
                alpha = if (done) 1f else 0.85f
                post {
                    setImageBitmap(StrokeRenderer.renderToBitmap(
                        h.nameStrokes, width.coerceAtLeast(1), height.coerceAtLeast(1)))
                }
            }
            row.addView(img)
        } else {
            row.addView(TextView(this).apply {
                text = h.name.ifBlank { "Habit" }
                setTextColor(if (done) Color.parseColor("#1A1A1A") else MUTED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                lp.marginStart = dp(8)
                layoutParams = lp
            })
        }
        return row
    }

    private fun infoText(msg: String) = TextView(this).apply {
        text = msg
        setTextColor(MUTED)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    companion object {
        private val ACCENT = Color.parseColor("#8C1D1D")
        private val MUTED = Color.parseColor("#6B6B6B")
    }
}
