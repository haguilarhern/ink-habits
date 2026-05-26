package com.boox.atomic.habits.ui.dashboard

import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.boox.atomic.habits.R
import com.boox.atomic.habits.boox.EInkUtils

/**
 * ViewHolder for a single habit check-in row.
 *
 * Displays a checkbox (tappable to mark today's completion), the habit name,
 * a frequency hint for non-daily habits, and a streak badge.
 */
class HabitCheckInViewHolder(
    itemView: View,
    private val onCheckIn: (habitId: Long, isCompleted: Boolean) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    private val habitCheckbox: CheckBox = itemView.findViewById(R.id.habitCheckbox)
    private val habitName: TextView = itemView.findViewById(R.id.habitName)
    private val frequencyHint: TextView = itemView.findViewById(R.id.frequencyHint)
    private val streakBadge: TextView = itemView.findViewById(R.id.streakBadge)

    private var currentHabitId: Long = 0L

    init {
        // Apply GU mode for optimal e-ink rendering
        try {
            EInkUtils.setGeneralMode(itemView)
        } catch (_: Exception) {
            // Non-Boox device
        }
    }

    fun bind(
        habitId: Long,
        name: String,
        isCompleted: Boolean,
        frequencyType: String,
        intervalDays: Int,
        daysOfWeek: String,
        streak: Int
    ) {
        currentHabitId = habitId
        habitName.text = name
        habitCheckbox.isChecked = isCompleted
        streakBadge.text = if (streak > 0) "$streak🔥" else ""

        // Show frequency hint for non-daily habits
        when (frequencyType) {
            "weekly" -> {
                frequencyHint.text = "(weekly)"
                frequencyHint.visibility = View.VISIBLE
            }
            "interval" -> {
                frequencyHint.text = "(every $intervalDays days)"
                frequencyHint.visibility = View.VISIBLE
            }
            "days_of_week" -> {
                val days = parseDaysOfWeek(daysOfWeek)
                if (days.isNotEmpty()) {
                    frequencyHint.text = "($days)"
                    frequencyHint.visibility = View.VISIBLE
                } else {
                    frequencyHint.visibility = View.GONE
                }
            }
            else -> {
                frequencyHint.visibility = View.GONE
            }
        }

        habitCheckbox.setOnCheckedChangeListener(null)
        habitCheckbox.isChecked = isCompleted
        habitCheckbox.setOnCheckedChangeListener { _, isChecked ->
            onCheckIn(habitId, isChecked)
        }
    }

    private fun parseDaysOfWeek(daysOfWeek: String): String {
        if (daysOfWeek.isBlank()) return ""
        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        return daysOfWeek.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 0..6 }
            .map { dayNames[it] }
            .joinToString("/")
    }
}
