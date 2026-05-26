package com.boox.atomic.habits.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import com.boox.atomic.habits.boox.StrokeRenderer

/**
 * A custom compound view that renders a single handwritten todo item.
 *
 * Layout (horizontal LinearLayout):
 * - [StylusCheckableView] checkbox on the left (40dp)
 * - [HandwritingFieldView] (read-only, confirmed) in the center showing handwritten strokes
 *
 * When checked, a strikethrough line is drawn over the handwriting area via
 * [StrokeRenderer.drawStrikethrough].
 */
class HandwritingTodoWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    // ── Children ────────────────────────────────────────────────────────────

    private val checkbox: StylusCheckableView
    private val handwritingField: HandwritingFieldView

    // ── State ───────────────────────────────────────────────────────────────

    /** Serialised stroke data, cached so we can pass it to the strikethrough renderer. */
    private var strokeData: String = ""

    /** Paint used for the strikethrough line drawn in [dispatchDraw]. */
    private val strikethroughPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 3f
        style = Paint.Style.FILL
        isAntiAlias = false
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Initialisation
    // ═══════════════════════════════════════════════════════════════════════════

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        // ── Create checkbox ────────────────────────────────────────────
        checkbox = StylusCheckableView(context).apply {
            layoutParams = LayoutParams(
                dpToPx(40),
                dpToPx(40)
            ).apply {
                marginEnd = dpToPx(8)
            }
            setOnCheckedChangeListener { checked ->
                invalidate()
            }
        }

        // ── Create read-only handwriting field ─────────────────────────
        handwritingField = HandwritingFieldView(context).apply {
            layoutParams = LayoutParams(
                0,
                LayoutParams.WRAP_CONTENT,
                1f // weight = 1, fills remaining space
            )
            setConfirmed(true)
        }

        addView(checkbox)
        addView(handwritingField)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Load handwritten stroke data into the handwriting field.
     */
    fun setStrokeData(data: String) {
        strokeData = data
        handwritingField.setStrokeData(data)
        invalidate()
    }

    /**
     * Set the checked (completed) state.
     * When checked, the strikethrough overlay is drawn over the handwriting.
     */
    fun setChecked(checked: Boolean) {
        checkbox.isChecked = checked
        invalidate()
    }

    /**
     * Return whether this todo is checked.
     */
    fun isChecked(): Boolean = checkbox.isChecked

    /**
     * Register a callback invoked when the checkbox state changes.
     */
    fun setOnCheckedChangeListener(listener: (Boolean) -> Unit) {
        checkbox.setOnCheckedChangeListener(listener)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Strikethrough overlay
    // ═══════════════════════════════════════════════════════════════════════════

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        // When checked, draw a strikethrough line across the handwriting area
        if (isChecked() && strokeData.isNotBlank()) {
            val hf = handwritingField

            // Compute the handwriting field's position in this LinearLayout's
            // coordinate space
            val offsetX = hf.left.toFloat()
            val offsetY = hf.top.toFloat()

            // Measure the stroke bounds and draw the line centred
            val bounds = StrokeRenderer.measureStrokes(strokeData)
            if (!bounds.isEmpty) {
                val centerY = bounds.centerY() + offsetY
                val left = bounds.left + offsetX
                val right = bounds.right + offsetX
                val padding = 6f

                canvas.drawLine(
                    left - padding,
                    centerY,
                    right + padding,
                    centerY,
                    strikethroughPaint
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
