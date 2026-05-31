package com.inkhabits.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import com.inkhabits.util.InkData
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
    /** Deserialized once on [setContent], not per-frame — keeps scrolling snappy. */
    private var ink: InkData? = null

    var completed: Boolean = false
        set(value) { field = value; invalidate() }

    var onStrike: (() -> Unit)? = null

    /** Fired on a quick tap (no strike drag) — used to enter edit mode on a to-do line. */
    var onTap: (() -> Unit)? = null

    /** When false, the view ignores touches (display-only) so a parent can handle taps. */
    var strikeEnabled: Boolean = true

    /** Ink rendering: cap scale (1f = natural size, no enlargement) and optional centering. */
    var inkMaxScale: Float = 6f
    var inkCenterHorizontal: Boolean = false

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
        ink = if (strokes.isNotBlank()) StrokeSerializer.deserialize(strokes) else null
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
        val ink = ink
        if (ink != null && !ink.isEmpty) {
            // Blit a cached bitmap instead of re-vectorizing the strokes every frame —
            // the renderer keeps an LRU cache keyed by content+size, so scrolling stays
            // cheap even with many handwritten names on screen.
            if (width > 0 && height > 0) {
                StrokeRenderer.renderToBitmap(strokes, width, height, Color.BLACK,
                    inkMaxScale, inkCenterHorizontal)?.let { canvas.drawBitmap(it, 0f, 0f, null) }
            }
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
    /** Set once we decide the gesture is a vertical scroll — we hand it to the list. */
    private var releasedToParent = false

    private val strikeThreshold get() = width * 0.35f
    private val touchSlop get() = 8f * resources.displayMetrics.density

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!strikeEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x; downY = event.y; liveX = event.x
                maxDx = 0f; maxDy = 0f; dragging = true; fired = false
                releasedToParent = false
                // Don't claim the gesture yet: a vertical drag must still scroll the list.
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (releasedToParent) return false
                liveX = event.x
                maxDx = maxOf(maxDx, abs(event.x - downX))
                maxDy = maxOf(maxDy, abs(event.y - downY))
                // Decide intent once past slop: vertical -> scroll, horizontal -> strike.
                if (!dragging || (maxDx < touchSlop && maxDy < touchSlop)) {
                    // still ambiguous
                } else if (maxDy > maxDx && maxDy > touchSlop) {
                    // Vertical: give the gesture back to the RecyclerView to scroll.
                    releasedToParent = true
                    parent?.requestDisallowInterceptTouchEvent(false)
                    invalidate()
                    return false
                } else {
                    // Horizontal: this is a strike; keep the gesture.
                    parent?.requestDisallowInterceptTouchEvent(true)
                    if (!fired && maxDx > strikeThreshold && maxDy < height * 0.5f) {
                        fired = true
                        onStrike?.invoke()
                    }
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                // Quick tap (no strike drag, no scroll) = request edit.
                if (event.actionMasked == MotionEvent.ACTION_UP && !fired && !releasedToParent) {
                    val slop = 12f * resources.displayMetrics.density
                    if (maxDx < slop && maxDy < slop) onTap?.invoke()
                }
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
