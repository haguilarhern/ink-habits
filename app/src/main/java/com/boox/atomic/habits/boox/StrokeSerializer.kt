package com.boox.atomic.habits.boox

import org.json.JSONArray

object StrokeSerializer {

    /**
     * Serialize a list of touch points (x, y, pressure) to a compact JSON string.
     * Format: [[x1,y1,w1],[x2,y2,w2],...]
     * where w = stroke width derived from pressure (2f + pressure * 10f).
     */
    fun serialize(points: List<Triple<Float, Float, Float>>): String {
        val jsonArray = JSONArray()
        for ((x, y, pressure) in points) {
            val width = 2f + pressure * 10f
            val pointArray = JSONArray()
            pointArray.put(x.toDouble())
            pointArray.put(y.toDouble())
            pointArray.put(width.toDouble())
            jsonArray.put(pointArray)
        }
        return jsonArray.toString()
    }

    /**
     * Deserialize a JSON string back to a list of touch points.
     * Returns triple of (x, y, width).
     */
    fun deserialize(data: String): List<Triple<Float, Float, Float>> {
        if (data.isBlank()) return emptyList()
        val jsonArray = JSONArray(data)
        val result = mutableListOf<Triple<Float, Float, Float>>()
        for (i in 0 until jsonArray.length()) {
            val pointArray = jsonArray.getJSONArray(i)
            val x = pointArray.getDouble(0).toFloat()
            val y = pointArray.getDouble(1).toFloat()
            val w = pointArray.getDouble(2).toFloat()
            result.add(Triple(x, y, w))
        }
        return result
    }
}
