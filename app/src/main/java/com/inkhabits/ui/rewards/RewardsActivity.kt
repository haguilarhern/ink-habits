package com.inkhabits.ui.rewards

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.inkhabits.data.AppDatabase
import com.inkhabits.data.entity.Habit
import com.inkhabits.data.entity.IdentityGoal
import com.inkhabits.data.entity.Reward
import com.inkhabits.databinding.ActivityRewardsBinding
import com.inkhabits.ui.writing.WritingHostActivity
import com.inkhabits.util.StrokeRenderer
import kotlinx.coroutines.launch

/**
 * Gamification tab: a ladder of self-rewards. Each reward unlocks when its target
 * streak is reached. Saved rewards are stacked under identity headers, like the home screen.
 * The "+" FAB opens AddRewardActivity in its own screen.
 */
class RewardsActivity : WritingHostActivity() {

    private lateinit var binding: ActivityRewardsBinding
    private lateinit var db: AppDatabase

    private var rewards: List<Reward> = emptyList()
    private var identities: List<IdentityGoal> = emptyList()
    private var habits: List<Habit> = emptyList()

    private val accent = Color.parseColor("#8C1D1D")
    private val muted = Color.parseColor("#6B6B6B")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.get(this)
        binding.backButton.setOnClickListener { finish() }
        binding.fabAddReward.setOnClickListener {
            startActivity(Intent(this, AddRewardActivity::class.java))
        }
        (binding.content.parent as? android.widget.ScrollView)?.let {
            com.inkhabits.eink.EInk.attachFastScroll(it)
        }
        load()
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            identities = db.identityGoalDao().getAll()
            habits = db.habitDao().getActive()
            rewards = db.rewardDao().getAll()
                .sortedWith(compareBy({ it.unlocked }, { it.targetStreak }))
            render()
        }
    }

    private fun identityName(id: IdentityGoal): String = id.name.ifBlank { "Identity ${id.id}" }
    private fun habitName(h: Habit): String = h.name.ifBlank { "Habit ${h.id}" }

    /** The identity a reward belongs to, for grouping (0 = none / any habit). */
    private fun rewardIdentityId(r: Reward): Long = when {
        r.identityId > 0 -> r.identityId
        r.habitId > 0 -> habits.find { it.id == r.habitId }?.identityGoalId ?: 0L
        else -> 0L
    }

    private fun render() {
        val c = binding.content
        c.removeAllViews()

        // --- Saved rewards, stacked under identity headers (like home) ---
        if (rewards.isEmpty()) {
            c.addView(emptyNote("No rewards yet. Tap + to promise yourself one."))
        } else {
            for (identity in identities) {
                val group = rewards.filter { rewardIdentityId(it) == identity.id }
                if (group.isEmpty()) continue
                c.addView(sectionHeader(identityName(identity)))
                group.forEach { c.addView(rewardRow(it)) }
            }
            val general = rewards.filter { rewardIdentityId(it) == 0L }
            if (general.isNotEmpty()) {
                c.addView(sectionHeader("Any habit"))
                general.forEach { c.addView(rewardRow(it)) }
            }
        }
    }

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
        if (r.title.isNotBlank() || !StrokeRenderer.hasInk(r.titleStrokes)) {
            texts.addView(TextView(this).apply {
                text = r.title.ifBlank { "Reward" }
                setTextColor(Color.parseColor("#1A1A1A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            })
        } else {
            texts.addView(ImageView(this).apply {
                scaleType = ImageView.ScaleType.FIT_START
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(26))
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
        row.addView(android.widget.Button(this).apply {
            text = "✕"; isAllCaps = false
            setTextColor(accent)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { deleteReward(r) }
        })
        return row
    }

    private fun deleteReward(r: Reward) {
        lifecycleScope.launch { db.rewardDao().delete(r); load() }
    }

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

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
