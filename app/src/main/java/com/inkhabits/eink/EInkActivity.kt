package com.inkhabits.eink

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.onyx.android.sdk.api.device.epd.UpdateMode

/**
 * Base activity that configures Boox e-ink update behavior and clears
 * ghosting on resume. No-ops gracefully on non-Boox devices.
 */
open class EInkActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            EInkUtils.setWindowMode(this, UpdateMode.GU)
        } catch (_: Throwable) {
        }
    }

    override fun onResume() {
        super.onResume()
        window.decorView.postDelayed({ cleanRefresh(window.decorView) }, 80L)
    }

    protected fun cleanRefresh(view: View) {
        try {
            EInkUtils.cleanRefresh(view)
        } catch (_: Throwable) {
        }
    }
}
