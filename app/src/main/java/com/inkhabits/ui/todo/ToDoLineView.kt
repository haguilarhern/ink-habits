package com.inkhabits.ui.todo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.inkhabits.R
import com.inkhabits.ui.widget.CheckBoxView
import com.inkhabits.ui.widget.HabitNameView

/**
 * One notebook-style to-do line: a rounded box showing the task + a checkbox.
 *
 * The line is purely a DISPLAY view — it holds no pen surface. Tapping it opens the
 * full-screen writing pad (a single dedicated Onyx surface), which is the only
 * stable way to do zero-lag pen input on this hardware. This avoids the surface
 * churn/freezes that come from putting live drawing surfaces inside a scrolling list.
 */
@SuppressLint("ViewConstructor")
class ToDoLineView(context: Context, val host: Host) : LinearLayout(context) {

    interface Host {
        /** Open the writing pad to create/edit this line's content. */
        fun onEditLine(line: ToDoLineView)
        fun onToggleDone(line: ToDoLineView, done: Boolean)
    }

    var todoId: Long = 0L          // 0 == blank line not yet saved
    var editable = true            // false for completed-tab rows (no re-editing)
    private var savedStrokes = ""

    private val nameView: HabitNameView
    private val hint: TextView
    val checkBox: CheckBoxView

    private val rowH = dp(72)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = context.getDrawable(R.drawable.pill_bg)
        setPadding(dp(16), dp(6), dp(8), dp(6))
        isClickable = true
        val lp = MarginLayoutParams(MarginLayoutParams.MATCH_PARENT, MarginLayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(16)
        layoutParams = lp

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
        addView(content, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        checkBox = CheckBoxView(context)
        addView(checkBox, LayoutParams(dp(40), dp(40)))
        checkBox.onToggle = { done -> host.onToggleDone(this, done) }

        // Strike to complete; tap to open the writing pad.
        nameView.onStrike = { host.onToggleDone(this, !checkBox.checked) }
        nameView.onTap = { if (editable) host.onEditLine(this) }
        content.setOnClickListener { if (editable) host.onEditLine(this) }
        setOnClickListener { if (editable) host.onEditLine(this) }
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
