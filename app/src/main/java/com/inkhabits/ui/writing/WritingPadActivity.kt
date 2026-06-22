package com.inkhabits.ui.writing

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.view.SurfaceHolder
import com.inkhabits.databinding.ActivityWritingPadBinding
import com.inkhabits.eink.EInkActivity
import com.inkhabits.util.InkPoint
import com.inkhabits.util.QuotePrefs
import com.inkhabits.util.StrokeSerializer
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.RawInputCallback
import com.onyx.android.sdk.pen.TouchHelper
import com.onyx.android.sdk.pen.data.TouchPointList

/**
 * Full-screen handwriting capture using the Onyx [TouchHelper] raw-drawing API.
 *
 * The Boox firmware renders the wet pen stroke directly to the EPD with no lag —
 * the app does NOT draw during the stroke (the move callbacks are intentionally
 * empty). When a stroke completes we copy its points into a backing bitmap so the
 * ink persists across refreshes, and serialize them on Done. This mirrors the
 * approach used by zero-lag drawing apps like boox-rapid-draw.
 */
class WritingPadActivity : EInkActivity() {

    private lateinit var binding: ActivityWritingPadBinding
    private var touchHelper: TouchHelper? = null

    private val strokes = mutableListOf<MutableList<InkPoint>>()
    private var bitmap: android.graphics.Bitmap? = null
    private var bmpCanvas: Canvas? = null
    private var surfaceW = 1
    private var surfaceH = 1

    private val strokeWidth = 6f

    private val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWritingPadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent.getStringExtra(EXTRA_TITLE)?.let { binding.padTitle.text = it }

        binding.cancelButton.setOnClickListener { setResult(RESULT_CANCELED); finish() }
        binding.clearButton.setOnClickListener { clearAll() }
        binding.doneButton.setOnClickListener { done() }
        // Tap outside the card to dismiss without saving.
        binding.scrim.setOnClickListener { setResult(RESULT_CANCELED); finish() }
        binding.card.setOnClickListener { /* swallow taps on the card */ }

        // Composite the writing surface above the translucent window.
        binding.surface.setZOrderOnTop(true)
        binding.surface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {}

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                surfaceW = width.coerceAtLeast(1)
                surfaceH = height.coerceAtLeast(1)
                ensureBitmap()
                loadExisting()
                renderToScreen()   // show white/existing ink BEFORE the firmware owns the surface
                setupTouchHelper()
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                touchHelper?.setRawDrawingEnabled(false)
                touchHelper?.closeRawDrawing()
                touchHelper = null
            }
        })
    }

    private fun ensureBitmap() {
        if (bitmap?.width == surfaceW && bitmap?.height == surfaceH) return
        bitmap?.recycle()
        bitmap = android.graphics.Bitmap.createBitmap(surfaceW, surfaceH, android.graphics.Bitmap.Config.ARGB_8888)
        bmpCanvas = Canvas(bitmap!!)
        bmpCanvas?.drawColor(Color.WHITE)
    }

    private fun loadExisting() {
        // Quote quick-edit (from the widget) pre-loads the saved quote ink so you
        // edit it rather than starting blank.
        val data = intent.getStringExtra(EXTRA_STROKES).takeUnless { it.isNullOrBlank() }
            ?: QuotePrefs.strokes(this).takeIf { intent.getBooleanExtra(EXTRA_SAVE_QUOTE, false) }
            ?: return
        if (data.isBlank()) return
        val ink = StrokeSerializer.deserialize(data)
        if (ink.isEmpty) return
        val sx = surfaceW.toFloat() / ink.srcWidth
        val sy = surfaceH.toFloat() / ink.srcHeight
        val scale = minOf(sx, sy)
        for (s in ink.strokes) {
            val scaled = s.map { InkPoint(it.x * scale, it.y * scale, it.w * scale) }.toMutableList()
            strokes.add(scaled)
        }
        drawAllToBitmap()
    }

    private fun setupTouchHelper() {
        val th = touchHelper ?: TouchHelper.create(binding.surface, callback).also { touchHelper = it }
        // Limit rect in the surface's local coordinates (full surface). Explicit size
        // is more reliable than getLocalVisibleRect, which can be empty/partial before
        // layout fully settles.
        val limit = Rect(0, 0, surfaceW, surfaceH)
        th.setStrokeColor(Color.BLACK)
        th.setStrokeStyle(TouchHelper.STROKE_STYLE_FOUNTAIN)
        th.openRawDrawing()
        th.setStrokeWidth(strokeWidth)
        th.setLimitRect(limit, ArrayList())
        th.setPenUpRefreshEnabled(true)
        th.setRawInputReaderEnable(true)
        th.setRawDrawingEnabled(true)
        th.setRawDrawingRenderEnabled(true)
    }

    private fun drawAllToBitmap() {
        val c = bmpCanvas ?: return
        c.drawColor(Color.WHITE)
        for (stroke in strokes) drawStroke(c, stroke)
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

    private fun renderToScreen() {
        val holder = binding.surface.holder
        val c = holder.lockCanvas() ?: return
        try {
            c.drawColor(Color.WHITE)
            bitmap?.let { c.drawBitmap(it, 0f, 0f, null) }
        } finally {
            holder.unlockCanvasAndPost(c)
        }
    }

    private fun clearAll() {
        strokes.clear()
        bmpCanvas?.drawColor(Color.WHITE)
        touchHelper?.setRawDrawingRenderEnabled(false)
        renderToScreen()
        touchHelper?.setRawDrawingRenderEnabled(true)
        touchHelper?.setRawDrawingEnabled(true)
    }

    private fun done() {
        val data = StrokeSerializer.serialize(surfaceW, surfaceH, strokes)
        // Quick-add mode (launched from the home-screen widget): save a new to-do
        // directly and refresh widgets, without ever opening the full app.
        if (intent.getBooleanExtra(EXTRA_SAVE_TODO, false)) {
            if (strokes.any { it.isNotEmpty() }) {
                kotlinx.coroutines.runBlocking {
                    val db = com.inkhabits.data.AppDatabase.get(applicationContext)
                    val order = db.toDoDao().getAll().size
                    db.toDoDao().insert(com.inkhabits.data.entity.ToDo(titleStrokes = data, sortOrder = order))
                }
                com.inkhabits.widget.WidgetCommon.updateAll(applicationContext)
            }
            finish()
            return
        }
        // Quick-edit mode (from the quote widget): save the handwritten quote straight
        // to QuotePrefs and refresh widgets, without opening the app. An empty pad
        // clears the custom quote, reverting to the rotating daily one.
        if (intent.getBooleanExtra(EXTRA_SAVE_QUOTE, false)) {
            if (strokes.any { it.isNotEmpty() }) {
                QuotePrefs.save(applicationContext, text = "", strokes = data, preferHandwritten = true)
            } else {
                QuotePrefs.clear(applicationContext)
            }
            com.inkhabits.widget.WidgetCommon.updateAll(applicationContext)
            finish()
            return
        }
        setResult(RESULT_OK, intent.putExtra(EXTRA_RESULT, data))
        finish()
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
            // Keep firmware ink; our bitmap copy guarantees persistence on refresh.
        }

        override fun onPenActive(point: TouchPoint?) {
            touchHelper?.setRawDrawingEnabled(true)
        }

        override fun onBeginRawErasing(b: Boolean, point: TouchPoint?) {}
        override fun onEndRawErasing(b: Boolean, point: TouchPoint?) {}
        override fun onRawErasingTouchPointMoveReceived(point: TouchPoint?) {}
        override fun onRawErasingTouchPointListReceived(list: TouchPointList?) {}

        override fun onPenUpRefresh(refreshRect: RectF?) {
            // Firmware keeps the rendered ink on screen; nothing to blit here
            // (locking the surface canvas while raw drawing owns it can stall).
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        touchHelper?.setRawDrawingEnabled(false)
        touchHelper?.closeRawDrawing()
        touchHelper = null
        bitmap?.recycle()
    }

    companion object {
        const val EXTRA_STROKES = "extra_strokes_in"
        const val EXTRA_RESULT = "extra_strokes_out"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SAVE_TODO = "extra_save_todo"  // widget quick-add: save a new to-do on Done
        const val EXTRA_SAVE_QUOTE = "extra_save_quote"  // quote widget: save the dashboard quote on Done
    }
}
