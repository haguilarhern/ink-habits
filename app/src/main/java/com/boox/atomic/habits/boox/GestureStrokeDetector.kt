package com.boox.atomic.habits.boox

import android.view.MotionEvent

/**
 * Detects stylus gestures from raw MotionEvent data (no Onyx SDK dependency).
 *
 * Captures touch points during ACTION_MOVE and analyzes the stroke
 * on ACTION_UP to determine if a gesture was drawn.
 */
class GestureStrokeDetector {

    private val points = mutableListOf<Pair<Float, Float>>()

    /** Call from onTouchEvent. Returns true if the event was consumed. */
    fun handleMotionEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                points.clear()
                points.add(Pair(event.x, event.y))
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                points.add(Pair(event.x, event.y))
                return true
            }
            MotionEvent.ACTION_UP -> {
                points.add(Pair(event.x, event.y))
                return true
            }
        }
        return false
    }

    /**
     * After ACTION_UP, analyze the captured points to detect a strikethrough.
     * A strikethrough = mostly horizontal line across the view.
     */
    fun isStrikethrough(): Boolean {
        if (points.size < 5) return false

        val first = points.first()
        val last = points.last()
        val dx = last.first - first.first
        val dy = last.second - first.second

        // STRIKETHROUGH: mostly horizontal, significant length
        // Allow slightly more vertical drift on e-ink stylus
        return Math.abs(dy) < 25f && dx > 60f
    }

    fun getPoints(): List<Pair<Float, Float>> = points.toList()

    fun clear() {
        points.clear()
    }
}