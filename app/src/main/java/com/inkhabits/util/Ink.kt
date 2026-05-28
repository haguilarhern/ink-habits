package com.inkhabits.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/** A single ink point: x, y, and stroke width at that point. */
data class InkPoint(val x: Float, val y: Float, val w: Float)

/** Parsed ink: the strokes plus the source canvas size they were captured in. */
data class InkData(
    val srcWidth: Int,
    val srcHeight: Int,
    val strokes: List<List<InkPoint>>
) {
    val isEmpty: Boolean get() = strokes.isEmpty() || strokes.all { it.isEmpty() }
}

/**
 * Compact serialization for handwritten strokes.
 *
 * Format: "W,H#stroke;stroke;..." where each stroke is "x,y,w|x,y,w|..."
 * Coordinates are stored in the capture coordinate space; [StrokeRenderer]
 * rescales to whatever size it is drawn at.
 */
object StrokeSerializer {

    fun serialize(srcWidth: Int, srcHeight: Int, strokes: List<List<InkPoint>>): String {
        if (strokes.all { it.isEmpty() }) return ""
        val body = strokes.filter { it.isNotEmpty() }.joinToString(";") { stroke ->
            stroke.joinToString("|") { p ->
                "${p.x.toInt()},${p.y.toInt()},${p.w.toInt()}"
            }
        }
        return "$srcWidth,$srcHeight#$body"
    }

    fun deserialize(data: String): InkData {
        if (data.isBlank() || !data.contains('#')) return InkData(1, 1, emptyList())
        return try {
            val (header, body) = data.split("#", limit = 2)
            val (w, h) = header.split(",").map { it.toInt() }
            val strokes = if (body.isBlank()) emptyList() else body.split(";").map { s ->
                s.split("|").mapNotNull { pt ->
                    val parts = pt.split(",")
                    if (parts.size >= 2) {
                        val x = parts[0].toFloatOrNull()
                        val y = parts[1].toFloatOrNull()
                        val pw = parts.getOrNull(2)?.toFloatOrNull() ?: 3f
                        if (x != null && y != null) InkPoint(x, y, pw) else null
                    } else null
                }
            }
            InkData(w.coerceAtLeast(1), h.coerceAtLeast(1), strokes)
        } catch (_: Exception) {
            InkData(1, 1, emptyList())
        }
    }
}

/** Renders serialized ink into a bitmap, scaled to fit the target size. */
object StrokeRenderer {

    fun hasInk(data: String): Boolean = !StrokeSerializer.deserialize(data).isEmpty

    fun renderToBitmap(
        data: String,
        targetWidth: Int,
        targetHeight: Int,
        color: Int = Color.BLACK
    ): Bitmap? {
        if (targetWidth <= 0 || targetHeight <= 0) return null
        val ink = StrokeSerializer.deserialize(data)
        val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        if (ink.isEmpty) return bmp
        val canvas = Canvas(bmp)
        drawInto(canvas, ink, targetWidth, targetHeight, color)
        return bmp
    }

    /**
     * Draws the ink scaled and centered to fill the target, fitting the ink's
     * bounding box (not the original capture canvas). This keeps saved
     * handwriting large and readable no matter where or how small it was drawn.
     */
    fun drawInto(
        canvas: Canvas,
        ink: InkData,
        targetWidth: Int,
        targetHeight: Int,
        color: Int,
        padding: Float = 8f
    ) {
        if (ink.isEmpty) return

        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        for (stroke in ink.strokes) for (p in stroke) {
            if (p.x < minX) minX = p.x; if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y; if (p.y > maxY) maxY = p.y
        }
        if (minX > maxX || minY > maxY) return

        val inkW = (maxX - minX).coerceAtLeast(1f)
        val inkH = (maxY - minY).coerceAtLeast(1f)
        val availW = (targetWidth - padding * 2).coerceAtLeast(1f)
        val availH = (targetHeight - padding * 2).coerceAtLeast(1f)
        val scale = minOf(availW / inkW, availH / inkH).coerceAtMost(MAX_SCALE)

        val drawW = inkW * scale
        val drawH = inkH * scale
        // left-align horizontally, center vertically (reads like a label)
        val offX = padding - minX * scale
        val offY = padding + (availH - drawH) / 2f - minY * scale

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        fun sw(w: Float) = (w * scale).coerceIn(2f, 10f)
        for (stroke in ink.strokes) {
            if (stroke.size == 1) {
                val p = stroke[0]
                paint.strokeWidth = sw(p.w)
                canvas.drawPoint(p.x * scale + offX, p.y * scale + offY, paint)
                continue
            }
            for (i in 1 until stroke.size) {
                val a = stroke[i - 1]; val b = stroke[i]
                paint.strokeWidth = sw(b.w)
                canvas.drawLine(a.x * scale + offX, a.y * scale + offY, b.x * scale + offX, b.y * scale + offY, paint)
            }
        }
    }

    private const val MAX_SCALE = 6f
}
