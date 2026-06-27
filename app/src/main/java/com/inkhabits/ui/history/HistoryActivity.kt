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
import com.inkhabits.data.entity.HabitCompletion
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
    private var frozenByHabit: Map<Long, Set<String>> = emptyMap()
    private var identityFrozen: Map<Long, Set<String>> = emptyMap()

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
                db.habitCompletionDao().observeAll(),
                db.streakFreezeDao().observeAll()
            ) { ids, hs, completions, freezes ->
                Snapshot(
                    ids, hs,
                    completions.groupBy { it.habitId }.mapValues { e -> e.value.map { it.date }.toSet() },
                    freezes.filter { it.habitId > 0 }.groupBy { it.habitId }
                        .mapValues { e -> e.value.map { it.date }.toSet() },
                    freezes.filter { it.identityId > 0 }.groupBy { it.identityId }
                        .mapValues { e -> e.value.map { it.date }.toSet() }
                )
            }.collect { snap ->
                identities = snap.identities
                habits = snap.habits
                completedByHabit = snap.completedByHabit
                frozenByHabit = snap.frozenByHabit
                identityFrozen = snap.identityFrozen
                binding.emptyState.visibility = if (snap.habits.isEmpty()) View.VISIBLE else View.GONE
                binding.calendar.invalidate()
                renderDetail(binding.calendar.selected ?: today)
                if (showingProgress) renderProgress()
            }
        }
    }

    private data class Snapshot(
        val identities: List<IdentityGoal>,
        val habits: List<Habit>,
        val completedByHabit: Map<Long, Set<String>>,
        val frozenByHabit: Map<Long, Set<String>>,
        val identityFrozen: Map<Long, Set<String>>
    )

    /** A habit's real completions plus its frozen days (frozen counts as done for streaks). */
    private fun effective(habitId: Long): Set<String> =
        (completedByHabit[habitId] ?: emptySet()) + (frozenByHabit[habitId] ?: emptySet())

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
        // Frozen days count as done so a protected streak stays visually intact.
        val done = due.count { effective(it.id).contains(ds) }
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
        // Past/today rows are tappable so you can check off (or un-check) a habit
        // you forgot to log; future days stay read-only.
        if (!date.isAfter(today)) {
            box.addView(infoText("Tap a habit to mark it done or undone."))
        }
        val ds = date.toString()
        for (h in due) {
            val done = completedByHabit[h.id]?.contains(ds) == true
            val frozen = !done && frozenByHabit[h.id]?.contains(ds) == true
            box.addView(detailRow(h, done, frozen, date))
        }
    }

    private fun detailRow(h: Habit, done: Boolean, frozen: Boolean, date: LocalDate): View {
        val future = date.isAfter(today)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(7), 0, dp(7))
            if (!future) {
                isClickable = true
                setOnClickListener { toggleCompletion(h, date, done) }
            }
        }
        val mark = TextView(this).apply {
            text = when {
                done -> "✓"
                frozen -> "❄"
                future -> "·"
                else -> "○"
            }
            setTextColor(when {
                done -> ACCENT
                frozen -> FROZEN
                else -> MUTED
            })
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

    /**
     * Toggle a habit's completion for [date] (today or any past day). The combine()
     * flow observing completions re-renders the day detail and recolors the calendar
     * automatically, so we only persist the change and refresh widgets/rewards here.
     */
    private fun toggleCompletion(h: Habit, date: LocalDate, currentlyDone: Boolean) {
        val ds = date.toString()
        lifecycleScope.launch {
            if (currentlyDone) {
                db.habitCompletionDao().delete(h.id, ds)
            } else {
                db.habitCompletionDao().insert(HabitCompletion(habitId = h.id, date = ds))
                com.inkhabits.util.Rewards.checkAndUnlock(this@HistoryActivity)
            }
            com.inkhabits.widget.WidgetCommon.updateAll(this@HistoryActivity)
        }
    }

    // ---- Progress view ----

    private fun renderProgress() {
        val pane = binding.progressPane
        pane.removeAllViews()
        if (habits.isEmpty()) {
            pane.addView(infoText("Add a habit to start tracking progress."))
            return
        }

        pane.addView(goalHealthOverview())
        pane.addView(streakMomentum())

        for (identity in identities) {
            val idHabits = habits.filter { it.identityGoalId == identity.id }
            if (idHabits.isEmpty()) continue
            pane.addView(identityCard(identity, idHabits))
        }
    }

    /**
     * Goal health: habits falling behind vs on track toward their goal streak.
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
            val streak = Streaks.computeStreak(h, effective(h.id), today)
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

    /**
     * Streak momentum: habits actively building a streak vs stale habits
     * that have been due many times but have zero streak.
     */
    private fun streakMomentum(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getDrawable(com.inkhabits.R.drawable.pill_bg)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(14); layoutParams = lp
        }

        card.addView(TextView(this).apply {
            text = "STREAK MOMENTUM"
            setTextColor(INK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setLetterSpacing(0.08f)
            typeface = font()
        })

        val momentumData = habits.map { h ->
            val streak = Streaks.computeStreak(h, effective(h.id), today)
            val dueCount = countOccurrencesSinceStart(h, today)
            h to Pair(streak, dueCount)
        }

        val progressing = momentumData.filter { it.second.first > 0 }
            .sortedByDescending { it.second.first }
        val stale = momentumData.filter { it.second.first == 0 && it.second.second > 0 }
            .sortedByDescending { it.second.second }

        if (progressing.isNotEmpty()) {
            card.addView(sectionLabel("Progressing", Color.parseColor("#2E7D32")))
            progressing.forEach { (h, data) ->
                card.addView(momentumRow(h, data.first, progressing = true))
            }
        }

        if (stale.isNotEmpty()) {
            card.addView(sectionLabel("Stale", Color.parseColor("#8C1D1D")))
            stale.forEach { (h, data) ->
                card.addView(momentumRow(h, data.first, progressing = false))
            }
        }

        if (progressing.isEmpty() && stale.isEmpty()) {
            card.addView(infoText("No streak data yet."))
        }

        return card
    }

    /** Count scheduled occurrences since the habit's start date. */
    private fun countOccurrencesSinceStart(h: Habit, today: LocalDate): Int {
        val startDate = LocalDate.ofEpochDay(h.startEpochDay)
        var d = startDate
        var count = 0
        var guard = 0
        while (!d.isAfter(today) && guard < 730) {
            if (Schedule.isDueOn(h, d)) count++
            d = d.plusDays(1)
            guard++
        }
        return count
    }

    private fun momentumRow(h: Habit, streak: Int, progressing: Boolean): View {
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
            text = if (progressing) "🔥 $streak" else "stale"
            setTextColor(if (progressing) Color.parseColor("#2E7D32") else ACCENT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = font()
        })

        return row
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

        // Tapping the header opens the full identity editor (name, icon, goal, and
        // its habits) — lets you change the identity as a whole, not just its streak.
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            setOnClickListener { openEditIdentity(identity.id) }
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
        header.addView(TextView(this).apply {
            text = "✎ edit"
            setTextColor(MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        })
        card.addView(header)

        val effByHabit = idHabits.associate { it.id to effective(it.id) }
        val perfect = Streaks.totalPerfectDays(
            idHabits, effByHabit, today, identityFrozen[identity.id] ?: emptySet())
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

        card.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                topMargin = dp(12); bottomMargin = dp(4)
            }
            setBackgroundColor(Color.parseColor("#E4E1D8"))
        })

        for (h in idHabits) card.addView(habitStat(h, goal))
        return card
    }

    private fun habitStat(h: Habit, identityGoal: Int): View {
        val completed = completedByHabit[h.id] ?: emptySet() // real, for the weekly bar chart
        val eff = effective(h.id)                            // freeze-aware, for streaks
        val streak = Streaks.computeStreak(h, eff, today)
        val best = Streaks.bestStreak(h, eff, today)
        val goal = Goals.habitGoal(h, identityGoal)
        val frac = (streak / goal.toFloat()).coerceIn(0f, 1f)

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, dp(6))
            isClickable = true
            setOnClickListener { promptHabitGoal(h, identityGoal) }
        }
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

        box.addView(BarChartView(this).apply {
            setData(weeklyCounts(completed, 12), weeklyMax(h))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(40))
            lp.topMargin = dp(8); layoutParams = lp
        })
        return box
    }

    private fun openEditIdentity(identityId: Long) {
        startActivity(android.content.Intent(this, com.inkhabits.ui.onboarding.OnboardingActivity::class.java)
            .putExtra(com.inkhabits.ui.onboarding.OnboardingActivity.EXTRA_EDIT_IDENTITY, identityId))
    }

    private fun promptHabitGoal(h: Habit, identityGoal: Int) {
        promptGoalNumber(
            title = "Goal for ${h.name.ifBlank { "this habit" }}",
            hint = "e.g. 30",
            suffix = "completions = 100%",
            resetLabel = "Inherit ($identityGoal)",
            current = h.goalDays
        ) { newGoal -> lifecycleScope.launch { db.habitDao().update(h.copy(goalDays = newGoal)) } }
    }

    private fun weeklyCounts(completed: Set<String>, weeks: Int): IntArray {
        val out = IntArray(weeks)
        for (i in 0 until weeks) {
            val ref = today.minusWeeks((weeks - 1 - i).toLong())
            out[i] = Streaks.weeklyCount(completed, ref)
        }
        return out
    }

    private fun weeklyMax(h: Habit): Int = when (h.frequencyType) {
        com.inkhabits.data.entity.Frequency.WEEKLY_COUNT -> h.weeklyTarget.coerceAtLeast(1)
        com.inkhabits.data.entity.Frequency.DAYS_OF_WEEK ->
            h.daysOfWeek.split(",").count { it.isNotBlank() }.coerceAtLeast(1)
        com.inkhabits.data.entity.Frequency.INTERVAL ->
            (7 / h.intervalDays.coerceAtLeast(1)).coerceAtLeast(1)
        else -> 7
    }

    private fun promptGoal(identity: IdentityGoal) {
        promptGoalNumber(
            title = "Goal for ${identity.name.ifBlank { "this identity" }}",
            hint = "e.g. ${Goals.DEFAULT}",
            suffix = "perfect days = 100%",
            resetLabel = "Use default (${Goals.DEFAULT})",
            current = identity.goalDays
        ) { newGoal -> lifecycleScope.launch { db.identityGoalDao().update(identity.copy(goalDays = newGoal)) } }
    }

    /**
     * Dialog with an open number box: the user types how many repetitions equal 100%.
     * Blank / 0 (or the [resetLabel] button) clears the goal back to inherit/default.
     */
    private fun promptGoalNumber(
        title: String, hint: String, suffix: String, resetLabel: String,
        current: Int, onSave: (Int) -> Unit
    ) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val box = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(android.text.InputFilter.LengthFilter(4))
            this.hint = hint
            if (current > 0) setText(current.toString())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            layoutParams = LinearLayout.LayoutParams(dp(100), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        container.addView(box)
        container.addView(TextView(this).apply {
            text = suffix
            setTextColor(MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.marginStart = dp(10); layoutParams = lp
        })
        com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this, com.inkhabits.R.style.ThemeOverlay_InkHabits_Dialog)
            .setTitle(title)
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                onSave((box.text?.toString()?.trim()?.toIntOrNull() ?: 0).coerceAtLeast(0))
            }
            .setNeutralButton(resetLabel) { _, _ -> onSave(0) }
            .setNegativeButton("Cancel", null)
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
        private val FROZEN = Color.parseColor("#2E5E8C")
    }
}
