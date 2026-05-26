package com.boox.atomic.habits.ui.dashboard

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.boox.atomic.habits.ui.widget.HandwritingTodoWidget

/**
 * ViewHolder for a single to-do item row.
 *
 * Uses [HandwritingTodoWidget] to display handwritten to-do strokes
 * with a checkbox and strikethrough on completion.
 */
class TodoItemViewHolder(
    itemView: View,
    private val onToggle: (todoId: Long, isCompleted: Boolean) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    private val handwritingTodoWidget: HandwritingTodoWidget

    private var currentTodoId: Long = 0L

    init {
        // The itemView is a FrameLayout or LinearLayout container.
        // Build the HandwritingTodoWidget programmatically as the sole child.
        if (itemView is ViewGroup) {
            itemView.removeAllViews()
        }

        handwritingTodoWidget = HandwritingTodoWidget(itemView.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        if (itemView is ViewGroup) {
            itemView.addView(handwritingTodoWidget)
        }
    }

    fun bind(
        todoId: Long,
        strokeData: String,
        isCompleted: Boolean
    ) {
        currentTodoId = todoId

        handwritingTodoWidget.setStrokeData(strokeData)
        handwritingTodoWidget.setChecked(isCompleted)

        handwritingTodoWidget.setOnCheckedChangeListener { isChecked ->
            onToggle(todoId, isChecked)
        }
    }
}