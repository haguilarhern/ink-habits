package com.inkhabits.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.inkhabits.data.AppDatabase
import com.inkhabits.data.entity.HabitCompletion
import com.inkhabits.databinding.ActivityDashboardBinding
import com.inkhabits.eink.EInkActivity
import com.inkhabits.ui.history.HistoryActivity
import com.inkhabits.ui.onboarding.OnboardingActivity
import com.inkhabits.ui.todo.ToDoActivity
import com.inkhabits.util.Quotes
import com.inkhabits.util.Schedule
import com.inkhabits.util.Streaks
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

class DashboardActivity : EInkActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var db: AppDatabase
    private lateinit var adapter: DashboardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.get(this)

        binding.quoteText.text = Quotes.forToday(LocalDate.now())

        adapter = DashboardAdapter(
            onToggle = { habitId, makeComplete -> toggle(habitId, makeComplete) },
            onAddIdentity = { openAddIdentity() }
        )
        binding.habitList.layoutManager = LinearLayoutManager(this)
        binding.habitList.adapter = adapter
        binding.habitList.itemAnimator = null

        binding.navRecords.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        binding.navTodo.setOnClickListener {
            startActivity(Intent(this, ToDoActivity::class.java))
        }
        binding.navHome.setOnClickListener {
            binding.habitList.smoothScrollToPosition(0)
        }

        maybeRequestNotificationPermission()
        observe()
    }

    private fun openAddIdentity() {
        startActivity(Intent(this, OnboardingActivity::class.java)
            .putExtra(OnboardingActivity.EXTRA_ADD_IDENTITY, true))
    }

    private val notifPermLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }

    private fun maybeRequestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val granted = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun observe() {
        val today = LocalDate.now()
        val todayStr = today.toString()
        lifecycleScope.launch {
            combine(
                db.identityGoalDao().observeAll(),
                db.habitDao().observeActive(),
                db.habitCompletionDao().observeAll()
            ) { identities, habits, completions ->
                val completedByHabit = completions.groupBy { it.habitId }
                    .mapValues { e -> e.value.map { it.date }.toSet() }

                val perfect = Streaks.perfectDayStreak(habits, completedByHabit, today)

                val items = mutableListOf<DashboardItem>()
                for (identity in identities) {
                    val due = habits.filter {
                        it.identityGoalId == identity.id && Schedule.isDueOn(it, today)
                    }
                    if (due.isEmpty()) continue
                    items.add(DashboardItem.Header(identity))
                    for (habit in due) {
                        val completed = completedByHabit[habit.id] ?: emptySet()
                        val time = Schedule.formatTime(habit.reminderMinutes)
                        val label = if (time.isEmpty()) Schedule.label(habit)
                        else "${Schedule.label(habit)} · $time"
                        items.add(
                            DashboardItem.HabitRow(
                                habit = habit,
                                identityIcon = identity.icon,
                                completedToday = todayStr in completed,
                                streak = Streaks.computeStreak(habit, completed, today),
                                scheduleLabel = label,
                                anchor = habit.anchor
                            )
                        )
                    }
                }
                items.add(DashboardItem.AddFooter)
                Pair(perfect, items)
            }.collect { (perfect, items) ->
                binding.streakNumber.text = perfect.toString()
                adapter.submit(items)
                val onlyFooter = items.size <= 1
                binding.emptyState.visibility = if (onlyFooter) View.VISIBLE else View.GONE
                binding.habitList.post { cleanRefresh(binding.habitList) }
            }
        }
    }

    private fun toggle(habitId: Long, makeComplete: Boolean) {
        val todayStr = LocalDate.now().toString()
        lifecycleScope.launch {
            if (makeComplete) {
                db.habitCompletionDao().insert(HabitCompletion(habitId = habitId, date = todayStr))
            } else {
                db.habitCompletionDao().delete(habitId, todayStr)
            }
            com.inkhabits.widget.WidgetCommon.updateAll(this@DashboardActivity)
        }
    }
}
