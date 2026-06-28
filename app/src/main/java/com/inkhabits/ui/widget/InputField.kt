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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.inkhabits.util.StrokeRenderer

/**
 * Hybrid text/handwriting input.
 *
 * TYPE mode is a plain EditText. WRITE mode is a tappable box that shows a preview of
 * the handwritten ink and opens the full-screen writing pad to create/edit it — the
 * same stable, smooth pattern the to-do list uses. There is NO inline drawing surface
 * (those churn and freeze the Onyx engine in scrolling forms). The host wires
 * [onRequestWrite] to its writing-pad launcher.
 */
class InputField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    enum class Mode { TYPE, INK }

    /** Host hook: open the writing pad with [existing] strokes; deliver the result to [onResult]. */
    var onRequestWrite: ((existing: String, onResult: (String) -> Unit) -> Unit)? = null

    private val toggleRow: LinearLayout
    private val clearChip: Button
    private val toggleType: Button
    private val toggleInk: Button
    private val editText: EditText
    private val inkBox: FrameLayout
    private val inkPreview: ImageView
    private val inkHint: TextView

    private var mode = Mode.INK
    private var typeOnly = false
    private var strokes = ""

    init {
        orientation = VERTICAL

        toggleRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.END
        }
        clearChip = chip("Clear").apply { styleChip(this, false); setTextColor(ACCENT) }
        toggleType = chip("Type")
        toggleInk = chip("Write")
        toggleRow.addView(clearChip)
        toggleRow.addView(toggleType)
        toggleRow.addView(toggleInk)
        addView(toggleRow)

        editText = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setTextColor(Color.BLACK)
            setHintTextColor(Color.parseColor("#9A9AA0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            maxLines = 1
        }
        addView(editText, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        // Tappable write box: shows the ink preview (or a hint) and opens the pad.
        inkBox = FrameLayout(context).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(Color.WHITE)
                setStroke(dp(1).coerceAtLeast(1), Color.parseColor("#D9D9DE"))
            }
            isClickable = true
            setOnClickListener { requestWrite() }
        }
        inkPreview = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = GONE
        }
        inkHint = TextView(context).apply {
            text = "Tap to write with your pen"
            setTextColor(Color.parseColor("#9A9AA0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            gravity = Gravity.CENTER
        }
        inkBox.addView(inkPreview, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, dp(110)))
        inkBox.addView(inkHint, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, dp(110), Gravity.CENTER))
        addView(inkBox, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        clearChip.setOnClickListener { strokes = ""; editText.setText(""); renderPreview() }
        toggleType.setOnClickListener { setMode(Mode.TYPE) }
        toggleInk.setOnClickListener { setMode(Mode.INK) }
        // e-ink first: default to handwriting.
        setMode(Mode.INK)
    }

    private fun requestWrite() {
        val handler = onRequestWrite ?: return
        handler(strokes) { result ->
            strokes = result
            renderPreview()
        }
    }

    private fun renderPreview() {
        val has = StrokeRenderer.hasInk(strokes)
        if (has) {
            val w = inkBox.width.takeIf { it > 0 } ?: dp(300)
            inkPreview.setImageBitmap(
                StrokeRenderer.renderToBitmap(strokes, w, dp(110), maxScale = 1f, centerHorizontal = true))
            inkPreview.visibility = VISIBLE
            inkHint.visibility = GONE
        } else {
            inkPreview.visibility = GONE
            inkHint.visibility = VISIBLE
        }
    }

    private fun chip(label: String): Button = Button(context).apply {
        text = label
        isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
        setPadding(dp(16), dp(6), dp(16), dp(6))
        elevation = 0f
        stateListAnimator = null
        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        lp.marginStart = dp(6)
        layoutParams = lp
    }

    private fun styleChip(b: Button, selected: Boolean) {
        b.background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            if (selected) setColor(ACCENT)
            else { setColor(Color.WHITE); setStroke(dp(1).coerceAtLeast(1), Color.parseColor("#D9D9DE")) }
        }
    }

    fun setHint(hint: String) {
        editText.hint = hint
        inkHint.text = "Tap to write — ${hint.removeSuffix("…")}"
    }

    /** Force plain-text entry and hide the Type/Write toggles. */
    fun setTypeOnly() {
        typeOnly = true
        toggleRow.visibility = GONE
        setMode(Mode.TYPE)
    }

    /** No-op kept for call sites; WRITE no longer holds a live surface to focus. */
    fun focusInk() { if (!typeOnly) setMode(Mode.INK) }

    fun setMode(newMode: Mode) {
        mode = newMode
        val type = newMode == Mode.TYPE
        editText.visibility = if (type) VISIBLE else GONE
        inkBox.visibility = if (type) GONE else VISIBLE
        styleChip(toggleType, type)
        toggleType.setTextColor(if (type) Color.WHITE else Color.parseColor("#0B0B0C"))
        styleChip(toggleInk, !type)
        toggleInk.setTextColor(if (!type) Color.WHITE else Color.parseColor("#0B0B0C"))
        clearChip.visibility = if (type) GONE else VISIBLE
        if (!type) renderPreview()
    }

    fun prefill(text: String, strokes: String) {
        if (strokes.isNotEmpty()) {
            this.strokes = strokes
            setMode(Mode.INK)
            inkBox.post { renderPreview() }
        } else if (text.isNotEmpty()) {
            setMode(Mode.TYPE)
            editText.setText(text)
        }
    }

    fun clear() {
        editText.setText("")
        strokes = ""
        setMode(Mode.INK)
        renderPreview()
    }

    /** Reset content for the next entry (no surface teardown needed). */
    fun prepareForNext() {
        editText.setText("")
        strokes = ""
        renderPreview()
    }

    fun getText(): String = if (mode == Mode.TYPE) editText.text.toString().trim() else ""

    fun getStrokes(): String = if (mode == Mode.INK && StrokeRenderer.hasInk(strokes)) strokes else ""

    fun hasContent(): Boolean =
        if (mode == Mode.TYPE) editText.text.toString().isNotBlank() else StrokeRenderer.hasInk(strokes)

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    companion object {
        private val ACCENT = Color.parseColor("#0B0B0C")
    }
}
