package com.boox.atomic.habits.ui.dashboard

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.boox.atomic.habits.R
import com.boox.atomic.habits.boox.EInkUtils
import com.boox.atomic.habits.boox.StrokeRenderer

/**
 * ViewHolder for a single habit check-in row.
 *
 * Renders handwriting stroke data as a custom Canvas view.
 * When checked, draws a strikethrough line over the strokes.
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

    init {
        // Apply GU mode for optimal e-ink rendering
        try {
            EInkUtils.setGeneralMode(itemView)
        } catch (_: Exception) {
            // Non-Boox device
        }
    }

    fun bind(
        habitId: Long,
        name: String,
        strokeData: String?,
        isCompleted: Boolean,
        frequencyType: String,
        intervalDays: Int,
        daysOfWeek: String,
        streak: Int
    ) {
        currentHabitId = habitId
        currentStrokeData = strokeData
        currentIsCompleted = isCompleted

        // If stroke data exists, replace the TextViev habitName with a
        // Canvas-based view that renders the handwriting strokes
        if (!strokeData.isNullOrBlank()) {
            habitName.text = "" // clear text
            habitName.setLayerType(View.LAYER_TYPE_HARDWARE, null)

            // Invalidate the TextViev to trigger custom drawing
            habitName.post { drawStrokesOnTextView(habitName, strokeData, isCompleted) }

            // Override the TextViev's drawing to render strokes on its Canvas
            habitName.visibility = View.VISIBLE
        } else {
            // Fallback: show text name
            habitName.text = name
            habitName.setLayerType(View.LAYER_TYPE_NONE, null)
            habitName.visibility = View.VISIBLE
        }

        habitCheckbox.isChecked = isCompleted
        streakBadge.text = if (streak > 0) "$streak🔥" else ""

        // Show frequency hint for non-daily habits
        when (frequencyType) {
            "weekly" -> {
                frequencyHint.text = "(weekly)"
                frequencyHint.visibility = View.VISIBLE
            }
            "interval" -> {
                frequencyHint.text = "(every $intervalDays days)"
                frequencyHint.visibility = View.VISIBLE
            }
            "days_of_week" -> {
                val days = parseDaysOfWeek(daysOfWeek)
                if (days.isNotEmpty()) {
                    frequencyHint.text = "($days)"
                    frequencyHint.visibility = View.VISIBLE
                } else {
                    frequencyHint.visibility = View.GONE
                }
            }
            else -> {
                frequencyHint.visibility = View.GONE
            }
        }

        habitCheckbox.setOnCheckedChangeListener(null)
        habitCheckbox.isChecked = isCompleted
        habitCheckbox.setOnCheckedChangeListener { _, isChecked ->
            currentIsCompleted = isChecked
            onCheckIn(habitId, isChecked)
            // Redraw strokes with strikethrough if needed
            if (!currentStrokeData.isNullOrBlank()) {
                habitName.post { drawStrokesOnTextView(habitName, currentStrokeData!!, isChecked) }
            }
        }
    }

    /**
     * Draws handwriting strokes onto the TextViev's canvas, optionally
     * with a strikethrough line when completed.
     */
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

        // Draw strokes centred in the TextViev area
        val bounds = StrokeRenderer.measureStrokes(strokeData)
        if (!bounds.isEmpty) {
            val offsetX = 4f
            val offsetY = (textView.height - bounds.height()) / 2f - bounds.top + 4f

            StrokeRenderer.drawStrokes(canvas, strokeData, offsetX, offsetY)

            if (isChecked) {
                StrokeRenderer.drawStrikethrough(canvas, strokeData, offsetX, offsetY)
            }
        }

        // Set the bitmap as the TextViev's compound drawable or background
        val drawable = BitmapDrawable(textView.context.resources, bitmap)
        textView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        textView.background = drawable
        textView.minimumHeight = bitmap.height
        // Adjust layout params to fit the strokes
        val lp = textView.layoutParams
        if (lp is ViewGroup.LayoutParams) {
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            textView.layoutParams = lp
        }
        textView.requestLayout()
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