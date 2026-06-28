package com.inkhabits.ui.todo

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
import com.inkhabits.data.entity.Priority
import com.inkhabits.data.entity.Recur
import com.inkhabits.data.entity.StageRole
import com.inkhabits.data.entity.TaskList
import com.inkhabits.data.entity.TaskStage
import com.inkhabits.data.entity.ToDo
import com.inkhabits.databinding.ActivityTodoBinding
import com.inkhabits.ui.writing.WritingHostActivity
import com.inkhabits.util.Schedule
import com.inkhabits.util.StrokeRenderer
import com.inkhabits.util.TaskRecurrence
import com.inkhabits.util.YearTally
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

/**
 * To-do page. Tasks are created with the "+" button: handwrite the task, then configure
 * its classification (list), due date, Eisenhower importance, and recurrence in one
 * popup. Tasks can be viewed as a flat list (filtered by list chips) or as a real 2×2
 * Eisenhower matrix.
 *
 * Each line is a display-only [ToDoLineView]; tapping one re-opens the full-screen
 * writing pad, long-pressing opens its options popup.
 */
class ToDoActivity : WritingHostActivity(), ToDoLineView.Host {

    private lateinit var binding: ActivityTodoBinding
    private lateinit var db: AppDatabase

    private val todos = mutableListOf<ToDo>()
    private var lists = listOf<TaskList>()
    private var stages = listOf<TaskStage>()
    private var nextOrder = 0

    private enum class TaskView { LIST, KANBAN, MATRIX, COMPLETED }
    private var view = TaskView.LIST

    /** Selected list filter: -1 = All, 0 = Inbox (unlisted), >0 = a specific list. */
    private var filterListId = -1L

    private val removalHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val pendingRemovals = HashMap<ToDoLineView, Runnable>()

    companion object {
        private const val REMOVE_DELAY = 2000L
        private val ACCENT = Color.parseColor("#0DA88F")
        private val MUTED = Color.parseColor("#5C5C5C")
        private val INK = Color.parseColor("#0B0B0C")
        private val HAIRLINE = Color.parseColor("#D9D9DE")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTodoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.get(this)

        // Back arrow mirrors the system back: collapse to the main List view first, only
        // leaving the screen when already on List.
        binding.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        // Back gesture: collapse to the main List view first; only leave the screen when
        // already on List. (Previously any view went straight back to Home.)
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (view != TaskView.LIST) { view = TaskView.LIST; render() }
                else { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
            }
        })
        binding.pomodoroButton.setOnClickListener {
            startActivity(android.content.Intent(this, com.inkhabits.ui.pomodoro.PomodoroActivity::class.java))
        }
        binding.viewSelector.setOnClickListener { showViewMenu() }
        binding.fabAddTask.setOnClickListener { createTask() }
        // Tap the "done this year" counter to review completed tasks (and un-check any
        // marked done by accident).
        binding.yearCounter.setOnClickListener { view = TaskView.COMPLETED; render() }

        refreshCounter()
        // Synchronous first paint: load tasks before the first frame so the list arrives
        // populated in one e-ink refresh instead of blank-then-filled. DB is tiny (~ms).
        runBlocking { loadData() }
        render()
    }

    /** onCreate already loaded synchronously; skip the duplicate reload on first resume. */
    private var firstResume = true

    override fun onResume() {
        super.onResume()
        if (firstResume) { firstResume = false; return }
        // Returning from a sub-screen (e.g. the Pomodoro timer, which can mark tasks done):
        // re-read so external changes show immediately instead of staying stale until the
        // activity is recreated. refreshCounter() also picks up the updated year tally.
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
            loadData()
            render()
        }
    }

    /** Reads task data into the in-memory fields. Shared by the async [load] and the
     *  synchronous first paint in onCreate. */
    private suspend fun loadData() {
        lists = db.taskListDao().getAll()
        stages = db.taskStageDao().getAll()
        todos.clear()
        todos.addAll(db.toDoDao().getAll())
        nextOrder = todos.size
        if (filterListId > 0 && lists.none { it.id == filterListId }) filterListId = -1L
    }

    private fun listById(id: Long): TaskList? = lists.find { it.id == id }

    // ── view selector / chips ──

    private fun viewName(v: TaskView) = when (v) {
        TaskView.LIST -> "List"
        TaskView.KANBAN -> "Kanban"
        TaskView.MATRIX -> "Matrix"
        TaskView.COMPLETED -> "Completed"
    }

    private fun showViewMenu() {
        val menu = android.widget.PopupMenu(this, binding.viewSelector)
        TaskView.values().forEachIndexed { i, v -> menu.menu.add(0, i, i, viewName(v)) }
        menu.setOnMenuItemClickListener { item ->
            view = TaskView.values()[item.itemId]; render(); true
        }
        menu.show()
    }

    private fun updateViewLabel() {
        val done = todos.count { it.isDone }
        val suffix = if (view == TaskView.COMPLETED && done > 0) " ($done)" else ""
        binding.viewSelector.text = "${viewName(view)}$suffix  ▾"
        binding.viewSelector.background = pill(Color.WHITE, true)
    }

    private fun pill(fill: Int, stroke: Boolean) = android.graphics.drawable.GradientDrawable().apply {
        cornerRadius = dp(14).toFloat()
        setColor(fill)
        if (stroke) setStroke(dp(1).coerceAtLeast(1), HAIRLINE)
    }

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
            background = pill(if (selected) ACCENT else Color.WHITE, !selected)
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
        background = pill(Color.WHITE, true)
        setOnClickListener { editList(null) }
    }

    // ── rendering ──

    private fun render() {
        clearPendingRemovals()
        updateViewLabel()
        renderChips()
        binding.lineContainer.removeAllViews()
        when (view) {
            TaskView.LIST -> renderActive()
            TaskView.KANBAN -> renderKanban()
            TaskView.MATRIX -> renderMatrix()
            TaskView.COMPLETED -> renderCompleted()
        }
    }

    private fun renderActive() {
        val items = todos.filter { !it.isDone && inFilter(it) }
            .sortedWith(compareBy({ it.priority == 0 }, { it.priority }, { it.sortOrder }))
        if (items.isEmpty()) {
            binding.lineContainer.addView(infoText("No tasks here yet. Tap + to add one."))
            return
        }
        binding.lineContainer.addView(infoText("Tap a task to rewrite · ✎ to edit details"))
        items.forEach { addLine(it, editable = true) }
    }

    private fun renderCompleted() {
        val done = todos.filter { it.isDone && inFilter(it) }
        if (done.isEmpty()) binding.lineContainer.addView(infoText("Nothing completed yet."))
        else done.forEach { addLine(it, editable = false) }
    }

    /** A real 2×2 Eisenhower grid: urgency on columns, importance on rows. */
    private fun renderMatrix() {
        val active = todos.filter { !it.isDone && inFilter(it) }
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        // Columns = urgency, rows = importance.
        grid.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(quadrantCell(Priority.DO, active), cellLp())       // urgent & important
            addView(quadrantCell(Priority.SCHEDULE, active), cellLp()) // important, not urgent
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        grid.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(quadrantCell(Priority.DELEGATE, active), cellLp()) // urgent, not important
            addView(quadrantCell(Priority.DROP, active), cellLp())     // neither
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        // Make the grid fill the visible area so the four quadrants read as a matrix.
        binding.lineContainer.addView(grid, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, screenGridHeight()))

        val none = active.filter { it.priority == 0 }
        if (none.isNotEmpty()) {
            binding.lineContainer.addView(TextView(this).apply {
                text = "UNPRIORITIZED — long-press to set importance"
                setTextColor(MUTED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setPadding(0, dp(14), 0, dp(6))
            })
            none.forEach { addLine(it, editable = true) }
        }
    }

    private fun cellLp() = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        .apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }

    private fun screenGridHeight(): Int = (resources.displayMetrics.heightPixels * 0.62f).toInt()

    private fun quadrantCell(priority: Int, active: List<ToDo>): View {
        val color = TaskRecurrence.priorityColor(priority)
        val cell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(Color.WHITE)
                setStroke(dp(1).coerceAtLeast(1), color)
            }
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        cell.addView(TextView(this).apply {
            text = TaskRecurrence.priorityTitle(priority).uppercase()
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setLetterSpacing(0.04f)
            typeface = font()
        })
        val scroll = android.widget.ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
        }
        val inner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val items = active.filter { it.priority == priority }.sortedBy { it.sortOrder }
        if (items.isEmpty()) {
            inner.addView(TextView(this).apply {
                text = "—"; setTextColor(MUTED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, dp(6), 0, 0)
            })
        } else {
            items.forEach { inner.addView(compactTask(it)) }
        }
        scroll.addView(inner)
        cell.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(4) })
        return cell
    }

    /** Compact task row for a matrix cell (small ink/text + checkbox), opens options on tap. */
    private fun compactTask(todo: ToDo): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(5), 0, dp(5))
            isClickable = true
            setOnClickListener { showTaskEditor(todo, isNew = false) }
        }
        if (StrokeRenderer.hasInk(todo.titleStrokes)) {
            row.addView(ImageView(this).apply {
                scaleType = ImageView.ScaleType.FIT_START
                layoutParams = LinearLayout.LayoutParams(0, dp(20), 1f)
                post {
                    setImageBitmap(StrokeRenderer.renderToBitmap(
                        todo.titleStrokes, width.coerceAtLeast(1), dp(20), maxScale = 1f))
                }
            })
        } else {
            row.addView(TextView(this).apply {
                text = todo.title.ifBlank { "Task" }
                setTextColor(INK)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
        if (todo.dueEpochDay > 0) {
            row.addView(TextView(this).apply {
                text = if (TaskRecurrence.isOverdue(todo.dueEpochDay)) "!" else "◷"
                setTextColor(if (TaskRecurrence.isOverdue(todo.dueEpochDay)) ACCENT else MUTED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(dp(4), 0, dp(4), 0)
            })
        }
        // Complete control: matrix shows only active tasks, so checking it off
        // marks the task done (it then leaves the matrix on the next render).
        row.addView(TextView(this).apply {
            text = "☐"
            setTextColor(MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setPadding(dp(6), 0, dp(2), 0)
            isClickable = true
            setOnClickListener { setTaskDone(todo, true) }
        })
        return row
    }

    // ── kanban ──

    /** Stages in display order: To Do first, custom stages by sortOrder, Done last. */
    private fun orderedStages(): List<TaskStage> = stages.sortedBy { it.sortOrder }
    private fun todoStage(): TaskStage? = stages.firstOrNull { it.role == StageRole.TODO }
    private fun doneStage(): TaskStage? = stages.firstOrNull { it.role == StageRole.DONE }

    /** The column a task belongs in: Done if completed, else its stage (default To Do). */
    private fun columnIdFor(t: ToDo, todoId: Long, doneId: Long): Long = when {
        t.isDone -> doneId
        stages.any { it.id == t.stageId && it.role != StageRole.DONE } -> t.stageId
        else -> todoId
    }

    private fun renderKanban() {
        // Ensure the two fixed stages exist before drawing the board.
        if (todoStage() == null || doneStage() == null) {
            binding.lineContainer.addView(infoText("Setting up board…"))
            ensureFixedStages()
            return
        }
        val ordered = orderedStages()
        val todoId = todoStage()!!.id
        val doneId = doneStage()!!.id
        val visible = todos.filter { inFilter(it) }
        val customs = ordered.filter { it.role == StageRole.NONE }

        val columns = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        ordered.forEach { st ->
            val items = visible.filter { columnIdFor(it, todoId, doneId) == st.id }
                .sortedWith(compareBy({ it.isDone }, { it.sortOrder }))
            columns.addView(kanbanColumn(st, items, customs))
        }
        columns.addView(addStageColumn())

        val hs = android.widget.HorizontalScrollView(this).apply { isFillViewport = false }
        hs.addView(columns)
        binding.lineContainer.addView(hs, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, screenGridHeight()))
    }

    private fun kanbanColumn(stage: TaskStage, items: List<ToDo>, customs: List<TaskStage>): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(Color.WHITE)
                setStroke(dp(1).coerceAtLeast(1), HAIRLINE)
            }
            setPadding(dp(10), dp(8), dp(10), dp(8))
            layoutParams = LinearLayout.LayoutParams(dp(228), LinearLayout.LayoutParams.MATCH_PARENT)
                .apply { marginEnd = dp(8) }
        }
        // Header row: [← reorder] name·count (tap to rename/delete) [→ reorder].
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(6))
        }
        val isCustom = stage.role == StageRole.NONE
        val customIdx = customs.indexOfFirst { it.id == stage.id }
        if (isCustom && customIdx > 0) header.addView(reorderArrow("‹") { moveStage(stage, -1, customs) })
        header.addView(TextView(this).apply {
            text = "${stage.name.ifBlank { "Stage" }}  ·  ${items.size}"
            setTextColor(INK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setLetterSpacing(0.03f)
            typeface = font()
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { editStage(stage) }
        })
        if (isCustom && customIdx < customs.lastIndex) header.addView(reorderArrow("›") { moveStage(stage, 1, customs) })
        // Visible edit/delete affordance on custom columns (To Do / Done are fixed).
        if (isCustom) header.addView(TextView(this).apply {
            text = "✎"
            setTextColor(MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setPadding(dp(6), 0, 0, 0)
            setOnClickListener { editStage(stage) }
        })
        col.addView(header)

        val scroll = android.widget.ScrollView(this).apply { isVerticalScrollBarEnabled = false }
        val inner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (items.isEmpty()) {
            inner.addView(TextView(this).apply {
                text = "—"; setTextColor(MUTED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f); setPadding(0, dp(6), 0, 0)
            })
        } else items.forEach { inner.addView(kanbanCard(it)) }
        scroll.addView(inner)
        col.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(4) })
        return col
    }

    private fun reorderArrow(glyph: String, onClick: () -> Unit) = TextView(this).apply {
        text = glyph
        setTextColor(MUTED)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        setPadding(dp(4), 0, dp(4), 0)
        setOnClickListener { onClick() }
    }

    private fun kanbanCard(todo: ToDo): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(Color.parseColor("#F2F2F4"))
            }
            setPadding(dp(10), dp(8), dp(6), dp(8))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(8); layoutParams = lp
            isClickable = true
            setOnClickListener { showTaskEditor(todo, isNew = false) }
        }
        // Name (ink or text).
        if (StrokeRenderer.hasInk(todo.titleStrokes)) {
            card.addView(ImageView(this).apply {
                scaleType = ImageView.ScaleType.FIT_START
                alpha = if (todo.isDone) 0.5f else 1f
                layoutParams = LinearLayout.LayoutParams(0, dp(20), 1f)
                post {
                    setImageBitmap(StrokeRenderer.renderToBitmap(
                        todo.titleStrokes, width.coerceAtLeast(1), dp(20), maxScale = 1f))
                }
            })
        } else {
            card.addView(TextView(this).apply {
                text = todo.title.ifBlank { "Task" }
                setTextColor(if (todo.isDone) MUTED else INK)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                if (todo.isDone) paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
        // Done tasks: a reopen control; others: advance to the next stage.
        if (todo.isDone) {
            card.addView(TextView(this).apply {
                text = "↺"
                setTextColor(MUTED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setPadding(dp(6), 0, dp(6), 0)
                setOnClickListener { setTaskDone(todo, false) }
            })
        } else {
            card.addView(TextView(this).apply {
                text = "→"
                setTextColor(ACCENT)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setPadding(dp(6), 0, dp(6), 0)
                setOnClickListener { moveForward(todo) }
            })
        }
        return card
    }

    private fun addStageColumn(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(12).toFloat()
            setColor(Color.WHITE)
            setStroke(dp(1).coerceAtLeast(1), HAIRLINE)
        }
        layoutParams = LinearLayout.LayoutParams(dp(140), LinearLayout.LayoutParams.MATCH_PARENT)
        addView(TextView(this@ToDoActivity).apply {
            text = "+ Stage"
            setTextColor(MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        })
        setOnClickListener { editStage(null) }
    }

    /** Advance a task one column. Landing on Done marks it done (feeds the counter). */
    private fun moveForward(todo: ToDo) {
        val ordered = orderedStages()
        val todoId = todoStage()?.id ?: return
        val doneId = doneStage()?.id ?: return
        val curId = columnIdFor(todo, todoId, doneId)
        val idx = ordered.indexOfFirst { it.id == curId }
        val next = ordered.getOrNull(idx + 1) ?: return
        if (next.role == StageRole.DONE) {
            setTaskDone(todo, true)
        } else {
            val updated = todo.copy(stageId = next.id)
            todos.indexOfFirst { it.id == todo.id }.takeIf { it >= 0 }?.let { todos[it] = updated }
            lifecycleScope.launch {
                db.toDoDao().update(updated)
                com.inkhabits.widget.WidgetCommon.updateAll(this@ToDoActivity)
                render()
            }
        }
    }

    /** Single source of truth for completion: updates the year counter, widgets, and
     *  regenerates recurring tasks. Used by the list, the matrix, the Kanban board,
     *  and (mirrored) the Pomodoro screen. */
    private fun setTaskDone(todo: ToDo, done: Boolean) {
        if (todo.isDone == done) return
        YearTally.add(this, if (done) 1 else -1)
        refreshCounter()
        val updated = todo.copy(isDone = done)
        todos.indexOfFirst { it.id == todo.id }.takeIf { it >= 0 }?.let { todos[it] = updated }
        lifecycleScope.launch {
            db.toDoDao().update(updated)
            if (done && TaskRecurrence.isRecurring(todo)) regenerate(todo)
            com.inkhabits.widget.WidgetCommon.updateAll(this@ToDoActivity)
            render()
        }
    }

    private fun moveStage(stage: TaskStage, dir: Int, customs: List<TaskStage>) {
        val idx = customs.indexOfFirst { it.id == stage.id }
        val other = customs.getOrNull(idx + dir) ?: return
        lifecycleScope.launch {
            db.taskStageDao().update(stage.copy(sortOrder = other.sortOrder))
            db.taskStageDao().update(other.copy(sortOrder = stage.sortOrder))
            load()
        }
    }

    private fun ensureFixedStages() {
        lifecycleScope.launch {
            val cur = db.taskStageDao().getAll()
            if (cur.none { it.role == StageRole.TODO })
                db.taskStageDao().insert(TaskStage(name = "To Do", role = StageRole.TODO, sortOrder = 0))
            if (cur.none { it.role == StageRole.DONE })
                db.taskStageDao().insert(TaskStage(name = "Done", role = StageRole.DONE, sortOrder = 1_000_000))
            load()
        }
    }

    private fun editStage(existing: TaskStage?) {
        val name = android.widget.EditText(this).apply {
            hint = "Stage name"
            setText(existing?.name ?: "")
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(dp(20), dp(16), dp(20), dp(8))
        }
        val builder = com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this, com.inkhabits.R.style.ThemeOverlay_InkHabits_Dialog)
            .setTitle(if (existing == null) "New stage" else "Edit stage")
            .setView(name)
            .setPositiveButton("Save") { _, _ ->
                val nm = name.text?.toString()?.trim().orEmpty()
                if (nm.isEmpty()) return@setPositiveButton
                lifecycleScope.launch {
                    if (existing == null) {
                        // New custom stage slots just before Done.
                        val maxCustom = stages.filter { it.role == StageRole.NONE }
                            .maxOfOrNull { it.sortOrder } ?: 0
                        db.taskStageDao().insert(TaskStage(name = nm, sortOrder = maxCustom + 1))
                    } else {
                        db.taskStageDao().update(existing.copy(name = nm))
                    }
                    load()
                }
            }
            .setNegativeButton("Cancel", null)
        // Only custom stages can be deleted; To Do / Done are fixed.
        if (existing != null && existing.role == StageRole.NONE) {
            builder.setNeutralButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    db.toDoDao().clearStage(existing.id) // tasks fall back to To Do
                    db.taskStageDao().delete(existing)
                    load()
                }
            }
        }
        builder.show()
    }

    private fun addLine(todo: ToDo, editable: Boolean) {
        val line = ToDoLineView(this, this)
        line.editable = editable
        binding.lineContainer.addView(line)
        line.bind(todo.id, todo.titleStrokes, todo.isDone)
        line.bindMeta(todo, listById(todo.listId))
    }

    private fun infoText(msg: String) = TextView(this).apply {
        text = msg
        setTextColor(MUTED)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        setPadding(dp(4), dp(8), 0, dp(8))
    }

    private fun font() = androidx.core.content.res.ResourcesCompat.getFont(
        this, com.inkhabits.R.font.inter_semibold)

    // ── creation / editing ──

    /** + button: write the task, then open the configure popup. */
    private fun createTask() {
        openWritingPad("", "New to-do") { strokes ->
            if (StrokeRenderer.hasInk(strokes)) {
                val listId = if (filterListId > 0) filterListId else 0L
                showTaskEditor(ToDo(titleStrokes = strokes, listId = listId), isNew = true)
            }
        }
    }

    override fun onEditLine(line: ToDoLineView) {
        openWritingPad(line.currentStrokes(), "To-do") { strokes -> applyRewrite(line, strokes) }
    }

    /** Rewrite (or clear→delete) an existing task's handwriting. */
    private fun applyRewrite(line: ToDoLineView, strokes: String) {
        val id = line.todoId
        if (id == 0L) return
        val idx = todos.indexOfFirst { it.id == id }
        if (idx < 0) return
        if (StrokeRenderer.hasInk(strokes)) {
            val updated = todos[idx].copy(titleStrokes = strokes)
            todos[idx] = updated
            line.applyStrokes(strokes)
            lifecycleScope.launch {
                db.toDoDao().update(updated)
                com.inkhabits.widget.WidgetCommon.updateAll(this@ToDoActivity)
            }
        } else {
            deleteTask(todos[idx])
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
                if (done && TaskRecurrence.isRecurring(task)) regenerate(task)
            }
            com.inkhabits.widget.WidgetCommon.updateAll(this@ToDoActivity)
        }
        val leavesView = (view != TaskView.COMPLETED && done) || (view == TaskView.COMPLETED && !done)
        if (leavesView) scheduleRemoval(line) else cancelRemoval(line)
        updateViewLabel()
    }

    /** Create a fresh, unchecked instance of a recurring task at its next due date. */
    private suspend fun regenerate(task: ToDo) {
        val next = TaskRecurrence.nextDue(task) ?: return
        val fresh = task.copy(
            id = 0, isDone = false, dueEpochDay = next.toEpochDay(),
            sortOrder = nextOrder++, createdAt = System.currentTimeMillis()
        )
        val id = db.toDoDao().insert(fresh)
        todos.add(fresh.copy(id = id))
        if (view != TaskView.COMPLETED) runOnUiThread { render() }
    }

    override fun onLineOptions(line: ToDoLineView) {
        val task = todos.find { it.id == line.todoId } ?: return
        showTaskEditor(task, isNew = false)
    }

    // ── task configure popup ──

    /**
     * The configure popup: handwriting preview + rewrite, classification, due date,
     * importance, and recurrence — all in one place. Saving inserts (new) or updates.
     */
    private fun showTaskEditor(task: ToDo, isNew: Boolean) {
        var working = task

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }

        // Handwriting preview + rewrite.
        val preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(40))
        }
        fun refreshPreview() {
            preview.post {
                preview.setImageBitmap(StrokeRenderer.renderToBitmap(
                    working.titleStrokes, preview.width.coerceAtLeast(1), dp(40), maxScale = 1f))
            }
        }
        root.addView(preview)
        refreshPreview()
        root.addView(textButton("✎ Rewrite") {
            openWritingPad(working.titleStrokes, "To-do") { strokes ->
                if (StrokeRenderer.hasInk(strokes)) { working = working.copy(titleStrokes = strokes); refreshPreview() }
            }
        })

        root.addView(divider())

        // Show only the fields relevant to the current view, so the editor stays focused.
        // Completed is a review view and shows everything.
        val listFields = view == TaskView.LIST || view == TaskView.COMPLETED
        val showImportance = view == TaskView.MATRIX || view == TaskView.COMPLETED
        val showStage = view == TaskView.KANBAN || view == TaskView.COMPLETED

        if (listFields) {
            // Classification (list).
            val listBtn = settingRow(root, "Classification") { chooseList(working.listId) { id ->
                working = working.copy(listId = id); it.text = listLabel(id) } }
            listBtn.text = listLabel(working.listId)
            // Due date.
            val dueBtn = settingRow(root, "Due date") { chooseDate(working.dueEpochDay) { ep ->
                working = working.copy(dueEpochDay = ep); it.text = dueLabel(ep) } }
            dueBtn.text = dueLabel(working.dueEpochDay)
            // Recurrence.
            val repeatBtn = settingRow(root, "Repeat") { chooseRecurrence(working) { upd ->
                working = upd; it.text = recurLabel(upd) } }
            repeatBtn.text = recurLabel(working)
        }
        if (showImportance) {
            // Importance (Eisenhower urgency/importance) — the Matrix dimension.
            val prioBtn = settingRow(root, "Importance") { choosePriority(working.priority) { p ->
                working = working.copy(priority = p); it.text = prioLabel(p) } }
            prioBtn.text = prioLabel(working.priority)
        }
        if (showStage) {
            // Kanban stage.
            val stageBtn = settingRow(root, "Stage") { chooseStage(working.stageId) { sid ->
                working = working.copy(stageId = sid); it.text = stageLabel(sid) } }
            stageBtn.text = stageLabel(working.stageId)
        }

        val builder = com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this, com.inkhabits.R.style.ThemeOverlay_InkHabits_Dialog)
            .setTitle(if (isNew) "New task" else "Task")
            .setView(android.widget.ScrollView(this).apply { addView(root) })
            .setPositiveButton("Save") { _, _ -> saveOrInsert(working, isNew) }
            .setNegativeButton("Cancel", null)
        if (!isNew) builder.setNeutralButton("Delete") { _, _ -> deleteTask(task) }
        builder.show()
    }

    private fun saveOrInsert(working: ToDo, isNew: Boolean) {
        lifecycleScope.launch {
            if (isNew) {
                val toSave = working.copy(sortOrder = nextOrder++)
                val id = db.toDoDao().insert(toSave)
                todos.add(toSave.copy(id = id))
            } else {
                val idx = todos.indexOfFirst { it.id == working.id }
                if (idx >= 0) todos[idx] = working
                db.toDoDao().update(working)
            }
            com.inkhabits.widget.WidgetCommon.updateAll(this@ToDoActivity)
            render()
        }
    }

    // ── editor controls ──

    /** A label + tappable value row, appended to [parent]; returns the value view. */
    private fun settingRow(parent: LinearLayout, label: String, onClick: (TextView) -> Unit): TextView {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))
            isClickable = true
        }
        row.addView(TextView(this).apply {
            text = label
            setTextColor(MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val value = TextView(this).apply {
            setTextColor(ACCENT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            gravity = Gravity.END
        }
        row.addView(value)
        row.setOnClickListener { onClick(value) }
        parent.addView(row)
        return value
    }

    private fun prioLabel(p: Int): String =
        if (p == Priority.NONE) "None" else TaskRecurrence.priorityTitle(p)

    private fun choosePriority(current: Int, onPick: (Int) -> Unit) {
        val values = listOf(Priority.DO, Priority.SCHEDULE, Priority.DELEGATE, Priority.DROP, Priority.NONE)
        val labels = values.map { if (it == Priority.NONE) "None" else TaskRecurrence.priorityTitle(it) }
            .toTypedArray()
        val checked = values.indexOf(current).coerceAtLeast(0)
        com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this, com.inkhabits.R.style.ThemeOverlay_InkHabits_Dialog)
            .setTitle("Importance")
            .setSingleChoiceItems(labels, checked) { d, which -> onPick(values[which]); d.dismiss() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun listLabel(id: Long): String =
        if (id <= 0) "Inbox" else listById(id)?.name?.ifBlank { "List" } ?: "Inbox"

    private fun dueLabel(ep: Long): String =
        if (ep <= 0) "None" else TaskRecurrence.dueLabel(ep)

    private fun recurLabel(t: ToDo): String =
        if (t.recurType == Recur.NONE) "Never" else TaskRecurrence.recurLabel(t)

    private fun stageLabel(id: Long): String =
        if (id <= 0) "None" else stages.find { it.id == id }?.name?.ifBlank { "Stage" } ?: "None"

    private fun chooseStage(current: Long, onPick: (Long) -> Unit) {
        // Done is set by completing a task, not by manual assignment, so it's excluded here.
        val selectable = stages.filter { it.role != StageRole.DONE }.sortedBy { it.sortOrder }
        val labels = (listOf("No stage") + selectable.map { it.name.ifBlank { "Stage" } } + "New stage…").toTypedArray()
        val ids = listOf(0L) + selectable.map { it.id }
        val checked = ids.indexOf(current).coerceAtLeast(0)
        com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this, com.inkhabits.R.style.ThemeOverlay_InkHabits_Dialog)
            .setTitle("Stage")
            .setSingleChoiceItems(labels, checked) { d, which ->
                if (which == labels.size - 1) editStage(null) else onPick(ids[which])
                d.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun chooseList(current: Long, onPick: (Long) -> Unit) {
        val labels = (listOf("Inbox (none)") + lists.map { it.name.ifBlank { "List" } } + "New list…").toTypedArray()
        val ids = listOf(0L) + lists.map { it.id }
        val checked = ids.indexOf(current).coerceAtLeast(0)
        com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this, com.inkhabits.R.style.ThemeOverlay_InkHabits_Dialog)
            .setTitle("Classification")
            .setSingleChoiceItems(labels, checked) { d, which ->
                if (which == labels.size - 1) editList(null) else onPick(ids[which])
                d.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun chooseDate(current: Long, onPick: (Long) -> Unit) {
        val base = if (current > 0) LocalDate.ofEpochDay(current) else LocalDate.now()
        val dlg = android.app.DatePickerDialog(
            this,
            { _, y, m, d -> onPick(LocalDate.of(y, m + 1, d).toEpochDay()) },
            base.year, base.monthValue - 1, base.dayOfMonth
        )
        dlg.setButton(android.app.DatePickerDialog.BUTTON_NEUTRAL, "Clear") { _, _ -> onPick(0) }
        dlg.show()
    }

    private fun chooseRecurrence(task: ToDo, onPick: (ToDo) -> Unit) {
        val labels = arrayOf("Does not repeat", "Daily", "Every N days…", "Specific weekdays…")
        com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this, com.inkhabits.R.style.ThemeOverlay_InkHabits_Dialog)
            .setTitle("Repeat")
            .setItems(labels) { _, which ->
                when (which) {
                    0 -> onPick(task.copy(recurType = Recur.NONE))
                    1 -> onPick(task.copy(recurType = Recur.DAILY))
                    2 -> pickInterval(task, onPick)
                    3 -> pickWeekdays(task, onPick)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun pickInterval(task: ToDo, onPick: (ToDo) -> Unit) {
        val box = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(android.text.InputFilter.LengthFilter(3))
            hint = "e.g. 3"
            if (task.recurInterval > 1) setText(task.recurInterval.toString())
            setPadding(dp(20), dp(16), dp(20), dp(8))
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this, com.inkhabits.R.style.ThemeOverlay_InkHabits_Dialog)
            .setTitle("Every how many days?")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                val n = (box.text?.toString()?.trim()?.toIntOrNull() ?: 1).coerceAtLeast(1)
                onPick(task.copy(recurType = Recur.INTERVAL, recurInterval = n))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun pickWeekdays(task: ToDo, onPick: (ToDo) -> Unit) {
        val names = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val current = Schedule.parseDays(task.recurDaysOfWeek)
        val checks = BooleanArray(7) { (it + 1) in current }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(
            this, com.inkhabits.R.style.ThemeOverlay_InkHabits_Dialog)
            .setTitle("Repeat on")
            .setMultiChoiceItems(names, checks) { _, which, isChecked -> checks[which] = isChecked }
            .setPositiveButton("Save") { _, _ ->
                val days = (0..6).filter { checks[it] }.map { it + 1 }.toSet()
                if (days.isEmpty()) onPick(task.copy(recurType = Recur.NONE, recurDaysOfWeek = ""))
                else onPick(task.copy(
                    recurType = Recur.DAYS_OF_WEEK, recurDaysOfWeek = Schedule.formatDays(days)))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun label(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextColor(MUTED)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        setPadding(0, dp(12), 0, dp(2))
    }

    private fun textButton(text: String, onClick: () -> Unit): TextView = TextView(this).apply {
        this.text = text
        setTextColor(ACCENT)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        setPadding(0, dp(6), 0, dp(6))
        setOnClickListener { onClick() }
    }

    private fun divider(): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
            .apply { topMargin = dp(8); bottomMargin = dp(4) }
        setBackgroundColor(Color.parseColor("#D9D9DE"))
    }

    private fun deleteTask(task: ToDo) {
        todos.indexOfFirst { it.id == task.id }.takeIf { it >= 0 }?.let { todos.removeAt(it) }
        lifecycleScope.launch {
            db.toDoDao().getById(task.id)?.let { db.toDoDao().delete(it) }
            com.inkhabits.widget.WidgetCommon.updateAll(this@ToDoActivity)
            render()
        }
    }

    // ── lists ──

    private fun editList(existing: TaskList?) {
        val palette = listOf("#0B0B0C", "#5C5C5C", "#5C5C5C", "#0B0B0C", "#6A1B9A", "#0B0B0C")
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val name = android.widget.EditText(this).apply {
            hint = "List name"
            setText(existing?.name ?: "")
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(0, dp(8), 0, dp(8))
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
        if (existing != null) builder.setNeutralButton("Delete") { _, _ -> deleteList(existing) }
        builder.show()
    }

    private fun deleteList(list: TaskList) {
        lifecycleScope.launch {
            db.toDoDao().clearList(list.id)
            db.taskListDao().delete(list)
            if (filterListId == list.id) filterListId = -1L
            load()
        }
    }

    // ── pending-removal animation ──

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
