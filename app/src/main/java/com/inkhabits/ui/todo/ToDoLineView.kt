package com.inkhabits.ui.todo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.inkhabits.R
import com.inkhabits.data.entity.TaskList
import com.inkhabits.data.entity.ToDo
import com.inkhabits.ui.widget.CheckBoxView
import com.inkhabits.ui.widget.HabitNameView
import com.inkhabits.util.TaskRecurrence

/**
 * One notebook-style to-do line: a rounded box showing the task + a checkbox, with an
 * optional metadata row (list · priority · due date · recurrence) underneath.
 *
 * The line is purely a DISPLAY view — it holds no pen surface. Tapping it opens the
 * full-screen writing pad (a single dedicated Onyx surface). Long-pressing a saved line
 * opens its options sheet (list / due date / priority / recurrence / delete).
 */
@SuppressLint("ViewConstructor")
class ToDoLineView(context: Context, val host: Host) : LinearLayout(context) {

    interface Host {
        /** Open the writing pad to create/edit this line's content. */
        fun onEditLine(line: ToDoLineView)
        fun onToggleDone(line: ToDoLineView, done: Boolean)
        /** Open the per-task options sheet (list / due / priority / recurrence). */
        fun onLineOptions(line: ToDoLineView)
    }

    var todoId: Long = 0L          // 0 == blank line not yet saved
    var editable = true            // false for completed-tab rows (no re-editing)
    private var savedStrokes = ""

    private val nameView: HabitNameView
    private val hint: TextView
    private val metaRow: LinearLayout
    val checkBox: CheckBoxView

    private val rowH = dp(72)

    init {
        orientation = VERTICAL
        background = context.getDrawable(R.drawable.pill_bg)
        setPadding(dp(16), dp(6), dp(8), dp(6))
        isClickable = true
        val lp = MarginLayoutParams(MarginLayoutParams.MATCH_PARENT, MarginLayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(16)
        layoutParams = lp

        val topRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val content = FrameLayout(context)
        nameView = HabitNameView(context).apply {
            inkMaxScale = 1f
            inkCenterHorizontal = true
        }
        hint = TextView(context).apply {
            text = "Write a to-do…"
            setTextColor(Color.parseColor("#B8B3A8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            gravity = Gravity.CENTER_VERTICAL
        }
        content.addView(nameView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, rowH))
        content.addView(hint, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, rowH, Gravity.CENTER_VERTICAL))
        topRow.addView(content, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        checkBox = CheckBoxView(context)
        topRow.addView(checkBox, LayoutParams(dp(40), dp(40)))
        checkBox.onToggle = { done -> host.onToggleDone(this, done) }
        addView(topRow, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        metaRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = GONE
            setPadding(0, 0, 0, dp(4))
        }
        addView(metaRow, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        // Strike to complete; tap to open the writing pad; long-press for options.
        nameView.onStrike = { host.onToggleDone(this, !checkBox.checked) }
        nameView.onTap = { if (editable) host.onEditLine(this) }
        content.setOnClickListener { if (editable) host.onEditLine(this) }
        setOnClickListener { if (editable) host.onEditLine(this) }
        setOnLongClickListener {
            if (todoId != 0L) { host.onLineOptions(this); true } else false
        }
    }

    fun bind(todoId: Long, strokes: String, done: Boolean) {
        this.todoId = todoId
        savedStrokes = strokes
        nameView.setContent("", strokes)
        nameView.completed = done
        checkBox.onToggle = null
        checkBox.checked = done
        checkBox.onToggle = { d -> host.onToggleDone(this, d) }
        updateDisplay()
    }

    /** Populate the metadata row from a task and its (optional) list. */
    fun bindMeta(todo: ToDo, list: TaskList?) {
        metaRow.removeAllViews()
        var any = false
        if (list != null) {
            metaRow.addView(dot(runCatching { Color.parseColor(list.colorHex) }.getOrDefault(Color.GRAY)))
            metaRow.addView(chip(list.name.ifBlank { "List" }, Color.parseColor("#6B6B6B")))
            any = true
        }
        if (todo.priority > 0) {
            metaRow.addView(chip(TaskRecurrence.priorityShort(todo.priority),
                TaskRecurrence.priorityColor(todo.priority)))
            any = true
        }
        if (todo.dueEpochDay > 0) {
            val overdue = TaskRecurrence.isOverdue(todo.dueEpochDay)
            metaRow.addView(chip(TaskRecurrence.dueLabel(todo.dueEpochDay),
                if (overdue) Color.parseColor("#8C1D1D") else Color.parseColor("#6B6B6B")))
            any = true
        }
        if (TaskRecurrence.isRecurring(todo)) {
            metaRow.addView(chip("↻ ${TaskRecurrence.recurLabel(todo)}", Color.parseColor("#2E5E8C")))
            any = true
        }
        metaRow.visibility = if (any) VISIBLE else GONE
    }

    private fun chip(text: String, color: Int): TextView = TextView(context).apply {
        this.text = text
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        setPadding(0, 0, dp(12), 0)
    }

    private fun dot(color: Int): View = View(context).apply {
        background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(color)
        }
        layoutParams = LayoutParams(dp(8), dp(8)).apply { marginEnd = dp(5) }
    }

    fun setDone(done: Boolean) {
        nameView.completed = done
        checkBox.onToggle = null
        checkBox.checked = done
        checkBox.onToggle = { d -> host.onToggleDone(this, d) }
    }

    fun currentStrokes(): String = savedStrokes

    /** Apply strokes returned from the writing pad. */
    fun applyStrokes(strokes: String) {
        savedStrokes = strokes
        nameView.setContent("", strokes)
        updateDisplay()
    }

    private fun updateDisplay() {
        val has = com.inkhabits.util.StrokeRenderer.hasInk(savedStrokes)
        nameView.visibility = if (has) VISIBLE else INVISIBLE
        hint.visibility = if (has) GONE else VISIBLE
        checkBox.visibility = if (has) VISIBLE else INVISIBLE
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
