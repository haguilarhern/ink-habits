package com.boox.atomic.habits.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boox.atomic.habits.R
import com.boox.atomic.habits.data.AppDatabase
import com.boox.atomic.habits.data.dao.DashboardItem
import com.boox.atomic.habits.data.entity.ToDo
import com.boox.atomic.habits.ui.EInkActivity
import com.boox.atomic.habits.ui.setup.SetupActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : EInkActivity() {

    private lateinit var db: AppDatabase
    private lateinit var dashboardAdapter: DashboardAdapter
    private lateinit var todoAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
    private lateinit var identityGoalsRecyclerView: RecyclerView
    private lateinit var todosRecyclerView: RecyclerView
    private lateinit var currentDateText: TextView

    private var todoList: List<ToDo> = emptyList()
    private var selectedDate: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.US)
    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val todayCal = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        db = AppDatabase.getInstance(this)

        // Settings icon
        findViewById<TextView>(R.id.settingsIcon)?.setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java).putExtra("edit_mode", true))
        }

        // Date navigation
        currentDateText = findViewById(R.id.currentDateText)
        findViewById<TextView>(R.id.prevDayBtn)?.setOnClickListener {
            selectedDate.add(Calendar.DAY_OF_MONTH, -1)
            loadDashboard()
        }
        findViewById<TextView>(R.id.nextDayBtn)?.setOnClickListener {
            selectedDate.add(Calendar.DAY_OF_MONTH, 1)
            loadDashboard()
        }

        setupRecyclerViews()
        loadDashboard()
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
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
            onRefreshNeeded = { loadDashboard() }
        )

        todoAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemCount(): Int = todoList.size

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val container = FrameLayout(parent.context)
                container.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                return TodoItemViewHolder(container) { todoId, completed ->
                    toggleTodo(todoId, completed)
                }
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val todo = todoList[position]
                (holder as TodoItemViewHolder).bind(todo.id, todo.strokeData, todo.isCompleted)
            }
        }

        identityGoalsRecyclerView.adapter = dashboardAdapter
        todosRecyclerView.adapter = todoAdapter

        optimizeRecyclerView(identityGoalsRecyclerView)
        optimizeRecyclerView(todosRecyclerView)
    }

    private fun loadDashboard() {
        // Update date display
        val isToday = selectedDate.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                selectedDate.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
        val dateLabel = if (isToday) "Today" else dateFormat.format(selectedDate.time)
        currentDateText.text = dateLabel

        val dateStr = dbDateFormat.format(selectedDate.time)

        // Set date source on adapter for calendar heatmap
        dashboardAdapter.setDataSource(dateStr, db, lifecycleScope)

        // Observe full dashboard for the selected date
        lifecycleScope.launch {
            db.habitDao().getFullDashboard(dateStr).collect { items ->
                dashboardAdapter.updateData(items)
            }
        }

        // Observe todos (not filtered by date — show active ones)
        lifecycleScope.launch {
            db.toDoDao().getAllToDos().collect { todos ->
                todoList = todos
                todoAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun handleCheckIn(habitId: Long, isCompleted: Boolean) {
        val dateStr = dbDateFormat.format(selectedDate.time)
        lifecycleScope.launch {
            if (isCompleted) {
                db.habitCompletionDao().insert(
                    com.boox.atomic.habits.data.entity.HabitCompletion(habitId = habitId, date = dateStr)
                )
            } else {
                db.habitCompletionDao().removeCompletion(habitId, dateStr)
            }
        }
    }

    private fun toggleTodo(todoId: Long, completed: Boolean) {
        lifecycleScope.launch {
            if (completed) db.toDoDao().complete(todoId)
            else db.toDoDao().uncomplete(todoId)
        }
    }
}