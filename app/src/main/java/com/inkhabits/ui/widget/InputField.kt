package com.inkhabits.ui.widget

import android.content.Context
import android.graphics.Color
import android.text.InputType
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout

/**
 * Hybrid text/handwriting input. TYPE mode is a plain EditText. WRITE mode is an
 * inline [InlineInkView] you write directly into with the pen (Onyx raw drawing,
 * zero-lag). Only one inline writer is active at a time across the screen.
 */
class InputField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    enum class Mode { TYPE, INK }

    private val clearChip: Button
    private val toggleType: Button
    private val toggleInk: Button
    private val editText: EditText
    private val inkBox: FrameLayout
    private val inkView: InlineInkView

    private var mode = Mode.TYPE

    init {
        orientation = VERTICAL

        val toggleRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.END
        }
        clearChip = chip("Clear").apply { setBackgroundColor(IDLE); setTextColor(ACCENT) }
        toggleType = chip("Type")
        toggleInk = chip("Write")
        toggleRow.addView(clearChip)
        toggleRow.addView(toggleType)
        toggleRow.addView(toggleInk)
        addView(toggleRow)

        editText = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setTextColor(Color.BLACK)
            setHintTextColor(Color.parseColor("#9A9A9A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            maxLines = 1
        }
        addView(editText, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        // Inline writing box with a thin border.
        inkBox = FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor("#C9C5BA")) // border color (acts as 1px frame)
            setPadding(2, 2, 2, 2)
        }
        inkView = InlineInkView(context)
        inkBox.addView(inkView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, dp(120)
        ))
        addView(inkBox, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        clearChip.setOnClickListener { inkView.clear() }
        toggleType.setOnClickListener { setMode(Mode.TYPE) }
        toggleInk.setOnClickListener { setMode(Mode.INK) }
        setMode(Mode.TYPE)
    }

    private fun chip(label: String): Button = Button(context).apply {
        text = label
        isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        minWidth = 0
        minimumWidth = 0
        setPadding(dp(14), dp(2), dp(14), dp(2))
        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        lp.marginStart = dp(6)
        layoutParams = lp
    }

    fun setHint(hint: String) {
        editText.hint = hint
    }

    fun setMode(newMode: Mode) {
        mode = newMode
        val type = newMode == Mode.TYPE
        editText.visibility = if (type) VISIBLE else GONE
        inkBox.visibility = if (type) GONE else VISIBLE
        toggleType.setBackgroundColor(if (type) ACCENT else IDLE)
        toggleType.setTextColor(if (type) Color.WHITE else Color.BLACK)
        toggleInk.setBackgroundColor(if (!type) ACCENT else IDLE)
        toggleInk.setTextColor(if (!type) Color.WHITE else Color.BLACK)
        clearChip.visibility = if (type) GONE else VISIBLE
        if (type) inkView.deactivate() else inkView.activate()
    }

    fun prefill(text: String, strokes: String) {
        if (strokes.isNotEmpty()) {
            setMode(Mode.INK)
            inkView.setStrokeData(strokes)
        } else if (text.isNotEmpty()) {
            setMode(Mode.TYPE)
            editText.setText(text)
        }
    }

    fun clear() {
        editText.setText("")
        inkView.clear()
        setMode(Mode.TYPE)
    }

    fun getText(): String = if (mode == Mode.TYPE) editText.text.toString().trim() else ""

    fun getStrokes(): String = if (mode == Mode.INK && inkView.hasStrokes()) inkView.getStrokeData() else ""

    fun hasContent(): Boolean =
        if (mode == Mode.TYPE) editText.text.toString().isNotBlank() else inkView.hasStrokes()

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    companion object {
        private val ACCENT = Color.parseColor("#8C1D1D")
        private val IDLE = Color.parseColor("#E8E8E8")
    }
}
