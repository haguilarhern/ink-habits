package com.inkhabits.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Compact bar histogram for completion counts (e.g. completions per week).
 * Each bar is scaled against [maxValue] (or the data max). E-ink friendly: solid
 * filled bars with a faint baseline.
 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var values: IntArray = IntArray(0)
    private var maxValue: Int = 1

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = Color.parseColor("#0B0B0C")
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = Color.parseColor("#F2F2F4")
    }

    /** Reused across draws so onDraw allocates nothing. */
    private val barRect = RectF()

    /** [max] caps the scale; pass the schedule's weekly target so full weeks top out. */
    fun setData(values: IntArray, max: Int = values.maxOrNull() ?: 1) {
        this.values = values
        this.maxValue = max.coerceAtLeast(1)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.isEmpty()) return
        val n = values.size
        val gap = (3 * resources.displayMetrics.density)
        val barW = (width - gap * (n - 1)) / n.toFloat()
        val r = barW * 0.25f
        for (i in values.indices) {
            val left = i * (barW + gap)
            val frac = values[i].toFloat() / maxValue
            val h = (height * frac).coerceIn(0f, height.toFloat())
            // faint full-height slot so empty weeks still read as a column
            barRect.set(left, height - barW.coerceAtMost(height.toFloat() * 0.18f),
                left + barW, height.toFloat())
            canvas.drawRoundRect(barRect, r, r, emptyPaint)
            if (h > 0f) {
                barRect.set(left, height - h, left + barW, height.toFloat())
                canvas.drawRoundRect(barRect, r, r, barPaint)
            }
        }
    }
}
