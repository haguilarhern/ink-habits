package com.inkhabits.ui.todo

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.inkhabits.data.AppDatabase
import com.inkhabits.data.entity.Priority
import com.inkhabits.data.entity.Recur
import com.inkhabits.data.entity.TaskList
import com.inkhabits.data.entity.ToDo
import com.inkhabits.databinding.ActivityTodoBinding
import com.inkhabits.ui.writing.WritingHostActivity
import com.inkhabits.util.Schedule
import com.inkhabits.util.StrokeRenderer
import com.inkhabits.util.TaskRecurrence
import com.inkhabits.util.YearTally
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Notebook-style to-do page. Tasks can be classified into [TaskList]s (filter chips),
 * given a due date / Eisenhower priority / recurrence (long-press a line for options),
 * and viewed as a flat list or an Eisenhower matrix.
 *
 * Each line is a display-only [ToDoLineView]; tapping one opens the full-screen writing
 * pad (a single dedicated pen surface) — no live drawing surfaces live in the list.
 */
class ToDoActivity : WritingHostActivity(), ToDoLineView.Host {

    private lateinit var binding: ActivityTodoBinding
    private lateinit var db: AppDatabase

    private val todos = mutableListOf<ToDo>()
    private var lists = listOf<TaskList>()
    private var nextOrder = 0

    private enum class Tab { ACTIVE, MATRIX, COMPLETED }
    private var tab = Tab.ACTIVE

    /** Selected list filter: -1 = All, 0 = Inbox (unlisted), >0 = a specific list. */
    private var filterListId = -1L

    private val removalHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val pendingRemovals = HashMap<ToDoLineView, Runnable>()

    companion object {
        private const val TRAILING_BLANKS = 8
        private const val REMOVE_DELAY = 2000L
        private val ACCENT = Color.parseColor("#8C1D1D")
        private val MUTED = Color.parseColor("#6B6B6B")
        private val INK = Color.parseColor("#1A1A1A")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTodoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.get(this)

        binding.backButton.setOnClickListener { finish() }
        binding.tabActive.setOnClickListener { switchTab(Tab.ACTIVE) }
        binding.tabMatrix.setOnClickListener { switchTab(Tab.MATRIX) }
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
            lists = db.taskListDao().getAll()
            todos.clear()
            todos.addAll(db.toDoDao().getAll())
            nextOrder = todos.size
            // Drop a stale filter that points at a deleted list.
            if (filterListId > 0 && lists.none { it.id == filterListId }) filterListId = -1L
            render()
        }
    }

    private fun listById(id: Long): TaskList? = lists.find { it.id == id }

    // ── tabs / chips / rendering ──

    private fun switchTab(t: Tab) {
        if (tab == t) return
        tab = t
        render()
    }

    private fun styleTabs() {
        styleTab(binding.tabActive, tab == Tab.ACTIVE)
        styleTab(binding.tabMatrix, tab == Tab.MATRIX)
        val doneCount = todos.count { it.isDone }
        binding.tabCompleted.text = if (doneCount > 0) "Completed ($doneCount)" else "Completed"
        styleTab(binding.tabCompleted, tab == Tab.COMPLETED)
    }

    private fun styleTab(tv: TextView, selected: Boolean) {
        tv.background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            if (selected) setColor(ACCENT) else {
                setColor(Color.WHITE); setStroke(dp(1).coerceAtLeast(1), Color.parseColor("#CFCBC0"))
            }
        }
        tv.setTextColor(if (selected) Color.WHITE else INK)
    }

    /** True when [t] matches the active list filter. */
    private fun inFilter(t: ToDo): Boolean = when (filterListId) {
        -1L -> true
        0L -> t.listId == 0L || listById(t.listId) == null
        else -> t.listId == filterListId
    }

    private fun renderChips() {
        val box = binding.listChips
        box.removeAllViews()
        box.addView(filterChip("All", -1L))
        box.addView(filterChip("Inbox", 0L))
        for (l in lists) box.addView(filterChip(l.name.ifBlank { "List" }, l.id, l.colorHex))
        box.addView(addChip())
    }

    private fun filterChip(label: String, id: Long, colorHex: String? = null): TextView {
        val selected = filterListId == id
        return TextView(this).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dp(13), dp(6), dp(13), dp(6))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(13).toFloat()
                if (selected) setColor(ACCENT) else {
                    setColor(Color.WHITE); setStroke(dp(1).coerceAtLeast(1), Color.parseColor("#CFCBC0"))
                }
            }
            setTextColor(when {
                selected -> Color.WHITE
                colorHex != null -> runCatching { Color.parseColor(colorHex) }.getOrDefault(INK)
                else -> INK
            })
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.marginEnd = dp(8); layoutParams = lp
            setOnClickListener { filterListId = id; render() }
            if (id > 0) setOnLongClickListener { editList(listById(id)); true }
        }
    }

    private fun addChip(): TextView = TextView(this).apply {
        text = "+ list"
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setPadding(dp(13), dp(6), dp(13), dp(6))
        setTextColor(MUTED)
        background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(13).toFloat()
            setColor(Color.WHITE); setStroke(dp(1).coerceAtLeast(1), Color.parseColor("#CFCBC0"))
        }
        setOnClickListener { editList(null) }
    }

    private fun render() {
        clearPendingRemovals()
        styleTabs()
        renderChips()
        binding.lineContainer.removeAllViews()
        when (tab) {
            Tab.ACTIVE -> renderActive()
            Tab.MATRIX -> renderMatrix()
            Tab.COMPLETED -> renderCompleted()
        }
    }

    private fun renderActive() {
        todos.filter { !it.isDone && inFilter(it) }
            .sortedWith(compareBy({ it.priority == 0 }, { it.priority }, { it.sortOrder }))
            .forEach { addLine(it, editable = true) }
        // Trailing blank lines create new tasks (in the selected list).
        repeat(TRAILING_BLANKS) { addBlankLine() }
    }

    private fun renderCompleted() {
        val done = todos.filter { it.isDone && inFilter(it) }
        if (done.isEmpty()) {
            binding.lineContainer.addView(infoText("Nothing completed yet."))
        } else {
            done.forEach { addLine(it, editable = false) }
        }
    }

    private fun renderMatrix() {
        val active = todos.filter { !it.isDone && inFilter(it) }
        val quadrants = listOf(
            Priority.DO to "Do first",
            Priority.SCHEDULE to "Schedule",
            Priority.DELEGATE to "Delegate",
            Priority.DROP to "Drop"
        )
        for ((p, title) in quadrants) {
            binding.lineContainer.addView(quadrantHeader(title, TaskRecurrence.priorityTitle(p),
                TaskRecurrence.priorityColor(p)))
            val items = active.filter { it.priority == p }.sortedBy { it.sortOrder }
            if (items.isEmpty()) binding.lineContainer.addView(infoText("—"))
            else items.forEach { addLine(it, editable = true) }
        }
        val none = active.filter { it.priority == 0 }
        if (none.isNotEmpty()) {
            binding.lineContainer.addView(quadrantHeader("Unprioritized",
                "Long-press a task to set its priority", MUTED))
            none.forEach { addLine(it, editable = true) }
        }
    }

    private fun quadrantHeader(title: String, sub: String, color: Int): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(14), 0, dp(6))
        addView(TextView(this@ToDoActivity).apply {
            text = title.uppercase()
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setLetterSpacing(0.06f)
            typeface = androidx.core.content.res.ResourcesCompat.getFont(
                this@ToDoActivity, com.inkhabits.R.font.inter_semibold)
        })
        addView(TextView(this@ToDoActivity).apply {
            text = sub
            setTextColor(MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        })
    }

    private fun addLine(todo: ToDo, editable: Boolean) {
        val line = ToDoLineView(this, this)
        line.editable = editable
        binding.lineContainer.addView(line)
        line.bind(todo.id, todo.titleStrokes, todo.isDone)
        line.bindMeta(todo, listById(todo.listId))
    }

    private fun addBlankLine() {
        binding.lineContainer.addView(ToDoLineView(this, this))
    }

    private fun infoText(msg: String) = TextView(this).apply {
        text = msg
        setTextColor(MUTED)
        setPadding(dp(4), dp(8), 0, 0)
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
                    // New blank lines inherit the currently filtered list.
                    val newListId = if (filterListId > 0) filterListId else 0L
                    val todo = ToDo(titleStrokes = strokes, sortOrder = nextOrder++, listId = newListId)
                    val id = db.toDoDao().insert(todo)
                    line.todoId = id
                    val saved = todo.copy(id = id)
                    todos.add(saved)
                    line.bindMeta(saved, listById(saved.listId))
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
                db.toDoDao().getById(id)?.let { db.toDoDao().delete(it) }
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
                val task = todos[idx]
                val updated = task.copy(isDone = done)
                todos[idx] = updated
                db.toDoDao().update(updated)
                // Completing a recurring task spawns its next occurrence.
                if (done && TaskRecurrence.isRecurring(task)) regenerate(task)
            }
            com.inkhabits.widget.WidgetCommon.updateAll(this@ToDoActivity)
        }
        val leavesTab = (tab != Tab.COMPLETED && done) || (tab == Tab.COMPLETED && !done)
        if (leavesTab) scheduleRemoval(line) else cancelRemoval(line)
        styleTabs()
    }

    /** Create a fresh, unchecked instance of a recurring task at its next due date. */
    private suspend fun regenerate(task: ToDo) {
        val next = TaskRecurrence.nextDue(task) ?: return
        val fresh = task.copy(
            id = 0,
            isDone = false,
            dueEpochDay = next.toEpochDay(),
            sortOrder = nextOrder++,
            createdAt = System.currentTimeMillis()
        )
        val id = db.toDoDao().insert(fresh)
        todos.add(fresh.copy(id = id))
        if (tab == Tab.ACTIVE || tab == Tab.MATRIX) runOnUiThread { render() }
    }

    override fun onLineOptions(line: ToDoLineView) {
        val task = todos.find { it.id == line.todoId } ?: return
        showOptions(task)
    }

    // ── per-task options ──

    private fun showOptions(task: ToDo) {
        val options = arrayOf("List…", "Due date…", "Priority…", "Repeat…", "Delete")
        com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this, com.inkhabits.R.style.ThemeOverlay_InkHabits_Dialog)
            .setTitle("Task options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickList(task)
                    1 -> pickDueDate(task)
                    2 -> pickPriority(task)
                    3 -> pickRecurrence(task)
                    4 -> deleteTask(task)
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun saveTask(updated: ToDo) {
        val idx = todos.indexOfFirst { it.id == updated.id }
        if (idx >= 0) todos[idx] = updated
        lifecycleScope.launch {
            db.toDoDao().update(updated)
            com.inkhabits.widget.WidgetCommon.updateAll(this@ToDoActivity)
            render()
        }
    }

    private fun deleteTask(task: ToDo) {
        todos.indexOfFirst { it.id == task.id }.takeIf { it >= 0 }?.let { todos.removeAt(it) }
        lifecycleScope.launch {
            db.toDoDao().getById(task.id)?.let { db.toDoDao().delete(it) }
            com.inkhabits.widget.WidgetCommon.updateAll(this@ToDoActivity)
            render()
        }
    }

    private fun pickList(task: ToDo) {
        val labels = (listOf("Inbox (none)") + lists.map { it.name.ifBlank { "List" } }).toTypedArray()
        val ids = listOf(0L) + lists.map { it.id }
        val checked = ids.indexOf(task.listId).coerceAtLeast(0)
        com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this, com.inkhabits.R.style.ThemeOverlay_InkHabits_Dialog)
            .setTitle("List")
            .setSingleChoiceItems(labels, checked) { d, which ->
                saveTask(task.copy(listId = ids[which])); d.dismiss()
            }
            .setNeutralButton("New list…") { _, _ -> editList(null) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun pickPriority(task: ToDo) {
        val labels = arrayOf(
            "P1 · Do (urgent & important)",
            "P2 · Schedule (important)",
            "P3 · Delegate (urgent)",
            "P4 · Drop (neither)",
            "None"
        )
        val values = listOf(Priority.DO, Priority.SCHEDULE, Priority.DELEGATE, Priority.DROP, Priority.NONE)
        val checked = values.indexOf(task.priority).coerceAtLeast(0)
        com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this, com.inkhabits.R.style.ThemeOverlay_InkHabits_Dialog)
            .setTitle("Priority")
            .setSingleChoiceItems(labels, checked) { d, which ->
                saveTask(task.copy(priority = values[which])); d.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun pickDueDate(task: ToDo) {
        val base = if (task.dueEpochDay > 0) LocalDate.ofEpochDay(task.dueEpochDay) else LocalDate.now()
        val dlg = android.app.DatePickerDialog(
            this,
            { _, y, m, d -> saveTask(task.copy(dueEpochDay = LocalDate.of(y, m + 1, d).toEpochDay())) },
            base.year, base.monthValue - 1, base.dayOfMonth
        )
        if (task.dueEpochDay > 0) {
            dlg.setButton(android.app.DatePickerDialog.BUTTON_NEUTRAL, "Clear") { _, _ ->
                saveTask(task.copy(dueEpochDay = 0))
            }
        }
        dlg.show()
    }

    private fun pickRecurrence(task: ToDo) {
        val labels = arrayOf("Does not repeat", "Daily", "Every N days…", "Specific weekdays…")
        com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this, com.inkhabits.R.style.ThemeOverlay_InkHabits_Dialog)
            .setTitle("Repeat")
            .setItems(labels) { _, which ->
                when (which) {
                    0 -> saveTask(task.copy(recurType = Recur.NONE))
                    1 -> saveTask(task.copy(recurType = Recur.DAILY))
                    2 -> pickInterval(task)
                    3 -> pickWeekdays(task)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun pickInterval(task: ToDo) {
        val box = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(android.text.InputFilter.LengthFilter(3))
            hint = "e.g. 3"
            if (task.recurInterval > 1) setText(task.recurInterval.toString())
            setPadding(dp(20), dp(12), dp(20), 0)
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this, com.inkhabits.R.style.ThemeOverlay_InkHabits_Dialog)
            .setTitle("Every how many days?")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                val n = (box.text?.toString()?.trim()?.toIntOrNull() ?: 1).coerceAtLeast(1)
                saveTask(task.copy(recurType = Recur.INTERVAL, recurInterval = n))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun pickWeekdays(task: ToDo) {
        val names = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val current = Schedule.parseDays(task.recurDaysOfWeek)
        val checks = BooleanArray(7) { (it + 1) in current }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this, com.inkhabits.R.style.ThemeOverlay_InkHabits_Dialog)
            .setTitle("Repeat on")
            .setMultiChoiceItems(names, checks) { _, which, isChecked -> checks[which] = isChecked }
            .setPositiveButton("Save") { _, _ ->
                val days = (0..6).filter { checks[it] }.map { it + 1 }.toSet()
                if (days.isEmpty()) saveTask(task.copy(recurType = Recur.NONE, recurDaysOfWeek = ""))
                else saveTask(task.copy(
                    recurType = Recur.DAYS_OF_WEEK, recurDaysOfWeek = Schedule.formatDays(days)))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── lists ──

    private fun editList(existing: TaskList?) {
        val palette = listOf("#8C1D1D", "#2E7D32", "#2E5E8C", "#B8860B", "#6A1B9A", "#1A1A1A")
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val name = android.widget.EditText(this).apply {
            hint = "List name"
            setText(existing?.name ?: "")
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }
        container.addView(name)

        var chosen = existing?.colorHex ?: palette.first()
        val swatches = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(14), 0, 0)
        }
        val views = mutableListOf<View>()
        palette.forEach { hex ->
            val v = View(this).apply {
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(Color.parseColor(hex))
                }
                layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply { marginEnd = dp(12) }
                setOnClickListener {
                    chosen = hex
                    views.forEach { it.alpha = 0.4f }
                    alpha = 1f
                }
                alpha = if (hex == chosen) 1f else 0.4f
            }
            views.add(v); swatches.addView(v)
        }
        container.addView(swatches)

        val builder = com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this, com.inkhabits.R.style.ThemeOverlay_InkHabits_Dialog)
            .setTitle(if (existing == null) "New list" else "Edit list")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val nm = name.text?.toString()?.trim().orEmpty()
                if (nm.isEmpty()) return@setPositiveButton
                lifecycleScope.launch {
                    if (existing == null) {
                        val id = db.taskListDao().insert(
                            TaskList(name = nm, colorHex = chosen, sortOrder = lists.size))
                        filterListId = id
                    } else {
                        db.taskListDao().update(existing.copy(name = nm, colorHex = chosen))
                    }
                    load()
                }
            }
            .setNegativeButton("Cancel", null)
        if (existing != null) {
            builder.setNeutralButton("Delete") { _, _ -> deleteList(existing) }
        }
        builder.show()
    }

    private fun deleteList(list: TaskList) {
        lifecycleScope.launch {
            db.toDoDao().clearList(list.id) // orphan tasks fall back to Inbox
            db.taskListDao().delete(list)
            if (filterListId == list.id) filterListId = -1L
            load()
        }
    }

    // ── pending-removal animation (kept from the original) ──

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
