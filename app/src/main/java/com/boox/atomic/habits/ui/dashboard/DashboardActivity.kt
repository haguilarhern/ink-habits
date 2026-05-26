package com.boox.atomic.habits.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boox.atomic.habits.R
import com.boox.atomic.habits.data.AppDatabase
import com.boox.atomic.habits.data.dao.DashboardItem
import com.boox.atomic.habits.data.dao.ToDoDao
import com.boox.atomic.habits.data.entity.HabitCompletion
import com.boox.atomic.habits.data.entity.ToDo
import com.boox.atomic.habits.ui.EInkActivity
import com.boox.atomic.habits.ui.setup.SetupActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main dashboard screen showing identity goals with expandable habits
 * and a separate to-do list.
 */
class DashboardActivity : EInkActivity() {

    private lateinit var db: AppDatabase
    private lateinit var dashboardAdapter: DashboardAdapter
    private lateinit var todoAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
    private lateinit var identityGoalsRecyclerView: RecyclerView
    private lateinit var todosRecyclerView: RecyclerView

    private var todoList: List<ToDo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        db = AppDatabase.getInstance(this)

        // Gear icon — top right header
        val headerRow = findViewById<LinearLayout?>(R.id.headerRow)
        if (headerRow != null) {
            val gearBtn = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_menu_edit)
                setPadding(12, 12, 12, 12)
                layoutParams = LinearLayout.LayoutParams(48, 48)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setOnClickListener {
                    val intent = Intent(this@DashboardActivity, SetupActivity::class.java)
                    intent.putExtra("edit_mode", true)
                    startActivity(intent)
                }
            }
            headerRow.addView(gearBtn)
        }

        setupRecyclerViews()
        observeData()

        // Navigate to setup
        val gearIcon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_manage)
            setPadding(8, 8, 8, 8)
            layoutParams = android.widget.Toolbar.LayoutParams(48, 48)
            setOnClickListener {
                startActivity(Intent(this@DashboardActivity, SetupActivity::class.java))
            }
        }
        // Add gear to the top-right of the activity
        // The toolbar/actionbar approach: let's set it up on the decor
    }

    override fun onResume() {
        super.onResume()
        // Refresh data in case it was modified in SetupActivity
        observeData()
    }

    private fun setupRecyclerViews() {
        identityGoalsRecyclerView = findViewById(R.id.identityGoalsRecyclerView)
        todosRecyclerView = findViewById(R.id.todosRecyclerView)

        identityGoalsRecyclerView.layoutManager = LinearLayoutManager(this)
        todosRecyclerView.layoutManager = LinearLayoutManager(this)

        dashboardAdapter = DashboardAdapter(
            onCheckIn = { habitId, isCompleted ->
                handleCheckIn(habitId, isCompleted)
            },
            onRefreshNeeded = {
                observeData()
            }
        )

        // Simple todo adapter
        todoAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemCount(): Int = todoList.size

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val view = layoutInflater.inflate(R.layout.item_todo, parent, false)
                return TodoItemViewHolder(view) { todoId, completed ->
                    toggleTodo(todoId, completed)
                }
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val todo = todoList[position]
                (holder as TodoItemViewHolder).bind(todo.id, todo.title, todo.isCompleted)
            }
        }

        identityGoalsRecyclerView.adapter = dashboardAdapter
        todosRecyclerView.adapter = todoAdapter

        optimizeRecyclerView(identityGoalsRecyclerView)
        optimizeRecyclerView(todosRecyclerView)
    }

    private fun observeData() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        // Observe full dashboard from HabitDao
        lifecycleScope.launch {
            db.habitDao().getFullDashboard(today).collect { items ->
                dashboardAdapter.updateData(items)
            }
        }

        // Observe todos
        lifecycleScope.launch {
            db.toDoDao().getAllToDos().collect { todos ->
                todoList = todos
                todoAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun handleCheckIn(habitId: Long, isCompleted: Boolean) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        lifecycleScope.launch {
            if (isCompleted) {
                db.habitCompletionDao().insert(
                    HabitCompletion(habitId = habitId, date = today)
                )
            } else {
                db.habitCompletionDao().removeCompletion(habitId, today)
            }
        }
    }

    private fun toggleTodo(todoId: Long, completed: Boolean) {
        lifecycleScope.launch {
            if (completed) {
                db.toDoDao().complete(todoId)
            } else {
                db.toDoDao().uncomplete(todoId)
            }
        }
    }
}
