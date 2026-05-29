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

    private val toggleRow: LinearLayout
    private val clearChip: Button
    private val toggleType: Button
    private val toggleInk: Button
    private val editText: EditText
    private val inkBox: FrameLayout
    private val inkView: InlineInkView
    private val inkPreview: android.widget.ImageView
    private val lockBtn: android.widget.ImageView

    private var mode = Mode.TYPE
    private var typeOnly = false

    init {
        orientation = VERTICAL

        toggleRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.END
        }
        clearChip = chip("Clear").apply { styleChip(this, selected = false); setTextColor(ACCENT) }
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

        // Inline writing box with a thin hairline frame. (SurfaceView can't clip to
        // rounded corners, so the frame stays square — clean 1px rule all around.)
        inkBox = FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor("#CFCBC0")) // border color (acts as 1px frame)
            val f = dp(1).coerceAtLeast(1)
            setPadding(f, f, f, f)
        }
        inkView = InlineInkView(context)
        inkBox.addView(inkView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, dp(120)
        ))

        // Static preview shown when the box is "locked" (released). The live SurfaceView
        // doesn't repaint reliably on e-ink once raw-drawing closes, so we display the
        // captured ink as a plain image instead — text never disappears.
        inkPreview = android.widget.ImageView(context).apply {
            scaleType = android.widget.ImageView.ScaleType.FIT_XY
            setBackgroundColor(Color.WHITE)
            visibility = GONE
            isClickable = true
            setOnClickListener { inkView.activate() } // tap to unlock and keep writing
        }
        inkBox.addView(inkPreview, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, dp(120)))

        // Lock / unlock button in the reserved corner. Open lock = editable (writing);
        // closed lock = locked (ink preserved, pen released). Tapping toggles.
        lockBtn = android.widget.ImageView(context).apply {
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(ACCENT)
            }
            setColorFilter(Color.WHITE)
            isClickable = true
            setOnClickListener { if (inkView.isActive()) inkView.deactivate() else inkView.activate() }
        }
        val lockLp = FrameLayout.LayoutParams(dp(40), dp(40), Gravity.BOTTOM or Gravity.END)
        lockLp.setMargins(0, 0, dp(7), dp(7))
        inkBox.addView(lockBtn, lockLp)

        addView(inkBox, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        // Surface ↔ preview swap + highlight follow the active/locked state.
        inkView.onActiveChanged = { applyInkState() }

        clearChip.setOnClickListener { inkView.clear(); applyInkState() }
        toggleType.setOnClickListener { setMode(Mode.TYPE) }
        toggleInk.setOnClickListener { setMode(Mode.INK); inkView.activate() }
        // e-ink first: default to handwriting. Don't auto-activate the pen on
        // construction (so a later field can't steal focus) — tapping the box claims it.
        setMode(Mode.INK)
        applyInkState()
    }

    private fun chip(label: String): Button = Button(context).apply {
        text = label
        isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        minWidth = 0
        minimumWidth = 0
        minHeight = 0
        minimumHeight = 0
        setPadding(dp(16), dp(6), dp(16), dp(6))
        elevation = 0f
        stateListAnimator = null
        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        lp.marginStart = dp(6)
        layoutParams = lp
    }

    /** Rounded pill background: accent fill when selected, hairline outline when idle. */
    private fun styleChip(b: Button, selected: Boolean) {
        b.background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            if (selected) {
                setColor(ACCENT)
            } else {
                setColor(Color.WHITE)
                setStroke(dp(1).coerceAtLeast(1), Color.parseColor("#CFCBC0"))
            }
        }
    }

    fun setHint(hint: String) {
        editText.hint = hint
    }

    /**
     * Force plain-text entry and hide the Type/Write toggles. Used for short cues
     * (e.g. the anchor) that we display as text — avoids a second raw-drawing
     * surface on screen, which is both cleaner and far more stable.
     */
    fun setTypeOnly() {
        typeOnly = true
        toggleRow.visibility = GONE
        setMode(Mode.TYPE)
    }

    /**
     * Reconcile the box to its current state:
     *  • active        → live surface, accent border, open (accent) lock.
     *  • locked w/ ink → static preview, gray border, closed (gray) lock.
     *  • empty + idle  → live surface ready to write, gray border, open lock.
     */
    private fun applyInkState() {
        val active = inkView.isActive()
        val hasInk = inkView.hasStrokes()
        // IMPORTANT: never toggle the SurfaceView's visibility — doing so destroys and
        // recreates the Onyx drawing surface, and that churn hangs the engine. The
        // surface stays alive; the locked preview is just an opaque overlay on top.
        when {
            active -> {
                inkPreview.visibility = GONE
                inkBox.setBackgroundColor(ACCENT)
                setLock(open = true, accent = true)
            }
            hasInk -> {
                // Reposition the ink into the box (always visible regardless of where it
                // was captured) at natural size (maxScale 1x), centered, on white.
                val tw = inkView.width.takeIf { it > 0 } ?: inkBox.width.takeIf { it > 0 } ?: dp(280)
                val th = inkView.height.takeIf { it > 0 } ?: dp(120)
                inkPreview.setImageBitmap(
                    com.inkhabits.util.StrokeRenderer.renderToBitmap(
                        inkView.getStrokeData(), tw, th, maxScale = 1f, centerHorizontal = true))
                inkPreview.visibility = VISIBLE
                inkBox.setBackgroundColor(IDLE_BORDER)
                setLock(open = false, accent = false)
                inkBox.post { try { com.inkhabits.eink.EInkUtils.cleanRefresh(inkBox) } catch (_: Throwable) {} }
            }
            else -> {
                inkPreview.visibility = GONE
                inkBox.setBackgroundColor(IDLE_BORDER)
                setLock(open = true, accent = false)
            }
        }
    }

    private fun setLock(open: Boolean, accent: Boolean) {
        lockBtn.setImageResource(
            if (open) com.inkhabits.R.drawable.ic_lock_open else com.inkhabits.R.drawable.ic_lock)
        (lockBtn.background as? android.graphics.drawable.GradientDrawable)
            ?.setColor(if (accent) ACCENT else DONE_IDLE)
    }

    /** Switch to handwriting and claim the pen now (used to move focus to this field). */
    fun focusInk() {
        if (typeOnly) return
        setMode(Mode.INK)
        inkView.activate()
    }

    fun setMode(newMode: Mode) {
        mode = newMode
        val type = newMode == Mode.TYPE
        editText.visibility = if (type) VISIBLE else GONE
        inkBox.visibility = if (type) GONE else VISIBLE
        styleChip(toggleType, type)
        toggleType.setTextColor(if (type) Color.WHITE else Color.parseColor("#1A1A1A"))
        styleChip(toggleInk, !type)
        toggleInk.setTextColor(if (!type) Color.WHITE else Color.parseColor("#1A1A1A"))
        clearChip.visibility = if (type) GONE else VISIBLE
        // Releasing on TYPE; activation in INK is on-demand (tap or the Write button).
        if (type) inkView.deactivate() else applyInkState()
    }

    fun prefill(text: String, strokes: String) {
        if (strokes.isNotEmpty()) {
            setMode(Mode.INK)
            post { inkView.setStrokeData(strokes); applyInkState() }
        } else if (text.isNotEmpty()) {
            setMode(Mode.TYPE)
            editText.setText(text)
        }
    }

    fun clear() {
        editText.setText("")
        inkView.clear()
        setMode(Mode.INK)
        applyInkState()
    }

    /**
     * Reset content for the next entry WITHOUT changing mode or hiding the ink
     * surface. Releases the pen first so raw-drawing isn't torn down mid-stroke —
     * destroying/hiding the surface while it's active is what froze the screen.
     */
    fun prepareForNext() {
        inkView.deactivate()
        editText.setText("")
        inkView.clear()
        applyInkState()
    }

    fun getText(): String = if (mode == Mode.TYPE) editText.text.toString().trim() else ""

    fun getStrokes(): String = if (mode == Mode.INK && inkView.hasStrokes()) inkView.getStrokeData() else ""

    fun hasContent(): Boolean =
        if (mode == Mode.TYPE) editText.text.toString().isNotBlank() else inkView.hasStrokes()

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    companion object {
        private val ACCENT = Color.parseColor("#8C1D1D")
        private val IDLE_BORDER = Color.parseColor("#CFCBC0")
        private val DONE_IDLE = Color.parseColor("#8E8A7F")
    }
}
