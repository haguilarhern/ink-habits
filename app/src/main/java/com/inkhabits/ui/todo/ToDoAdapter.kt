package com.inkhabits.ui.todo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.inkhabits.data.entity.ToDo
import com.inkhabits.databinding.ItemTodoBinding

class ToDoAdapter(
    private val onToggle: (ToDo, Boolean) -> Unit,
    private val onDelete: (ToDo) -> Unit
) : RecyclerView.Adapter<ToDoAdapter.VH>() {

    private var items: List<ToDo> = emptyList()

    fun submit(newItems: List<ToDo>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemTodoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    inner class VH(private val b: ItemTodoBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(todo: ToDo) {
            b.todoName.setContent(todo.title, todo.titleStrokes)
            b.todoName.completed = todo.isDone

            b.checkBox.onToggle = null
            b.checkBox.checked = todo.isDone
            b.checkBox.onToggle = { done -> onToggle(todo, done) }

            b.todoName.onStrike = { onToggle(todo, !todo.isDone) }
            b.deleteButton.setOnClickListener { onDelete(todo) }
        }
    }
}
