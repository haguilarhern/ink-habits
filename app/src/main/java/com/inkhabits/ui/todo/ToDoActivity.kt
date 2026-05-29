package com.inkhabits.ui.todo

import android.graphics.Color
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.inkhabits.data.AppDatabase
import com.inkhabits.data.entity.ToDo
import com.inkhabits.databinding.ActivityTodoBinding
import com.inkhabits.ui.writing.WritingHostActivity
import com.inkhabits.util.StrokeRenderer
import com.inkhabits.util.YearTally
import kotlinx.coroutines.launch

/**
 * Notebook-style to-do page with two sub-tabs (To-Do / Completed). Each line is a
 * display-only [ToDoLineView]; tapping one opens the full-screen writing pad (a single
 * dedicated pen surface) — no live drawing surfaces live in the list, so it's stable.
 * Cumulative "done this year" counter in the header.
 */
class ToDoActivity : WritingHostActivity(), ToDoLineView.Host {

    private lateinit var binding: ActivityTodoBinding
    private lateinit var db: AppDatabase

    private val todos = mutableListOf<ToDo>()
    private var nextOrder = 0
    private enum class Tab { ACTIVE, COMPLETED }
    private var tab = Tab.ACTIVE

    private val removalHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val pendingRemovals = HashMap<ToDoLineView, Runnable>()

    companion object {
        private const val TRAILING_BLANKS = 8
        private const val REMOVE_DELAY = 2000L
        private val ACCENT = Color.parseColor("#8C1D1D")
        private val MUTED = Color.parseColor("#6B6B6B")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTodoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.get(this)

        binding.backButton.setOnClickListener { finish() }
        binding.tabActive.setOnClickListener { switchTab(Tab.ACTIVE) }
        binding.tabCompleted.setOnClickListener { switchTab(Tab.COMPLETED) }

        refreshCounter()
        load()
    }

    override fun onDestroy() {
        clearPendingRemovals()
        super.onDestroy()
    }

    private fun refreshCounter() {
        binding.yearCount.text = YearTally.get(this).toString()
    }

    private fun load() {
        lifecycleScope.launch {
            todos.clear()
            todos.addAll(db.toDoDao().getAll())
            nextOrder = todos.size
            render()
        }
    }

    // ── tabs / rendering ──

    private fun switchTab(t: Tab) {
        if (tab == t) return
        tab = t
        render()
    }

    private fun styleTabs() {
        styleTab(binding.tabActive, tab == Tab.ACTIVE)
        val doneCount = todos.count { it.isDone }
        binding.tabCompleted.text = if (doneCount > 0) "Completed ($doneCount)" else "Completed"
        styleTab(binding.tabCompleted, tab == Tab.COMPLETED)
    }

    private fun styleTab(tv: android.widget.TextView, selected: Boolean) {
        tv.background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            if (selected) setColor(ACCENT) else {
                setColor(Color.WHITE); setStroke(dp(1).coerceAtLeast(1), Color.parseColor("#CFCBC0"))
            }
        }
        tv.setTextColor(if (selected) Color.WHITE else Color.parseColor("#1A1A1A"))
    }

    private fun render() {
        clearPendingRemovals()
        styleTabs()
        binding.lineContainer.removeAllViews()
        if (tab == Tab.ACTIVE) {
            todos.filter { !it.isDone }.forEach { addLine(it, editable = true) }
            repeat(TRAILING_BLANKS) { addBlankLine() }
        } else {
            val done = todos.filter { it.isDone }
            if (done.isEmpty()) {
                binding.lineContainer.addView(android.widget.TextView(this).apply {
                    text = "Nothing completed yet."
                    setTextColor(MUTED)
                    setPadding(dp(4), dp(16), 0, 0)
                })
            } else {
                done.forEach { addLine(it, editable = false) }
            }
        }
    }

    private fun addLine(todo: ToDo, editable: Boolean) {
        val line = ToDoLineView(this, this)
        line.editable = editable
        binding.lineContainer.addView(line)
        line.bind(todo.id, todo.titleStrokes, todo.isDone)
    }

    private fun addBlankLine() {
        binding.lineContainer.addView(ToDoLineView(this, this))
    }

    // ── ToDoLineView.Host ──

    override fun onEditLine(line: ToDoLineView) {
        openWritingPad(line.currentStrokes(), "To-do") { result -> applyResult(line, result) }
    }

    private fun applyResult(line: ToDoLineView, strokes: String) {
        if (StrokeRenderer.hasInk(strokes)) {
            val wasBlank = line.todoId == 0L
            line.applyStrokes(strokes)
            lifecycleScope.launch {
                if (wasBlank) {
                    val todo = ToDo(titleStrokes = strokes, sortOrder = nextOrder++)
                    val id = db.toDoDao().insert(todo)
                    line.todoId = id
                    todos.add(todo.copy(id = id))
                    addBlankLine()
                } else {
                    val idx = todos.indexOfFirst { it.id == line.todoId }
                    if (idx >= 0) {
                        val updated = todos[idx].copy(titleStrokes = strokes)
                        todos[idx] = updated
                        db.toDoDao().update(updated)
                    }
                }
                com.inkhabits.widget.WidgetCommon.updateAll(this@ToDoActivity)
            }
        } else if (line.todoId != 0L) {
            // Cleared an existing to-do -> delete it.
            val id = line.todoId
            lifecycleScope.launch {
                todos.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { todos.removeAt(it) }
                db.toDoDao().getAll().firstOrNull { it.id == id }?.let { db.toDoDao().delete(it) }
                com.inkhabits.widget.WidgetCommon.updateAll(this@ToDoActivity)
            }
            binding.lineContainer.removeView(line)
        }
    }

    override fun onToggleDone(line: ToDoLineView, done: Boolean) {
        if (line.todoId == 0L) { line.setDone(false); return }
        line.setDone(done)
        YearTally.add(this, if (done) 1 else -1)
        refreshCounter()
        lifecycleScope.launch {
            val idx = todos.indexOfFirst { it.id == line.todoId }
            if (idx >= 0) {
                val updated = todos[idx].copy(isDone = done)
                todos[idx] = updated
                db.toDoDao().update(updated)
            }
            com.inkhabits.widget.WidgetCommon.updateAll(this@ToDoActivity)
        }
        val leavesTab = (tab == Tab.ACTIVE && done) || (tab == Tab.COMPLETED && !done)
        if (leavesTab) scheduleRemoval(line) else cancelRemoval(line)
        styleTabs()
    }

    /** Keep the toggled row visible briefly so it can be un-checked, then drop it. */
    private fun scheduleRemoval(line: ToDoLineView) {
        cancelRemoval(line)
        val r = Runnable {
            pendingRemovals.remove(line)
            binding.lineContainer.removeView(line)
        }
        pendingRemovals[line] = r
        removalHandler.postDelayed(r, REMOVE_DELAY)
    }

    private fun cancelRemoval(line: ToDoLineView) {
        pendingRemovals.remove(line)?.let { removalHandler.removeCallbacks(it) }
    }

    private fun clearPendingRemovals() {
        pendingRemovals.values.forEach { removalHandler.removeCallbacks(it) }
        pendingRemovals.clear()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
