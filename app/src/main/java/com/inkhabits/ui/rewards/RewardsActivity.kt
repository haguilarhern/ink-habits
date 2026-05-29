package com.inkhabits.ui.rewards

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.inkhabits.data.AppDatabase
import com.inkhabits.data.entity.Reward
import com.inkhabits.databinding.ActivityRewardsBinding
import com.inkhabits.ui.widget.InputField
import com.inkhabits.ui.writing.WritingHostActivity
import com.inkhabits.util.InkRecognizer
import com.inkhabits.util.Rewards
import com.inkhabits.util.StrokeRenderer
import kotlinx.coroutines.launch

/**
 * Gamification tab: a ladder of self-rewards. Each reward unlocks when any habit
 * reaches its target streak (the user is notified). Reinforces the habit loop.
 */
class RewardsActivity : WritingHostActivity() {

    private lateinit var binding: ActivityRewardsBinding
    private lateinit var db: AppDatabase

    private val milestones = listOf(3, 7, 14, 21, 30, 60, 90, 180, 365)
    private var targetIdx = 1 // default 7
    private var rewardInput: InputField? = null
    private var targetLabel: TextView? = null
    private var maxStreak = 0
    private var rewards: List<Reward> = emptyList()

    private val accent = Color.parseColor("#8C1D1D")
    private val muted = Color.parseColor("#6B6B6B")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.get(this)
        binding.backButton.setOnClickListener { finish() }
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            maxStreak = Rewards.maxHabitStreak(db)
            rewards = db.rewardDao().getAll().sortedWith(compareBy({ it.unlocked }, { it.targetStreak }))
            render()
        }
    }

    private fun render() {
        val c = binding.content
        c.removeAllViews()

        c.addView(TextView(this).apply {
            text = "Longest active streak: $maxStreak " + if (maxStreak == 1) "day" else "days"
            setTextColor(muted)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, 0, 0, dp(8))
        })

        rewards.forEach { c.addView(rewardRow(it)) }

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

        // Streak target stepper
        c.addView(label("Unlock when a habit reaches"))
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
            text = if (r.unlocked) "Unlocked ✓" else "🔒 ${r.targetStreak}-day streak"
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
        lifecycleScope.launch {
            val title = if (text.isNotBlank()) text
                else if (StrokeRenderer.hasInk(strokes)) InkRecognizer.recognize(strokes).orEmpty()
                else ""
            val order = db.rewardDao().getAll().size
            db.rewardDao().insert(Reward(
                title = title, titleStrokes = strokes, targetStreak = target,
                unlocked = target <= maxStreak,
                unlockedAt = if (target <= maxStreak) System.currentTimeMillis() else 0,
                sortOrder = order))
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

    private fun stepBtn(label: String, onClick: () -> Unit): Button = Button(this).apply {
        text = label; isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        minWidth = 0; minimumWidth = dp(48)
        setOnClickListener { onClick() }
    }

    private fun toast(m: String) = android.widget.Toast.makeText(this, m, android.widget.Toast.LENGTH_SHORT).show()

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
