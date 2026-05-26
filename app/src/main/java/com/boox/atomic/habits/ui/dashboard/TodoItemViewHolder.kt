package com.boox.atomic.habits.ui.dashboard

import android.text.SpannableString
import android.text.style.StrikethroughSpan
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.boox.atomic.habits.R

/**
 * ViewHolder for a single to-do item row.
 *
 * Displays a checkbox and the to-do title. Clicking toggles completion,
 * which applies a strikethrough effect to the title text.
 */
class TodoItemViewHolder(
    itemView: View,
    private val onToggle: (todoId: Long, isCompleted: Boolean) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    private val todoCheckbox: CheckBox = itemView.findViewById(R.id.todoCheckbox)
    private val todoTitle: TextView = itemView.findViewById(R.id.todoTitle)

    private var currentTodoId: Long = 0L

    fun bind(
        todoId: Long,
        title: String,
        isCompleted: Boolean
    ) {
        currentTodoId = todoId
        todoTitle.text = if (isCompleted) {
            val spannable = SpannableString(title)
            spannable.setSpan(
                StrikethroughSpan(),
                0,
                title.length,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable
        } else {
            title
        }
        todoCheckbox.isChecked = isCompleted

        todoCheckbox.setOnCheckedChangeListener(null)
        todoCheckbox.isChecked = isCompleted
        todoCheckbox.setOnCheckedChangeListener { _, isChecked ->
            onToggle(todoId, isChecked)
        }
    }
}
