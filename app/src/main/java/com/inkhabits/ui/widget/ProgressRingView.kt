package com.inkhabits.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * A coarse circular progress ring for the e-ink Pomodoro timer. It is only ever
 * re-drawn once per minute (the timer ticks in whole minutes to avoid the heavy
 * ghosting a per-second sweep would cause), so a simple two-arc design reads cleanly.
 */
class ProgressRingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    /** Remaining fraction, 0f..1f. */
    var progress: Float = 1f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }

    var ringColor: Int = com.inkhabits.util.Accent.color(context)
        set(value) { field = value; invalidate() }

    private val density = resources.displayMetrics.density
    private val stroke = 10f * density

    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        color = Color.parseColor("#D9D9DE")
    }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
        color = ringColor
    }
    private val box = RectF()

    override fun onDraw(canvas: Canvas) {
        val pad = stroke / 2f + 1f
        box.set(pad, pad, width - pad, height - pad)
        canvas.drawArc(box, 0f, 360f, false, track)
        arc.color = ringColor
        canvas.drawArc(box, -90f, -360f * progress, false, arc)
    }
}
