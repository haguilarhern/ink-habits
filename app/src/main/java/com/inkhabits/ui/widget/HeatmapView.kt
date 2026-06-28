package com.inkhabits.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Compact completion grid: 7 columns (one week per row). Cell states:
 * 0 = not scheduled, 1 = completed, 2 = scheduled but missed.
 */
class HeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var states: IntArray = IntArray(0)
    private val cols = 7

    private val completedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = Color.parseColor("#0B0B0C")
    }
    private val missedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2f; color = Color.parseColor("#C7C7CC")
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = Color.parseColor("#F2F2F4")
    }

    /** Reused across draws so onDraw allocates nothing. */
    private val cellRect = RectF()

    fun setStates(values: IntArray) {
        states = values
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val rows = if (states.isEmpty()) 1 else (states.size + cols - 1) / cols
        val cell = width / cols.toFloat()
        val height = (cell * rows).toInt()
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (states.isEmpty()) return
        val cell = width / cols.toFloat()
        val gap = cell * 0.14f
        val r = cell * 0.18f
        for (i in states.indices) {
            val col = i % cols
            val row = i / cols
            val left = col * cell + gap
            val top = row * cell + gap
            cellRect.set(left, top, left + cell - gap * 2, top + cell - gap * 2)
            when (states[i]) {
                1 -> canvas.drawRoundRect(cellRect, r, r, completedPaint)
                2 -> canvas.drawRoundRect(cellRect, r, r, missedPaint)
                else -> canvas.drawRoundRect(cellRect, r, r, emptyPaint)
            }
        }
    }
}
