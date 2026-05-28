package com.inkhabits.ui.writing

import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import com.inkhabits.eink.EInkActivity

/** Implemented by hosts that contain InputFields so a field can open the writing pad. */
interface WritingPadLauncher {
    fun openWritingPad(existing: String, title: String, onResult: (String) -> Unit)
}

/**
 * Base activity that owns the writing-pad ActivityResult plumbing. Any contained
 * [com.inkhabits.ui.widget.InputField] in write mode calls [openWritingPad].
 */
abstract class WritingHostActivity : EInkActivity(), WritingPadLauncher {

    private var pending: ((String) -> Unit)? = null

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data?.getStringExtra(WritingPadActivity.EXTRA_RESULT) ?: ""
            pending?.invoke(data)
        }
        pending = null
    }

    override fun openWritingPad(existing: String, title: String, onResult: (String) -> Unit) {
        pending = onResult
        val intent = Intent(this, WritingPadActivity::class.java)
            .putExtra(WritingPadActivity.EXTRA_STROKES, existing)
            .putExtra(WritingPadActivity.EXTRA_TITLE, title)
        launcher.launch(intent)
    }
}
