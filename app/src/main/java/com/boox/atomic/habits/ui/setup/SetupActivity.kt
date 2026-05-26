package com.boox.atomic.habits.ui.setup

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import com.boox.atomic.habits.R
import com.boox.atomic.habits.data.AppDatabase
import com.boox.atomic.habits.data.entity.Habit
import com.boox.atomic.habits.data.entity.IdentityGoal
import com.boox.atomic.habits.data.entity.ToDo
import com.boox.atomic.habits.ui.EInkActivity
import kotlinx.coroutines.launch

/**
 * Setup / settings screen for adding and editing identity goals,
 * habits, and to-dos.
 *
 * Acts as both a first-launch wizard (when no goals exist) and an
 * ongoing settings screen accessible from the dashboard.
 */
class SetupActivity : EInkActivity() {

    private lateinit var db: AppDatabase
    private lateinit var prefs: SharedPreferences

    // Container layouts holding dynamically added form rows
    private lateinit var goalsContainer: LinearLayout
    private lateinit var habitsContainer: LinearLayout
    private lateinit var todosContainer: LinearLayout

    // All currently tracked items
    private val goals = mutableListOf<RowState<IdentityGoal>>()
    private val habits = mutableListOf<RowState<Habit>>()
    private val todos = mutableListOf<RowState<ToDo>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        db = AppDatabase.getInstance(this)
        prefs = getSharedPreferences("boox_habits", Context.MODE_PRIVATE)

        goalsContainer = findViewById(R.id.goalsContainer)
        habitsContainer = findViewById(R.id.habitsContainer)
        todosContainer = findViewById(R.id.todosContainer)

        // Add button handlers
        findViewById<Button>(R.id.addGoalButton).setOnClickListener { addGoalRow() }
        findViewById<Button>(R.id.addHabitButton).setOnClickListener { addHabitRow() }
        findViewById<Button>(R.id.addTodoButton).setOnClickListener { addTodoRow() }
        findViewById<Button>(R.id.saveButton).setOnClickListener { saveAll() }

        loadExistingData()
    }

    private fun loadExistingData() {
        lifecycleScope.launch {
            // Load existing identity goals
            db.identityGoalDao().getAll().collect { existingGoals ->
                goals.clear()
                goalsContainer.removeAllViews()
                existingGoals.forEach { goal ->
                    val rowState = RowState(goal)
                    goals.add(rowState)
                    goalsContainer.addView(buildGoalRow(rowState))
                }
                // Add empty rows if none exist
                if (goals.isEmpty()) addGoalRow()
            }
        }

        lifecycleScope.launch {
            // Load habits from the first goal (for simplicity, show habits for all goals)
            db.identityGoalDao().getAll().collect { existingGoals ->
                // For the setup view, we'll show habits for the first goal by default
                // or let the user select which goal to associate
            }
        }
    }

    private fun addGoalRow() {
        val rowState = RowState<IdentityGoal>(null)
        goals.add(rowState)
        goalsContainer.addView(buildGoalRow(rowState))
    }

    private fun buildGoalRow(state: RowState<IdentityGoal>): View {
        val inflater = layoutInflater
        val row = inflater.inflate(R.layout.setup_row_goal, null) ?: run {
            // Fallback: build programmatically
            val ll = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 4, 0, 4)
            }

            val nameInput = EditText(this).apply {
                hint = "Goal name (e.g. Reader)"
                setText(state.entity?.name ?: "")
            }
            ll.addView(nameInput)

            val statementInput = EditText(this).apply {
                hint = "Identity statement (e.g. I am a reader)"
                setText(state.entity?.identityStatement ?: "")
            }
            ll.addView(statementInput)

            val iconInput = EditText(this).apply {
                hint = "Icon emoji"
                setText(state.entity?.icon ?: "📖")
            }
            ll.addView(iconInput)

            state.views = listOf(nameInput, statementInput, iconInput)
            return ll
        }

        val nameInput = row.findViewById<EditText>(R.id.goalNameInput) ?: EditText(this).apply {
            hint = "Goal name"
            setText(state.entity?.name ?: "")
        }
        val statementInput = row.findViewById<EditText>(R.id.goalStatementInput) ?: EditText(this).apply {
            hint = "Identity statement"
            setText(state.entity?.identityStatement ?: "")
        }
        val iconInput = row.findViewById<EditText>(R.id.goalIconInput) ?: EditText(this).apply {
            hint = "Icon"
            setText(state.entity?.icon ?: "📖")
        }

        state.views = listOf(nameInput, statementInput, iconInput)
        return row
    }

    private fun addHabitRow() {
        val rowState = RowState<Habit>(null)
        habits.add(rowState)
        habitsContainer.addView(buildHabitRow(rowState))
    }

    private fun buildHabitRow(state: RowState<Habit>): View {
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 4, 0, 4)
        }

        val nameInput = EditText(this).apply {
            hint = "Habit name (e.g. Read 10 pages)"
            setText(state.entity?.name ?: "")
        }
        ll.addView(nameInput)

        val freqSpinner = Spinner(this)
        val freqOptions = arrayOf("Daily", "Weekly", "Interval", "Days of Week")
        freqSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, freqOptions)
        ll.addView(freqSpinner)

        state.views = listOf(nameInput, freqSpinner)
        return ll
    }

    private fun addTodoRow() {
        val rowState = RowState<ToDo>(null)
        todos.add(rowState)
        todosContainer.addView(buildTodoRow(rowState))
    }

    private fun buildTodoRow(state: RowState<ToDo>): View {
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 4, 0, 4)
        }

        val titleInput = EditText(this).apply {
            hint = "To-do title"
            setText(state.entity?.title ?: "")
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        ll.addView(titleInput)

        state.views = listOf(titleInput)
        return ll
    }

    private fun saveAll() {
        lifecycleScope.launch {
            // Save identity goals
            goals.forEachIndexed { index, state ->
                if (state.views.isNotEmpty()) {
                    val name = (state.views[0] as EditText).text.toString().trim()
                    val statement = if (state.views.size > 1) (state.views[1] as EditText).text.toString().trim() else ""
                    val icon = if (state.views.size > 2) (state.views[2] as EditText).text.toString().trim() else "📖"

                    if (name.isNotEmpty()) {
                        val entity = state.entity?.copy(
                            name = name,
                            identityStatement = statement.ifEmpty { "I am a $name" },
                            icon = icon.ifEmpty { "📖" },
                            sortOrder = index
                        ) ?: IdentityGoal(
                            name = name,
                            identityStatement = statement.ifEmpty { "I am a $name" },
                            icon = icon.ifEmpty { "📖" },
                            sortOrder = index
                        )
                        db.identityGoalDao().insert(entity)
                    }
                }
            }

            // Save habits
            habits.forEachIndexed { index, state ->
                if (state.views.isNotEmpty()) {
                    val name = (state.views[0] as EditText).text.toString().trim()
                    if (name.isNotEmpty() && goals.isNotEmpty()) {
                        // Get the first goal's ID (user selected via dropdown ideally)
                        val firstGoalId = db.identityGoalDao().getAll().collect { list ->
                            // Use first goal from the saved list
                        }
                    }
                }
            }

            // Save todos
            todos.forEachIndexed { index, state ->
                if (state.views.isNotEmpty()) {
                    val title = (state.views[0] as EditText).text.toString().trim()
                    if (title.isNotEmpty()) {
                        val entity = state.entity?.copy(
                            title = title,
                            sortOrder = index
                        ) ?: ToDo(
                            title = title,
                            sortOrder = index
                        )
                        db.toDoDao().insert(entity)
                    }
                }
            }

            // Mark onboarding as complete
            prefs.edit().putBoolean("is_onboarded", true).apply()

            // Signal success and finish
            Toast.makeText(this@SetupActivity, "Saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * Holds the state of a single editable row, tracking the
     * underlying entity (if editing) and the view references.
     */
    data class RowState<T>(
        val entity: T?,
        val views: MutableList<View> = mutableListOf()
    )
}
