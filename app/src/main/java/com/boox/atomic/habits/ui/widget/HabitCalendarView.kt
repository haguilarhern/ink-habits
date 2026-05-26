package com.boox.atomic.habits.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.*

/**
 * Compact month calendar grid for e-ink displays.
 *
 * Shows the current month as a grid where each day is:
 *   ● filled circle = completed
 *   ○ empty circle = not completed/missed
 *   no circle = future date
 *
 * Designed to be narrow (fixed 7-column week layout) and shallow
 * (4-6 rows depending on month). E-ink optimized: grayscale only,
 * no colors, anti-aliasing off.
 */
class HabitCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val circlePaint = Paint().apply {
        isAntiAlias = true   // circles benefit from anti-aliasing
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.DKGRAY
    }

    private val fillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.DKGRAY
    }

    private val textPaint = Paint().apply {
        isAntiAlias = false
        textSize = 28f
        color = Color.DKGRAY
        textAlign = Paint.Align.CENTER
    }

    private val dayNamePaint = Paint().apply {
        isAntiAlias = false
        textSize = 22f
        color = Color.GRAY
        textAlign = Paint.Align.CENTER
    }

    private val todayPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.BLACK
    }

    // The completion dates set as "yyyy-MM-dd" strings
    private val completedDates = mutableSetOf<String>()
    private var displayDate: Calendar = Calendar.getInstance()

    private val monthYearFormat = SimpleDateFormat("MMM yyyy", Locale.US)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")

    /** Set which dates are completed. Dates should be "yyyy-MM-dd" format. */
    fun setCompletedDates(dates: List<String>) {
        completedDates.clear()
        completedDates.addAll(dates)
        invalidate()
    }

    /** Set the month/year to display. Defaults to current month. */
    fun setDisplayMonth(year: Int, month: Int) {
        displayDate = Calendar.getInstance().apply {
            set(year, month, 1)
        }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = resolveSize(280, widthMeasureSpec) // ~280px for 7 columns
        val cellSize = w / 7f
        val cal = displayDate.clone() as Calendar
        val maxWeeks = cal.getActualMaximum(Calendar.WEEK_OF_MONTH)
        val h = (40 + maxWeeks * (cellSize * 0.85f)).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val cellW = width / 7f
        val cellH = cellW * 0.85f
        val radius = cellW * 0.28f
        val startY = 38f

        // Draw month/year header
        val headerText = monthYearFormat.format(displayDate.time)
        textPaint.textSize = 32f
        textPaint.isFakeBoldText = true
        canvas.drawText(headerText, width / 2f, 28f, textPaint)

        // Draw day name headers
        textPaint.textSize = dayNamePaint.textSize
        textPaint.isFakeBoldText = false
        for (d in 0 until 7) {
            val x = d * cellW + cellW / 2f
            canvas.drawText(dayNames[d], x, startY, dayNamePaint)
        }

        // Calendar grid
        val cal = displayDate.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // Convert from Java DAY_OF_WEEK (1=Sunday) to our grid (0=Monday)
        var startCol = (firstDayOfWeek - Calendar.MONDAY + 7) % 7
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val todayStr = dateFormat.format(Date())

        var row = 0
        var col = startCol

        for (day in 1..daysInMonth) {
            val x = col * cellW + cellW / 2f
            val y = startY + dayNamePaint.textSize + 10 + row * cellH + cellH / 2f

            val dateStr = String.format(
                Locale.US, "%04d-%02d-%02d",
                displayDate.get(Calendar.YEAR),
                displayDate.get(Calendar.MONTH) + 1,
                day
            )

            val isToday = dateStr == todayStr
            val isCompleted = completedDates.contains(dateStr)
            val isFuture = dateStr > todayStr

            if (isFuture) {
                // Future day: draw day number faintly, no circle
                textPaint.color = Color.LTGRAY
                textPaint.textSize = 22f
                textPaint.isFakeBoldText = false
                canvas.drawText(day.toString(), x, y + 8f, textPaint)
                textPaint.color = Color.DKGRAY
            } else if (isCompleted) {
                // Completed: filled circle
                canvas.drawCircle(x, y, radius, circlePaint)
                canvas.drawCircle(x, y, radius, fillPaint)
                // Day number in white on filled circle
                textPaint.color = Color.WHITE
                textPaint.textSize = 20f
                textPaint.isFakeBoldText = false
                canvas.drawText(day.toString(), x, y + 7f, textPaint)
                textPaint.color = Color.DKGRAY
            } else {
                // Missed: empty circle outline
                canvas.drawCircle(x, y, radius, circlePaint)
                // Day number
                textPaint.textSize = 20f
                textPaint.isFakeBoldText = false
                canvas.drawText(day.toString(), x, y + 7f, textPaint)
            }

            // Today indicator: bold circle
            if (isToday) {
                canvas.drawCircle(x, y, radius + 2f, todayPaint)
            }

            col++
            if (col >= 7) {
                col = 0
                row++
            }
        }
    }
}