package com.boox.atomic.habits.ui.setup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.lifecycle.lifecycleScope
import com.boox.atomic.habits.R
import com.boox.atomic.habits.data.AppDatabase
import com.boox.atomic.habits.data.entity.Habit
import com.boox.atomic.habits.data.entity.IdentityGoal
import com.boox.atomic.habits.data.entity.ToDo
import com.boox.atomic.habits.ui.EInkActivity
import com.boox.atomic.habits.ui.dashboard.DashboardActivity
import com.boox.atomic.habits.ui.widget.HandwritingFieldView
import kotlinx.coroutines.launch

/**
 * Guided 5-step onboarding wizard using handwriting input for everything.
 *
 * Step 0: "Who do you want to become?" — Write your identity goal name
 * Step 1: "What habits would [GOAL] do?" — Handwrite habits
 * Step 2: "Another identity?" — Loop or continue
 * Step 3: "Any to-do's?" — Handwrite todos
 * Step 4: Done → Dashboard
 */
class SetupActivity : EInkActivity() {

    private lateinit var db: AppDatabase
    private lateinit var stepIndicator: TextView
    private lateinit var stepTitle: TextView
    private lateinit var stepDescription: TextView
    private lateinit var contentArea: LinearLayout
    private lateinit var nextButton: Button
    private lateinit var backButton: Button
    private lateinit var skipButton: Button

    private var currentStep = 0
    private var currentGoalId: Long? = null
    private var currentGoalName: String = ""
    private val createdGoals = mutableListOf<IdentityGoal>()

    // Wizard state
    private var goalNameField: HandwritingFieldView? = null
    private var goalStatementInput: EditText? = null
    private var selectedIcon: String = "📖"
    private val habitFieldStates = mutableListOf<HabitFieldState>()
    private val todoFieldStates = mutableListOf<TodoFieldState>()
    private val ICONS = listOf("📖", "🏃", "✍️", "🎨", "🧘", "💻", "🎵", "💪", "🌱", "🎯", "🧪", "🎭")

    data class HabitFieldState(val nameField: HandwritingFieldView, val freqGroup: RadioGroup, val root: View)
    data class TodoFieldState(val field: HandwritingFieldView, val root: View)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        db = AppDatabase.getInstance(this)
        stepIndicator = findViewById(R.id.stepIndicator)
        stepTitle = findViewById(R.id.stepTitle)
        stepDescription = findViewById(R.id.stepDescription)
        contentArea = findViewById(R.id.contentArea)
        nextButton = findViewById(R.id.nextButton)
        backButton = findViewById(R.id.backButton)
        skipButton = findViewById(R.id.skipButton)

        val isEditMode = intent.getBooleanExtra("edit_mode", false)
        if (isEditMode) { showManageView(); return }

        renderWizardStep()
        nextButton.setOnClickListener { onNext() }
        backButton.setOnClickListener { onBack() }
        skipButton.setOnClickListener { onSkip() }
    }

    private fun renderWizardStep() {
        contentArea.removeAllViews()
        backButton.visibility = if (currentStep == 0) View.GONE else View.VISIBLE
        skipButton.visibility = View.GONE

        when (currentStep) {
            0 -> renderStep0()
            1 -> renderStep1()
            2 -> renderStep2()
            3 -> renderStep3()
            4 -> renderStep4()
        }
    }

    private fun setHeader(step: String, title: String, desc: String) {
        stepIndicator.text = step; stepTitle.text = title; stepDescription.text = desc
    }

    // ─── Step 0: Identity Goal ───────────────────

    private fun renderStep0() {
        setHeader("Step 1/5", "Who do you want to become?", "Write your identity name with the stylus, then confirm.")

        val scroll = ScrollView(this)
        val ll = LinearLayout(this).apply { orientation = VERTICAL; setPadding(0, 8, 0, 8) }

        ll.addView(TextView(this).apply { text = "Write your identity name:"; textSize = 14f })
        goalNameField = HandwritingFieldView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 120)
            setOnConfirmListener { /* auto-confirm on user action */ }
        }
        ll.addView(goalNameField!!)

        val confirmBtn = Button(this).apply {
            text = "✓ Confirm handwriting"
            setOnClickListener { goalNameField?.setConfirmed(true) }
        }
        ll.addView(confirmBtn)

        ll.addView(TextView(this).apply {
            text = "Identity statement (optional, type or leave blank):"
            textSize = 12f; setPadding(0, 16, 0, 4)
        })
        goalStatementInput = EditText(this).apply {
            hint = "e.g. I am becoming a READER"
            textSize = 14f; setPadding(8, 8, 8, 8)
        }
        ll.addView(goalStatementInput!!)

        ll.addView(TextView(this).apply {
            text = "Pick an icon:"; textSize = 14f; setPadding(0, 12, 0, 4)
        })
        val iconRow = LinearLayout(this).apply { orientation = HORIZONTAL }
        for (icon in ICONS) {
            iconRow.addView(Button(this).apply {
                text = icon; textSize = 22f; setPadding(4, 2, 4, 2)
                setOnClickListener { selectedIcon = icon }
            })
        }
        ll.addView(iconRow)

        nextButton.text = "Next →"
        scroll.addView(ll)
        contentArea.addView(scroll)
    }

    // ─── Step 1: Habits ───────────────────

    private fun renderStep1() {
        setHeader("Step 2/5", "What would $currentGoalName do?", "Write each habit with the stylus, confirm, then pick frequency.")

        val scroll = ScrollView(this)
        val ll = LinearLayout(this).apply { orientation = VERTICAL; setPadding(0, 8, 0, 8) }
        ll.addView(TextView(this).apply {
            text = "Habits for $currentGoalName:"; textSize = 14f
            textStyle = android.graphics.Typeface.BOLD
        })

        val container = LinearLayout(this).apply { orientation = VERTICAL }
        ll.addView(container)

        ll.addView(Button(this).apply {
            text = "+ Add habit"; setOnClickListener { addHabitRow(container) }
        })

        if (habitFieldStates.isEmpty()) addHabitRow(container)

        scroll.addView(ll)
        nextButton.text = "Done with this goal →"
        contentArea.addView(scroll)
    }

    private fun addHabitRow(container: LinearLayout) {
        val root = LinearLayout(this).apply { orientation = VERTICAL; setPadding(0, 8, 0, 8) }

        val hw = HandwritingFieldView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 100)
            hint = "Write habit name..."
        }
        root.addView(hw)

        val btnRow = LinearLayout(this).apply { orientation = HORIZONTAL }
        btnRow.addView(Button(this).apply {
            text = "✓ Confirm"; setOnClickListener { hw.setConfirmed(true) }
        })

        val freqGroup = RadioGroup(this).apply { orientation = HORIZONTAL; setPadding(16, 0, 0, 0) }
        freqGroup.addView(RadioButton(this).apply { text = "Daily"; isChecked = true })
        freqGroup.addView(RadioButton(this).apply { text = "Weekly" })
        btnRow.addView(freqGroup)

        btnRow.addView(Button(this).apply {
            text = "✕"; setOnClickListener {
                container.removeView(root)
                habitFieldStates.removeAll { it.nameField == hw }
            }
        })
        root.addView(btnRow)

        container.addView(root)
        habitFieldStates.add(HabitFieldState(hw, freqGroup, root))
    }

    // ─── Step 2: Another Goal ───────────────────

    private fun renderStep2() {
        setHeader("Step 3/5", "Another identity goal?", "You can add more identities. Each one gets its own habits.")

        val ll = LinearLayout(this).apply { orientation = VERTICAL; setPadding(0, 16, 0, 0) }
        ll.addView(TextView(this).apply {
            text = "Your identities:"; textSize = 14f; textStyle = android.graphics.Typeface.BOLD
        })
        for (g in createdGoals) {
            ll.addView(TextView(this).apply {
                text = "${g.icon ?: "📖"} ${g.name}"; textSize = 16f; setPadding(16, 8, 0, 8)
            })
        }
        nextButton.text = "Yes, add another →"
        skipButton.text = "Skip, go to todos"; skipButton.visibility = View.VISIBLE
        contentArea.addView(ll)
    }

    // ─── Step 3: To-dos ───────────────────

    private fun renderStep3() {
        setHeader("Step 4/5", "Any to-do's?", "Write each task with the stylus.")
        val scroll = ScrollView(this)
        val ll = LinearLayout(this).apply { orientation = VERTICAL; setPadding(0, 8, 0, 8) }

        val container = LinearLayout(this).apply { orientation = VERTICAL }
        ll.addView(container)

        ll.addView(Button(this).apply {
            text = "+ Add todo"
            setOnClickListener {
                val root = LinearLayout(this@SetupActivity).apply { orientation = HORIZONTAL; setPadding(0, 8, 0, 8) }
                val hw = HandwritingFieldView(this@SetupActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 80, 1f)
                }
                root.addView(hw)
                root.addView(Button(this@SetupActivity).apply {
                    text = "✓"; setOnClickListener { hw.setConfirmed(true) }
                })
                root.addView(Button(this@SetupActivity).apply {
                    text = "✕"; setOnClickListener { container.removeView(root); todoFieldStates.removeAll { it.field == hw } }
                })
                container.addView(root)
                todoFieldStates.add(TodoFieldState(hw, root))
            }
        })

        scroll.addView(ll)
        nextButton.text = "Done →"; skipButton.text = "Skip to-dos"; skipButton.visibility = View.VISIBLE
        contentArea.addView(scroll)
    }

    // ─── Step 4: Done ───────────────────

    private fun renderStep4() {
        setHeader("Step 5/5", "You're all set!", "Your Atomic Habits are ready.")
        val ll = LinearLayout(this).apply { orientation = VERTICAL; setPadding(0, 16, 0, 0) }
        for (g in createdGoals) {
            ll.addView(TextView(this).apply {
                text = "${g.icon ?: "📖"} ${g.name}"; textSize = 18f
                textStyle = android.graphics.Typeface.BOLD; setPadding(16, 8, 0, 8)
            })
        }
        ll.addView(TextView(this).apply {
            text = "\nTap the gear icon on the dashboard to add or edit later."
            textSize = 13f; setTextColor(0xFF666666.toInt()); setPadding(16, 8, 0, 8)
        })
        nextButton.text = "Go to Dashboard 🚀"; skipButton.visibility = View.GONE
        contentArea.addView(ll)
    }

    // ─── Navigation ───────────────────

    private fun onNext() {
        when (currentStep) {
            0 -> {
                val field = goalNameField ?: return
                if (field.hasStrokes() && !field.isConfirmed()) {
                    Toast.makeText(this, "Confirm your handwriting first (tap ✓)", Toast.LENGTH_SHORT).show()
                    return
                }
                val name = if (field.hasStrokes()) "" else "IDENTITY"
                val strokeData = field.getStrokeData()
                val statement = goalStatementInput?.text.toString().trim()
                currentGoalName = "your identity"

                lifecycleScope.launch {
                    val id = db.identityGoalDao().insert(IdentityGoal(
                        name = name,
                        identityStatement = statement.ifEmpty { "I am becoming who I choose to be" },
                        icon = selectedIcon, strokeData = strokeData, sortOrder = createdGoals.size))
                    currentGoalId = id
                    createdGoals.add(IdentityGoal(id, name, statement.ifEmpty { "I am becoming who I choose to be" }, selectedIcon, strokeData, createdGoals.size))
                    val goalNameFromIcon = when (selectedIcon) {
                        "📖" -> "READER"; "🏃" -> "ATHLETE"; "✍️" -> "WRITER"; "🎨" -> "ARTIST"
                        "🧘" -> "YOGI"; "💻" -> "CODER"; "🎵" -> "MUSICIAN"; "💪" -> "FITNESS"
                        "🌱" -> "GROWER"; "🎯" -> "FOCUSED"; "🧪" -> "SCIENTIST"; "🎭" -> "CREATIVE"
                        else -> "GOAL"
                    }
                    currentGoalName = goalNameFromIcon
                    currentStep = 1; renderWizardStep()
                }
            }
            1 -> {
                lifecycleScope.launch {
                    currentGoalId?.let { gid ->
                        for ((idx, state) in habitFieldStates.withIndex()) {
                            val strokeData = state.nameField.getStrokeData()
                            if (state.nameField.hasStrokes() && !state.nameField.isConfirmed()) continue
                            if (state.nameField.hasStrokes() || state.nameField.isConfirmed()) {
                                val checkedId = state.freqGroup.checkedRadioButtonId
                                val isDaily = checkedId != -1
                                db.habitDao().insert(Habit(
                                    identityGoalId = gid, name = "",
                                    strokeData = strokeData,
                                    frequencyType = "daily", sortOrder = idx))
                            }
                        }
                    }
                    habitFieldStates.clear()
                    currentStep = 2; renderWizardStep()
                }
            }
            2 -> { currentStep = 0; renderWizardStep() }
            3 -> {
                lifecycleScope.launch {
                    for ((idx, state) in todoFieldStates.withIndex()) {
                        val strokeData = state.field.getStrokeData()
                        if (state.field.hasStrokes()) {
                            db.toDoDao().insert(ToDo(title = "", strokeData = strokeData, sortOrder = idx))
                        }
                    }
                    todoFieldStates.clear()
                    currentStep = 4; renderWizardStep()
                }
            }
            4 -> {
                getSharedPreferences("boox_habits_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("is_onboarded", true).apply()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }
        }
    }

    private fun onBack() { if (currentStep > 0) { currentStep--; renderWizardStep() } }
    private fun onSkip() { when (currentStep) { 2 -> { currentStep = 3; renderWizardStep() } 3 -> { currentStep = 4; renderWizardStep() } } }

    // ─── Manage View ───────────────────

    private fun showManageView() {
        stepIndicator.text = "Manage"; stepTitle.text = "Your Identity Goals"
        stepDescription.text = "Add, edit, or delete goals and habits."
        backButton.visibility = View.GONE; skipButton.visibility = View.GONE
        nextButton.text = "Back to Dashboard"
        nextButton.setOnClickListener { finish() }

        contentArea.removeAllViews()
        lifecycleScope.launch {
            db.identityGoalDao().getAll().collect { goals ->
                contentArea.removeAllViews()
                for (goal in goals) {
                    val card = LinearLayout(this@SetupActivity).apply {
                        orientation = VERTICAL; setPadding(12, 12, 12, 12); setBackgroundColor(0xFFF5F5F5.toInt())
                    }
                    // Goal header
                    val header = LinearLayout(this@SetupActivity).apply { orientation = HORIZONTAL }
                    header.addView(TextView(this@SetupActivity).apply {
                        text = "${goal.icon ?: "📖"} ${goal.name.ifEmpty { "Identity" }}"
                        textSize = 16f; textStyle = android.graphics.Typeface.BOLD
                        layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
                    })
                    header.addView(Button(this@SetupActivity).apply {
                        text = "✕ Delete"; setTextColor(0xFFE17055.toInt()); textSize = 12f
                        setOnClickListener { lifecycleScope.launch { db.identityGoalDao().delete(goal) } }
                    })
                    card.addView(header)

                    // Load habits for this goal
                    db.habitDao().getHabitsForGoal(goal.id).collect { habits ->
                        // Remove old habit items (keep header)
                        while (card.childCount > 1) card.removeViewAt(1)
                        for (habit in habits) {
                            val hr = LinearLayout(this@SetupActivity).apply { orientation = HORIZONTAL; setPadding(24, 6, 0, 6) }
                            hr.addView(TextView(this@SetupActivity).apply {
                                text = if (habit.strokeData.isNotEmpty()) "✎ Handwriting" else habit.name.ifEmpty { "Habit" }
                                textSize = 14f; layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
                            })
                            hr.addView(Button(this@SetupActivity).apply {
                                text = "✕"; textSize = 10f
                                setOnClickListener { lifecycleScope.launch { db.habitDao().delete(habit) } }
                            })
                            card.addView(hr)
                        }
                    }

                    card.addView(Button(this@SetupActivity).apply {
                        text = "+ Add habit to ${goal.name.ifEmpty { "goal" }}"; textSize = 12f
                        setOnClickListener {
                            val input = EditText(this@SetupActivity).apply { hint = "Habit name" }
                            AlertDialog.Builder(this@SetupActivity).setTitle("Add Habit").setView(input)
                                .setPositiveButton("Add") { _, _ ->
                                    val n = input.text.toString().trim()
                                    if (n.isNotEmpty()) lifecycleScope.launch { db.habitDao().insert(Habit(identityGoalId = goal.id, name = n)) }
                                }.setNegativeButton("Cancel", null).show()
                        }
                    })
                    contentArea.addView(card)
                    contentArea.addView(View(this@SetupActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 2)
                        setBackgroundColor(0xFFEEEEEE.toInt()); minimumHeight = 2
                    })
                }
                contentArea.addView(Button(this@SetupActivity).apply {
                    text = "+ Add New Identity Goal"
                    setOnClickListener {
                        val input = EditText(this@SetupActivity).apply { hint = "Goal name" }
                        AlertDialog.Builder(this@SetupActivity).setTitle("New Identity Goal").setView(input)
                            .setPositiveButton("Add") { _, _ ->
                                val n = input.text.toString().trim()
                                if (n.isNotEmpty()) lifecycleScope.launch {
                                    db.identityGoalDao().insert(IdentityGoal(name = n.uppercase()))
                                }
                            }.setNegativeButton("Cancel", null).show()
                    }
                })
            }
        }
    }
}