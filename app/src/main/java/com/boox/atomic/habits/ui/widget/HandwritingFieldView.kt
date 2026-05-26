package com.boox.atomic.habits.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PathEffect
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.boox.atomic.habits.boox.GestureStrokeDetector
import com.boox.atomic.habits.boox.StrokeSerializer
import com.boox.atomic.habits.boox.StrokeRenderer

/**
 * A lightweight handwriting capture View that replaces EditText fields with a small
 * stylus/finger drawing canvas. Stores strokes as serializable data for persistence.
 *
 * Used for goals, habits, and todos where stylus input is preferred over typing.
 */
class HandwritingFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── Stroke storage ─────────────────────────────────────────────────────

    /** Each stroke is a list of (x, y, width) triples. */
    private val strokes = mutableListOf<List<Triple<Float, Float, Float>>>()

    /** Points being accumulated during the current touch gesture. */
    private val currentStroke = mutableListOf<Triple<Float, Float, Float>>()

    // ── Drawing state ──────────────────────────────────────────────────────

    private var isConfirmed = false

    /** Backing bitmap for persistent rendering. */
    private var bitmap: Bitmap? = null

    /** Canvas that draws onto [bitmap]. */
    private var canvas: Canvas? = null

    // ── Paints ─────────────────────────────────────────────────────────────

    /** Paint for drawing ink strokes. */
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.argb(220, 40, 40, 50) // dark ink
    }

    /** Paint for the faint border around the writing area. */
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    /** Dashed path effect used when the field is in editing mode. */
    private val dashedEffect: PathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)

    /** Paint for the confirm checkmark badge. */
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    /** Paint for the badge background rectangle. */
    private val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 60, 60, 70)
        style = Paint.Style.FILL
    }

    // ── Callbacks ──────────────────────────────────────────────────────────

    private var onConfirmListener: (() -> Unit)? = null
    private var onStrikethroughListener: (() -> Unit)? = null
    private val gestureDetector = GestureStrokeDetector()

    // ── Helpers ────────────────────────────────────────────────────────────

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    // ── Constants ──────────────────────────────────────────────────────────

    private val defaultWidthDp = 200
    private val defaultHeightDp = 80
    private val borderWidthPx = 1f
    private val confirmedBorderWidthPx = 2f
    private val minStrokeWidth = 3f
    private val maxStrokeWidth = 12f
    private val badgePadding = 8f
    private val badgeRadius = 16f

    // ═══════════════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Restore stroke data previously obtained from [getStrokeData].
     * Replaces all current strokes and redraws.
     */
    fun setStrokeData(data: String) {
        strokes.clear()
        currentStroke.clear()
        bitmap?.eraseColor(Color.TRANSPARENT)

        if (data.isBlank()) {
            invalidate()
            return
        }

        try {
            val allPoints = StrokeSerializer.deserialize(data)
            if (allPoints.isEmpty()) { invalidate(); return }
            // Store all points as a single flat stroke
            strokes.clear()
            strokes.add(allPoints)
            // Redraw
            strokes.forEach { stroke ->
                drawStrokeOnBitmap(stroke)
            }
            invalidate()
        } catch (_: Exception) {
        }
    }

    /**
     * Serialize all stored strokes to JSON via StrokeSerializer.
     */
    fun getStrokeData(): String {
        if (strokes.isEmpty()) return ""
        // Flatten all strokes into a single point list for serializer
        val allPoints = strokes.flatten()
        return StrokeSerializer.serialize(allPoints)
    }

    /** Erase all strokes and reset the canvas. */

    /** Whether this field is locked (read-only / confirmed). */
    fun isConfirmed(): Boolean = isConfirmed

    /**
     * Lock or unlock editing.
     * @param confirmed true to make the field read-only, false to allow editing.
     */
    fun setConfirmed(confirmed: Boolean) {
        if (isConfirmed != confirmed) {
            isConfirmed = confirmed
            invalidate()
        }
    }

    /** Register a callback invoked when the field is confirmed (locked). */
    fun setOnConfirmListener(l: () -> Unit) {
        onConfirmListener = l
    }

    fun setOnStrikethroughListener(l: () -> Unit) {
        onStrikethroughListener = l
    }

    fun clear() {
        strokes.clear()
        currentStroke.clear()
        bitmap?.eraseColor(Color.TRANSPARENT)
        invalidate()
    }

    /** Returns true when at least one completed stroke exists. */
    fun hasStrokes(): Boolean = strokes.isNotEmpty()

    // ═══════════════════════════════════════════════════════════════════════
    // View lifecycle
    // ═══════════════════════════════════════════════════════════════════════

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Recreate backing bitmap at the new size
        val newBitmap = Bitmap.createBitmap(
            maxOf(w, 1),
            maxOf(h, 1),
            Bitmap.Config.ARGB_8888
        )
        val newCanvas = Canvas(newBitmap)

        // If we had an old bitmap, copy its content across
        bitmap?.let { old ->
            old.recycle()
        }

        bitmap = newBitmap
        canvas = newCanvas

        // Redraw all existing strokes onto the fresh bitmap
        redrawAllStrokes()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec)
            MeasureSpec.AT_MOST -> MeasureSpec.getSize(widthMeasureSpec)
            else -> (defaultWidthDp * resources.displayMetrics.density).toInt()
        }
        val h = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            MeasureSpec.AT_MOST -> MeasureSpec.getSize(heightMeasureSpec)
            else -> (defaultHeightDp * resources.displayMetrics.density).toInt()
        }
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // ── Draw border ───────────────────────────────────────────────
        drawBorder(canvas)

        // ── Draw the persistent bitmap ────────────────────────────────
        bitmap?.let { bmp ->
            canvas.drawBitmap(bmp, 0f, 0f, null)
        }

        // ── Draw the current (in-progress) stroke on top ──────────────
        if (currentStroke.isNotEmpty() && !isConfirmed) {
            drawStroke(canvas, currentStroke)
        }

        // ── Confirmed badge ───────────────────────────────────────────
        if (isConfirmed && strokes.isNotEmpty()) {
            drawConfirmBadge(canvas)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Touch / stylus input
    // ═══════════════════════════════════════════════════════════════════════

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Accept stylus AND finger input
        val toolType = event.getToolType(0)
        if (toolType != MotionEvent.TOOL_TYPE_STYLUS &&
            toolType != MotionEvent.TOOL_TYPE_FINGER
        ) {
            return false
        }

        // When confirmed (read-only): only detect strikethrough gestures
        if (isConfirmed) {
            gestureDetector.handleMotionEvent(event)
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                if (gestureDetector.isStrikethrough()) {
                    onStrikethroughListener?.invoke()
                }
                gestureDetector.clear()
            }
            return true
        }

        val x = event.x
        val y = event.y
        val pressure = event.pressure.coerceIn(0f, 1f)
        val width = minStrokeWidth + (maxStrokeWidth - minStrokeWidth) * pressure

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Haptic feedback on stylus down
                triggerHapticFeedback()

                currentStroke.clear()
                currentStroke.add(Triple(x, y, width))
                lastTouchX = x
                lastTouchY = y
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                currentStroke.add(Triple(x, y, width))

                // Draw incrementally onto the backing bitmap for performance
                drawStrokeSegment(
                    canvas = canvas,
                    x1 = lastTouchX,
                    y1 = lastTouchY,
                    x2 = x,
                    y2 = y,
                    width = width
                )

                lastTouchX = x
                lastTouchY = y

                // Redraw the view so the in-progress stroke shows
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                // Store the completed stroke
                if (currentStroke.size >= 2) {
                    strokes.add(currentStroke.toList())
                } else if (currentStroke.size == 1) {
                    // Single tap – draw a small dot
                    val pt = currentStroke[0]
                    strokePaint.strokeWidth = pt.third
                    canvas?.drawPoint(pt.first, pt.second, strokePaint)
                    strokes.add(currentStroke.toList())
                }

                currentStroke.clear()
                invalidate()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                currentStroke.clear()
                invalidate()
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Private drawing helpers
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Draws the appropriate border style depending on confirmation state.
     *
     * - Editing mode: 1px gray dashed border.
     * - Confirmed, empty: 1px gray solid border.
     * - Confirmed, with content: 2px green-ish solid border.
     */
    private fun drawBorder(c: Canvas) {
        borderPaint.strokeWidth = borderWidthPx

        when {
            !isConfirmed -> {
                // Editing – dashed gray
                borderPaint.color = Color.argb(120, 160, 160, 160)
                borderPaint.pathEffect = dashedEffect
            }
            strokes.isEmpty() -> {
                // Confirmed but empty – solid gray
                borderPaint.color = Color.argb(120, 160, 160, 160)
                borderPaint.pathEffect = null
            }
            else -> {
                // Confirmed with content – solid green-ish
                borderPaint.color = Color.argb(180, 76, 175, 80)
                borderPaint.strokeWidth = confirmedBorderWidthPx
                borderPaint.pathEffect = null
            }
        }

        val inset = borderPaint.strokeWidth / 2f
        c.drawRect(
            inset,
            inset,
            width.toFloat() - inset,
            height.toFloat() - inset,
            borderPaint
        )

        // Reset path effect for subsequent draws
        borderPaint.pathEffect = null
    }

    /**
     * Draws a completed stroke as a series of connected segments.
     */
    private fun drawStroke(c: Canvas, points: List<Triple<Float, Float, Float>>) {
        if (points.size < 2) {
            if (points.size == 1) {
                val (px, py, pw) = points[0]
                strokePaint.strokeWidth = pw
                c.drawPoint(px, py, strokePaint)
            }
            return
        }

        var prevX = points[0].first
        var prevY = points[0].second
        for (i in 1 until points.size) {
            val (x, y, w) = points[i]
            strokePaint.strokeWidth = w
            c.drawLine(prevX, prevY, x, y, strokePaint)
            prevX = x
            prevY = y
        }
    }

    /**
     * Draws a single line segment between two points at the given width.
     * Used for incremental drawing during ACTION_MOVE for performance.
     */
    private fun drawStrokeSegment(
        targetCanvas: Canvas?,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        width: Float
    ) {
        val c = targetCanvas ?: return
        strokePaint.strokeWidth = width
        c.drawLine(x1, y1, x2, y2, strokePaint)
    }

    /**
     * Redraws all completed strokes onto the backing bitmap from scratch.
     * Called after setStrokeData / onSizeChanged.
     */
    private fun redrawAllStrokes() {
        val bmp = bitmap ?: return
        val c = canvas ?: return
        bmp.eraseColor(Color.TRANSPARENT)
        for (stroke in strokes) {
            drawStroke(c, stroke)
        }
    }

    /**
     * Draws a subtle dark-gray rounded rect with a white "✓" character
     * at the bottom-right corner.
     */
    private fun drawConfirmBadge(c: Canvas) {
        val badgeText = "\u2713"
        val textWidth = badgePaint.measureText(badgeText)
        val textHeight = badgePaint.descent() - badgePaint.ascent()

        val bgLeft = width.toFloat() - textWidth - badgePadding * 3
        val bgTop = height.toFloat() - textHeight - badgePadding * 2
        val bgRight = width.toFloat() - badgePadding
        val bgBottom = height.toFloat() - badgePadding

        c.drawRoundRect(bgLeft, bgTop, bgRight, bgBottom, badgeRadius, badgeRadius, badgeBgPaint)

        val textX = (bgLeft + bgRight) / 2f
        val textY = bgBottom - badgePadding - badgePaint.descent()
        c.drawText(badgeText, textX, textY, badgePaint)
    }

    /**
     * Triggers a short haptic feedback vibration on stylus down.
     * Compatible with both old and new Android vibration APIs.
     */
    private fun triggerHapticFeedback() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = ContextCompat.getSystemService(context, VibratorManager::class.java)
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(
                    20L,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
                v.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(20L)
            }
        }
    }
}