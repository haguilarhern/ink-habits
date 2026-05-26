package com.boox.atomic.habits.boox

import com.onyx.android.sdk.pen.data.TouchPoint

enum class StylusGesture {
    CHECKMARK,
    STRIKETHROUGH,
    CIRCLE,
    NONE
}

object StylusGestureRecognizer {
    fun recognize(points: List<TouchPoint>): StylusGesture {
        if (points.size < 5) return StylusGesture.NONE

        val first = points.first()
        val last = points.last()
        val dx = last.x - first.x
        val dy = last.y - first.y

        // CHECKMARK: starts down-right, turns up-right
        if (dx > 30 && dy > -10 && dy < 40) {
            val midY = points[points.size / 2].y
            if (midY > first.y + 10 && last.y < midY) {
                return StylusGesture.CHECKMARK
            }
        }

        // STRIKETHROUGH: mostly horizontal
        if (Math.abs(dy) < 15 && dx > 50) {
            return StylusGesture.STRIKETHROUGH
        }

        // CIRCLE: bounding box approx square
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        val width = maxX - minX
        val height = maxY - minY
        if (width in 20f..80f && height in 20f..80f && Math.abs(width - height) < 20) {
            return StylusGesture.CIRCLE
        }

        return StylusGesture.NONE
    }
}