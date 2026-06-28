package com.inkhabits.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.time.LocalDate
import java.time.YearMonth

/**
 * Month calendar that marks each day by completion status and lets the user tap a
 * day to inspect it. Status codes come from a host-supplied provider so the view
 * stays agnostic about habit scheduling.
 */
class CalendarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val NONE_DUE = 0   // nothing scheduled
        const val DUE_NONE = 1   // scheduled, none done
        const val PARTIAL = 2    // some done
        const val ALL_DONE = 3   // everything done
        const val FUTURE = 4     // beyond today
    }

    var month: YearMonth = YearMonth.now()
        set(value) { field = value; requestLayout(); invalidate() }

    var selected: LocalDate? = LocalDate.now()
        set(value) { field = value; invalidate() }

    /** day -> status code */
    var statusProvider: (LocalDate) -> Int = { NONE_DUE }
        set(value) { field = value; invalidate() }

    var onSelect: ((LocalDate) -> Unit)? = null

    private val today = LocalDate.now()
    private val density = resources.displayMetrics.density
    private val headerH = 28 * density
    private var cellW = 0f
    private var cellH = 0f
    private var rows = 6

    private val accent = Color.parseColor("#2A4A8C")
    private val ink = Color.parseColor("#0B0B0C")
    private val muted = Color.parseColor("#9A9AA0")
    private val rule = Color.parseColor("#D9D9DE")
    private val futureNum = Color.parseColor("#C7C7CC")

    /** Reused across draws so onDraw allocates nothing (less GC churn on e-ink). */
    private val cellRect = RectF()

    private val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 15 * density
    }
    private val dowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 11 * density
        color = muted
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 1.8f * density
    }

    private val dows = listOf("M", "T", "W", "T", "F", "S", "S")

    /** Monday=0 … Sunday=6 offset of the first day of the month. */
    private fun firstOffset(): Int = (month.atDay(1).dayOfWeek.value + 6) % 7

    private fun computeRows(): Int {
        val total = firstOffset() + month.lengthOfMonth()
        return Math.ceil(total / 7.0).toInt().coerceIn(4, 6)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        rows = computeRows()
        cellW = width / 7f
        cellH = cellW * 0.92f
        val height = (headerH + rows * cellH).toInt()
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        // Weekday header
        for (i in 0..6) {
            val cx = cellW * (i + 0.5f)
            canvas.drawText(dows[i], cx, headerH * 0.72f, dowPaint)
        }
        val offset = firstOffset()
        val days = month.lengthOfMonth()
        val r = minOf(cellW, cellH) * 0.36f
        for (d in 1..days) {
            val idx = offset + d - 1
            val col = idx % 7
            val row = idx / 7
            val cx = cellW * (col + 0.5f)
            val cy = headerH + cellH * (row + 0.5f)
            val date = month.atDay(d)
            val status = statusProvider(date)
            val isSel = date == selected
            val isToday = date == today

            // selection ring (rounded square)
            if (isSel) {
                stroke.color = ink; stroke.strokeWidth = 2f * density
                val half = minOf(cellW, cellH) * 0.46f
                cellRect.set(cx - half, cy - half, cx + half, cy + half)
                canvas.drawRoundRect(cellRect, 6 * density, 6 * density, stroke)
            }

            when (status) {
                ALL_DONE -> {
                    fill.color = accent
                    canvas.drawCircle(cx, cy, r, fill)
                    numPaint.color = Color.WHITE
                }
                PARTIAL -> {
                    stroke.color = accent; stroke.strokeWidth = 2.4f * density
                    canvas.drawCircle(cx, cy, r, stroke)
                    numPaint.color = ink
                }
                DUE_NONE -> {
                    stroke.color = rule; stroke.strokeWidth = 1.5f * density
                    canvas.drawCircle(cx, cy, r, stroke)
                    numPaint.color = ink
                }
                FUTURE -> numPaint.color = futureNum
                else -> numPaint.color = muted
            }

            val baseline = cy - (numPaint.descent() + numPaint.ascent()) / 2f
            canvas.drawText(d.toString(), cx, baseline, numPaint)

            if (isToday) {
                fill.color = if (status == ALL_DONE) Color.WHITE else accent
                canvas.drawCircle(cx, cy + r * 0.92f, 1.8f * density, fill)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            val col = (event.x / cellW).toInt().coerceIn(0, 6)
            val row = ((event.y - headerH) / cellH).toInt()
            if (row < 0) return true
            val day = row * 7 + col - firstOffset() + 1
            if (day in 1..month.lengthOfMonth()) {
                val date = month.atDay(day)
                selected = date
                onSelect?.invoke(date)
                performClick()
            }
            return true
        }
        return true
    }

    override fun performClick(): Boolean { super.performClick(); return true }
}
