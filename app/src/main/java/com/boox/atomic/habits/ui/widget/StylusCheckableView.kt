package com.boox.atomic.habits.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.boox.atomic.habits.boox.EInkUtils

class StylusCheckableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.BLACK
    }

    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }

    private val checkPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.WHITE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    var isChecked: Boolean = false
        set(value) {
            field = value
            invalidate()
            EInkUtils.cleanRefresh(this)
        }

    private var onCheckedChangeListener: ((Boolean) -> Unit)? = null

    fun setOnCheckedChangeListener(listener: (Boolean) -> Unit) {
        onCheckedChangeListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = minOf(width, height).coerceAtMost(80)
        val padding = 4f
        val left = padding
        val top = padding
        val right = size - padding
        val bottom = size - padding

        // Draw box
        paint.style = Paint.Style.STROKE
        canvas.drawRect(left, top, right, bottom, paint)

        if (isChecked) {
            // Fill
            canvas.drawRect(left, top, right, bottom, fillPaint)
            // Checkmark
            val cx = (left + right) / 2f
            val cy = (top + bottom) / 2f
            canvas.drawLine(left + size * 0.2f, cy, cx, bottom - size * 0.2f, checkPaint)
            canvas.drawLine(cx, bottom - size * 0.2f, right - size * 0.15f, top + size * 0.2f, checkPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            isChecked = !isChecked
            onCheckedChangeListener?.invoke(isChecked)
            performClick()
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = resolveSize(60, widthMeasureSpec)
        setMeasuredDimension(size, size)
    }
}