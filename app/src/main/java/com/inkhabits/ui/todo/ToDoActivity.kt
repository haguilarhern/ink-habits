package com.inkhabits.ui.todo

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.inkhabits.data.AppDatabase
import com.inkhabits.data.entity.ToDo
import com.inkhabits.databinding.ActivityTodoBinding
import com.inkhabits.ui.writing.WritingHostActivity
import kotlinx.coroutines.launch

class ToDoActivity : WritingHostActivity() {

    private lateinit var binding: ActivityTodoBinding
    private lateinit var db: AppDatabase
    private lateinit var adapter: ToDoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTodoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.get(this)

        binding.backButton.setOnClickListener { finish() }
        binding.input.setHint("New to-do…")

        adapter = ToDoAdapter(
            onToggle = { todo, done ->
                lifecycleScope.launch {
                    db.toDoDao().update(todo.copy(isDone = done))
                    com.inkhabits.widget.WidgetCommon.updateAll(this@ToDoActivity)
                }
            },
            onDelete = { todo ->
                lifecycleScope.launch {
                    db.toDoDao().delete(todo)
                    com.inkhabits.widget.WidgetCommon.updateAll(this@ToDoActivity)
                }
            }
        )
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter
        binding.list.itemAnimator = null

        binding.addButton.setOnClickListener { addTodo() }

        lifecycleScope.launch {
            db.toDoDao().observeAll().collect { todos ->
                adapter.submit(todos)
                binding.emptyState.visibility = if (todos.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun addTodo() {
        if (!binding.input.hasContent()) {
            Toast.makeText(this, "Write or type a to-do first", Toast.LENGTH_SHORT).show()
            return
        }
        val title = binding.input.getText()
        val strokes = binding.input.getStrokes()
        lifecycleScope.launch {
            val order = db.toDoDao().getAll().size
            db.toDoDao().insert(ToDo(title = title, titleStrokes = strokes, sortOrder = order))
            com.inkhabits.widget.WidgetCommon.updateAll(this@ToDoActivity)
            binding.input.clear()
        }
    }
}
