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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

class DashboardActivity : EInkActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var db: AppDatabase
    private lateinit var adapter: DashboardAdapter

    /** How the home list is ordered. */
    private enum class SortMode { IDENTITY, TIME }
    private var sortMode = SortMode.IDENTITY

    /** Latest observed data, kept so re-sorting doesn't require a re-query. */
    private data class Snapshot(
        val identities: List<com.inkhabits.data.entity.IdentityGoal>,
        val habits: List<com.inkhabits.data.entity.Habit>,
        val completedByHabit: Map<Long, Set<String>>,
        val perfect: Int
    )
    private var snapshot: Snapshot? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.get(this)

        binding.dateTitle.text = LocalDate.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d"))

        binding.quoteCard.setOnClickListener {
            startActivity(Intent(this, com.inkhabits.ui.quote.QuoteEditActivity::class.java))
        }

        adapter = DashboardAdapter(
            onToggle = { habitId, makeComplete -> toggle(habitId, makeComplete) },
            onAddIdentity = { openAddIdentity() },
            onEditIdentity = { identityId -> openEditIdentity(identityId) }
        )
        binding.habitList.layoutManager = LinearLayoutManager(this)
        binding.habitList.adapter = adapter
        binding.habitList.itemAnimator = null
        binding.habitList.setHasFixedSize(true)
        binding.habitList.setItemViewCacheSize(12)
        com.inkhabits.eink.EInk.attachFastScroll(binding.habitList)

        binding.navRecords.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        binding.navTodo.setOnClickListener {
            startActivity(Intent(this, ToDoActivity::class.java))
        }
        binding.navRewards.setOnClickListener {
            startActivity(Intent(this, com.inkhabits.ui.rewards.RewardsActivity::class.java))
        }
        binding.navHome.setOnClickListener {
            binding.habitList.smoothScrollToPosition(0)
        }

        sortMode = loadSortMode()
        updateSortLabel()
        binding.sortToggle.setOnClickListener {
            sortMode = if (sortMode == SortMode.IDENTITY) SortMode.TIME else SortMode.IDENTITY
            saveSortMode()
            updateSortLabel()
            renderList()
        }

        binding.fabAdd.setOnClickListener { showAddMenu() }

        renderQuote()
        maybeRequestNotificationPermission()
        observe()
    }

    override fun onResume() {
        super.onResume()
        renderQuote() // reflect a quote the user may have just edited
    }

    private fun renderQuote() {
        val text = com.inkhabits.util.QuotePrefs.text(this)
        val strokes = com.inkhabits.util.QuotePrefs.strokes(this)
        val hasInk = com.inkhabits.util.StrokeRenderer.hasInk(strokes)
        val prefHand = com.inkhabits.util.QuotePrefs.preferHandwritten(this)
        // Typed text by default; show handwriting if preferred (or if there's no text).
        val showInk = hasInk && (prefHand || text.isBlank())

        if (showInk) {
            binding.quoteText.visibility = View.GONE
            binding.quoteInk.visibility = View.VISIBLE
            binding.quoteInk.post {
                val w = binding.quoteInk.width.takeIf { it > 0 } ?: 600
                val h = binding.quoteInk.height.takeIf { it > 0 } ?: 150
                binding.quoteInk.setImageBitmap(
                    com.inkhabits.util.StrokeRenderer.renderToBitmap(strokes, w, h))
            }
        } else {
            binding.quoteInk.visibility = View.GONE
            binding.quoteText.visibility = View.VISIBLE
            binding.quoteText.text =
                if (text.isNotBlank()) text else Quotes.forToday(LocalDate.now())
        }

        // Offer the typed<->handwritten toggle only when both forms exist.
        if (text.isNotBlank() && hasInk) {
            binding.quoteToggle.visibility = View.VISIBLE
            binding.quoteToggle.text = if (showInk) "Aa typed" else "✍ handwritten"
            binding.quoteToggle.setOnClickListener {
                com.inkhabits.util.QuotePrefs.setPreferHandwritten(this, !showInk)
                renderQuote()
            }
        } else {
            binding.quoteToggle.visibility = View.GONE
        }
    }

    private fun openAddIdentity() {
        startActivity(Intent(this, OnboardingActivity::class.java)
            .putExtra(OnboardingActivity.EXTRA_ADD_IDENTITY, true))
    }

    private fun openEditIdentity(identityId: Long) {
        startActivity(Intent(this, OnboardingActivity::class.java)
            .putExtra(OnboardingActivity.EXTRA_EDIT_IDENTITY, identityId))
    }

    private fun showAddMenu() {
        val popup = android.widget.PopupMenu(this, binding.fabAdd)
        popup.menu.add(0, 1, 0, "Add identity")
        popup.menu.add(0, 2, 1, "Add habit")
        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                1 -> { openAddIdentity(); true }
                2 -> { openAddHabit(); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun openAddHabit() {
        startActivity(Intent(this, OnboardingActivity::class.java)
            .putExtra(OnboardingActivity.EXTRA_ADD_HABIT, true))
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
        // Synchronous first paint: build the initial snapshot before the first frame so the
        // habit list arrives populated in one e-ink refresh instead of blank-then-filled.
        // DB is tiny/local (~ms). renderList() uses DiffUtil so the later live emission of
        // identical data is a no-op.
        runBlocking {
            snapshot = buildSnapshot(
                db.identityGoalDao().getAll(),
                db.habitDao().getActive(),
                db.habitCompletionDao().getAll(),
                db.streakFreezeDao().getAll()
            )
        }
        renderList()

        lifecycleScope.launch {
            combine(
                db.identityGoalDao().observeAll(),
                db.habitDao().observeActive(),
                db.habitCompletionDao().observeAll(),
                db.streakFreezeDao().observeAll()
            ) { identities, habits, completions, freezes ->
                buildSnapshot(identities, habits, completions, freezes)
            }.drop(1).collect { snap ->
                snapshot = snap
                renderList()
            }
        }
    }

    /** Builds a [Snapshot], merging habit-totem freezes into completions so a frozen day
     *  counts as done for streaks (today is never frozen, so checkbox state stays accurate). */
    private fun buildSnapshot(
        identities: List<com.inkhabits.data.entity.IdentityGoal>,
        habits: List<com.inkhabits.data.entity.Habit>,
        completions: List<HabitCompletion>,
        freezes: List<com.inkhabits.data.entity.StreakFreeze>
    ): Snapshot {
        val frozenByHabit = freezes.filter { it.habitId > 0 }
            .groupBy { it.habitId }.mapValues { e -> e.value.map { it.date }.toSet() }
        val completedByHabit = completions.groupBy { it.habitId }
            .mapValues { e -> e.value.map { it.date }.toSet() }
            .toMutableMap().apply {
                frozenByHabit.forEach { (id, dates) -> this[id] = (this[id] ?: emptySet()) + dates }
            }
        val perfect = Streaks.perfectDayStreak(habits, completedByHabit, LocalDate.now())
        return Snapshot(identities, habits, completedByHabit, perfect)
    }

    /** Rebuilds the list from the latest snapshot using the current sort mode. */
    private fun renderList() {
        val snap = snapshot ?: return
        val today = LocalDate.now()
        val todayStr = today.toString()
        binding.streakNumber.text = snap.perfect.toString()

        val items = if (sortMode == SortMode.TIME) buildByTime(snap, today, todayStr)
        else buildByIdentity(snap, today, todayStr)
        items.add(DashboardItem.AddFooter)

        adapter.submit(items)
        binding.emptyState.visibility = if (items.size <= 1) View.VISIBLE else View.GONE
        // Avoid a full-screen GC flash on every change; clean only periodically.
        binding.habitList.post { com.inkhabits.eink.EInk.afterChange(binding.habitList) }
    }

    private fun rowFor(
        habit: com.inkhabits.data.entity.Habit,
        identityIcon: String,
        today: LocalDate,
        todayStr: String,
        completedByHabit: Map<Long, Set<String>>
    ): DashboardItem.HabitRow {
        val completed = completedByHabit[habit.id] ?: emptySet()
        // Show only the time of day (specific or broad) — not the frequency/days.
        val label = Schedule.formatTime(habit.reminderMinutes).ifEmpty { "Any time" }
        return DashboardItem.HabitRow(
            habit = habit,
            identityIcon = identityIcon,
            completedToday = todayStr in completed,
            streak = Streaks.computeStreak(habit, completed, today),
            scheduleLabel = label,
            anchor = habit.anchor,
            anchorStrokes = habit.anchorStrokes
        )
    }

    /** Default order: grouped under each identity. */
    private fun buildByIdentity(snap: Snapshot, today: LocalDate, todayStr: String): MutableList<DashboardItem> {
        val items = mutableListOf<DashboardItem>()
        for (identity in snap.identities) {
            val due = snap.habits.filter {
                it.identityGoalId == identity.id && Schedule.isDueOn(it, today)
            }
            if (due.isEmpty()) continue
            items.add(DashboardItem.Header(identity))
            for (habit in due) items.add(rowFor(habit, identity.icon, today, todayStr, snap.completedByHabit))
        }
        return items
    }

    /** Chronological order: grouped under Morning / Afternoon / Evening / Anytime. */
    private fun buildByTime(snap: Snapshot, today: LocalDate, todayStr: String): MutableList<DashboardItem> {
        val iconById = snap.identities.associate { it.id to it.icon }
        val due = snap.habits.filter { Schedule.isDueOn(it, today) }
            .sortedBy { Schedule.timeKey(it.reminderMinutes) }
        val items = mutableListOf<DashboardItem>()
        var section: String? = null
        for (habit in due) {
            val s = Schedule.timeSection(habit.reminderMinutes)
            if (s != section) { items.add(DashboardItem.SectionHeader(s)); section = s }
            items.add(rowFor(habit, iconById[habit.identityGoalId] ?: "star", today, todayStr, snap.completedByHabit))
        }
        return items
    }

    private fun updateSortLabel() {
        binding.sortToggle.text = if (sortMode == SortMode.TIME) "◷  By time" else "≡  By identity"
    }

    private fun loadSortMode(): SortMode =
        if (getSharedPreferences("dashboard", MODE_PRIVATE).getString("sort", "IDENTITY") == "TIME")
            SortMode.TIME else SortMode.IDENTITY

    private fun saveSortMode() {
        getSharedPreferences("dashboard", MODE_PRIVATE).edit().putString("sort", sortMode.name).apply()
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
            // Gamification: completing may push a habit over a reward's streak target.
            if (makeComplete) com.inkhabits.util.Rewards.checkAndUnlock(this@DashboardActivity)
        }
    }
}
