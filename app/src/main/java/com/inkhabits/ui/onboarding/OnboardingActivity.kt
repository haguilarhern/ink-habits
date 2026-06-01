package com.inkhabits.ui.onboarding

import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.inkhabits.data.AppDatabase
import com.inkhabits.data.entity.Habit
import com.inkhabits.data.entity.IdentityGoal
import com.inkhabits.databinding.ActivityOnboardingBinding
import com.inkhabits.ui.dashboard.DashboardActivity
import com.inkhabits.ui.widget.InputField
import com.inkhabits.ui.widget.SchedulePicker
import com.inkhabits.ui.widget.ScheduleConfig
import com.inkhabits.ui.writing.WritingHostActivity
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Identity-first onboarding wizard:
 * Welcome -> name an identity (type/ink + icon) -> add its habits (each with a
 * schedule) -> "add another identity?" loop -> finish to the dashboard.
 */
class OnboardingActivity : WritingHostActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var db: AppDatabase

    private enum class Step { WELCOME, IDENTITY, HABITS, REVIEW, ANOTHER }
    private var step = Step.WELCOME
    private var addMode = false

    // Minimal Lucide line icons, by key (see HabitIcons).
    private val icons = com.inkhabits.ui.widget.HabitIcons.keys

    // Pending identity being built (saved only when leaving the HABITS step).
    private var pendingName = ""
    private var pendingStrokes = ""
    private var pendingIcon = com.inkhabits.ui.widget.HabitIcons.DEFAULT

    private var identityField: InputField? = null
    private val createdIdentities = mutableListOf<IdentityGoal>()

    // Habits added for the current identity (one at a time via "Add this habit").
    private val pendingHabits = mutableListOf<PendingHabit>()
    private var habitInput: InputField? = null
    private var habitSchedule: SchedulePicker? = null
    private var habitAnchor: InputField? = null
    private var habitReminder: Int = -1
    private var timeRefresh: (() -> Unit)? = null
    // Goal streaks (0 = default / inherit). Identity goal + the in-progress habit goal.
    private var pendingIdentityGoal: Int = 0
    private var pendingHabitGoal: Int = 0
    private var habitGoalRefresh: (() -> Unit)? = null
    // A habit pulled back from the Review step to edit (re-populates the entry).
    private var editingHabit: PendingHabit? = null
    private var editingHabitId: Long = 0L  // DB id carried through an edit so history is kept

    // Editing an existing identity (0 = creating a new one).
    private var editIdentityId: Long = 0L
    private val originalHabitIds = mutableSetOf<Long>()

    private val interRegular by lazy { androidx.core.content.res.ResourcesCompat.getFont(this, com.inkhabits.R.font.inter_regular)!! }
    private val interSemiBold by lazy { androidx.core.content.res.ResourcesCompat.getFont(this, com.inkhabits.R.font.inter_semibold)!! }

    private data class PendingHabit(
        val name: String,
        val strokes: String,
        val cfg: ScheduleConfig,
        val reminderMinutes: Int,
        val anchor: String,
        val anchorStrokes: String,
        val goalDays: Int = 0,  // 0 = inherit the identity's goal
        val habitId: Long = 0L  // existing DB row when editing; 0 = new
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.get(this)

        addMode = intent.getBooleanExtra(EXTRA_ADD_IDENTITY, false)
        if (addMode) step = Step.IDENTITY
        editIdentityId = intent.getLongExtra(EXTRA_EDIT_IDENTITY, 0L)

        // Warm up handwriting recognition (anchor OCR) so the model is ready when needed.
        com.inkhabits.util.InkRecognizer.preload()

        binding.skipButton.setOnClickListener { onSkip() }
        binding.skipButton.typeface = interRegular
        binding.backButton.setOnClickListener { onBack() }
        // Keep the active inline pen region aligned while scrolling.
        binding.scroll.setOnScrollChangeListener { _, _, _, _, _ ->
            com.inkhabits.ui.widget.InlineInkView.current?.refreshLimit()
        }

        if (editIdentityId != 0L) loadIdentityForEdit() else render()
    }

    /** Load an existing identity + its habits into the flow, opening on the Review step. */
    private fun loadIdentityForEdit() {
        lifecycleScope.launch {
            val goal = db.identityGoalDao().getAll().firstOrNull { it.id == editIdentityId }
            if (goal == null) { render(); return@launch }
            pendingName = goal.name
            pendingStrokes = goal.nameStrokes
            pendingIcon = goal.icon
            pendingIdentityGoal = goal.goalDays
            pendingHabits.clear()
            originalHabitIds.clear()
            db.habitDao().getActive().filter { it.identityGoalId == editIdentityId }.forEach { h ->
                originalHabitIds.add(h.id)
                pendingHabits.add(PendingHabit(
                    name = h.name, strokes = h.nameStrokes,
                    cfg = ScheduleConfig(h.frequencyType, h.daysOfWeek, h.intervalDays, h.weeklyTarget),
                    reminderMinutes = h.reminderMinutes,
                    anchor = h.anchor, anchorStrokes = h.anchorStrokes,
                    goalDays = h.goalDays, habitId = h.id))
            }
            step = Step.REVIEW
            render()
        }
    }

    private fun render() {
        binding.contentArea.removeAllViews()
        // Entry views only live on the HABITS step — drop stale refs elsewhere so
        // save logic never reads a detached input.
        if (step != Step.HABITS) {
            habitInput = null; habitSchedule = null; habitAnchor = null; timeRefresh = null
        }
        // Top bar: back hidden on first screen; skip hidden on the final loop screen.
        val onFirst = step == Step.WELCOME || (step == Step.IDENTITY && addMode)
        binding.backButton.visibility = if (onFirst) View.INVISIBLE else View.VISIBLE
        binding.skipButton.visibility =
            if (step == Step.ANOTHER || editIdentityId != 0L) View.GONE else View.VISIBLE
        when (step) {
            Step.WELCOME -> renderWelcome()
            Step.IDENTITY -> renderIdentity()
            Step.HABITS -> renderHabits()
            Step.REVIEW -> renderReview()
            Step.ANOTHER -> renderAnother()
        }
    }

    /** A full-width primary action button placed in the content flow (below the step's content). */
    private fun primaryCta(text: String, onClick: () -> Unit): MaterialButton {
        val btn = MaterialButton(this).apply {
            this.text = text
            isAllCaps = false
            typeface = interSemiBold
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            cornerRadius = dp(12)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56))
            lp.topMargin = dp(24)
            layoutParams = lp
            setOnClickListener { onClick() }
        }
        binding.contentArea.addView(btn)
        return btn
    }

    private fun header(indicator: String, title: String, subtitle: String) {
        binding.stepIndicator.apply {
            text = indicator
            setTypeface(interSemiBold)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setLetterSpacing(0.14f)
        }
        binding.stepTitle.apply {
            text = title
            setTypeface(interSemiBold)
            setLetterSpacing(-0.02f)
        }
        binding.stepSubtitle.apply {
            text = subtitle
            setTypeface(interRegular)
        }
    }

    private fun renderWelcome() {
        header("WELCOME", "Ink Habits", "")
        binding.contentArea.addView(android.widget.ImageView(this).apply {
            setImageResource(com.inkhabits.R.drawable.il_welcome)
            adjustViewBounds = true
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(150))
            lp.topMargin = dp(12); lp.bottomMargin = dp(20)
            lp.gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = lp
        })
        binding.contentArea.addView(TextView(this).apply {
            text = "Build habits by becoming the person who already has them.\n\n" +
                "First you'll choose an identity — who you want to become — then add the " +
                "habits that person does. You can write with your pen or type."
            setTextColor(Color.parseColor("#5A5A5A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setLineSpacing(4f, 1.25f)
            typeface = interRegular
        })
        primaryCta("Get started →") { step = Step.IDENTITY; render() }
    }

    private fun renderIdentity() {
        header("IDENTITY", "Who do you want to become?", "e.g. a Reader, an Athlete, a Writer")

        binding.contentArea.addView(label("Name this identity"))
        val field = inkField("I am a…", "Name your identity")
        identityField = field
        binding.contentArea.addView(field)
        field.prefill(pendingName, pendingStrokes)

        binding.contentArea.addView(label("Pick an icon").apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(16)
            layoutParams = lp
        })

        val iconButtons = mutableListOf<android.widget.ImageButton>()
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (key in icons) {
            val b = android.widget.ImageButton(this).apply {
                tag = key
                setImageResource(com.inkhabits.ui.widget.HabitIcons.resFor(key))
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                setPadding(dp(13), dp(13), dp(13), dp(13))
                val lp = LinearLayout.LayoutParams(dp(54), dp(54))
                lp.marginEnd = dp(8)
                layoutParams = lp
            }
            b.setOnClickListener {
                pendingIcon = key
                iconButtons.forEach { styleIcon(it, it.tag == pendingIcon) }
            }
            styleIcon(b, key == pendingIcon)
            iconButtons.add(b)
            row.addView(b)
        }
        binding.contentArea.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        })

        binding.contentArea.addView(label("Goal streak (perfect days)").apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(16); layoutParams = lp
        })
        binding.contentArea.addView(goalChooser(
            labels = listOf("Default (${com.inkhabits.util.Goals.DEFAULT})") +
                com.inkhabits.util.Goals.PRESETS.map { "$it days" },
            values = listOf(0) + com.inkhabits.util.Goals.PRESETS,
            currentValue = { pendingIdentityGoal },
            onPick = { pendingIdentityGoal = it }))

        primaryCta("Next →") { onIdentityNext() }
    }

    private fun renderHabits() {
        val who = pendingName.ifBlank { "this person" }
        val editing = editingHabit != null
        editingHabit?.let { pendingHabitGoal = it.goalDays }  // load goal for the chooser
        header("HABITS", if (editing) "Edit habit" else "What would $who do?",
            "Write a habit, set how often, and an optional anchor.")

        binding.contentArea.addView(label("Habit"))
        val input = inkField("Habit name…", "Write the habit")
        habitInput = input
        binding.contentArea.addView(input)

        binding.contentArea.addView(label("How often?").apply {
            (layoutParams as? LinearLayout.LayoutParams)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(6); layoutParams = lp
        })
        val picker = SchedulePicker(this)
        habitSchedule = picker
        binding.contentArea.addView(picker)

        binding.contentArea.addView(label("Time of day (optional)").apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(10); layoutParams = lp
        })
        binding.contentArea.addView(timeSelector())

        binding.contentArea.addView(label("Anchor — after what? (optional)").apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(10); layoutParams = lp
        })
        val anchor = inkField("e.g. after my morning coffee", "Anchor — after what?")
        habitAnchor = anchor
        binding.contentArea.addView(anchor)

        binding.contentArea.addView(label("Goal streak").apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(10); layoutParams = lp
        })
        binding.contentArea.addView(goalChooser(
            labels = listOf("Inherit identity") +
                com.inkhabits.util.Goals.PRESETS.map { "$it-day streak" },
            values = listOf(0) + com.inkhabits.util.Goals.PRESETS,
            currentValue = { pendingHabitGoal },
            onPick = { pendingHabitGoal = it },
            registerRefresh = { habitGoalRefresh = it }))

        // Add-another stays here for quick entry; primary goes to the review list.
        if (!editing) {
            binding.contentArea.addView(MaterialButton(
                this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = "✓ Add & write another"; isAllCaps = false
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52))
                lp.topMargin = dp(10); layoutParams = lp
                setOnClickListener {
                    if (habitInput?.hasContent() != true) toast("Write or type the habit first")
                    else captureAndAdd(reset = true) { toast("Habit added") }
                }
            })
        }
        val cta = if (editing) "Save habit →" else "Review →"
        primaryCta(cta) {
            captureAndAdd(reset = false) {
                if (pendingHabits.isEmpty()) toast("Add at least one habit first")
                else { step = Step.REVIEW; render() }
            }
        }

        // Re-populate the entry when editing an existing habit.
        editingHabit?.let { h ->
            input.prefill(h.name, h.strokes)
            anchor.prefill(h.anchor, h.anchorStrokes)
            picker.setConfig(h.cfg)
            habitReminder = h.reminderMinutes
            timeRefresh?.invoke()
            editingHabit = null
        }
    }

    /** Review/confirm screen: list created habits with edit + delete. */
    private fun renderReview() {
        val editingIdentity = editIdentityId != 0L
        header("REVIEW", if (editingIdentity) "Edit ${pendingName.ifBlank { "identity" }}" else "Your habits",
            "Confirm, edit, or remove — then save.")

        // When editing an existing identity, let the user change its name/icon too.
        binding.contentArea.addView(MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "Edit identity name & icon"; isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48))
            lp.bottomMargin = dp(6); layoutParams = lp
            setOnClickListener { step = Step.IDENTITY; render() }
        })

        if (pendingHabits.isEmpty()) {
            binding.contentArea.addView(label("No habits yet."))
        }
        pendingHabits.forEachIndexed { idx, h ->
            binding.contentArea.addView(reviewRow(idx, h))
        }
        binding.contentArea.addView(MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "+ Add another habit"; isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52))
            lp.topMargin = dp(14); layoutParams = lp
            setOnClickListener { step = Step.HABITS; render() }
        })
        primaryCta("Save identity →") { saveIdentityAndHabits() }
    }

    private fun reviewRow(index: Int, h: PendingHabit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            background = getDrawable(com.inkhabits.R.drawable.pill_bg)
            setPadding(dp(14), dp(10), dp(6), dp(10))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(8); layoutParams = lp
            isClickable = true
            setOnClickListener { editHabit(index) } // tap to edit
        }
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        texts.addView(TextView(this).apply {
            text = h.name.ifBlank { if (h.strokes.isNotEmpty()) "Handwritten habit" else "Habit" }
            setTextColor(Color.parseColor("#1A1A1A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = interSemiBold
        })
        val sched = com.inkhabits.util.Schedule.label(
            com.inkhabits.data.entity.Habit(
                identityGoalId = 0, frequencyType = h.cfg.frequencyType,
                daysOfWeek = h.cfg.daysOfWeek, intervalDays = h.cfg.intervalDays,
                weeklyTarget = h.cfg.weeklyTarget))
        val time = com.inkhabits.util.Schedule.formatTime(h.reminderMinutes)
        texts.addView(TextView(this).apply {
            text = if (time.isEmpty()) sched else "$sched · $time"
            setTextColor(Color.parseColor("#6B6B6B"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        })
        row.addView(texts)
        row.addView(Button(this).apply {
            text = "✕"; isAllCaps = false
            setTextColor(Color.parseColor("#8C1D1D"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { pendingHabits.removeAt(index); render() }
        })
        return row
    }

    private fun editHabit(index: Int) {
        val h = pendingHabits.removeAt(index)
        editingHabit = h
        editingHabitId = h.habitId  // preserve the DB row so completion history survives
        step = Step.HABITS; render()
    }

    /**
     * Capture the current entry (if any), resolve its anchor/name via OCR off the UI
     * thread, add it to the list, then run [then]. When [reset] is true the entry is
     * cleared in place for another habit (without rebuilding — keeps the ink surface alive).
     */
    private fun captureAndAdd(reset: Boolean, then: () -> Unit) {
        val input = habitInput
        if (input == null || !input.hasContent()) { then(); return }
        val name = input.getText(); val strokes = input.getStrokes()
        val cfg = habitSchedule!!.getConfig(); val reminder = habitReminder
        val anchorText = habitAnchor?.getText().orEmpty()
        val anchorStrokes = habitAnchor?.getStrokes().orEmpty()
        val goalSnap = pendingHabitGoal
        if (reset) {
            input.prepareForNext()
            habitAnchor?.prepareForNext()
            habitReminder = -1
            pendingHabitGoal = 0
            timeRefresh?.invoke()
            habitGoalRefresh?.invoke()
            input.focusInk()
            binding.scroll.post { cleanRefresh(binding.root) }
        }
        val carryId = editingHabitId
        editingHabitId = 0L
        lifecycleScope.launch {
            val (aText, aStrokes) = resolveAnchor(anchorText, anchorStrokes)
            val resolvedName = resolveName(name, strokes)
            pendingHabits.add(PendingHabit(resolvedName, strokes, cfg, reminder, aText, aStrokes, goalSnap, carryId))
            then()
        }
    }

    /** Daypart chips ("Any / Morning / Afternoon / Evening") + a specific-time chip. */
    private fun timeSelector(): View {
        val scroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        scroll.addView(row)

        val entries = listOf(
            "Any time" to com.inkhabits.util.Schedule.TIME_ANY,
            "Morning" to com.inkhabits.util.Schedule.TIME_MORNING,
            "Afternoon" to com.inkhabits.util.Schedule.TIME_AFTERNOON,
            "Evening" to com.inkhabits.util.Schedule.TIME_EVENING,
        )
        val chips = mutableListOf<Pair<TextView, Int>>()
        val pickChip = timeChip("Pick time")

        fun refresh() {
            for ((c, v) in chips) styleTimeChip(c, habitReminder == v)
            val specific = habitReminder >= 0
            pickChip.text = if (specific) com.inkhabits.util.Schedule.formatTime(habitReminder) else "Pick time"
            styleTimeChip(pickChip, specific)
        }

        for ((labelTxt, value) in entries) {
            val c = timeChip(labelTxt)
            c.setOnClickListener { habitReminder = value; refresh() }
            chips.add(c to value)
            row.addView(c)
        }
        pickChip.setOnClickListener { pickTime { refresh() } }
        row.addView(pickChip)

        timeRefresh = { refresh() }
        refresh()
        return scroll
    }

    private fun timeChip(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        typeface = interSemiBold
        setPadding(dp(14), dp(7), dp(14), dp(7))
        isClickable = true
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.marginEnd = dp(8)
        layoutParams = lp
    }

    private fun styleTimeChip(c: TextView, on: Boolean) {
        c.background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            if (on) setColor(Color.parseColor("#8C1D1D"))
            else { setColor(Color.WHITE); setStroke(dp(1), Color.parseColor("#CFCBC0")) }
        }
        c.setTextColor(if (on) Color.WHITE else Color.parseColor("#1A1A1A"))
    }

    private fun pickTime(onPicked: () -> Unit) {
        val start = if (habitReminder >= 0) habitReminder else 8 * 60
        TimePickerDialog(this, { _, hour, minute ->
            habitReminder = hour * 60 + minute
            onPicked()
        }, start / 60, start % 60, false).show()
    }

    /** Typed name wins; otherwise OCR the handwriting for a preview transcription ("" if none). */
    private suspend fun resolveName(text: String, strokes: String): String {
        if (text.isNotBlank()) return text
        if (!com.inkhabits.util.StrokeRenderer.hasInk(strokes)) return ""
        return com.inkhabits.util.InkRecognizer.recognize(strokes).orEmpty()
    }

    /**
     * Turn an anchor entry into displayable form: typed text wins; otherwise run
     * handwriting recognition. Falls back to keeping the ink if OCR is unavailable.
     */
    private suspend fun resolveAnchor(text: String, strokes: String): Pair<String, String> {
        if (text.isNotBlank()) return text to ""
        if (!com.inkhabits.util.StrokeRenderer.hasInk(strokes)) return "" to ""
        val recognized = com.inkhabits.util.InkRecognizer.recognize(strokes)
        return if (!recognized.isNullOrBlank()) recognized to "" else "" to strokes
    }

    private fun renderAnother() {
        header("IDENTITIES", "Add another identity?", "Each identity has its own set of habits.")
        binding.contentArea.addView(label("Created so far"))
        for (g in createdIdentities) {
            binding.contentArea.addView(TextView(this).apply {
                text = g.name.ifBlank { "Handwritten identity" }
                setTextColor(Color.BLACK)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                setPadding(0, dp(8), 0, dp(8))
                setCompoundDrawablesRelativeWithIntrinsicBounds(
                    com.inkhabits.ui.widget.HabitIcons.resFor(g.icon), 0, 0, 0)
                compoundDrawablePadding = dp(10)
                androidx.core.widget.TextViewCompat.setCompoundDrawableTintList(
                    this, android.content.res.ColorStateList.valueOf(Color.parseColor("#1A1A1A")))
            })
        }
        binding.contentArea.addView(MaterialButton(
            this, null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "+ Add another identity"; isAllCaps = false
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(12)
            layoutParams = lp
            setOnClickListener { startNewIdentity() }
        })
        primaryCta("Finish →") { finishOnboarding() }
    }

    // ── navigation ──

    private fun onIdentityNext() {
        val field = identityField ?: return
        if (!field.hasContent()) {
            toast("Write or type an identity name first")
            return
        }
        pendingName = field.getText()
        pendingStrokes = field.getStrokes()
        // When editing an existing identity, return to the review list; otherwise go add habits.
        step = if (editIdentityId != 0L) Step.REVIEW else Step.HABITS
        render()
    }

    private fun onBack() {
        when (step) {
            Step.WELCOME -> finish()
            Step.IDENTITY -> when {
                editIdentityId != 0L -> { step = Step.REVIEW; render() }
                addMode -> finish()
                else -> { step = Step.WELCOME; render() }
            }
            Step.HABITS -> {
                step = if (pendingHabits.isNotEmpty() || editIdentityId != 0L) Step.REVIEW else Step.IDENTITY
                render()
            }
            Step.REVIEW -> if (editIdentityId != 0L) finish() else { step = Step.IDENTITY; render() }
            Step.ANOTHER -> {}
        }
    }

    /** Subtle top "Skip": leave onboarding for the dashboard (or just close in add mode). */
    private fun onSkip() {
        finishOnboarding()
    }

    private fun startNewIdentity() {
        pendingName = ""; pendingStrokes = ""; pendingIcon = com.inkhabits.ui.widget.HabitIcons.DEFAULT
        pendingHabits.clear()
        habitReminder = -1
        step = Step.IDENTITY; render()
    }

    private fun saveIdentityAndHabits() {
        if (pendingHabits.isEmpty()) {
            toast("Add at least one habit for this identity")
            return
        }
        // Snapshot values on the main thread before the coroutine.
        val name = pendingName
        val strokes = pendingStrokes
        val icon = pendingIcon
        val today = LocalDate.now().toEpochDay()
        val habitData = pendingHabits.toMutableList()

        val editingId = editIdentityId
        lifecycleScope.launch {
            // Transcribe a handwritten identity name so previews show it (ink still wins on screen).
            val idName = resolveName(name, strokes)

            if (editingId != 0L) {
                // ── Update an existing identity ──
                val existing = db.identityGoalDao().getAll().firstOrNull { it.id == editingId }
                db.identityGoalDao().update(IdentityGoal(
                    id = editingId, name = idName, nameStrokes = strokes, icon = icon,
                    goalDays = pendingIdentityGoal,
                    sortOrder = existing?.sortOrder ?: 0,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()))
                val keptIds = mutableSetOf<Long>()
                habitData.forEachIndexed { idx, h ->
                    if (h.habitId != 0L) {
                        val cur = db.habitDao().getById(h.habitId)
                        db.habitDao().update((cur ?: Habit(identityGoalId = editingId, startEpochDay = today)).copy(
                            id = h.habitId, identityGoalId = editingId, name = h.name, nameStrokes = h.strokes,
                            frequencyType = h.cfg.frequencyType, daysOfWeek = h.cfg.daysOfWeek,
                            intervalDays = h.cfg.intervalDays, weeklyTarget = h.cfg.weeklyTarget,
                            reminderMinutes = h.reminderMinutes, anchor = h.anchor, anchorStrokes = h.anchorStrokes,
                            goalDays = h.goalDays, sortOrder = idx, isActive = true))
                        keptIds.add(h.habitId)
                    } else {
                        db.habitDao().insert(Habit(
                            identityGoalId = editingId, name = h.name, nameStrokes = h.strokes,
                            frequencyType = h.cfg.frequencyType, daysOfWeek = h.cfg.daysOfWeek,
                            intervalDays = h.cfg.intervalDays, weeklyTarget = h.cfg.weeklyTarget,
                            startEpochDay = today, reminderMinutes = h.reminderMinutes,
                            anchor = h.anchor, anchorStrokes = h.anchorStrokes,
                            goalDays = h.goalDays, sortOrder = idx))
                    }
                }
                // Soft-delete removed habits (keep their completion history).
                (originalHabitIds - keptIds).forEach { id ->
                    db.habitDao().getById(id)?.let { db.habitDao().update(it.copy(isActive = false)) }
                }
                com.inkhabits.widget.WidgetCommon.updateAll(this@OnboardingActivity)
                finish()
                return@launch
            }

            // ── Create a new identity ──
            val order = db.identityGoalDao().count()
            val goalId = db.identityGoalDao().insert(
                IdentityGoal(name = idName, nameStrokes = strokes, icon = icon,
                    goalDays = pendingIdentityGoal, sortOrder = order)
            )
            habitData.forEachIndexed { idx, h ->
                db.habitDao().insert(
                    Habit(
                        identityGoalId = goalId,
                        name = h.name,
                        nameStrokes = h.strokes,
                        frequencyType = h.cfg.frequencyType,
                        daysOfWeek = h.cfg.daysOfWeek,
                        intervalDays = h.cfg.intervalDays,
                        weeklyTarget = h.cfg.weeklyTarget,
                        startEpochDay = today,
                        reminderMinutes = h.reminderMinutes,
                        anchor = h.anchor,
                        anchorStrokes = h.anchorStrokes,
                        goalDays = h.goalDays,
                        sortOrder = idx
                    )
                )
            }
            createdIdentities.add(IdentityGoal(id = goalId, name = idName, nameStrokes = strokes, icon = icon, sortOrder = order))
            pendingHabits.clear()
            step = Step.ANOTHER
            render()
        }
    }

    private fun finishOnboarding() {
        if (addMode) {
            finish()
        } else {
            startActivity(Intent(this, DashboardActivity::class.java))
            finishAffinity()
        }
    }

    companion object {
        const val EXTRA_ADD_IDENTITY = "add_identity"
        const val EXTRA_EDIT_IDENTITY = "edit_identity"  // Long identity id to edit
    }

    // ── helpers ──

    /** An InputField whose WRITE mode opens the full-screen writing pad. */
    private fun inkField(hint: String, padTitle: String): InputField = InputField(this).apply {
        setHint(hint)
        onRequestWrite = { existing, onResult -> openWritingPad(existing, padTitle) { onResult(it) } }
    }

    private fun label(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextColor(Color.parseColor("#5A5A5A"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        typeface = interSemiBold
        setLetterSpacing(0.06f)
        setPadding(0, dp(16), 0, dp(4))
    }

    private fun styleIcon(b: android.widget.ImageButton, on: Boolean) {
        b.background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(12).toFloat()
            if (on) {
                setColor(Color.parseColor("#8C1D1D"))
            } else {
                setColor(Color.WHITE)
                setStroke(dp(1), Color.parseColor("#CFCBC0"))
            }
        }
        b.setColorFilter(if (on) Color.WHITE else Color.parseColor("#1A1A1A"))
        b.elevation = 0f
        b.stateListAnimator = null
    }

    private fun stepBtn(s: String, onClick: () -> Unit): MaterialButton = MaterialButton(
        this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
    ).apply {
        text = s; isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        minWidth = 0; minimumWidth = dp(48)
        layoutParams = LinearLayout.LayoutParams(dp(54), dp(48))
        setOnClickListener { onClick() }
    }

    /** A −/+ stepper over [labels]/[values]; reports the picked value via [onPick]. */
    private fun goalChooser(
        labels: List<String>, values: List<Int>,
        currentValue: () -> Int, onPick: (Int) -> Unit,
        registerRefresh: ((() -> Unit) -> Unit)? = null
    ): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(4); layoutParams = lp
        }
        val tv = TextView(this).apply {
            gravity = android.view.Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = interSemiBold
            layoutParams = LinearLayout.LayoutParams(dp(170), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        var idx = values.indexOf(currentValue()).coerceAtLeast(0)
        fun show() { tv.text = labels[idx] }
        row.addView(stepBtn("−") { if (idx > 0) { idx--; show(); onPick(values[idx]) } })
        row.addView(tv)
        row.addView(stepBtn("+") { if (idx < values.size - 1) { idx++; show(); onPick(values[idx]) } })
        show()
        registerRefresh?.invoke { idx = values.indexOf(currentValue()).coerceAtLeast(0); show() }
        return row
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
