package com.inkhabits.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.inkhabits.databinding.ItemAddFooterBinding
import com.inkhabits.databinding.ItemHabitBinding
import com.inkhabits.databinding.ItemIdentityHeaderBinding
import com.inkhabits.databinding.ItemSectionHeaderBinding
import com.inkhabits.util.StrokeRenderer

class DashboardAdapter(
    private val onToggle: (habitId: Long, makeComplete: Boolean) -> Unit,
    private val onAddIdentity: () -> Unit,
    private val onEditIdentity: (identityId: Long) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<DashboardItem> = emptyList()

    fun submit(newItems: List<DashboardItem>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(o: Int, n: Int): Boolean {
                val a = items[o]; val b = newItems[n]
                return when {
                    a is DashboardItem.Header && b is DashboardItem.Header -> a.identity.id == b.identity.id
                    a is DashboardItem.SectionHeader && b is DashboardItem.SectionHeader -> a.title == b.title
                    a is DashboardItem.HabitRow && b is DashboardItem.HabitRow -> a.habit.id == b.habit.id
                    a is DashboardItem.AddFooter && b is DashboardItem.AddFooter -> true
                    else -> false
                }
            }
            override fun areContentsTheSame(o: Int, n: Int) = items[o] == newItems[n]
        })
        items = newItems
        diff.dispatchUpdatesTo(this)
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is DashboardItem.Header -> TYPE_HEADER
        is DashboardItem.SectionHeader -> TYPE_SECTION
        is DashboardItem.HabitRow -> TYPE_HABIT
        is DashboardItem.AddFooter -> TYPE_FOOTER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderVH(ItemIdentityHeaderBinding.inflate(inflater, parent, false))
            TYPE_SECTION -> SectionVH(ItemSectionHeaderBinding.inflate(inflater, parent, false))
            TYPE_FOOTER -> FooterVH(ItemAddFooterBinding.inflate(inflater, parent, false))
            else -> HabitVH(ItemHabitBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is DashboardItem.Header -> (holder as HeaderVH).bind(item)
            is DashboardItem.SectionHeader -> (holder as SectionVH).bind(item)
            is DashboardItem.HabitRow -> (holder as HabitVH).bind(item)
            is DashboardItem.AddFooter -> (holder as FooterVH).bind()
        }
    }

    override fun getItemCount(): Int = items.size

    inner class HeaderVH(private val b: ItemIdentityHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: DashboardItem.Header) {
            val id = item.identity
            b.root.setOnClickListener { onEditIdentity(id.id) }
            b.identityIcon.setImageResource(com.inkhabits.ui.widget.HabitIcons.resFor(id.icon))
            if (StrokeRenderer.hasInk(id.nameStrokes)) {
                b.identityName.visibility = View.GONE
                b.identityNameInk.visibility = View.VISIBLE
                b.identityNameInk.post {
                    val h = b.identityNameInk.height.takeIf { it > 0 } ?: 84
                    // Render at a width proportional to the ink so the trailing rule sits flush.
                    val w = (h * 4).coerceAtLeast(120)
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
            b.pillIcon.setImageResource(com.inkhabits.ui.widget.HabitIcons.resFor(item.identityIcon))
            b.habitName.setContent(h.name, h.nameStrokes)
            b.habitName.completed = item.completedToday
            b.scheduleLabel.text = item.scheduleLabel

            // Anchor cue above the habit: handwritten ink, or gray text, or nothing.
            when {
                StrokeRenderer.hasInk(item.anchorStrokes) -> {
                    b.anchorLabel.visibility = View.GONE
                    b.anchorInk.visibility = View.VISIBLE
                    b.anchorInk.post {
                        val h = b.anchorInk.height.takeIf { it > 0 } ?: 56
                        b.anchorInk.setImageBitmap(
                            StrokeRenderer.renderToBitmap(item.anchorStrokes, h * 6, h))
                    }
                }
                item.anchor.isNotBlank() -> {
                    b.anchorInk.visibility = View.GONE
                    b.anchorLabel.visibility = View.VISIBLE
                    b.anchorLabel.text = "after ${item.anchor}"
                }
                else -> {
                    b.anchorLabel.visibility = View.GONE
                    b.anchorInk.visibility = View.GONE
                }
            }

            b.streakText.text = if (item.streak > 0) "🔥${item.streak}" else ""

            b.checkBox.onToggle = null
            b.checkBox.checked = item.completedToday
            b.checkBox.onToggle = { makeComplete -> onToggle(h.id, makeComplete) }

            b.habitName.onStrike = { onToggle(h.id, !item.completedToday) }

            b.pillCard.setOnClickListener { onToggle(h.id, !item.completedToday) }
        }
    }

    inner class SectionVH(private val b: ItemSectionHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: DashboardItem.SectionHeader) {
            b.sectionTitle.text = item.title.uppercase()
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
        private const val TYPE_SECTION = 3
    }
}
