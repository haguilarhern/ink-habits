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
import kotlinx.coroutines.launch

/**
 * Step-by-step Atomic Habits onboarding wizard + management view.
 *
 * Onboarding flow:
 *   Step 0: "Who do you want to become?" → Identity goal
 *   Step 1: "What habits would [GOAL] do?" → Add habits
 *   Step 2: "Another identity?" → Add another or continue
 *   Step 3: "Any to-dos?" → Optional
 *   Step 4: "Done!" → Dashboard
 *
 * Management view: When opened from dashboard gear, shows all goals with delete.
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

    // Wizard step state
    private var goalNameInput: EditText? = null
    private var goalStatementInput: EditText? = null
    private var goalIconTv: TextView? = null
    private var selectedIcon: String = "📖"
    private val habitRows = mutableListOf<HabitRowState>()
    private val todoRows = mutableListOf<TodoRowState>()
    private val ICONS = listOf("📖", "🏃", "✍️", "🎨", "🧘", "💻", "🎵", "💪", "🌱", "🎯", "🧪", "🎭")

    data class HabitRowState(val nameInput: EditText, val isDaily: Boolean)
    data class TodoRowState(val titleInput: EditText)

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
        val prefs = getSharedPreferences("boox_habits_prefs", Context.MODE_PRIVATE)

        if (isEditMode) {
            showManageView()
        } else {
            renderWizardStep()
        }

        nextButton.setOnClickListener { onNext() }
        backButton.setOnClickListener { onBack() }
        skipButton.setOnClickListener { onSkip() }
    }

    // ==================== WIZARD ====================

    private fun renderWizardStep() {
        contentArea.removeAllViews()
        backButton.visibility = if (currentStep == 0) View.GONE else View.VISIBLE

        when (currentStep) {
            0 -> renderStep0_identity()
            1 -> renderStep1_habits()
            2 -> renderStep2_anotherGoal()
            3 -> renderStep3_todos()
            4 -> renderStep4_done()
        }
    }

    private fun updateHeader(step: String, title: String, desc: String) {
        stepIndicator.text = step
        stepTitle.text = title
        stepDescription.text = desc
    }

    // --- Step 0: Identity Goal ---

    private fun renderStep0_identity() {
        updateHeader("Step 1/5", "Who do you want to become?", "Atomic Habits starts with identity. What kind of person are you becoming?")

        val ll = LinearLayout(this).apply { orientation = VERTICAL; setPadding(0, 8, 0, 8) }

        // Name
        ll.addView(TextView(this).apply { text = "Identity name"; textSize = 14f })
        goalNameInput = EditText(this).apply {
            hint = "e.g. READER, ATHLETE, WRITER"; textSize = 16f; setPadding(8, 8, 8, 8)
        }
        ll.addView(goalNameInput!!)

        // Statement
        ll.addView(TextView(this).apply { text = "Identity statement (optional)"; textSize = 14f; setPadding(0, 12, 0, 0) })
        goalStatementInput = EditText(this).apply {
            hint = "e.g. I am becoming a READER"; textSize = 14f; setPadding(8, 8, 8, 8)
        }
        ll.addView(goalStatementInput!!)

        // Icon picker
        ll.addView(TextView(this).apply { text = "Pick an icon"; textSize = 14f; setPadding(0, 12, 0, 4) })
        goalIconTv = TextView(this).apply { textSize = 36f; text = "📖"; setPadding(0, 4, 0, 8) }
        ll.addView(goalIconTv!!)

        val iconRow = LinearLayout(this).apply { orientation = HORIZONTAL }
        for (icon in ICONS) {
            val btn = Button(this).apply {
                text = icon; textSize = 22f; setPadding(6, 4, 6, 4)
                setOnClickListener {
                    selectedIcon = icon; goalIconTv?.text = icon
                }
            }
            iconRow.addView(btn)
        }
        ll.addView(iconRow)

        nextButton.text = "Next →"; skipButton.visibility = View.GONE
        contentArea.addView(ll)
    }

    // --- Step 1: Habits ---

    private fun renderStep1_habits() {
        updateHeader("Step 2/5", "What would $currentGoalName do?", "Add habits that support this identity. Start small — 2-5 minute habits build streaks fastest.")

        val scroll = ScrollView(this)
        val ll = LinearLayout(this).apply { orientation = VERTICAL; setPadding(0, 8, 0, 8) }

        val container = LinearLayout(this).apply { orientation = VERTICAL }
        ll.addView(container)

        ll.addView(Button(this).apply {
            text = "+ Add habit"; setOnClickListener { addHabitRow(container) }
        })

        if (habitRows.isEmpty()) addHabitRow(container)

        scroll.addView(ll)
        nextButton.text = "Done with this goal →"; skipButton.visibility = View.GONE
        contentArea.addView(scroll)
    }

    private fun addHabitRow(container: LinearLayout) {
        val row = LinearLayout(this).apply { orientation = VERTICAL; setPadding(0, 8, 0, 8) }
        val input = EditText(this).apply { hint = "Habit name (e.g. Read 10 pages)"; textSize = 14f }
        row.addView(input)

        val freqRow = LinearLayout(this).apply { orientation = HORIZONTAL }
        freqRow.addView(RadioButton(this).apply { text = "Daily"; isChecked = true })
        freqRow.addView(RadioButton(this).apply { text = "Weekly" })
        row.addView(freqRow)

        row.addView(Button(this).apply {
            text = "✕ Remove"; textSize = 12f
            setOnClickListener { container.removeView(row); habitRows.removeAll { it.nameInput === input } }
        })

        container.addView(row)
        habitRows.add(HabitRowState(input, true))
    }

    // --- Step 2: Another Goal ---

    private fun renderStep2_anotherGoal() {
        updateHeader("Step 3/5", "Another identity goal?", "You can add more identities. Each one gets its own set of habits.")

        val ll = LinearLayout(this).apply { orientation = VERTICAL; setPadding(0, 16, 0, 0) }
        ll.addView(TextView(this).apply {
            text = "Your identities so far:"; textSize = 14f; textStyle = android.graphics.Typeface.BOLD
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

    // --- Step 3: To-dos ---

    private fun renderStep3_todos() {
        updateHeader("Step 4/5", "Any to-do's?", "Add one-time tasks. You can always add more later from the dashboard.")
        val scroll = ScrollView(this)
        val ll = LinearLayout(this).apply { orientation = VERTICAL; setPadding(0, 8, 0, 8) }

        val container = LinearLayout(this).apply { orientation = VERTICAL }
        ll.addView(container)

        ll.addView(Button(this).apply {
            text = "+ Add todo"
            setOnClickListener {
                val inner = LinearLayout(this@SetupActivity).apply { orientation = HORIZONTAL; setPadding(0, 8, 0, 8) }
                val ti = EditText(this@SetupActivity).apply { hint = "Todo"; layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f) }
                inner.addView(ti)
                inner.addView(Button(this@SetupActivity).apply {
                    text = "✕"; setOnClickListener { container.removeView(inner); todoRows.removeAll { it.titleInput === ti } }
                })
                container.addView(inner)
                todoRows.add(TodoRowState(ti))
            }
        })

        scroll.addView(ll)
        nextButton.text = "Done →"; skipButton.text = "Skip to-dos"; skipButton.visibility = View.VISIBLE
        contentArea.addView(scroll)
    }

    // --- Step 4: Done ---

    private fun renderStep4_done() {
        updateHeader("Step 5/5", "You're all set!", "Your Atomic Habits foundation is ready.")
        val ll = LinearLayout(this).apply { orientation = VERTICAL; setPadding(0, 16, 0, 0) }
        for (g in createdGoals) {
            ll.addView(TextView(this).apply {
                text = "${g.icon ?: "📖"} ${g.name}"; textSize = 18f; textStyle = android.graphics.Typeface.BOLD; setPadding(16, 8, 0, 8)
            })
        }
        ll.addView(TextView(this).apply {
            text = "\nYou can always edit from the dashboard (gear icon)."
            textSize = 13f; setTextColor(0xFF666666.toInt()); setPadding(16, 8, 0, 8)
        })
        nextButton.text = "Go to Dashboard 🚀"; skipButton.visibility = View.GONE
        contentArea.addView(ll)
    }

    // ==================== NAVIGATION ====================

    private fun onNext() {
        when (currentStep) {
            0 -> {
                val name = goalNameInput?.text.toString().trim()
                if (name.isEmpty()) { Toast.makeText(this, "Enter an identity name", Toast.LENGTH_SHORT).show(); return }
                val statement = goalStatementInput?.text.toString().trim()
                currentGoalName = name.uppercase()
                lifecycleScope.launch {
                    val id = db.identityGoalDao().insert(IdentityGoal(
                        name = currentGoalName, identityStatement = statement.ifEmpty { "I am a $name" },
                        icon = selectedIcon, sortOrder = createdGoals.size))
                    currentGoalId = id
                    createdGoals.add(IdentityGoal(id, currentGoalName, statement.ifEmpty { "I am a $name" }, selectedIcon, createdGoals.size))
                    currentStep = 1; renderWizardStep()
                }
            }
            1 -> {
                lifecycleScope.launch {
                    currentGoalId?.let { gid ->
                        for ((idx, hr) in habitRows.withIndex()) {
                            val hname = hr.nameInput.text.toString().trim()
                            if (hname.isNotEmpty()) db.habitDao().insert(Habit(identityGoalId = gid, name = hname, frequencyType = "daily", sortOrder = idx))
                        }
                    }
                    habitRows.clear()
                    currentStep = 2; renderWizardStep()
                }
            }
            2 -> { currentStep = 0; renderWizardStep() } // Add another goal
            3 -> {
                lifecycleScope.launch {
                    for ((idx, tr) in todoRows.withIndex()) {
                        val t = tr.titleInput.text.toString().trim()
                        if (t.isNotEmpty()) db.toDoDao().insert(ToDo(title = t, sortOrder = idx))
                    }
                    todoRows.clear()
                    currentStep = 4; renderWizardStep()
                }
            }
            4 -> {
                getSharedPreferences("boox_habits_prefs", Context.MODE_PRIVATE).edit().putBoolean("is_onboarded", true).apply()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }
        }
    }

    private fun onBack() {
        if (currentStep > 0) { currentStep--; renderWizardStep() }
    }

    private fun onSkip() {
        when (currentStep) { 2 -> { currentStep = 3; renderWizardStep() } 3 -> { currentStep = 4; renderWizardStep() } }
    }

    // ==================== MANAGE VIEW ====================

    private fun showManageView() {
        stepIndicator.text = "Manage"
        stepTitle.text = "Your Identity Goals"
        stepDescription.text = "Add, edit, or delete goals and habits."
        backButton.visibility = View.GONE; skipButton.visibility = View.GONE
        nextButton.text = "Back to Dashboard"
        nextButton.setOnClickListener { finish() }

        // Content area is already inside a ScrollView from XML — just add to it directly
        refreshManageView()
    }

    private fun refreshManageView() {
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
                        text = "${goal.icon ?: "📖"} ${goal.name}"; textSize = 16f; textStyle = android.graphics.Typeface.BOLD
                        layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
                    })
                    header.addView(Button(this@SetupActivity).apply {
                        text = "✕ Delete"; setTextColor(0xFFE17055.toInt()); textSize = 12f
                        setOnClickListener { lifecycleScope.launch { db.identityGoalDao().delete(goal); refreshManageView() } }
                    })
                    card.addView(header)

                    // Add habit button
                    val addBtn = Button(this@SetupActivity).apply {
                        text = "+ Add habit to ${goal.name}"; textSize = 12f
                        setOnClickListener { showAddHabitDialog(goal.id) }
                    }

                    // Load habits
                    val habits = try {
                        var result = listOf<Habit>()
                        db.habitDao().getHabitsForGoal(goal.id).collect { h -> result = h; if (h.isNotEmpty()) return@collect }
                        result
                    } catch (e: Exception) { emptyList() }

                    for (habit in habits) {
                        val hr = LinearLayout(this@SetupActivity).apply { orientation = HORIZONTAL; setPadding(24, 6, 0, 6) }
                        hr.addView(TextView(this@SetupActivity).apply {
                            text = "□ ${habit.name}"; textSize = 14f
                            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
                        })
                        hr.addView(Button(this@SetupActivity).apply {
                            text = "✕"; textSize = 10f
                            setOnClickListener { lifecycleScope.launch { db.habitDao().delete(habit); refreshManageView() } }
                        })
                        card.addView(hr)
                    }

                    card.addView(addBtn)
                    contentArea.addView(card)
                    contentArea.addView(View(this@SetupActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 2)
                        setBackgroundColor(0xFFEEEEEE.toInt()); minimumHeight = 2
                    })
                }

                contentArea.addView(Button(this@SetupActivity).apply {
                    text = "+ Add New Identity Goal"
                    setOnClickListener { showAddGoalDialog() }
                })
            }
        }
    }

    private fun showAddHabitDialog(goalId: Long) {
        val input = EditText(this).apply { hint = "Habit name" }
        AlertDialog.Builder(this).setTitle("Add Habit").setView(input)
            .setPositiveButton("Add") { _, _ ->
                val n = input.text.toString().trim()
                if (n.isNotEmpty()) lifecycleScope.launch { db.habitDao().insert(Habit(identityGoalId = goalId, name = n)); refreshManageView() }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun showAddGoalDialog() {
        val input = EditText(this).apply { hint = "Identity name (e.g. ARTIST)" }
        AlertDialog.Builder(this).setTitle("New Identity Goal").setView(input)
            .setPositiveButton("Add") { _, _ ->
                val n = input.text.toString().trim()
                if (n.isNotEmpty()) lifecycleScope.launch {
                    db.identityGoalDao().insert(IdentityGoal(name = n.uppercase(), identityStatement = "I am a $n"))
                    refreshManageView()
                }
            }.setNegativeButton("Cancel", null).show()
    }
}