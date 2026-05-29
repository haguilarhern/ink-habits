package com.inkhabits.ui.quote

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.inkhabits.databinding.ActivityQuoteEditBinding
import com.inkhabits.ui.writing.WritingHostActivity
import com.inkhabits.util.InkRecognizer
import com.inkhabits.util.QuotePrefs
import com.inkhabits.util.StrokeRenderer
import kotlinx.coroutines.launch

/** Editor for the user's custom dashboard quote — typed or handwritten (auto-transcribed). */
class QuoteEditActivity : WritingHostActivity() {

    private lateinit var binding: ActivityQuoteEditBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuoteEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.input.setHint("Your quote…")
        binding.input.prefill(QuotePrefs.text(this), QuotePrefs.strokes(this))

        binding.backButton.setOnClickListener { finish() }

        binding.useDaily.setOnClickListener {
            QuotePrefs.clear(this)
            com.inkhabits.widget.WidgetCommon.updateAll(this)
            finish()
        }

        binding.saveButton.setOnClickListener { save() }
    }

    private fun save() {
        if (!binding.input.hasContent()) {
            QuotePrefs.clear(this)
            com.inkhabits.widget.WidgetCommon.updateAll(this)
            finish()
            return
        }
        val typed = binding.input.getText()
        val strokes = binding.input.getStrokes()
        binding.saveButton.isEnabled = false
        binding.saveButton.text = "Saving…"
        lifecycleScope.launch {
            // Transcribe handwriting so the home screen can show clean typed text by default.
            val text = if (typed.isNotBlank()) typed
                else if (StrokeRenderer.hasInk(strokes)) InkRecognizer.recognize(strokes).orEmpty()
                else ""
            // Default to typed view when we have text; fall back to handwriting otherwise.
            QuotePrefs.save(this@QuoteEditActivity, text, strokes, preferHandwritten = text.isBlank())
            com.inkhabits.widget.WidgetCommon.updateAll(this@QuoteEditActivity)
            finish()
        }
    }
}
