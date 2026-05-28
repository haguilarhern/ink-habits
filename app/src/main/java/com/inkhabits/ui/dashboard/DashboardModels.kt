package com.inkhabits.ui.dashboard

import com.inkhabits.data.entity.Habit
import com.inkhabits.data.entity.IdentityGoal

/** Flattened items for the dashboard RecyclerView. */
sealed class DashboardItem {
    data class Header(val identity: IdentityGoal) : DashboardItem()
    data class HabitRow(
        val habit: Habit,
        val identityIcon: String,
        val completedToday: Boolean,
        val streak: Int,
        val scheduleLabel: String,
        val anchor: String
    ) : DashboardItem()
    object AddFooter : DashboardItem()
}
