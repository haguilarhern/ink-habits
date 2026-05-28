package com.inkhabits.ui.history

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.inkhabits.data.AppDatabase
import com.inkhabits.data.entity.Habit
import com.inkhabits.databinding.ActivityHistoryBinding
import com.inkhabits.eink.EInkActivity
import com.inkhabits.util.Schedule
import com.inkhabits.util.Streaks
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HistoryItem(
    val habit: Habit,
    val current: Int,
    val best: Int,
    val states: IntArray,
    val label: String
)

class HistoryActivity : EInkActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var db: AppDatabase
    private val adapter = HistoryAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.get(this)

        binding.backButton.setOnClickListener { finish() }
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter
        binding.list.itemAnimator = null

        val today = LocalDate.now()
        lifecycleScope.launch {
            combine(
                db.habitDao().observeActive(),
                db.habitCompletionDao().observeAll()
            ) { habits, completions ->
                val byHabit = completions.groupBy { it.habitId }
                    .mapValues { e -> e.value.map { it.date }.toSet() }
                habits.map { habit ->
                    val completed = byHabit[habit.id] ?: emptySet()
                    HistoryItem(
                        habit = habit,
                        current = Streaks.computeStreak(habit, completed, today),
                        best = Streaks.bestStreak(habit, completed, today),
                        states = Streaks.dayStates(habit, completed, today, 35),
                        label = Schedule.label(habit)
                    )
                }
            }.collect { items ->
                adapter.submit(items)
                binding.emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
}
