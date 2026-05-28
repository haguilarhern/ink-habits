package com.inkhabits.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import com.inkhabits.util.StrokeRenderer
import com.inkhabits.util.StrokeSerializer
import kotlin.math.abs

/**
 * Displays a habit's name as either typed text or handwritten ink, and lets the
 * user mark it done by striking a horizontal line across it with the pen/finger.
 * When completed it draws a strikethrough.
 */
class HabitNameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var text: String = ""
    private var strokes: String = ""

    var completed: Boolean = false
        set(value) { field = value; invalidate() }

    var onStrike: (() -> Unit)? = null

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20f, resources.displayMetrics))
        try {
            typeface = androidx.core.content.res.ResourcesCompat.getFont(context, com.inkhabits.R.font.patrick_hand)
        } catch (_: Throwable) {
        }
    }
    private val strikePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8C1D1D")
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }

    fun setContent(text: String, strokes: String) {
        this.text = text
        this.strokes = strokes
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = (56 * resources.displayMetrics.density).toInt()
        setMeasuredDimension(
            resolveSize((200 * resources.displayMetrics.density).toInt(), widthMeasureSpec),
            resolveSize(h, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (StrokeRenderer.hasInk(strokes)) {
            val ink = StrokeSerializer.deserialize(strokes)
            // preserve aspect: render into a width proportional to height
            StrokeRenderer.drawInto(canvas, ink, width, height, Color.BLACK)
        } else if (text.isNotEmpty()) {
            val y = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(text, 4f, y, textPaint)
        }
        if (completed) {
            val y = height / 2f
            canvas.drawLine(4f, y, width - 8f, y, strikePaint)
        } else if (dragging) {
            // Live strike line following the finger/pen as you cross.
            val y = height / 2f
            canvas.drawLine(minOf(downX, liveX), y, maxOf(downX, liveX), y, strikePaint)
        }
    }

    private var downX = 0f
    private var downY = 0f
    private var liveX = 0f
    private var maxDx = 0f
    private var maxDy = 0f
    private var dragging = false
    private var fired = false

    private val strikeThreshold get() = width * 0.35f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x; downY = event.y; liveX = event.x
                maxDx = 0f; maxDy = 0f; dragging = true; fired = false
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                liveX = event.x
                maxDx = maxOf(maxDx, abs(event.x - downX))
                maxDy = maxOf(maxDy, abs(event.y - downY))
                // Fire the moment the cross is long enough — feels live, no wait for lift.
                if (!fired && maxDx > strikeThreshold && maxDy < height * 0.5f) {
                    fired = true
                    onStrike?.invoke()
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
