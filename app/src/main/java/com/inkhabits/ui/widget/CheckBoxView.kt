package com.inkhabits.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * A circular check control in the spirit of Apple's Reminders: a thin open ring when
 * unchecked, a solid ink-filled circle with a white check when done. High-contrast and
 * large for e-ink touch. Tapping toggles it and notifies [onToggle].
 */
class CheckBoxView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var checked: Boolean = false
        set(value) { field = value; invalidate() }

    /** Filled-state color — solid ink by default (monochrome system). */
    var fillColor: Int = Color.parseColor("#0A7D6A")
        set(value) { field = value; fill.color = value; invalidate() }

    var onToggle: ((Boolean) -> Unit)? = null

    private val density = resources.displayMetrics.density

    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * density
        color = Color.parseColor("#C7C7CC")   // tertiary ring, unchecked
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#0A7D6A")
    }
    private val check = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * density
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
        val diameter = minOf(width, height) - 14f * density
        val cx = width / 2f
        val cy = height / 2f
        val r = diameter / 2f
        if (checked) {
            canvas.drawCircle(cx, cy, r, fill)
            // check mark
            val s = diameter
            val l = cx - r
            val t = cy - r
            canvas.drawLine(l + s * 0.28f, t + s * 0.52f, l + s * 0.44f, t + s * 0.68f, check)
            canvas.drawLine(l + s * 0.44f, t + s * 0.68f, l + s * 0.74f, t + s * 0.34f, check)
        } else {
            canvas.drawCircle(cx, cy, r - ring.strokeWidth / 2f, ring)
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
