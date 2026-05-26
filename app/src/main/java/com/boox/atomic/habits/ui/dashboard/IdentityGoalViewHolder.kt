package com.boox.atomic.habits.ui.dashboard

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.boox.atomic.habits.R

/**
 * ViewHolder for an identity goal header row.
 *
 * Displays the goal's icon, name, and an expand/collapse arrow.
 * Clicking the row toggles the visibility of its child habits.
 */
class IdentityGoalViewHolder(
    itemView: View,
    private val onToggle: (adapterPosition: Int) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    val iconTextView: TextView = itemView.findViewById(R.id.iconTextView)
    val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
    val expandArrow: TextView = itemView.findViewById(R.id.expandArrow)
    val habitsContainer: View = itemView.findViewById(R.id.habitsContainer)

    fun bind(isExpanded: Boolean) {
        expandArrow.text = if (isExpanded) "▼" else "▶"
        habitsContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE

        itemView.setOnClickListener {
            val pos = bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onToggle(pos)
            }
        }
    }
}
