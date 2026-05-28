package com.inkhabits.ui.onboarding

import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
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

    private enum class Step { WELCOME, IDENTITY, HABITS, ANOTHER }
    private var step = Step.WELCOME
    private var addMode = false

    private val icons = listOf("★", "📖", "🏃", "✍️", "🎨", "🧘", "💻", "🎵", "💪", "🌱", "🎯", "🍳")

    // Pending identity being built (saved only when leaving the HABITS step).
    private var pendingName = ""
    private var pendingStrokes = ""
    private var pendingIcon = "★"

    private var identityField: InputField? = null
    private val createdIdentities = mutableListOf<IdentityGoal>()

    // Habits added for the current identity (one at a time via "Add this habit").
    private val pendingHabits = mutableListOf<PendingHabit>()
    private var habitInput: InputField? = null
    private var habitSchedule: SchedulePicker? = null
    private var habitAnchor: InputField? = null
    private var habitReminder: Int = -1

    private data class PendingHabit(
        val name: String,
        val strokes: String,
        val cfg: ScheduleConfig,
        val reminderMinutes: Int,
        val anchor: String,
        val anchorStrokes: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.get(this)

        addMode = intent.getBooleanExtra(EXTRA_ADD_IDENTITY, false)
        if (addMode) step = Step.IDENTITY

        binding.skipButton.setOnClickListener { onSkip() }
        binding.backButton.setOnClickListener { onBack() }
        // Keep the active inline pen region aligned while scrolling.
        binding.scroll.setOnScrollChangeListener { _, _, _, _, _ ->
            com.inkhabits.ui.widget.InlineInkView.current?.refreshLimit()
        }
        render()
    }

    private fun render() {
        binding.contentArea.removeAllViews()
        // Top bar: back hidden on first screen; skip hidden on the final loop screen.
        val onFirst = step == Step.WELCOME || (step == Step.IDENTITY && addMode)
        binding.backButton.visibility = if (onFirst) View.INVISIBLE else View.VISIBLE
        binding.skipButton.visibility = if (step == Step.ANOTHER) View.GONE else View.VISIBLE
        when (step) {
            Step.WELCOME -> renderWelcome()
            Step.IDENTITY -> renderIdentity()
            Step.HABITS -> renderHabits()
            Step.ANOTHER -> renderAnother()
        }
    }

    /** A full-width primary action button placed in the content flow (below the step's content). */
    private fun primaryCta(text: String, onClick: () -> Unit) {
        binding.contentArea.addView(MaterialButton(this).apply {
            this.text = text
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56))
            lp.topMargin = dp(20)
            layoutParams = lp
            setOnClickListener { onClick() }
        })
    }

    private fun header(indicator: String, title: String, subtitle: String) {
        binding.stepIndicator.text = indicator
        binding.stepTitle.text = title
        binding.stepSubtitle.text = subtitle
    }

    private fun renderWelcome() {
        header("WELCOME", "Ink Habits", "")
        binding.contentArea.addView(TextView(this).apply {
            text = "Build habits by becoming the person who already has them.\n\n" +
                "First you'll choose an identity — who you want to become — then add the " +
                "habits that person does. You can write with your pen or type."
            setTextColor(Color.parseColor("#5A5A5A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setLineSpacing(0f, 1.2f)
        })
        primaryCta("Get started →") { step = Step.IDENTITY; render() }
    }

    private fun renderIdentity() {
        header("IDENTITY", "Who do you want to become?", "e.g. a Reader, an Athlete, a Writer")

        binding.contentArea.addView(label("Name this identity"))
        val field = InputField(this).apply { setHint("I am a…") }
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

        val iconButtons = mutableListOf<Button>()
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (icon in icons) {
            val b = Button(this).apply {
                text = icon; isAllCaps = false
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                minWidth = 0; minimumWidth = dp(52)
                val lp = LinearLayout.LayoutParams(dp(52), dp(52))
                lp.marginEnd = dp(6)
                layoutParams = lp
            }
            b.setOnClickListener {
                pendingIcon = icon
                iconButtons.forEach { styleIcon(it, it.text == pendingIcon) }
            }
            styleIcon(b, icon == pendingIcon)
            iconButtons.add(b)
            row.addView(b)
        }
        binding.contentArea.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        })

        primaryCta("Next →") { onIdentityNext() }
    }

    private fun renderHabits() {
        val who = pendingName.ifBlank { "this person" }
        header("HABITS", "What would $who do?", "Write a habit, set how often, then tap Add. Repeat for more.")

        // Already-added habits
        if (pendingHabits.isNotEmpty()) {
            binding.contentArea.addView(label("Habits added (${pendingHabits.size})"))
            pendingHabits.forEachIndexed { idx, h ->
                binding.contentArea.addView(pendingHabitRow(idx, h))
            }
            binding.contentArea.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                setBackgroundColor(Color.parseColor("#E0E0E0"))
                (layoutParams as LinearLayout.LayoutParams).topMargin = dp(8)
            })
        }

        // New habit entry
        binding.contentArea.addView(label("Add a habit").apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(12)
            layoutParams = lp
        })
        val input = InputField(this).apply { setHint("Habit name…") }
        habitInput = input
        binding.contentArea.addView(input)

        binding.contentArea.addView(label("How often?").apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(6)
            layoutParams = lp
        })
        val picker = SchedulePicker(this)
        habitSchedule = picker
        binding.contentArea.addView(picker)

        // Time of day (optional)
        binding.contentArea.addView(label("Time of day (optional)").apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(10); layoutParams = lp
        })
        val timeBtn = MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            text = timeButtonLabel()
        }
        timeBtn.setOnClickListener { pickTime(timeBtn) }
        binding.contentArea.addView(timeBtn)

        // Anchor cue (optional, habit-stacking) — type or write, like the habit.
        binding.contentArea.addView(label("Anchor — after what? (optional)").apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(10); layoutParams = lp
        })
        val anchor = InputField(this).apply { setHint("e.g. after my morning coffee") }
        habitAnchor = anchor
        binding.contentArea.addView(anchor)

        // Secondary action: add the current habit to the list.
        binding.contentArea.addView(MaterialButton(
            this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "✓ Add this habit"; isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52))
            lp.topMargin = dp(10)
            layoutParams = lp
            setOnClickListener { addPendingHabit() }
        })

        // Primary action: finish this identity.
        primaryCta(if (pendingHabits.isEmpty()) "Done →" else "Done (${pendingHabits.size}) →") {
            saveIdentityAndHabits()
        }
    }

    private fun pendingHabitRow(index: Int, h: PendingHabit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
        }
        row.addView(TextView(this).apply {
            text = if (h.strokes.isNotEmpty()) "✎ ${if (h.name.isNotBlank()) h.name else "Handwritten"}"
            else h.name.ifBlank { "Habit" }
            setTextColor(Color.BLACK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(TextView(this).apply {
            text = com.inkhabits.util.Schedule.label(
                com.inkhabits.data.entity.Habit(
                    identityGoalId = 0, frequencyType = h.cfg.frequencyType,
                    daysOfWeek = h.cfg.daysOfWeek, intervalDays = h.cfg.intervalDays,
                    weeklyTarget = h.cfg.weeklyTarget
                )
            )
            setTextColor(Color.parseColor("#6B6B6B"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.marginStart = dp(8)
            layoutParams = lp
        })
        row.addView(Button(this).apply {
            text = "✕"; isAllCaps = false
            setTextColor(Color.parseColor("#8C1D1D"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { pendingHabits.removeAt(index); rerenderHabits() }
        })
        return row
    }

    private fun timeButtonLabel(): String =
        if (habitReminder < 0) "Any time" else com.inkhabits.util.Schedule.formatTime(habitReminder)

    private fun pickTime(btn: MaterialButton) {
        val start = if (habitReminder >= 0) habitReminder else 8 * 60
        TimePickerDialog(this, { _, hour, minute ->
            habitReminder = hour * 60 + minute
            btn.text = timeButtonLabel()
        }, start / 60, start % 60, false).show()
    }

    private fun addPendingHabit() {
        val input = habitInput ?: return
        if (!input.hasContent()) {
            toast("Write or type the habit first")
            return
        }
        pendingHabits.add(
            PendingHabit(
                input.getText(), input.getStrokes(), habitSchedule!!.getConfig(),
                habitReminder,
                habitAnchor?.getText().orEmpty(), habitAnchor?.getStrokes().orEmpty()
            )
        )
        habitReminder = -1
        rerenderHabits()
    }

    private fun rerenderHabits() {
        binding.contentArea.removeAllViews()
        renderHabits()
    }

    private fun renderAnother() {
        header("IDENTITIES", "Add another identity?", "Each identity has its own set of habits.")
        binding.contentArea.addView(label("Created so far"))
        for (g in createdIdentities) {
            binding.contentArea.addView(TextView(this).apply {
                text = "${g.icon}  ${g.name.ifBlank { "Handwritten identity" }}"
                setTextColor(Color.BLACK)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                setPadding(0, dp(8), 0, dp(8))
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
        step = Step.HABITS; render()
    }

    private fun onBack() {
        when (step) {
            Step.WELCOME -> finish()
            Step.IDENTITY -> if (addMode) finish() else { step = Step.WELCOME; render() }
            Step.HABITS -> { step = Step.IDENTITY; render() }
            Step.ANOTHER -> {}
        }
    }

    /** Subtle top "Skip": leave onboarding for the dashboard (or just close in add mode). */
    private fun onSkip() {
        finishOnboarding()
    }

    private fun startNewIdentity() {
        pendingName = ""; pendingStrokes = ""; pendingIcon = "★"
        pendingHabits.clear()
        habitReminder = -1
        step = Step.IDENTITY; render()
    }

    private fun saveIdentityAndHabits() {
        // If the user wrote a habit but didn't tap "Add", fold it in automatically.
        habitInput?.let { if (it.hasContent()) {
            pendingHabits.add(PendingHabit(
                it.getText(), it.getStrokes(), habitSchedule!!.getConfig(),
                habitReminder,
                habitAnchor?.getText().orEmpty(), habitAnchor?.getStrokes().orEmpty()
            ))
        } }
        if (pendingHabits.isEmpty()) {
            toast("Add at least one habit for this identity")
            return
        }
        // Snapshot values on the main thread before the coroutine.
        val name = pendingName
        val strokes = pendingStrokes
        val icon = pendingIcon
        val today = LocalDate.now().toEpochDay()
        val habitData = pendingHabits.toList()

        lifecycleScope.launch {
            val order = db.identityGoalDao().count()
            val goalId = db.identityGoalDao().insert(
                IdentityGoal(name = name, nameStrokes = strokes, icon = icon, sortOrder = order)
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
                        sortOrder = idx
                    )
                )
            }
            createdIdentities.add(IdentityGoal(id = goalId, name = name, nameStrokes = strokes, icon = icon, sortOrder = order))
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
    }

    // ── helpers ──

    private fun label(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextColor(Color.parseColor("#5A5A5A"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
    }

    private fun styleIcon(b: Button, on: Boolean) {
        b.setBackgroundColor(if (on) Color.parseColor("#8C1D1D") else Color.parseColor("#E8E8E8"))
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
