package com.inkhabits.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * A large, high-contrast check box for e-ink. Tapping toggles it and notifies
 * [onToggle]. Filled state uses the accent color so it reads on Kaleido 3.
 */
class CheckBoxView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var checked: Boolean = false
        set(value) { field = value; invalidate() }

    var onToggle: ((Boolean) -> Unit)? = null

    private val box = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.BLACK
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#8C1D1D")
    }
    private val check = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.WHITE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = (44 * resources.displayMetrics.density).toInt()
        setMeasuredDimension(resolveSize(size, widthMeasureSpec), resolveSize(size, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pad = 8f
        val s = minOf(width, height) - pad * 2
        val left = pad; val top = (height - s) / 2f
        val right = left + s; val bottom = top + s
        val r = 6f
        if (checked) {
            canvas.drawRoundRect(left, top, right, bottom, r, r, fill)
            canvas.drawLine(left + s * 0.24f, top + s * 0.52f, left + s * 0.44f, bottom - s * 0.24f, check)
            canvas.drawLine(left + s * 0.44f, bottom - s * 0.24f, right - s * 0.20f, top + s * 0.26f, check)
        } else {
            canvas.drawRoundRect(left, top, right, bottom, r, r, box)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            checked = !checked
            onToggle?.invoke(checked)
            performClick()
            return true
        }
        return true
    }

    override fun performClick(): Boolean { super.performClick(); return true }
}
