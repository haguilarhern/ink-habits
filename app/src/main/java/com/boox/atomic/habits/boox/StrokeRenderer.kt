package com.boox.atomic.habits.boox

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

object StrokeRenderer {

    /**
     * Draw strokes onto a Canvas from serialized stroke data.
     */
    fun drawStrokes(
        canvas: Canvas?,
        strokeData: String,
        offsetX: Float = 0f,
        offsetY: Float = 0f
    ) {
        if (canvas == null || strokeData.isBlank()) return
        val points = StrokeSerializer.deserialize(strokeData)
        if (points.isEmpty()) return
        drawStroke(canvas, points, offsetX, offsetY)
    }

    /**
     * Draw a strikethrough line across the bounding box of the strokes.
     */
    fun drawStrikethrough(
        canvas: Canvas?,
        strokeData: String,
        offsetX: Float = 0f,
        offsetY: Float = 0f
    ) {
        if (canvas == null || strokeData.isBlank()) return
        val bounds = measureStrokes(strokeData)
        if (bounds.isEmpty) return

        val centerY = bounds.centerY() + offsetY
        val left = bounds.left + offsetX
        val right = bounds.right + offsetX

        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 3f
            style = Paint.Style.FILL
            isAntiAlias = false
        }

        val padding = 6f
        canvas.drawLine(left - padding, centerY, right + padding, centerY, paint)
    }

    /**
     * Calculate the bounding box of all strokes.
     */
    fun measureStrokes(strokeData: String): RectF {
        if (strokeData.isBlank()) return RectF()
        val points = StrokeSerializer.deserialize(strokeData)
        if (points.isEmpty()) return RectF()

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for ((x, y) in points) {
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
        }

        return RectF(minX, minY, maxX, maxY)
    }

    /**
     * Draw a single path of points with varying stroke width per segment.
     */
    private fun drawStroke(
        canvas: Canvas,
        points: List<Triple<Float, Float, Float>>,
        offsetX: Float,
        offsetY: Float
    ) {
        if (points.size < 2) return

        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = false
        }

        for (i in 0 until points.size - 1) {
            val (x1, y1, w1) = points[i]
            val (x2, y2, w2) = points[i + 1]

            // Average width for this segment
            paint.strokeWidth = (w1 + w2) / 2f
            canvas.drawLine(
                x1 + offsetX, y1 + offsetY,
                x2 + offsetX, y2 + offsetY,
                paint
            )
        }
    }
}
