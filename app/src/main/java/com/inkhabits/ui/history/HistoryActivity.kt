package com.inkhabits.ui.history

import android.app.AlertDialog
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
import com.inkhabits.data.entity.IdentityGoal
import com.inkhabits.databinding.ActivityHistoryBinding
import com.inkhabits.eink.EInkActivity
import com.inkhabits.ui.widget.BarChartView
import com.inkhabits.ui.widget.CalendarView
import com.inkhabits.ui.widget.HabitIcons
import com.inkhabits.ui.widget.ProgressBarView
import com.inkhabits.util.Goals
import com.inkhabits.util.Schedule
import com.inkhabits.util.Streaks
import com.inkhabits.util.StrokeRenderer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Records screen with two views:
 *  - Calendar: a month grid; tapping a day shows habits done / missed that day.
 *  - Progress: per-identity goal bars (perfect days toward a target) plus
 *    per-habit completion bars and a weekly histogram.
 */
class HistoryActivity : EInkActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var db: AppDatabase

    private val today = LocalDate.now()
    private var identities: List<IdentityGoal> = emptyList()
    private var habits: List<Habit> = emptyList()
    private var completedByHabit: Map<Long, Set<String>> = emptyMap()

    private var showingProgress = false

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

        binding.tabCalendar.setOnClickListener { switchTo(false) }
        binding.tabProgress.setOnClickListener { switchTo(true) }
        com.inkhabits.eink.EInk.attachFastScroll(binding.scroll)

        updateMonthLabel()

        lifecycleScope.launch {
            combine(
                db.identityGoalDao().observeAll(),
                db.habitDao().observeActive(),
                db.habitCompletionDao().observeAll()
            ) { ids, hs, completions ->
                Triple(ids, hs, completions.groupBy { it.habitId }
                    .mapValues { e -> e.value.map { it.date }.toSet() })
            }.collect { (ids, hs, byHabit) ->
                identities = ids
                habits = hs
                completedByHabit = byHabit
                binding.emptyState.visibility = if (hs.isEmpty()) View.VISIBLE else View.GONE
                binding.calendar.invalidate()
                renderDetail(binding.calendar.selected ?: today)
                if (showingProgress) renderProgress()
            }
        }
    }

    private fun switchTo(progress: Boolean) {
        showingProgress = progress
        binding.calendarPane.visibility = if (progress) View.GONE else View.VISIBLE
        binding.progressPane.visibility = if (progress) View.VISIBLE else View.GONE
        binding.tabCalendar.setTextColor(if (progress) MUTED else INK)
        binding.tabProgress.setTextColor(if (progress) INK else MUTED)
        if (progress) renderProgress()
    }

    // ---- Calendar view ----

    private fun changeMonth(delta: Int) {
        val m = binding.calendar.month.plusMonths(delta.toLong())
        binding.calendar.month = m
        val sel = if (YearMonth.from(today) == m) today else m.atDay(1)
        binding.calendar.selected = sel
        updateMonthLabel()
        renderDetail(sel)
    }

    private fun updateMonthLabel() {
        binding.monthLabel.text = binding.calendar.month.atDay(1).format(monthFmt)
    }

    private fun dueOn(date: LocalDate, list: List<Habit> = habits): List<Habit> =
        list.filter { it.startEpochDay <= date.toEpochDay() && Schedule.isDueOn(it, date) }

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

    // ---- Progress view ----

    private fun renderProgress() {
        val pane = binding.progressPane
        pane.removeAllViews()
        if (habits.isEmpty()) {
            pane.addView(infoText("Add a habit to start tracking progress."))
            return
        }

        // --- Goal health overview at the top ---
        pane.addView(goalHealthOverview())

        for (identity in identities) {
            val idHabits = habits.filter { it.identityGoalId == identity.id }
            if (idHabits.isEmpty()) continue
            pane.addView(identityCard(identity, idHabits))
        }
    }

    /**
     * At-a-glance section: habits that are falling behind vs habits on track
     * toward their goal streak. Sorted by progress % ascending.
     */
    private fun goalHealthOverview(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getDrawable(com.inkhabits.R.drawable.pill_bg)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(14); layoutParams = lp
        }

        card.addView(TextView(this).apply {
            text = "GOAL HEALTH"
            setTextColor(INK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setLetterSpacing(0.08f)
            typeface = font()
        })

        val goalData = habits.map { h ->
            val identity = identities.find { it.id == h.identityGoalId }
            val identityGoalVal = identity?.let { Goals.identityGoal(it) } ?: Goals.DEFAULT
            val goal = Goals.habitGoal(h, identityGoalVal)
            val completed = completedByHabit[h.id] ?: emptySet()
            val streak = Streaks.computeStreak(h, completed, today)
            val pct = if (goal > 0) (streak * 100f / goal).coerceAtMost(100f) else 0f
            h to pct
        }.sortedBy { it.second }

        val struggling = goalData.filter { it.second < 50f }
        val onTrack = goalData.filter { it.second >= 50f }

        if (struggling.isNotEmpty()) {
            card.addView(sectionLabel("Needs attention", Color.parseColor("#8C1D1D")))
            struggling.forEach { (h, pct) -> card.addView(healthRow(h, pct)) }
        }

        if (onTrack.isNotEmpty()) {
            card.addView(sectionLabel("On track", Color.parseColor("#2E7D32")))
            onTrack.forEach { (h, pct) -> card.addView(healthRow(h, pct)) }
        }

        if (struggling.isEmpty() && onTrack.isEmpty()) {
            card.addView(infoText("No goal data yet."))
        }

        return card
    }

    private fun sectionLabel(text: String, color: Int): View = TextView(this).apply {
        this.text = text
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setLetterSpacing(0.06f)
        typeface = font()
        setPadding(0, dp(10), 0, dp(6))
    }

    private fun healthRow(h: Habit, pct: Float): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(4))
        }

        val nameView: View = if (StrokeRenderer.hasInk(h.nameStrokes)) {
            ImageView(this).apply {
                scaleType = ImageView.ScaleType.FIT_START
                layoutParams = LinearLayout.LayoutParams(0, dp(20), 1f)
                post { setImageBitmap(StrokeRenderer.renderToBitmap(
                    h.nameStrokes, width.coerceAtLeast(1), dp(20), maxScale = 1f)) }
            }
        } else {
            TextView(this).apply {
                text = h.name.ifBlank { "Habit" }
                setTextColor(INK)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
        }
        row.addView(nameView)

        row.addView(TextView(this).apply {
            text = "${pct.toInt()}%"
            setTextColor(if (pct >= 50f) Color.parseColor("#2E7D32") else ACCENT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = font()
        })

        return row
    }

    private fun identityCard(identity: IdentityGoal, idHabits: List<Habit>): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getDrawable(com.inkhabits.R.drawable.pill_bg)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(14); layoutParams = lp
        }

        // Header: icon + name
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(ImageView(this).apply {
            setImageResource(HabitIcons.resFor(identity.icon))
            setColorFilter(INK)
            layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(10) }
        })
        header.addView(TextView(this).apply {
            text = identity.name.ifBlank { "Identity" }.uppercase()
            setTextColor(INK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setLetterSpacing(0.08f)
            typeface = font()
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        card.addView(header)

        // Goal progress (perfect days toward the identity's goal). Tap to change.
        val perfect = Streaks.totalPerfectDays(idHabits, completedByHabit, today)
        val goal = Goals.identityGoal(identity)
        val goalRow = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, dp(2))
            isClickable = true
            setOnClickListener { promptGoal(identity) }
        }
        goalRow.addView(ProgressBarView(this).apply {
            progress = (perfect / goal.toFloat()).coerceIn(0f, 1f)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(10))
        })
        goalRow.addView(TextView(this).apply {
            val pct = ((perfect * 100f / goal).coerceAtMost(100f)).toInt()
            text = "$perfect / $goal perfect days · $pct%  ·  tap to change"
            setTextColor(MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, dp(6), 0, 0)
        })
        card.addView(goalRow)

        // Divider
        card.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                topMargin = dp(12); bottomMargin = dp(4)
            }
            setBackgroundColor(Color.parseColor("#E4E1D8"))
        })

        // Per-habit stats (streak toward each habit's effective goal)
        for (h in idHabits) card.addView(habitStat(h, goal))
        return card
    }

    private fun habitStat(h: Habit, identityGoal: Int): View {
        val completed = completedByHabit[h.id] ?: emptySet()
        val streak = Streaks.computeStreak(h, completed, today)
        val best = Streaks.bestStreak(h, completed, today)
        val goal = Goals.habitGoal(h, identityGoal)
        val frac = (streak / goal.toFloat()).coerceIn(0f, 1f)

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, dp(6))
            isClickable = true
            setOnClickListener { promptHabitGoal(h, identityGoal) }
        }
        // Name + percent
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        }
        if (StrokeRenderer.hasInk(h.nameStrokes)) {
            top.addView(ImageView(this).apply {
                scaleType = ImageView.ScaleType.FIT_START
                layoutParams = LinearLayout.LayoutParams(0, dp(22), 1f)
                post { setImageBitmap(StrokeRenderer.renderToBitmap(
                    h.nameStrokes, width.coerceAtLeast(1), dp(22), maxScale = 1f)) }
            })
        } else {
            top.addView(TextView(this).apply {
                text = h.name.ifBlank { "Habit" }
                setTextColor(Color.parseColor("#1A1A1A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
        top.addView(TextView(this).apply {
            text = "${(frac * 100).toInt()}%"
            setTextColor(ACCENT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = font()
        })
        box.addView(top)

        box.addView(ProgressBarView(this).apply {
            progress = frac
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(8))
            lp.topMargin = dp(6); layoutParams = lp
        })

        box.addView(TextView(this).apply {
            val inherit = if (Goals.habitInherits(h)) " (inherited)" else ""
            text = "🔥 streak $streak / $goal$inherit · best $best · tap to set goal"
            setTextColor(MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(0, dp(5), 0, 0)
        })

        // Weekly histogram: completions per week over the last 12 weeks.
        box.addView(BarChartView(this).apply {
            setData(weeklyCounts(completed, 12), weeklyMax(h))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(40))
            lp.topMargin = dp(8); layoutParams = lp
        })
        return box
    }

    private fun promptHabitGoal(h: Habit, identityGoal: Int) {
        val presets = Goals.PRESETS
        val labels = mutableListOf("Inherit identity goal ($identityGoal)")
        labels.addAll(presets.map { "$it-day streak" })
        AlertDialog.Builder(this)
            .setTitle("Goal for ${h.name.ifBlank { "this habit" }}")
            .setItems(labels.toTypedArray()) { _, which ->
                val newGoal = if (which == 0) 0 else presets[which - 1]
                lifecycleScope.launch { db.habitDao().update(h.copy(goalDays = newGoal)) }
            }
            .show()
    }

    /** Completions in each of the last [weeks] calendar weeks, oldest first. */
    private fun weeklyCounts(completed: Set<String>, weeks: Int): IntArray {
        val out = IntArray(weeks)
        for (i in 0 until weeks) {
            val ref = today.minusWeeks((weeks - 1 - i).toLong())
            out[i] = Streaks.weeklyCount(completed, ref)
        }
        return out
    }

    /** A sensible top of the histogram scale for this habit's schedule. */
    private fun weeklyMax(h: Habit): Int = when (h.frequencyType) {
        com.inkhabits.data.entity.Frequency.WEEKLY_COUNT -> h.weeklyTarget.coerceAtLeast(1)
        com.inkhabits.data.entity.Frequency.DAYS_OF_WEEK ->
            h.daysOfWeek.split(",").count { it.isNotBlank() }.coerceAtLeast(1)
        com.inkhabits.data.entity.Frequency.INTERVAL ->
            (7 / h.intervalDays.coerceAtLeast(1)).coerceAtLeast(1)
        else -> 7
    }

    private fun promptGoal(identity: IdentityGoal) {
        val presets = Goals.PRESETS
        val labels = presets.map { "$it perfect days" }.toMutableList()
        labels.add("Use default (${Goals.DEFAULT})")
        AlertDialog.Builder(this)
            .setTitle("Goal for ${identity.name.ifBlank { "this identity" }}")
            .setItems(labels.toTypedArray()) { _, which ->
                val newGoal = if (which < presets.size) presets[which] else 0
                lifecycleScope.launch {
                    db.identityGoalDao().update(identity.copy(goalDays = newGoal))
                    // Flow will re-emit and re-render.
                }
            }
            .show()
    }

    private fun infoText(msg: String) = TextView(this).apply {
        text = msg
        setTextColor(MUTED)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun font() =
        androidx.core.content.res.ResourcesCompat.getFont(this, com.inkhabits.R.font.inter_semibold)

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    companion object {
        private val ACCENT = Color.parseColor("#8C1D1D")
        private val MUTED = Color.parseColor("#6B6B6B")
        private val INK = Color.parseColor("#1A1A1A")
    }
}
