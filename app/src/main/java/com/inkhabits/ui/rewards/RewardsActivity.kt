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
    private var auraBalance: Long = 0
    private var econ = com.inkhabits.data.entity.EconomyState()

    private val accent = Color.parseColor("#0A7D6A")
    private val muted = Color.parseColor("#5C5C5C")
    private val frozen = Color.parseColor("#5C5C5C")

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
            econ = com.inkhabits.util.Economy.state(db)
            auraBalance = com.inkhabits.util.Economy.balance(db)
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

        // --- Aura wallet + totem shop ---
        c.addView(shopCard())

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
            setColorFilter(if (r.unlocked) accent else Color.parseColor("#9A9AA0"))
            layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(12) }
        })
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        if (r.title.isNotBlank() || !StrokeRenderer.hasInk(r.titleStrokes)) {
            texts.addView(TextView(this).apply {
                text = r.title.ifBlank { "Reward" }
                setTextColor(Color.parseColor("#0B0B0C"))
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
            text = if (r.unlocked) "Unlocked · ${basisLabel(r)}"
                   else "${r.targetStreak}-day streak · ${basisLabel(r)}"
            setTextColor(if (r.unlocked) accent else muted)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            // Locked rewards get a small mono lock; unlocked get a check.
            val icon = if (r.unlocked) com.inkhabits.R.drawable.ic_check
                       else com.inkhabits.R.drawable.ic_lock
            setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
            compoundDrawablePadding = dp(5)
            compoundDrawableTintList = android.content.res.ColorStateList.valueOf(
                if (r.unlocked) accent else muted)
        })
        row.addView(texts)
        row.addView(ImageView(this).apply {
            setImageResource(com.inkhabits.R.drawable.ic_delete)
            setColorFilter(Color.parseColor("#9A9AA0"))
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener { deleteReward(r) }
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
        })
        return row
    }

    private fun deleteReward(r: Reward) {
        lifecycleScope.launch { db.rewardDao().delete(r); load() }
    }

    // ---- Aura wallet + totem shop ----

    /**
     * Wallet header (current aura + owned totems) and a small shop to buy protective
     * totems. A totem is auto-consumed to freeze a streak the day after a miss.
     */
    private fun shopCard(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getDrawable(com.inkhabits.R.drawable.pill_bg)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(6); layoutParams = lp
        }

        // Wallet hero: big serif aura balance.
        val wallet = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
        }
        wallet.addView(TextView(this).apply {
            text = auraBalance.toString()
            setTextColor(Color.parseColor("#0B0B0C"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
            includeFontPadding = false
            typeface = androidx.core.content.res.ResourcesCompat.getFont(
                this@RewardsActivity, com.inkhabits.R.font.eb_garamond)
        })
        wallet.addView(TextView(this).apply {
            text = "AURA"
            setTextColor(muted)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setLetterSpacing(0.12f)
            typeface = androidx.core.content.res.ResourcesCompat.getFont(
                this@RewardsActivity, com.inkhabits.R.font.inter_semibold)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.marginStart = dp(8); lp.bottomMargin = dp(5); layoutParams = lp
        })
        card.addView(wallet)
        card.addView(TextView(this).apply {
            text = "Earn ${com.inkhabits.util.Economy.AURA_PER_COMPLETION} per check-off · " +
                "${com.inkhabits.util.Economy.AURA_PER_PERFECT_DAY} per perfect day"
            setTextColor(muted)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, dp(4), 0, dp(8))
        })

        card.addView(divider())
        card.addView(shopRow(
            "Habit totem",
            "Protects one missed day of a habit · owned ${econ.habitTotems}",
            com.inkhabits.util.Economy.COST_HABIT_TOTEM
        ) { buy(habit = true) })
        card.addView(shopRow(
            "Identity totem",
            "Protects one missed perfect-day · owned ${econ.identityTotems}",
            com.inkhabits.util.Economy.COST_IDENTITY_TOTEM
        ) { buy(habit = false) })

        return card
    }

    private fun divider(): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
            topMargin = dp(10); bottomMargin = dp(2)
        }
        setBackgroundColor(Color.parseColor("#D9D9DE"))
    }

    /** A pill control rendered as a TextView (avoids Material Button's coloured tint). */
    private fun pillButton(label: String, filled: Boolean, onClick: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            isAllCaps = false
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            typeface = androidx.core.content.res.ResourcesCompat.getFont(
                this@RewardsActivity, com.inkhabits.R.font.inter_semibold)
            setTextColor(if (filled) Color.WHITE else Color.parseColor("#9A9AA0"))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                if (filled) setColor(Color.parseColor("#0A7D6A"))
                else { setColor(Color.WHITE); setStroke(dp(1).coerceAtLeast(1), Color.parseColor("#D1D1D6")) }
            }
            setOnClickListener { onClick() }
        }

    private fun shopRow(title: String, sub: String, cost: Int, onBuy: () -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(2))
        }
        row.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(TextView(this@RewardsActivity).apply {
                text = title
                setTextColor(frozen)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            })
            addView(TextView(this@RewardsActivity).apply {
                text = sub
                setTextColor(muted)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            })
        })
        val affordable = auraBalance >= cost
        row.addView(pillButton("$cost ✦", affordable) { if (affordable) onBuy() }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { marginStart = dp(10) }
        })
        return row
    }

    private fun buy(habit: Boolean) {
        lifecycleScope.launch {
            val ok = if (habit) com.inkhabits.util.Economy.buyHabitTotem(db)
                     else com.inkhabits.util.Economy.buyIdentityTotem(db)
            android.widget.Toast.makeText(
                this@RewardsActivity,
                if (ok) "Totem acquired" else "Not enough aura",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            if (ok) load()
        }
    }

    private fun sectionHeader(text: String): TextView = TextView(this).apply {
        this.text = text.uppercase()
        setTextColor(Color.parseColor("#0B0B0C"))
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
