package com.inkhabits.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.inkhabits.databinding.ItemHistoryBinding
import com.inkhabits.util.StrokeRenderer

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.VH>() {

    private var items: List<HistoryItem> = emptyList()

    fun submit(newItems: List<HistoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    class VH(private val b: ItemHistoryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: HistoryItem) {
            val h = item.habit
            if (StrokeRenderer.hasInk(h.nameStrokes)) {
                b.habitNameText.visibility = View.GONE
                b.habitNameInk.visibility = View.VISIBLE
                b.habitNameInk.post {
                    val w = b.habitNameInk.width.takeIf { it > 0 } ?: 500
                    val ht = b.habitNameInk.height.takeIf { it > 0 } ?: 100
                    b.habitNameInk.setImageBitmap(StrokeRenderer.renderToBitmap(h.nameStrokes, w, ht))
                }
            } else {
                b.habitNameInk.visibility = View.GONE
                b.habitNameText.visibility = View.VISIBLE
                b.habitNameText.text = h.name.ifBlank { "Habit" }
            }
            b.statsLine.text = "Current 🔥 ${item.current}   ·   Best ★ ${item.best}   ·   ${item.label}"
            b.heatmap.setStates(item.states)
        }
    }
}
