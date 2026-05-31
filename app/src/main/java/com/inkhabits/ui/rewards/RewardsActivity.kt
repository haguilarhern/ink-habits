package com.inkhabits.ui.rewards

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.inkhabits.data.AppDatabase
import com.inkhabits.data.entity.Habit
import com.inkhabits.data.entity.IdentityGoal
import com.inkhabits.data.entity.Reward
import com.inkhabits.databinding.ActivityRewardsBinding
import com.inkhabits.ui.widget.InputField
import com.inkhabits.ui.writing.WritingHostActivity
import com.inkhabits.util.InkRecognizer
import com.inkhabits.util.Rewards
import com.inkhabits.util.StrokeRenderer
import kotlinx.coroutines.launch

/**
 * Gamification tab: a ladder of self-rewards. Each reward unlocks when its target
 * streak is reached — measured against any habit, a chosen habit, or an identity's
 * perfect-day streak (the user picks the basis). Reinforces the habit loop.
 */
class RewardsActivity : WritingHostActivity() {

    private lateinit var binding: ActivityRewardsBinding
    private lateinit var db: AppDatabase

    private val milestones = listOf(3, 7, 14, 21, 30, 60, 90, 180, 365)
    private var targetIdx = 1 // default 7
    private var rewardInput: InputField? = null
    private var targetLabel: TextView? = null

    private var rewards: List<Reward> = emptyList()
    private var identities: List<IdentityGoal> = emptyList()
    private var habits: List<Habit> = emptyList()

    /** Which division of rewards is shown (subtab filter). */
    private enum class RewardFilter { HABIT, IDENTITY }
    private var rewardFilter = RewardFilter.HABIT

    /** A selectable basis for a reward's streak. */
    private data class TargetOption(val label: String, val habitId: Long, val identityId: Long)
    private var targetOptions: List<TargetOption> = emptyList()
    private var selectedTarget = 0

    private val accent = Color.parseColor("#8C1D1D")
    private val muted = Color.parseColor("#6B6B6B")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.get(this)
        binding.backButton.setOnClickListener { finish() }
        (binding.content.parent as? android.widget.ScrollView)?.let {
            com.inkhabits.eink.EInk.attachFastScroll(it)
        }
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            identities = db.identityGoalDao().getAll()
            habits = db.habitDao().getActive()
            rewards = db.rewardDao().getAll()
                .sortedWith(compareBy({ it.unlocked }, { it.targetStreak }))
            buildTargetOptions()
            render()
        }
    }

    private fun buildTargetOptions() {
        val opts = mutableListOf(TargetOption("Any habit", 0, 0))
        identities.forEach { id ->
            opts.add(TargetOption("Identity · ${identityName(id)}", 0, id.id))
        }
        habits.forEach { h ->
            opts.add(TargetOption(habitName(h), h.id, 0))
        }
        targetOptions = opts
        if (selectedTarget >= opts.size) selectedTarget = 0
    }

    private fun identityName(id: IdentityGoal): String =
        id.name.ifBlank { "Identity ${id.id}" }

    private fun habitName(h: Habit): String =
        h.name.ifBlank { "Habit ${h.id}" }

    private fun render() {
        val c = binding.content
        c.removeAllViews()

        // Subtab filter: choose the habit division or the identity division.
        c.addView(filterTabs())
        val shown = if (rewardFilter == RewardFilter.IDENTITY)
            rewards.filter { it.identityId > 0L }
        else rewards.filter { it.identityId == 0L }
        if (shown.isEmpty()) {
            c.addView(emptyNote(
                if (rewardFilter == RewardFilter.IDENTITY) "No identity-based rewards yet."
                else "No habit-based rewards yet."))
        } else shown.forEach { c.addView(rewardRow(it)) }

        c.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
            setBackgroundColor(Color.parseColor("#E4E1D8"))
            (layoutParams as LinearLayout.LayoutParams).topMargin = dp(16)
            (layoutParams as LinearLayout.LayoutParams).bottomMargin = dp(8)
        })

        c.addView(label("Add a reward"))
        val input = InputField(this).apply {
            setHint("e.g. movie night")
            onRequestWrite = { existing, onResult -> openWritingPad(existing, "Your reward") { onResult(it) } }
        }
        rewardInput = input
        c.addView(input)

        // Basis: which streak unlocks it.
        c.addView(label("Track the streak of"))
        val spinner = Spinner(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item,
            targetOptions.map { it.label })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(selectedTarget)
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, idd: Long) {
                selectedTarget = pos
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }
        c.addView(spinner)

        // Streak target stepper
        c.addView(label("Unlock when it reaches"))
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(stepBtn("−") { if (targetIdx > 0) { targetIdx--; refreshTarget() } })
        val tl = TextView(this).apply {
            setTextColor(Color.parseColor("#1A1A1A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = androidx.core.content.res.ResourcesCompat.getFont(this@RewardsActivity, com.inkhabits.R.font.inter_semibold)
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(dp(150), LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutParams = lp
        }
        targetLabel = tl
        row.addView(tl)
        row.addView(stepBtn("+") { if (targetIdx < milestones.size - 1) { targetIdx++; refreshTarget() } })
        c.addView(row)
        refreshTarget()

        c.addView(MaterialButton(this).apply {
            text = "Add reward"; isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52))
            lp.topMargin = dp(14); layoutParams = lp
            setOnClickListener { addReward() }
        })
    }

    private fun refreshTarget() {
        val n = milestones[targetIdx]
        targetLabel?.text = "$n-day streak"
    }

    /** Human label for what a saved reward tracks. */
    private fun basisLabel(r: Reward): String = when {
        r.habitId > 0 -> habits.find { it.id == r.habitId }?.let { habitName(it) } ?: "Habit"
        r.identityId > 0 -> identities.find { it.id == r.identityId }?.let { identityName(it) } ?: "Identity"
        else -> "Any habit"
    }

    private fun rewardRow(r: Reward): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = getDrawable(com.inkhabits.R.drawable.pill_bg)
            setPadding(dp(14), dp(10), dp(6), dp(10))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(8); layoutParams = lp
        }
        row.addView(ImageView(this).apply {
            setImageResource(com.inkhabits.R.drawable.ic_gift)
            setColorFilter(if (r.unlocked) accent else Color.parseColor("#B8B3A8"))
            layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(12) }
        })
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        // Prize name: text if present, else an ink thumbnail.
        if (r.title.isNotBlank() || !StrokeRenderer.hasInk(r.titleStrokes)) {
            texts.addView(TextView(this).apply {
                text = r.title.ifBlank { "Reward" }
                setTextColor(Color.parseColor("#1A1A1A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            })
        } else {
            texts.addView(ImageView(this).apply {
                scaleType = ImageView.ScaleType.FIT_START
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(26))
                layoutParams = lp
                post { setImageBitmap(StrokeRenderer.renderToBitmap(r.titleStrokes, width.coerceAtLeast(1), dp(26), maxScale = 1f)) }
            })
        }
        texts.addView(TextView(this).apply {
            text = if (r.unlocked) "Unlocked ✓ · ${basisLabel(r)}"
                   else "🔒 ${r.targetStreak}-day streak · ${basisLabel(r)}"
            setTextColor(if (r.unlocked) accent else muted)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        })
        row.addView(texts)
        row.addView(Button(this).apply {
            text = "✕"; isAllCaps = false
            setTextColor(accent)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { deleteReward(r) }
        })
        return row
    }

    private fun addReward() {
        val input = rewardInput ?: return
        if (!input.hasContent()) { toast("Write or type the reward first"); return }
        val text = input.getText(); val strokes = input.getStrokes()
        val target = milestones[targetIdx]
        val basis = targetOptions.getOrElse(selectedTarget) { TargetOption("Any habit", 0, 0) }
        lifecycleScope.launch {
            val title = if (text.isNotBlank()) text
                else if (StrokeRenderer.hasInk(strokes)) InkRecognizer.recognize(strokes).orEmpty()
                else ""
            val order = db.rewardDao().getAll().size
            val draft = Reward(
                title = title, titleStrokes = strokes, targetStreak = target,
                habitId = basis.habitId, identityId = basis.identityId, sortOrder = order)
            // Unlock immediately if the chosen streak is already there.
            val already = Rewards.streakFor(db, draft) >= target
            db.rewardDao().insert(draft.copy(
                unlocked = already,
                unlockedAt = if (already) System.currentTimeMillis() else 0))
            load()
        }
    }

    private fun deleteReward(r: Reward) {
        lifecycleScope.launch { db.rewardDao().delete(r); load() }
    }

    private fun label(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextColor(Color.parseColor("#5A5A5A"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        typeface = androidx.core.content.res.ResourcesCompat.getFont(this@RewardsActivity, com.inkhabits.R.font.inter_semibold)
        setLetterSpacing(0.06f)
        setPadding(0, dp(14), 0, dp(4))
    }

    /** Bold division header (BY HABIT / BY IDENTITY). */
    private fun sectionHeader(text: String): TextView = TextView(this).apply {
        this.text = text.uppercase()
        setTextColor(Color.parseColor("#1A1A1A"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        typeface = androidx.core.content.res.ResourcesCompat.getFont(this@RewardsActivity, com.inkhabits.R.font.inter_semibold)
        setLetterSpacing(0.08f)
        setPadding(0, dp(18), 0, dp(6))
    }

    private fun emptyNote(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextColor(muted)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        setPadding(0, dp(4), 0, dp(4))
    }

    /** Two-segment subtab (By habit / By identity) with an underline on the active one. */
    private fun filterTabs(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }
        fun seg(text: String, f: RewardFilter): View {
            val active = rewardFilter == f
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                isClickable = true
                setOnClickListener { if (rewardFilter != f) { rewardFilter = f; render() } }
            }
            box.addView(TextView(this).apply {
                this.text = text
                gravity = Gravity.CENTER
                setPadding(0, dp(9), 0, dp(8))
                setTextColor(if (active) accent else muted)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                typeface = androidx.core.content.res.ResourcesCompat.getFont(
                    this@RewardsActivity, com.inkhabits.R.font.inter_semibold)
            })
            box.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(2))
                setBackgroundColor(if (active) accent else android.graphics.Color.TRANSPARENT)
            })
            return box
        }
        row.addView(seg("By habit", RewardFilter.HABIT))
        row.addView(seg("By identity", RewardFilter.IDENTITY))
        return row
    }

    private fun stepBtn(label: String, onClick: () -> Unit): Button = Button(this).apply {
        text = label; isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        minWidth = 0; minimumWidth = dp(48)
        setOnClickListener { onClick() }
    }

    private fun toast(m: String) = android.widget.Toast.makeText(this, m, android.widget.Toast.LENGTH_SHORT).show()

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
