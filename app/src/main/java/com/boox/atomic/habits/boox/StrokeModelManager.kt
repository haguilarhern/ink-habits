package com.boox.atomic.habits.boox

/**
 * Represents a single stroke record holding touch points and metadata.
 * Wraps the Jetpack Ink [androidx.ink] data model for stroke persistence.
 */
data class StrokeRecord(
    val id: Long,
    val touchPoints: List<Pair<Float, Float>>,
    val timestamp: Long = System.currentTimeMillis(),
    val brushType: String = "pen",
    val color: Int = 0xFF000000.toInt(),
    val strokeWidth: Float = 3.0f
)

/**
 * Manager for collecting, storing, and retrieving ink strokes.
 * Integrates with Jetpack Ink API (androidx.ink) primitives.
 *
 * In a full implementation, this would use:
 * - androidx.ink.geometry.ImmutableStroke
 * - androidx.ink.authoring.StrokeInput
 * - androidx.ink.brush.BrushProvider
 */
class StrokeModelManager {

    private val strokes = mutableListOf<StrokeRecord>()
    private var nextId: Long = 1L

    /**
     * Add a stroke from a list of coordinate pairs.
     * In production, this would accept [androidx.ink.authoring.TouchPointList]
     * from a ScribbleEngine or stylus input source.
     */
    fun addStroke(points: List<Pair<Float, Float>>, brushType: String = "pen"): StrokeRecord {
        val stroke = StrokeRecord(
            id = nextId++,
            touchPoints = points.toList(),
            brushType = brushType
        )
        strokes.add(stroke)
        return stroke
    }

    /**
     * Add a pre-constructed [StrokeRecord] to the collection.
     */
    fun addStroke(stroke: StrokeRecord): StrokeRecord {
        strokes.add(stroke)
        if (stroke.id >= nextId) {
            nextId = stroke.id + 1
        }
        return stroke
    }

    /**
     * Returns an immutable copy of all stored strokes.
     */
    fun getStrokes(): List<StrokeRecord> {
        return strokes.toList()
    }

    /**
     * Remove all strokes from the model.
     */
    fun clearAll() {
        strokes.clear()
    }

    /**
     * Returns the current number of stored strokes.
     */
    fun getStrokeCount(): Int {
        return strokes.size
    }

    /**
     * Remove a specific stroke by its ID.
     */
    fun removeStroke(id: Long): Boolean {
        return strokes.removeAll { it.id == id }
    }

    /**
     * Encode a stroke's touch points to a compact string for persistence.
     */
    fun encodePoints(points: List<Pair<Float, Float>>): String {
        return points.joinToString(";") { (x, y) ->
            "${x.toInt()},${y.toInt()}"
        }
    }

    /**
     * Decode a compact string back into touch point pairs.
     */
    fun decodePoints(encoded: String): List<Pair<Float, Float>> {
        return encoded.split(";").mapNotNull { segment ->
            val parts = segment.split(",")
            if (parts.size == 2) {
                parts[0].toFloatOrNull()?.let { x ->
                    parts[1].toFloatOrNull()?.let { y ->
                        Pair(x, y)
                    }
                }
            } else null
        }
    }
}