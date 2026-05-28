package com.inkhabits.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.inkhabits.databinding.ItemAddFooterBinding
import com.inkhabits.databinding.ItemHabitBinding
import com.inkhabits.databinding.ItemIdentityHeaderBinding
import com.inkhabits.util.StrokeRenderer

class DashboardAdapter(
    private val onToggle: (habitId: Long, makeComplete: Boolean) -> Unit,
    private val onAddIdentity: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<DashboardItem> = emptyList()

    fun submit(newItems: List<DashboardItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is DashboardItem.Header -> TYPE_HEADER
        is DashboardItem.HabitRow -> TYPE_HABIT
        is DashboardItem.AddFooter -> TYPE_FOOTER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderVH(ItemIdentityHeaderBinding.inflate(inflater, parent, false))
            TYPE_FOOTER -> FooterVH(ItemAddFooterBinding.inflate(inflater, parent, false))
            else -> HabitVH(ItemHabitBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is DashboardItem.Header -> (holder as HeaderVH).bind(item)
            is DashboardItem.HabitRow -> (holder as HabitVH).bind(item)
            is DashboardItem.AddFooter -> (holder as FooterVH).bind()
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderVH(private val b: ItemIdentityHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: DashboardItem.Header) {
            val id = item.identity
            if (StrokeRenderer.hasInk(id.nameStrokes)) {
                b.identityName.visibility = View.GONE
                b.identityNameInk.visibility = View.VISIBLE
                b.identityNameInk.post {
                    val w = b.identityNameInk.width.takeIf { it > 0 } ?: 400
                    val h = b.identityNameInk.height.takeIf { it > 0 } ?: 100
                    b.identityNameInk.setImageBitmap(StrokeRenderer.renderToBitmap(id.nameStrokes, w, h))
                }
            } else {
                b.identityNameInk.visibility = View.GONE
                b.identityName.visibility = View.VISIBLE
                b.identityName.text = id.name.ifBlank { "Identity" }.uppercase()
            }
        }
    }

    inner class HabitVH(private val b: ItemHabitBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: DashboardItem.HabitRow) {
            val h = item.habit
            b.pillIcon.text = item.identityIcon
            b.habitName.setContent(h.name, h.nameStrokes)
            b.habitName.completed = item.completedToday
            b.scheduleLabel.text = item.scheduleLabel
            when {
                StrokeRenderer.hasInk(item.anchorStrokes) -> {
                    b.anchorLabel.visibility = View.GONE
                    b.anchorInkRow.visibility = View.VISIBLE
                    b.anchorInk.post {
                        val w = b.anchorInk.width.takeIf { it > 0 } ?: 80
                        val h = b.anchorInk.height.takeIf { it > 0 } ?: 28
                        b.anchorInk.setImageBitmap(
                            StrokeRenderer.renderToBitmap(item.anchorStrokes, w, h)
                        )
                    }
                }
                item.anchor.isNotBlank() -> {
                    b.anchorInkRow.visibility = View.GONE
                    b.anchorLabel.visibility = View.VISIBLE
                    b.anchorLabel.text = "after ${item.anchor}"
                }
                else -> {
                    b.anchorLabel.visibility = View.GONE
                    b.anchorInkRow.visibility = View.GONE
                }
            }
            b.streakText.text = if (item.streak > 0) "🔥${item.streak}" else ""

            val hasAnchor = b.anchorLabel.visibility == View.VISIBLE ||
                b.anchorInkRow.visibility == View.VISIBLE
            b.contextColumn.visibility = if (hasAnchor) View.VISIBLE else View.GONE

            b.checkBox.onToggle = null
            b.checkBox.checked = item.completedToday
            b.checkBox.onToggle = { makeComplete -> onToggle(h.id, makeComplete) }

            b.habitName.onStrike = { onToggle(h.id, !item.completedToday) }

            b.pillCard.setOnClickListener { onToggle(h.id, !item.completedToday) }
        }
    }

    inner class FooterVH(private val b: ItemAddFooterBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind() {
            b.addIdentity.setOnClickListener { onAddIdentity() }
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_HABIT = 1
        private const val TYPE_FOOTER = 2
    }
}
