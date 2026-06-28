package com.inkhabits.util

import android.content.Context

/**
 * The app's single accent colour, user-selectable and persisted. Everything that draws
 * the accent (completions, streaks, progress, active tab, primary actions) reads
 * [color]; the rest of the UI stays monochrome. Changing it calls Activity.recreate()
 * so every screen re-reads it.
 *
 * The palette is intentionally limited to deep, saturated tones that render as real
 * colour on the Boox Kaleido 3 panel (light/pastel hues desaturate into muddy grays).
 */
object Accent {

    private const val PREFS = "ui"
    private const val KEY = "accent_color"

    /** Deep ink-blue — a nod to fountain-pen ink. The default. */
    const val DEFAULT = 0xFF2A4A8C.toInt()

    /** name -> ARGB. Curated for Kaleido (deep + saturated). */
    val palette: List<Pair<String, Int>> = listOf(
        "Ink blue" to 0xFF2A4A8C.toInt(),
        "Turquoise" to 0xFF0A7D6A.toInt(),
        "Emerald" to 0xFF0F7A45.toInt(),
        "Cobalt" to 0xFF1E57C0.toInt(),
        "Violet" to 0xFF5B3CC4.toInt(),
        "Plum" to 0xFF7A3E8C.toInt(),
        "Crimson" to 0xFF9E2A3C.toInt(),
        "Terracotta" to 0xFF9A4A1E.toInt(),
        "Graphite" to 0xFF2B2B2E.toInt(),
    )

    fun color(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY, DEFAULT)

    fun set(context: Context, argb: Int) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY, argb).apply()
}
