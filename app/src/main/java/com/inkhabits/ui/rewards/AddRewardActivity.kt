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
import com.inkhabits.databinding.ActivityAddRewardBinding
import com.inkhabits.ui.widget.InputField
import com.inkhabits.ui.writing.WritingHostActivity
import com.inkhabits.util.InkRecognizer
import com.inkhabits.util.Rewards
import com.inkhabits.util.StrokeRenderer
import kotlinx.coroutines.launch

/**
 * Standalone screen for adding a new reward. Launched from the Rewards FAB.
 */
class AddRewardActivity : WritingHostActivity() {

    private lateinit var binding: ActivityAddRewardBinding
    private lateinit var db: AppDatabase

    private val milestones = listOf(3, 7, 14, 21, 30, 60, 90, 180, 365)
    private var targetIdx = 1 // default 7
    private var rewardInput: InputField? = null
    private var targetLabel: TextView? = null

    private var identities: List<IdentityGoal> = emptyList()
    private var habits: List<Habit> = emptyList()

    private enum class TargetKind { HABIT, IDENTITY }
    private var targetKind = TargetKind.HABIT
    private var selHabit = 0
    private var selIdentity = 0
    private var targetSpinner: Spinner? = null
    private var segHabitText: TextView? = null
    private var segHabitLine: View? = null
    private var segIdText: TextView? = null
    private var segIdLine: View? = null

    private val accent = Color.parseColor("#8C1D1D")
    private val muted = Color.parseColor("#6B6B6B")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddRewardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.get(this)
        binding.backButton.setOnClickListener { finish() }
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            identities = db.identityGoalDao().getAll()
            habits = db.habitDao().getActive()
            render()
        }
    }

    private fun identityName(id: IdentityGoal): String = id.name.ifBlank { "Identity ${id.id}" }
    private fun habitName(h: Habit): String = h.name.ifBlank { "Habit ${h.id}" }

    private fun render() {
        val c = binding.content
        c.removeAllViews()

        c.addView(label("Your reward"))
        val input = InputField(this).apply {
            setHint("e.g. movie night")
            onRequestWrite = { existing, onResult -> openWritingPad(existing, "Your reward") { onResult(it) } }
        }
        rewardInput = input
        c.addView(input)

        c.addView(label("Track the streak of"))
        c.addView(kindTabs())
        val spinner = Spinner(this)
        targetSpinner = spinner
        c.addView(spinner)
        updateKindUi()

        c.addView(label("Unlock when it reaches"))
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(stepBtn("−") { if (targetIdx > 0) { targetIdx--; refreshTarget() } })
        val tl = TextView(this).apply {
            setTextColor(Color.parseColor("#1A1A1A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = androidx.core.content.res.ResourcesCompat.getFont(this@AddRewardActivity, com.inkhabits.R.font.inter_semibold)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(150), LinearLayout.LayoutParams.WRAP_CONTENT)
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

    private fun kindTabs(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(4) }
        }
        fun seg(text: String, kind: TargetKind): View {
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                isClickable = true
                setOnClickListener { if (targetKind != kind) { targetKind = kind; updateKindUi() } }
            }
            val tv = TextView(this).apply {
                this.text = text
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, dp(7))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                typeface = androidx.core.content.res.ResourcesCompat.getFont(
                    this@AddRewardActivity, com.inkhabits.R.font.inter_semibold)
            }
            val line = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(2))
            }
            box.addView(tv); box.addView(line)
            if (kind == TargetKind.HABIT) { segHabitText = tv; segHabitLine = line }
            else { segIdText = tv; segIdLine = line }
            return box
        }
        row.addView(seg("Habit", TargetKind.HABIT))
        row.addView(seg("Identity", TargetKind.IDENTITY))
        return row
    }

    private fun updateKindUi() {
        styleSeg(segHabitText, segHabitLine, targetKind == TargetKind.HABIT)
        styleSeg(segIdText, segIdLine, targetKind == TargetKind.IDENTITY)
        updateTargetSpinner()
    }

    private fun styleSeg(tv: TextView?, line: View?, active: Boolean) {
        tv?.setTextColor(if (active) accent else muted)
        line?.setBackgroundColor(if (active) accent else Color.TRANSPARENT)
    }

    private fun updateTargetSpinner() {
        val sp = targetSpinner ?: return
        val labels = if (targetKind == TargetKind.HABIT)
            listOf("Any habit") + habits.map { habitName(it) }
        else identities.map { identityName(it) }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sp.adapter = adapter
        if (labels.isNotEmpty()) {
            val sel = (if (targetKind == TargetKind.HABIT) selHabit else selIdentity)
                .coerceIn(0, labels.size - 1)
            sp.setSelection(sel)
        }
        sp.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, idd: Long) {
                if (targetKind == TargetKind.HABIT) selHabit = pos else selIdentity = pos
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }
    }

    private fun refreshTarget() {
        targetLabel?.text = "${milestones[targetIdx]}-day streak"
    }

    private fun addReward() {
        val input = rewardInput ?: return
        if (!input.hasContent()) { toast("Write or type the reward first"); return }
        val text = input.getText(); val strokes = input.getStrokes()
        val target = milestones[targetIdx]

        var habitId = 0L
        var identityId = 0L
        if (targetKind == TargetKind.HABIT) {
            if (selHabit > 0) habitId = habits.getOrNull(selHabit - 1)?.id ?: 0L
        } else {
            identityId = identities.getOrNull(selIdentity)?.id ?: 0L
            if (identityId == 0L) { toast("Create an identity first"); return }
        }

        lifecycleScope.launch {
            val title = if (text.isNotBlank()) text
                else if (StrokeRenderer.hasInk(strokes)) InkRecognizer.recognize(strokes).orEmpty()
                else ""
            val order = db.rewardDao().getAll().size
            val draft = Reward(
                title = title, titleStrokes = strokes, targetStreak = target,
                habitId = habitId, identityId = identityId, sortOrder = order)
            val already = Rewards.streakFor(db, draft) >= target
            db.rewardDao().insert(draft.copy(
                unlocked = already,
                unlockedAt = if (already) System.currentTimeMillis() else 0))
            finish()
        }
    }

    private fun label(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextColor(Color.parseColor("#5A5A5A"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        typeface = androidx.core.content.res.ResourcesCompat.getFont(this@AddRewardActivity, com.inkhabits.R.font.inter_semibold)
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
