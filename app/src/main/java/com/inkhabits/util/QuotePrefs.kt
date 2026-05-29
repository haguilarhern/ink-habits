package com.inkhabits.util

import android.content.Context

/**
 * A user-set custom quote shown on the dashboard, instead of the rotating daily one.
 * Handwritten quotes are transcribed (OCR) so they can be shown as clean typed text
 * by default, with the original handwriting available on request.
 */
object QuotePrefs {
    private const val PREFS = "quote"
    private const val KEY_TEXT = "custom_text"
    private const val KEY_STROKES = "custom_strokes"
    private const val KEY_PREFER_HW = "prefer_handwritten"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun text(c: Context): String = prefs(c).getString(KEY_TEXT, "").orEmpty()
    fun strokes(c: Context): String = prefs(c).getString(KEY_STROKES, "").orEmpty()
    fun preferHandwritten(c: Context): Boolean = prefs(c).getBoolean(KEY_PREFER_HW, false)

    fun hasCustom(c: Context): Boolean =
        text(c).isNotBlank() || StrokeRenderer.hasInk(strokes(c))

    fun save(c: Context, text: String, strokes: String, preferHandwritten: Boolean) {
        prefs(c).edit()
            .putString(KEY_TEXT, text)
            .putString(KEY_STROKES, strokes)
            .putBoolean(KEY_PREFER_HW, preferHandwritten)
            .apply()
    }

    fun setPreferHandwritten(c: Context, value: Boolean) {
        prefs(c).edit().putBoolean(KEY_PREFER_HW, value).apply()
    }

    fun clear(c: Context) {
        prefs(c).edit().remove(KEY_TEXT).remove(KEY_STROKES).remove(KEY_PREFER_HW).apply()
    }
}
