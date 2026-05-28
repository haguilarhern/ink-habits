package com.inkhabits.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.inkhabits.util.InkPoint
import com.inkhabits.util.StrokeSerializer
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.RawInputCallback
import com.onyx.android.sdk.pen.TouchHelper
import com.onyx.android.sdk.pen.data.TouchPointList

/**
 * Inline handwriting box backed by the Onyx [TouchHelper] raw-drawing engine
 * (firmware-rendered, zero-lag). Because raw drawing is a single global resource
 * bound to one surface, only ONE InlineInkView is "active" at a time: activating
 * one closes the previous. Tapping a box claims it. The host should call
 * [refreshLimit] on scroll so the pen region tracks the box's position.
 */
class InlineInkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    private var touchHelper: TouchHelper? = null
    private val strokes = mutableListOf<MutableList<InkPoint>>()
    private var bitmap: android.graphics.Bitmap? = null
    private var bmpCanvas: Canvas? = null
    private var w = 1
    private var h = 1
    private var surfaceReady = false
    private var active = false
    private val strokeWidth = 6f

    // Auto-release the pen a moment after writing stops, so other controls
    // (time, anchor, scroll) respond to the pen instead of being captured.
    private val idleHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val idleRunnable = Runnable { if (active) deactivate() }
    private fun scheduleIdle() {
        idleHandler.removeCallbacks(idleRunnable)
        idleHandler.postDelayed(idleRunnable, 1800L)
    }
    private fun cancelIdle() = idleHandler.removeCallbacks(idleRunnable)

    private val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val checkCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8C1D1D")
    }
    private val checkIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 3.2f * resources.displayMetrics.density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    init {
        holder.addCallback(this)
        isClickable = true
    }

    // ── public API ──

    fun hasStrokes(): Boolean = strokes.any { it.isNotEmpty() }

    fun getStrokeData(): String = StrokeSerializer.serialize(w, h, strokes)

    fun setStrokeData(data: String) {
        strokes.clear()
        val ink = StrokeSerializer.deserialize(data)
        if (!ink.isEmpty) {
            val sx = (if (w > 0) w else ink.srcWidth).toFloat() / ink.srcWidth
            val sy = (if (h > 0) h else ink.srcHeight).toFloat() / ink.srcHeight
            val scale = minOf(sx, sy)
            for (s in ink.strokes) {
                strokes.add(s.map { InkPoint(it.x * scale, it.y * scale, it.w * scale) }.toMutableList())
            }
        }
        drawAllToBitmap()
        renderPersistent()
    }

    fun clear() {
        strokes.clear()
        bmpCanvas?.drawColor(Color.WHITE)
        if (active && touchHelper != null) {
            touchHelper?.setRawDrawingRenderEnabled(false)
            renderPersistent()
            touchHelper?.setRawDrawingRenderEnabled(true)
            touchHelper?.setRawDrawingEnabled(true)
        } else {
            renderPersistent()
        }
        // Clear any e-ink ghosting left by the wiped strokes.
        try {
            com.inkhabits.eink.EInkUtils.cleanRefresh(this)
        } catch (_: Throwable) {
        }
    }

    fun activate() {
        if (current === this && active) return
        current?.let { if (it !== this) it.deactivate() }
        current = this
        active = true
        if (surfaceReady) setupRaw()
    }

    fun deactivate() {
        active = false
        cancelIdle()
        try {
            touchHelper?.setRawDrawingEnabled(false)
            touchHelper?.closeRawDrawing()
        } catch (_: Throwable) {
        }
        touchHelper = null
        if (current === this) current = null
        renderPersistent() // keep the captured ink visible while inactive
    }

    /** Re-sync the raw-drawing region to the box's current on-screen position. */
    fun refreshLimit() {
        if (!active || !surfaceReady) return
        val th = touchHelper ?: return
        th.setLimitRect(Rect(0, 0, w, h), excludeRects())
    }

    // ── surface lifecycle ──

    override fun surfaceCreated(holder: SurfaceHolder) {}

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        w = width.coerceAtLeast(1)
        h = height.coerceAtLeast(1)
        surfaceReady = true
        ensureBitmap()
        drawAllToBitmap()
        renderPersistent()
        if (active) setupRaw()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceReady = false
        try {
            touchHelper?.setRawDrawingEnabled(false)
            touchHelper?.closeRawDrawing()
        } catch (_: Throwable) {
        }
        touchHelper = null
    }

    override fun onDetachedFromWindow() {
        if (active) deactivate()
        super.onDetachedFromWindow()
    }

    // Claim writing focus on tap (finger/pen) when not already active.
    // When active, a tap in the reserved corner (✓) deactivates to release
    // the pen so other controls become responsive again.
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            cancelIdle()
            if (!active) { activate(); return true }
            val c = reservedCorner
            if (event.x > w - c && event.y > h - c) {
                deactivate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    // ── internals ──

    /** Bottom-right corner reserved for the "done" button; excluded from raw drawing. */
    private val reservedCorner: Int get() = (54 * resources.displayMetrics.density).toInt()

    private fun excludeRects(): ArrayList<Rect> {
        val c = reservedCorner
        return arrayListOf(Rect(w - c, h - c, w, h))
    }

    private fun setupRaw() {
        val th = touchHelper ?: TouchHelper.create(this, callback).also { touchHelper = it }
        // Use the box's actual bounds; getLocalVisibleRect can return empty before
        // layout, which makes Onyx capture the pen across the whole screen.
        val limit = Rect(0, 0, w, h)
        th.setStrokeColor(Color.BLACK)
        th.setStrokeStyle(TouchHelper.STROKE_STYLE_FOUNTAIN)
        th.openRawDrawing()
        th.setStrokeWidth(strokeWidth)
        th.setLimitRect(limit, excludeRects())
        th.setPenUpRefreshEnabled(true)
        th.setRawInputReaderEnable(true)
        // Let the pen's eraser button erase strokes (like Onyx Notes).
        try { th.enableSideBtnErase(true) } catch (_: Throwable) {}
        try { th.setEraserRawDrawingEnabled(true) } catch (_: Throwable) {}
        th.setRawDrawingEnabled(true)
        th.setRawDrawingRenderEnabled(true)
    }

    private fun ensureBitmap() {
        if (bitmap?.width == w && bitmap?.height == h) return
        bitmap?.recycle()
        bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        bmpCanvas = Canvas(bitmap!!)
        bmpCanvas?.drawColor(Color.WHITE)
    }

    private fun drawAllToBitmap() {
        val c = bmpCanvas ?: return
        c.drawColor(Color.WHITE)
        for (s in strokes) drawStroke(c, s)
    }

    private fun drawStroke(c: Canvas, stroke: List<InkPoint>) {
        if (stroke.size == 1) {
            c.drawPoint(stroke[0].x, stroke[0].y, inkPaint)
            return
        }
        for (i in 1 until stroke.size) {
            val a = stroke[i - 1]; val b = stroke[i]
            c.drawLine(a.x, a.y, b.x, b.y, inkPaint)
        }
    }

    private fun renderPersistent() {
        if (!surfaceReady) return
        val c = holder.lockCanvas() ?: return
        try {
            c.drawColor(Color.WHITE)
            bitmap?.let { c.drawBitmap(it, 0f, 0f, null) }
            if (active) drawCheckmark(c)
        } finally {
            holder.unlockCanvasAndPost(c)
        }
    }

    private fun drawCheckmark(canvas: Canvas) {
        val size = reservedCorner
        val pad = size * 0.22f
        val cx = w - size / 2f
        val cy = h - size / 2f
        val radius = size / 2f - pad
        canvas.drawCircle(cx, cy, radius, checkCirclePaint)
        val inner = radius * 0.52f
        canvas.drawLine(cx - inner, cy, cx - inner * 0.3f, cy + inner, checkIconPaint)
        canvas.drawLine(cx - inner * 0.3f, cy + inner, cx + inner, cy - inner * 0.3f, checkIconPaint)
    }

    private val callback = object : RawInputCallback() {
        override fun onBeginRawDrawing(b: Boolean, point: TouchPoint?) {}
        override fun onEndRawDrawing(b: Boolean, point: TouchPoint?) {}
        override fun onRawDrawingTouchPointMoveReceived(point: TouchPoint?) {}

        override fun onRawDrawingTouchPointListReceived(list: TouchPointList) {
            val pts = list.points ?: return
            if (pts.isEmpty()) return
            val stroke = pts.map { InkPoint(it.x, it.y, strokeWidth) }.toMutableList()
            strokes.add(stroke)
            bmpCanvas?.let { drawStroke(it, stroke) }
            post { scheduleIdle() } // release the pen if writing pauses
        }

        override fun onPenActive(point: TouchPoint?) {
            touchHelper?.setRawDrawingEnabled(true)
            post { cancelIdle() } // pen is back on the box; keep writing
        }

        override fun onBeginRawErasing(b: Boolean, point: TouchPoint?) {}
        override fun onEndRawErasing(b: Boolean, point: TouchPoint?) {}
        override fun onRawErasingTouchPointMoveReceived(point: TouchPoint?) {}
        override fun onRawErasingTouchPointListReceived(list: TouchPointList?) {}
        override fun onPenUpRefresh(refreshRect: android.graphics.RectF?) {}
    }

    companion object {
        /** The single currently-active inline writer. */
        var current: InlineInkView? = null
    }
}
