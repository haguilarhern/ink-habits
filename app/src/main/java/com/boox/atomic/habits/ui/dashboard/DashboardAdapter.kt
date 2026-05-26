package com.boox.atomic.habits.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.boox.atomic.habits.R
import com.boox.atomic.habits.data.dao.DashboardItem

/**
 * Expandable adapter that displays identity goals as top-level items
 * with child habits revealed on expand/collapse.
 *
 * Data model: [DashboardItem] from the Room query joins identity goals
 * with their habits. The adapter groups them by goal ID.
 */
class DashboardAdapter(
    private var items: MutableList<ExpandableItem> = mutableListOf(),
    private val onCheckIn: (habitId: Long, isCompleted: Boolean) -> Unit,
    private val onRefreshNeeded: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var currentDateStr: String = ""
    private var dbRef: com.boox.atomic.habits.data.AppDatabase? = null
    private var coroutineScope: kotlinx.coroutines.CoroutineScope? = null

    fun setDataSource(dateStr: String, db: com.boox.atomic.habits.data.AppDatabase, scope: kotlinx.coroutines.CoroutineScope) {
        currentDateStr = dateStr
        dbRef = db
        coroutineScope = scope
    }

    companion object {
        private const val TYPE_GOAL = 0
        private const val TYPE_HABIT = 1
    }

    private val expandedState = mutableMapOf<Long, Boolean>()

    data class ExpandableItem(
        val type: Int,
        val goalId: Long,
        val goalName: String = "",
        val goalIcon: String = "",
        val goalStatement: String = "",
        val habitId: Long = 0L,
        val habitName: String = "",
        val habitStrokeData: String? = null,
        val frequencyType: String = "daily",
        val intervalDays: Int = 1,
        val daysOfWeek: String = "",
        val isCompletedToday: Boolean = false,
        val streak: Int = 0
    )

    override fun getItemViewType(position: Int): Int {
        return items[position].type
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_GOAL -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_identity_goal, parent, false)
                IdentityGoalViewHolder(view, ::toggleGoal)
            }
            TYPE_HABIT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_habit_checkin, parent, false)
                HabitCheckInViewHolder(view, onCheckIn)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is IdentityGoalViewHolder -> {
                holder.iconTextView.text = item.goalIcon
                holder.nameTextView.text = item.goalName
                val isExpanded = expandedState[item.goalId] ?: false
                holder.bind(isExpanded)
            }
            is HabitCheckInViewHolder -> {
                holder.bind(
                    habitId = item.habitId,
                    name = item.habitName,
                    strokeData = item.habitStrokeData,
                    isCompleted = item.isCompletedToday,
                    frequencyType = item.frequencyType,
                    intervalDays = item.intervalDays,
                    daysOfWeek = item.daysOfWeek,
                    streak = item.streak,
                    dateStr = currentDateStr,
                    db = dbRef,
                    scope = coroutineScope
                )
            }
        }
    }

    /**
     * Toggles the expand/collapse state of a goal header at the given adapter position.
     */
    private fun toggleGoal(position: Int) {
        val item = items[position]
        if (item.type != TYPE_GOAL) return

        val goalId = item.goalId
        val currentlyExpanded = expandedState[goalId] ?: false
        expandedState[goalId] = !currentlyExpanded

        rebuildFlatList()
    }

    /**
     * Rebuilds the flat item list based on current grouped data and expanded state.
     */
    fun updateData(dashboardItems: List<DashboardItem>) {
        val grouped = dashboardItems.groupBy { it.id }
        val newItems = mutableListOf<ExpandableItem>()

        grouped.forEach { (goalId, itemsForGoal) ->
            val first = itemsForGoal.first()

            // Add goal header
            newItems.add(
                ExpandableItem(
                    type = TYPE_GOAL,
                    goalId = goalId,
                    goalName = first.name,
                    goalIcon = first.icon,
                    goalStatement = first.identityStatement
                )
            )

            // Add habits if expanded
            if (expandedState[goalId] == true) {
                itemsForGoal.forEach { di ->
                    if (di.habitId != null) {
                        newItems.add(
                            ExpandableItem(
                                type = TYPE_HABIT,
                                goalId = goalId,
                                habitId = di.habitId,
                                habitName = di.habitName ?: "",
                                habitStrokeData = di.habitStrokeData,
                                frequencyType = di.frequencyType ?: "daily",
                                intervalDays = di.intervalDays ?: 1,
                                daysOfWeek = di.daysOfWeek ?: "",
                                isCompletedToday = di.isCompletedToday == 1
                            )
                        )
                    }
                }
            }
        }

        items = newItems
        notifyDataSetChanged()
    }

    private fun rebuildFlatList() {
        // Re-group from current items is tricky; simpler: just call updateData again
        // But the caller needs to re-supply data. For toggle we can rebuild from
        // the current items list.
        onRefreshNeeded()
    }
}
