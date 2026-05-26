package com.boox.atomic.habits.ui.dashboard

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.boox.atomic.habits.R
import com.boox.atomic.habits.boox.EInkUtils
import com.boox.atomic.habits.boox.StrokeRenderer
import com.boox.atomic.habits.data.AppDatabase
import com.boox.atomic.habits.ui.widget.HabitCalendarView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewHolder for a single habit check-in row.
 *
 * Renders handwriting strokes + shows a compact month calendar heatmap
 * below the habit name. Calendar updates with completion data from DB.
 */
class HabitCheckInViewHolder(
    itemView: View,
    private val onCheckIn: (habitId: Long, isCompleted: Boolean) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    private val habitCheckbox: CheckBox = itemView.findViewById(R.id.habitCheckbox)
    private val habitName: TextView = itemView.findViewById(R.id.habitName)
    private val frequencyHint: TextView = itemView.findViewById(R.id.frequencyHint)
    private val streakBadge: TextView = itemView.findViewById(R.id.streakBadge)

    private var currentHabitId: Long = 0L
    private var currentStrokeData: String? = null
    private var currentIsCompleted: Boolean = false
    private var currentDateStr: String = ""
    private var dbRef: AppDatabase? = null
    private var scope: CoroutineScope? = null
    private var calendarView: HabitCalendarView? = null
    private val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    init {
        try { EInkUtils.setGeneralMode(itemView) } catch (_: Exception) {}
    }

    fun bind(
        habitId: Long,
        name: String,
        strokeData: String?,
        isCompleted: Boolean,
        frequencyType: String,
        intervalDays: Int,
        daysOfWeek: String,
        streak: Int,
        dateStr: String = "",
        db: AppDatabase? = null,
        scope: CoroutineScope? = null
    ) {
        currentHabitId = habitId
        currentStrokeData = strokeData
        currentIsCompleted = isCompleted
        currentDateStr = dateStr
        dbRef = db
        this.scope = scope

        // Render handwriting strokes or fallback text
        if (!strokeData.isNullOrBlank()) {
            habitName.text = ""
            habitName.post { drawStrokesOnTextView(habitName, strokeData, isCompleted) }
            habitName.visibility = View.VISIBLE
        } else {
            habitName.text = name
            habitName.visibility = View.VISIBLE
        }

        habitCheckbox.isChecked = isCompleted
        streakBadge.text = if (streak > 0) "$streak" else ""

        // Frequency hint
        when (frequencyType) {
            "weekly" -> { frequencyHint.text = "(weekly)"; frequencyHint.visibility = View.VISIBLE }
            "interval" -> { frequencyHint.text = "(every $intervalDays d)"; frequencyHint.visibility = View.VISIBLE }
            "days_of_week" -> {
                val days = parseDaysOfWeek(daysOfWeek)
                if (days.isNotEmpty()) { frequencyHint.text = "($days)"; frequencyHint.visibility = View.VISIBLE }
                else frequencyHint.visibility = View.GONE
            }
            else -> frequencyHint.visibility = View.GONE
        }

        habitCheckbox.setOnCheckedChangeListener(null)
        habitCheckbox.isChecked = isCompleted
        habitCheckbox.setOnCheckedChangeListener { _, isChecked ->
            currentIsCompleted = isChecked
            onCheckIn(habitId, isChecked)
            if (!currentStrokeData.isNullOrBlank()) {
                habitName.post { drawStrokesOnTextView(habitName, currentStrokeData!!, isChecked) }
            }
        }

        // Load calendar heatmap for this habit
        loadCalendar()
    }

    private fun loadCalendar() {
        if (dbRef == null || scope == null || currentHabitId == 0L) return

        // Use the selected date from the dashboard
        val cal = if (currentDateStr.isNotBlank()) {
            try {
                val d = df.parse(currentDateStr) ?: Date()
                Calendar.getInstance().apply { time = d }
            } catch (_: Exception) { Calendar.getInstance() }
        } else {
            Calendar.getInstance()
        }

        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)

        // Calculate month start/end
        val monthStart = Calendar.getInstance().apply { set(year, month, 1) }
        val monthEnd = Calendar.getInstance().apply {
            set(year, month, 1)
            add(Calendar.MONTH, 1)
            add(Calendar.DAY_OF_MONTH, -1)
        }

        val startStr = df.format(monthStart.time)
        val endStr = df.format(monthEnd.time)

        scope?.launch {
            val dates = dbRef!!.habitCompletionDao().getCompletionsInRange(currentHabitId, startStr, endStr)

            // Find or create calendar view
            var calView = calendarView
            if (calView == null) {
                calView = HabitCalendarView(itemView.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(24, 4, 8, 4)
                }
                calendarView = calView

                // Add calendar below the existing content in the itemView
                // itemView is a LinearLayout — add calendar as a child
                if (itemView is LinearLayout) {
                    itemView.addView(calView)
                } else if (itemView is ViewGroup) {
                    itemView.addView(calView)
                }
            }

            calView.setDisplayMonth(year, month)
            calView.setCompletedDates(dates)
            calView.visibility = View.VISIBLE
        }
    }

    private fun drawStrokesOnTextView(
        textView: TextView,
        strokeData: String,
        isChecked: Boolean
    ) {
        val bitmap = Bitmap.createBitmap(
            maxOf(textView.width, 1),
            maxOf(textView.height, 1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        val bounds = StrokeRenderer.measureStrokes(strokeData)
        if (!bounds.isEmpty) {
            val offsetX = 4f
            val offsetY = (textView.height - bounds.height()) / 2f - bounds.top + 4f
            StrokeRenderer.drawStrokes(canvas, strokeData, offsetX, offsetY)
            if (isChecked) {
                StrokeRenderer.drawStrikethrough(canvas, strokeData, offsetX, offsetY)
            }
        }

        val drawable = BitmapDrawable(textView.context.resources, bitmap)
        textView.background = drawable
        textView.minimumHeight = bitmap.height
    }

    private fun parseDaysOfWeek(daysOfWeek: String): String {
        if (daysOfWeek.isBlank()) return ""
        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        return daysOfWeek.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 0..6 }
            .map { dayNames[it] }
            .joinToString("/")
    }
}